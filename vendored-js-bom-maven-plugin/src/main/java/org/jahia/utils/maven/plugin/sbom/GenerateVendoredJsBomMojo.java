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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Hex;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Generates a CycloneDX 1.4 Software Bill of Materials (SBOM) for manually packaged JavaScript libraries
 * (vendored JavaScript files), suitable for merging with Maven and Webpack-generated BOMs before uploading to
 * Dependency-Track.
 * <p>
 * The input is a hand-written, partial CycloneDX 1.4 BOM (see {@link VendoredJsBom}). This goal only enriches
 * it, every other field is kept as written:
 * <ul>
 * <li>each file component gets the SHA-256 of its file; a SHA-256 already written in the manifest is verified
 * and a mismatch fails the build</li>
 * <li>each {@value VendoredJsBom#SOURCE_FOLDER_PROPERTY} folder is expanded into one hashed file component per
 * {@code .js} file below it, the same files {@code validate-vendored-js-manifest} considers covered</li>
 * <li>a library backed by exactly one file also gets that file's SHA-256</li>
 * <li>missing {@code bom-ref}s, a new {@code serialNumber} and {@code metadata} (timestamp, tool, module) are
 * added</li>
 * </ul>
 *
 * @goal generate-vendored-js-bom
 * @phase process-resources
 * @requiresProject true
 * @threadSafe true
 */
public class GenerateVendoredJsBomMojo extends AbstractMojo {

    private static final String SHA256_ALGORITHM = "SHA-256";
    private static final String TOOL_VENDOR = "Jahia";
    private static final String TOOL_NAME = "vendored-js-bom-maven-plugin";
    private static final String COMPONENT_SOURCE_PROPERTY = "jahia:component-source";

    /**
     * The path to the vendored JavaScript manifest, a partial CycloneDX 1.4 JSON BOM.
     * Relative to project basedir.
     *
     * @parameter default-value="vendored-js.cdx.json"
     */
    private String manifestFile;

    /**
     * The output directory for the generated BOM file.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * The name of the output BOM file (without extension).
     *
     * @parameter default-value="vendored-js-bom.cdx"
     */
    private String outputFileName;

    /**
     * Project base directory.
     *
     * @parameter expression="${project.basedir}"
     * @required
     */
    private File basedir;

    /**
     * Skip execution if manifest file does not exist.
     *
     * @parameter default-value="true"
     */
    private boolean skipIfMissing;

    /**
     * @parameter default-value="${project.groupId}"
     * @readonly
     */
    private String projectGroupId;

    /**
     * @parameter default-value="${project.artifactId}"
     * @readonly
     */
    private String projectArtifactId;

    /**
     * @parameter default-value="${project.version}"
     * @readonly
     */
    private String projectVersion;

    /**
     * @parameter default-value="${plugin.version}"
     * @readonly
     */
    private String pluginVersion;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File manifest = new File(basedir, manifestFile);

        if (!manifest.exists()) {
            if (skipIfMissing) {
                getLog().info("Vendored JS manifest not found at " + manifest.getAbsolutePath() + "; skipping BOM generation.");
                return;
            } else {
                throw new MojoFailureException("Vendored JS manifest not found at " + manifest.getAbsolutePath());
            }
        }

        try {
            getLog().info("Loading vendored JavaScript manifest from " + manifest.getAbsolutePath());
            VendoredJsBom bom = VendoredJsBom.load(manifest);

            getLog().info("Generating CycloneDX " + VendoredJsBom.SPEC_VERSION + " SBOM from " + bom.getLibraries().size() + " component(s)");
            Path basePath = basedir.getCanonicalFile().toPath();
            for (ObjectNode library : bom.getLibraries()) {
                enrichLibrary(library, basePath);
            }
            enrichDocument(bom.getRoot());

            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            File outputFile = new File(outputDirectory, outputFileName + ".json");
            bom.write(outputFile);

            getLog().info("Vendored JS BOM written to " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate vendored JS BOM: " + e.getMessage(), e);
        }
    }

    private void enrichDocument(ObjectNode root) {
        root.put("serialNumber", "urn:uuid:" + UUID.randomUUID());
        if (!root.has("version")) {
            root.put("version", 1);
        }

        ObjectNode metadata = VendoredJsBom.getOrCreateObject(root, "metadata");
        metadata.put("timestamp", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        VendoredJsBom.getOrCreateArray(metadata, "tools").addObject()
                .put("vendor", TOOL_VENDOR)
                .put("name", TOOL_NAME)
                .put("version", pluginVersion);

        if (!metadata.has("component") && projectArtifactId != null) {
            ObjectNode subject = metadata.putObject("component");
            subject.put("type", "application");
            if (projectGroupId != null && projectVersion != null) {
                String purl = "pkg:maven/" + projectGroupId + "/" + projectArtifactId + "@" + projectVersion;
                subject.put("bom-ref", purl);
                subject.put("purl", purl);
            }
            subject.put("name", projectArtifactId);
            subject.put("version", projectVersion);
        }

        reorder(root, "bomFormat", "specVersion", "serialNumber", "version", "metadata", "components");
    }

    private void enrichLibrary(ObjectNode library, Path basePath) throws IOException {
        String name = library.path("name").asText();
        if (library.has("hashes")) {
            throw new IOException("Component " + name + " must not declare \"hashes\" itself: declare them on its file"
                    + " components; the library hash is computed when the library consists of a single file");
        }
        if (!library.hasNonNull("bom-ref")) {
            library.put("bom-ref", library.hasNonNull("purl") ? library.get("purl").asText()
                    : "vendored-js:" + name + (library.hasNonNull("version") ? "@" + library.get("version").asText() : ""));
        }
        String libraryRef = library.get("bom-ref").asText();

        // Canonical path -> file component describing it
        Map<Path, ObjectNode> files = new LinkedHashMap<>();
        for (ObjectNode fileComponent : VendoredJsBom.getFileComponents(library)) {
            String filePath = fileComponent.get("name").asText();
            Path file = resolve(filePath, basePath);
            if (!Files.isRegularFile(file)) {
                throw new IOException("File component " + filePath + " of " + name + " is not an existing file"
                        + (Files.isDirectory(file) ? "; declare folders with the " + VendoredJsBom.SOURCE_FOLDER_PROPERTY + " property" : ""));
            }
            if (files.put(file, fileComponent) != null) {
                throw new IOException("File " + filePath + " is listed more than once in " + name);
            }
            applyHash(fileComponent, file, filePath);
            if (!fileComponent.hasNonNull("bom-ref")) {
                fileComponent.put("bom-ref", libraryRef + "#" + relativize(basePath, file));
            }
        }

        for (String folderPath : VendoredJsBom.getFolderPaths(library)) {
            Path folder = resolve(folderPath, basePath);
            if (!Files.isDirectory(folder)) {
                throw new IOException("Folder " + folderPath + " of " + name + " is not an existing directory");
            }
            List<Path> jsFiles = listJsFiles(folder, basePath);
            if (jsFiles.isEmpty()) {
                getLog().warn("Manifest folder contains no .js files; nothing to hash: " + folderPath);
            }
            for (Path file : jsFiles) {
                if (!files.containsKey(file)) {
                    String relativePath = relativize(basePath, file);
                    ObjectNode fileComponent = VendoredJsBom.getOrCreateArray(library, "components").addObject();
                    fileComponent.put("type", VendoredJsBom.FILE_TYPE);
                    fileComponent.put("bom-ref", libraryRef + "#" + relativePath);
                    fileComponent.put("name", relativePath);
                    applyHash(fileComponent, file, relativePath);
                    files.put(file, fileComponent);
                }
            }
        }

        if (files.size() == 1) {
            library.set("hashes", files.values().iterator().next().get("hashes").deepCopy());
        }

        if (!VendoredJsBom.hasProperty(library, COMPONENT_SOURCE_PROPERTY)) {
            VendoredJsBom.getOrCreateArray(library, "properties").addObject()
                    .put("name", COMPONENT_SOURCE_PROPERTY)
                    .put("value", "vendored-javascript");
        }
    }

    /**
     * Sets the computed SHA-256 as the only hash of the component. A SHA-256 written in the manifest pins the file:
     * it must match.
     */
    private void applyHash(ObjectNode fileComponent, Path file, String displayPath) throws IOException {
        String sha256 = computeSha256(file.toFile());
        for (JsonNode hash : fileComponent.path("hashes")) {
            if (SHA256_ALGORITHM.equals(hash.path("alg").asText(null))
                    && !sha256.equals(hash.path("content").asText("").toLowerCase(Locale.ROOT))) {
                throw new IOException("SHA-256 of " + displayPath + " does not match the one declared in the manifest:"
                        + " the file changed since it was recorded (expected " + hash.path("content").asText()
                        + ", found " + sha256 + ")");
            }
        }
        ArrayNode hashes = fileComponent.putArray("hashes");
        hashes.addObject().put("alg", SHA256_ALGORITHM).put("content", sha256);
    }

    private Path resolve(String manifestPath, Path basePath) throws IOException {
        Path resolved = new File(basedir, manifestPath).getCanonicalFile().toPath();
        checkInsideBasedir(resolved, basePath, manifestPath);
        return resolved;
    }

    private static void checkInsideBasedir(Path resolvedPath, Path basePath, String manifestPath) throws IOException {
        if (!resolvedPath.startsWith(basePath)) {
            throw new IOException("Manifest path escapes project basedir: " + manifestPath);
        }
    }

    /**
     * Same file selection as {@code validate-vendored-js-manifest}: every {@code .js} file below the folder,
     * as canonical paths.
     */
    private static List<Path> listJsFiles(Path directory, Path basePath) throws IOException {
        List<Path> jsFiles;
        try (Stream<Path> walk = Files.walk(directory)) {
            jsFiles = walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".js"))
                    .sorted()
                    .collect(Collectors.toList());
        }
        List<Path> canonical = new ArrayList<>(jsFiles.size());
        for (Path jsFile : jsFiles) {
            // A symbolic link inside the folder could point outside the project
            Path resolved = jsFile.toFile().getCanonicalFile().toPath();
            checkInsideBasedir(resolved, basePath, relativize(basePath, jsFile));
            canonical.add(resolved);
        }
        return canonical;
    }

    private static String relativize(Path basePath, Path file) {
        return basePath.relativize(file).toString().replace('\\', '/');
    }

    /**
     * Moves the given fields first, in that order, to keep the output readable; other fields keep their order.
     */
    private static void reorder(ObjectNode node, String... firstFields) {
        Map<String, JsonNode> rest = new LinkedHashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
            Map.Entry<String, JsonNode> field = it.next();
            rest.put(field.getKey(), field.getValue());
        }
        node.removeAll();
        for (String name : firstFields) {
            JsonNode value = rest.remove(name);
            if (value != null) {
                node.set(name, value);
            }
        }
        node.setAll(rest);
    }

    private String computeSha256(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            return Hex.encodeHexString(digest.digest());
        } catch (Exception e) {
            throw new IOException("Failed to compute SHA-256 for " + file.getAbsolutePath(), e);
        }
    }
}
