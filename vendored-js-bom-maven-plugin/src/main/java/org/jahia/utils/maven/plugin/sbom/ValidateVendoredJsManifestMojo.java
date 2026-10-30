/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2026 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.utils.maven.plugin.sbom;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Validates that all manually packaged JavaScript files are represented in the
 * vendored-js.cdx.json manifest, either as a file component of a library or below one of its
 * {@value VendoredJsBom#SOURCE_FOLDER_PROPERTY} folders. Scans configured resource directories and fails if
 * unreferenced files are found (unless explicitly excluded). Only files with one of the {@code fileExtensions}
 * ({@code js} by default) are checked.
 *
 * @goal validate-vendored-js-manifest
 * @phase validate
 * @requiresProject true
 * @threadSafe true
 */
public class ValidateVendoredJsManifestMojo extends AbstractMojo {

    /**
     * The path to the vendored JavaScript manifest, a partial CycloneDX 1.4 JSON BOM.
     * Relative to project basedir.
     *
     * @parameter default-value="vendored-js.cdx.json"
     */
    private String manifestFile;

    /**
     * Comma-separated list of directory paths (relative to basedir) to scan for vendored files.
     * Example: src/main/resources/javascript,src/main/resources/css
     *
     * @parameter default-value="src/main/resources/javascript"
     */
    private String vendoredDirectories;

    /**
     * Comma-separated file extensions that must be declared in the manifest, e.g. {@code js,css,swf}. Also
     * decides which files a {@value VendoredJsBom#SOURCE_FOLDER_PROPERTY} folder covers; use the same value
     * for {@code generate-vendored-js-bom}, typically in the plugin-level configuration.
     *
     * @parameter default-value="js"
     */
    private String fileExtensions;

    /**
     * Comma-separated list of file patterns to exclude from validation
     * (e.g., min.js for minified files already covered by non-min versions).
     *
     * @parameter default-value=""
     */
    private String excludePatterns;

    /**
     * Skip validation if the manifest does not exist.
     *
     * @parameter default-value="true"
     */
    private boolean skipIfNoManifest;

    /**
     * Project base directory.
     *
     * @parameter expression="${project.basedir}"
     * @required
     */
    private File basedir;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File manifest = new File(basedir, manifestFile);
        if (!manifest.exists()) {
            handleMissingManifest(manifest);
            return;
        }

        try {
            VendoredJsBom parsed = VendoredJsBom.load(manifest);
            ManifestData manifestData = extractManifestData(parsed);

            getLog().info("Validating vendored " + fileExtensions + " files against manifest (" + manifestData.files.size() + " files and "
                    + manifestData.directories.size() + " directory references listed)");

            List<String> unreferenced = scanDirectoriesForUnreferencedFiles(manifestData);

            if (!unreferenced.isEmpty()) {
                reportUnreferencedFiles(unreferenced);
                throw new MojoFailureException(
                        "Unreferenced vendored files found. Update " + manifestFile + " to include all files.");
            }

            getLog().info("Vendored files manifest validation passed.");
        } catch (java.io.IOException e) {
            throw new MojoExecutionException("Failed to validate vendored JS manifest: " + e.getMessage(), e);
        }
    }

    private void handleMissingManifest(File manifest) throws MojoFailureException {
        if (skipIfNoManifest) {
            getLog().debug("No vendored JS manifest found at " + manifest.getAbsolutePath() + "; skipping validation.");
        } else {
            throw new MojoFailureException("Vendored JS manifest not found at " + manifest.getAbsolutePath());
        }
    }

    private ManifestData extractManifestData(VendoredJsBom parsed) {
        ManifestData data = new ManifestData();
        for (ObjectNode library : parsed.getLibraries()) {
            for (String filePath : VendoredJsBom.getFilePaths(library)) {
                data.files.add(normalizePath(filePath));
            }
            for (String folderPath : VendoredJsBom.getFolderPaths(library)) {
                String normalizedPath = normalizePath(folderPath);
                data.directories.add(normalizedPath.endsWith("/") ? normalizedPath : normalizedPath + "/");
            }
        }
        return data;
    }

    private List<String> scanDirectoriesForUnreferencedFiles(ManifestData manifestData) {
        List<String> unreferenced = new ArrayList<>();
        String[] directories = vendoredDirectories.split(",");
        List<String> extensions = VendoredJsBom.parseExtensions(fileExtensions);

        for (String dir : directories) {
            dir = dir.trim();
            File vendoredDir = new File(basedir, dir);
            if (vendoredDir.exists() && vendoredDir.isDirectory()) {
                scanForUnreferencedFiles(vendoredDir, vendoredDir, extensions, manifestData, unreferenced);
            }
        }

        return unreferenced;
    }

    private void reportUnreferencedFiles(List<String> unreferenced) {
        getLog().error("Found " + unreferenced.size() + " unreferenced vendored file(s):");
        for (String file : unreferenced) {
            getLog().error("  - " + file);
        }
    }

    private static class ManifestData {
        List<String> files = new ArrayList<>();
        List<String> directories = new ArrayList<>();
    }

    private void scanForUnreferencedFiles(File scanRoot, File dir, List<String> extensions, ManifestData manifestData,
            List<String> unreferenced) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanForUnreferencedFiles(scanRoot, file, extensions, manifestData, unreferenced);
            } else if (file.isFile() && VendoredJsBom.hasExtension(file.getName(), extensions)) {
                String basedirRelativePath = normalizePath(basedir.toPath().relativize(file.toPath()).toString());
                String scanRootRelativePath = normalizePath(scanRoot.toPath().relativize(file.toPath()).toString());

                if (!isExcluded(basedirRelativePath)
                        && !isExcluded(scanRootRelativePath)
                        && !isManifested(basedirRelativePath, scanRootRelativePath, manifestData.files, manifestData.directories)) {
                    unreferenced.add(file.getAbsolutePath());
                }
            }
        }
    }

    private boolean isManifested(String basedirRelativePath, String scanRootRelativePath,
            List<String> manifestedFiles, List<String> manifestedDirectories) {
        if (manifestedFiles.contains(basedirRelativePath) || manifestedFiles.contains(scanRootRelativePath)) {
            return true;
        }

        for (String directory : manifestedDirectories) {
            if (basedirRelativePath.startsWith(directory) || scanRootRelativePath.startsWith(directory)) {
                return true;
            }
        }

        return false;
    }

    private boolean isExcluded(String filePath) {
        if (excludePatterns == null || excludePatterns.trim().isEmpty()) {
            return false;
        }
        String normalizedFilePath = normalizePath(filePath);
        String[] patterns = excludePatterns.split(",");
        for (String pattern : patterns) {
            String normalizedPattern = normalizePath(pattern.trim());
            if (!normalizedPattern.isEmpty() && normalizedFilePath.contains(normalizedPattern)) {
                return true;
            }
        }
        return false;
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }

        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        // Case-insensitive matching on Windows only.
        String osName = System.getProperty("os.name");
        if (osName != null && osName.toLowerCase(Locale.ROOT).contains("win")) {
            normalized = normalized.toLowerCase(Locale.ROOT);
        }
        return normalized;
    }
}
