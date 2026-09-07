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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Validates that all manually packaged JavaScript files are represented in the
 * vendored-js.yaml manifest. Scans configured resource directories and fails if
 * unreferenced .js files are found (unless explicitly excluded).
 *
 * @goal validate-vendored-js-manifest
 * @phase validate
 * @requiresProject true
 */
public class ValidateVendoredJsManifestMojo extends AbstractMojo {

    /**
     * Comma-separated list of directory paths (relative to basedir) to scan
     * for vendored JavaScript files. Example: assets/src/main/resources/javascript,default/src/main/resources/javascript
     *
     * @parameter default-value="src/main/resources/javascript"
     */
    private String vendoredJsDirectories;

    /**
     * Comma-separated list of file patterns to exclude from validation
     * (e.g., min.js for minified files already covered by non-min versions).
     *
     * @parameter default-value=""
     */
    private String excludePatterns;

    /**
     * Skip validation if no vendored-js.yaml manifest exists.
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
        File manifest = new File(basedir, "vendored-js.yaml");
        if (!manifest.exists()) {
            handleMissingManifest(manifest);
            return;
        }

        try {
            VendoredJsManifest parsed = VendoredJsManifestParser.parse(manifest);
            ManifestData manifestData = extractManifestData(parsed);

            getLog().info("Validating vendored JavaScript files against manifest (" + manifestData.files.size() + " files and "
                    + manifestData.directories.size() + " directory references listed)");

            List<String> unreferenced = scanDirectoriesForUnreferencedFiles(manifestData);

            if (!unreferenced.isEmpty()) {
                reportUnreferencedFiles(unreferenced);
                throw new MojoFailureException(
                        "Unreferenced vendored JavaScript files found. Update vendored-js.yaml to include all files.");
            }

            getLog().info("Vendored JavaScript manifest validation passed.");
        } catch (java.io.IOException e) {
            throw new MojoExecutionException("Failed to validate vendored JS manifest: " + e.getMessage(), e);
        }
    }

    private void handleMissingManifest(File manifest) throws MojoFailureException {
        if (skipIfNoManifest) {
            getLog().debug("No vendored-js.yaml manifest found; skipping validation.");
        } else {
            throw new MojoFailureException("vendored-js.yaml manifest not found at " + manifest.getAbsolutePath());
        }
    }

    private ManifestData extractManifestData(VendoredJsManifest parsed) {
        ManifestData data = new ManifestData();
        for (VendoredJsComponent component : parsed.getComponents()) {
            if (component.getFiles() != null) {
                for (String rawPath : component.getFiles()) {
                    if (rawPath == null || rawPath.trim().isEmpty()) {
                        continue;
                    }
                    classifyManifestPath(rawPath, data);
                }
            }
        }
        return data;
    }

    private void classifyManifestPath(String rawPath, ManifestData data) {
        String normalizedPath = normalizePath(rawPath);
        File pathFile = new File(basedir, rawPath);
        boolean isDirectoryReference = normalizedPath.endsWith("/") || pathFile.isDirectory();

        if (isDirectoryReference) {
            data.directories.add(normalizedPath.endsWith("/") ? normalizedPath : normalizedPath + "/");
        } else {
            data.files.add(normalizedPath);
        }
    }

    private List<String> scanDirectoriesForUnreferencedFiles(ManifestData manifestData) {
        List<String> unreferenced = new ArrayList<>();
        String[] directories = vendoredJsDirectories.split(",");

        for (String dir : directories) {
            dir = dir.trim();
            File vendoredDir = new File(basedir, dir);
            if (vendoredDir.exists() && vendoredDir.isDirectory()) {
                scanForUnreferencedFiles(vendoredDir, vendoredDir, manifestData.files, manifestData.directories, unreferenced);
            }
        }

        return unreferenced;
    }

    private void reportUnreferencedFiles(List<String> unreferenced) {
        getLog().error("Found " + unreferenced.size() + " unreferenced vendored JavaScript file(s):");
        for (String file : unreferenced) {
            getLog().error("  - " + file);
        }
    }

    private static class ManifestData {
        List<String> files = new ArrayList<>();
        List<String> directories = new ArrayList<>();
    }

    private void scanForUnreferencedFiles(File scanRoot, File dir, List<String> manifestedFiles,
            List<String> manifestedDirectories, List<String> unreferenced) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanForUnreferencedFiles(scanRoot, file, manifestedFiles, manifestedDirectories, unreferenced);
            } else if (file.isFile() && file.getName().endsWith(".js")) {
                String basedirRelativePath = normalizePath(basedir.toPath().relativize(file.toPath()).toString());
                String scanRootRelativePath = normalizePath(scanRoot.toPath().relativize(file.toPath()).toString());

                if (!isExcluded(basedirRelativePath)
                        && !isExcluded(scanRootRelativePath)
                        && !isManifested(basedirRelativePath, scanRootRelativePath, manifestedFiles, manifestedDirectories)) {
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
