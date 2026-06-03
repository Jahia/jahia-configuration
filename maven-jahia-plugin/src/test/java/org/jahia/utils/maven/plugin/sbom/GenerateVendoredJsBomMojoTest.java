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
import java.io.FileWriter;
import java.lang.reflect.Field;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link GenerateVendoredJsBomMojo}.
 */
public class GenerateVendoredJsBomMojoTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    /**
     * Creates a mojo instance with fields set via reflection (matching how Maven injects parameters).
     */
    private GenerateVendoredJsBomMojo createMojo(File basedir, File outputDirectory, String manifestFile)
            throws Exception {
        GenerateVendoredJsBomMojo mojo = new GenerateVendoredJsBomMojo();
        setField(mojo, "basedir", basedir);
        setField(mojo, "outputDirectory", outputDirectory);
        setField(mojo, "manifestFile", manifestFile);
        setField(mojo, "outputFileName", "vendored-js-bom.cdx");
        setField(mojo, "skipIfMissing", true);
        return mojo;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testSkipsExecutionWhenManifestMissing() throws Exception {
        File basedir = tmpFolder.newFolder("project");
        File outputDir = tmpFolder.newFolder("output");

        GenerateVendoredJsBomMojo mojo = createMojo(basedir, outputDir, "vendored-js.yaml");
        // Should not throw — skipIfMissing is true by default
        mojo.execute();

        // No BOM file should be generated
        assertFalse(new File(outputDir, "vendored-js-bom.cdx.json").exists());
    }

    @Test
    public void testGeneratesBomFromManifest() throws Exception {
        File basedir = tmpFolder.newFolder("project");
        File outputDir = tmpFolder.newFolder("output");

        // Create a simple JS file for hashing
        File jsDir = new File(basedir, "src/main/resources/javascript");
        jsDir.mkdirs();
        File jsFile = new File(jsDir, "lib.js");
        try (FileWriter fw = new FileWriter(jsFile)) {
            fw.write("/* test library */");
        }

        // Write a minimal manifest
        File manifest = new File(basedir, "vendored-js.yaml");
        try (FileWriter fw = new FileWriter(manifest)) {
            fw.write("components:\n");
            fw.write("  - name: test-lib\n");
            fw.write("    version: \"1.0.0\"\n");
            fw.write("    type: library\n");
            fw.write("    licenses:\n");
            fw.write("      - MIT\n");
            fw.write("    files:\n");
            fw.write("      - src/main/resources/javascript/lib.js\n");
        }

        GenerateVendoredJsBomMojo mojo = createMojo(basedir, outputDir, "vendored-js.yaml");
        mojo.execute();

        File bomFile = new File(outputDir, "vendored-js-bom.cdx.json");
        assertTrue("BOM file should be generated", bomFile.exists());

        // Validate JSON structure
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(bomFile);

        assertEquals("1.4", root.get("specVersion").asText());
        assertTrue(root.has("components"));
        assertEquals(1, root.get("components").size());

        JsonNode component = root.get("components").get(0);
        assertEquals("test-lib", component.get("name").asText());
        assertEquals("1.0.0", component.get("version").asText());
        assertEquals("library", component.get("type").asText());
    }

    @Test
    public void testHashesAreComputedForExistingFiles() throws Exception {
        File basedir = tmpFolder.newFolder("project");
        File outputDir = tmpFolder.newFolder("output");

        File jsDir = new File(basedir, "js");
        jsDir.mkdirs();
        File jsFile = new File(jsDir, "app.js");
        try (FileWriter fw = new FileWriter(jsFile)) {
            fw.write("console.log('hello');");
        }

        File manifest = new File(basedir, "vendored-js.yaml");
        try (FileWriter fw = new FileWriter(manifest)) {
            fw.write("components:\n");
            fw.write("  - name: app\n");
            fw.write("    version: \"1.0\"\n");
            fw.write("    files:\n");
            fw.write("      - js/app.js\n");
        }

        GenerateVendoredJsBomMojo mojo = createMojo(basedir, outputDir, "vendored-js.yaml");
        mojo.execute();

        File bomFile = new File(outputDir, "vendored-js-bom.cdx.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(bomFile);

        JsonNode hashes = root.get("components").get(0).get("hashes");
        assertNotNull("Hashes should be present for existing files", hashes);
        assertTrue(hashes.isArray());
        assertEquals(1, hashes.size());
        assertEquals("SHA-256", hashes.get(0).get("alg").asText());

        String hashContent = hashes.get(0).get("content").asText();
        assertNotNull(hashContent);
        // SHA-256 hex string should be 64 characters
        assertEquals(64, hashContent.length());
    }

    @Test
    public void testDirectoryReferenceDoesNotProduceHash() throws Exception {
        File basedir = tmpFolder.newFolder("project");
        File outputDir = tmpFolder.newFolder("output");

        // Create a directory reference in the manifest (no individual file listed)
        File jsDir = new File(basedir, "vendor/moment");
        jsDir.mkdirs();
        new File(jsDir, "moment.js").createNewFile();

        File manifest = new File(basedir, "vendored-js.yaml");
        try (FileWriter fw = new FileWriter(manifest)) {
            fw.write("components:\n");
            fw.write("  - name: moment\n");
            fw.write("    version: \"2.29.1\"\n");
            fw.write("    files:\n");
            fw.write("      - vendor/moment/\n");
        }

        GenerateVendoredJsBomMojo mojo = createMojo(basedir, outputDir, "vendored-js.yaml");
        mojo.execute();

        File bomFile = new File(outputDir, "vendored-js-bom.cdx.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(bomFile);

        // Directory entries are not regular files, so no hash should be produced
        JsonNode hashes = root.get("components").get(0).get("hashes");
        // hashes may be null or an empty array since the path points to a directory
        assertTrue("Directory reference should not produce file hashes",
                hashes == null || hashes.isNull() || hashes.size() == 0);
    }

    @Test
    public void testPropertiesContainSourcePathsAndComponentSource() throws Exception {
        File basedir = tmpFolder.newFolder("project");
        File outputDir = tmpFolder.newFolder("output");

        File jsDir = new File(basedir, "js");
        jsDir.mkdirs();
        File jsFile = new File(jsDir, "util.js");
        try (FileWriter fw = new FileWriter(jsFile)) {
            fw.write("/* util */");
        }

        File manifest = new File(basedir, "vendored-js.yaml");
        try (FileWriter fw = new FileWriter(manifest)) {
            fw.write("components:\n");
            fw.write("  - name: util\n");
            fw.write("    version: \"0.1\"\n");
            fw.write("    files:\n");
            fw.write("      - js/util.js\n");
        }

        GenerateVendoredJsBomMojo mojo = createMojo(basedir, outputDir, "vendored-js.yaml");
        mojo.execute();

        File bomFile = new File(outputDir, "vendored-js-bom.cdx.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(bomFile);

        JsonNode properties = root.get("components").get(0).get("properties");
        assertNotNull("Properties should be present", properties);
        assertTrue(properties.isArray());

        boolean hasSourcePaths = false;
        boolean hasComponentSource = false;
        for (JsonNode prop : properties) {
            String name = prop.get("name").asText();
            if ("jahia:source-paths".equals(name)) {
                hasSourcePaths = true;
            }
            if ("jahia:component-source".equals(name)) {
                assertEquals("vendored-javascript", prop.get("value").asText());
                hasComponentSource = true;
            }
        }
        assertTrue("Property jahia:source-paths should be present", hasSourcePaths);
        assertTrue("Property jahia:component-source should be present", hasComponentSource);
    }
}
