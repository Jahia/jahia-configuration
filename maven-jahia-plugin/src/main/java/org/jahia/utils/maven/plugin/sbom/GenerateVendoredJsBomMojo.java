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
 *     Enterprises Distribution - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.utils.maven.plugin.sbom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Hex;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Generates a CycloneDX 1.4 Software Bill of Materials (SBOM) for manually packaged JavaScript libraries
 * (vendored JavaScript files). Reads from a vendored-js.yaml manifest and outputs a JSON BOM suitable
 * for merging with Maven and Webpack-generated BOMs before uploading to Dependency-Track.
 *
 * @goal generate-vendored-js-bom
 * @phase process-resources
 * @requiresProject true
 */
public class GenerateVendoredJsBomMojo extends AbstractMojo {
    
    private static final String SHA256_ALGORITHM = "SHA-256";

    /**
     * The path to the vendored JavaScript manifest file (YAML).
     * Relative to project basedir.
     *
     * @parameter default-value="vendored-js.yaml"
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
            VendoredJsManifest parsedManifest = VendoredJsManifestParser.parse(manifest);

            getLog().info("Generating CycloneDX 1.4 SBOM from " + parsedManifest.getComponents().size() + " component(s)");
            BomModel bom = generateBom(parsedManifest);

            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            File outputFile = new File(outputDirectory, outputFileName + ".json");
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
                BomSerializer.serializeToJson(bom, writer);
            }

            getLog().info("Vendored JS BOM written to " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate vendored JS BOM: " + e.getMessage(), e);
        }
    }

    private BomModel generateBom(VendoredJsManifest manifest) throws IOException {
        BomModel bom = new BomModel();
        bom.setBomVersion("1");
        bom.setSpecVersion("1.4");
        bom.setVersion(1);

        List<ComponentModel> components = new ArrayList<>();
        for (VendoredJsComponent componentDef : manifest.getComponents()) {
            components.add(buildComponent(componentDef));
        }

        bom.setComponents(components);
        return bom;
    }

    private ComponentModel buildComponent(VendoredJsComponent componentDef) throws IOException {
        ComponentModel component = new ComponentModel();

        setBasicProperties(component, componentDef);
        addPurl(component, componentDef);
        addLicenses(component, componentDef);
        addHashes(component, componentDef);
        addSupplier(component, componentDef);
        addCopyright(component, componentDef);
        addProperties(component, componentDef);
        addDescription(component, componentDef);

        return component;
    }

    private void setBasicProperties(ComponentModel component, VendoredJsComponent componentDef) {
        String componentType = componentDef.getType();
        component.setType(componentType != null ? componentType : "library");
        component.setName(componentDef.getName());
        component.setVersion(componentDef.getVersion());
    }

    private void addPurl(ComponentModel component, VendoredJsComponent componentDef) {
        if (componentDef.getPurl() != null) {
            component.setPurl(componentDef.getPurl());
        }
    }

    private void addLicenses(ComponentModel component, VendoredJsComponent componentDef) {
        if (componentDef.getLicenses() != null && !componentDef.getLicenses().isEmpty()) {
            List<ComponentModel.LicenseChoice> licenseChoices = new ArrayList<>();
            for (String licenseId : componentDef.getLicenses()) {
                licenseChoices.add(new ComponentModel.LicenseChoice(licenseId));
            }
            component.setLicenses(licenseChoices);
        }
    }

    private void addHashes(ComponentModel component, VendoredJsComponent componentDef) throws IOException {
        if (componentDef.getFiles() == null || componentDef.getFiles().isEmpty()) {
            return;
        }

        Path basePath = basedir.getCanonicalFile().toPath();
        for (String filePath : componentDef.getFiles()) {
            processFilePath(component, filePath, basePath);
        }
    }

    private void processFilePath(ComponentModel component, String filePath, Path basePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            return;
        }

        File resolved = new File(basedir, filePath).getCanonicalFile();
        Path resolvedPath = resolved.toPath();

        if (!resolvedPath.startsWith(basePath)) {
            throw new IOException("Manifest path escapes project basedir: " + filePath);
        }

        if (!resolved.exists()) {
            getLog().warn("Manifest path not found; skipping hash computation: " + filePath);
            return;
        }

        if (resolved.isFile()) {
            processFile(component, resolved);
        }
    }


    private void processFile(ComponentModel component, File file) throws IOException {
        addHashToComponent(component, file);
    }

    private void addHashToComponent(ComponentModel component, File file) throws IOException {
        String sha256 = computeSha256(file);
        Map<String, String> hash = new HashMap<>();
        hash.put("alg", SHA256_ALGORITHM);
        hash.put("content", sha256);
        component.addHash(hash);
    }

    private void addSupplier(ComponentModel component, VendoredJsComponent componentDef) {
        if (componentDef.getSupplier() != null) {
            component.setSupplier(new ComponentModel.OrganizationalEntity(componentDef.getSupplier()));
        }
    }

    private void addCopyright(ComponentModel component, VendoredJsComponent componentDef) {
        if (componentDef.getCopyright() != null) {
            component.setCopyright(componentDef.getCopyright());
        }
    }

    private void addProperties(ComponentModel component, VendoredJsComponent componentDef) {
        List<ComponentModel.Property> properties = new ArrayList<>();
        if (componentDef.getFiles() != null && !componentDef.getFiles().isEmpty()) {
            properties.add(new ComponentModel.Property("jahia:source-paths", String.join(",", componentDef.getFiles())));
        }
        if (componentDef.isModified()) {
            properties.add(new ComponentModel.Property("jahia:modified", "true"));
        }
        properties.add(new ComponentModel.Property("jahia:component-source", "vendored-javascript"));
        component.setProperties(properties);
    }

    private void addDescription(ComponentModel component, VendoredJsComponent componentDef) {
        if (componentDef.getNotes() != null) {
            component.setDescription(componentDef.getNotes());
        }
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
