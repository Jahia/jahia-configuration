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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.cyclonedx.Version;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.parsers.JsonParser;
import org.junit.Assume;
import org.junit.Before;
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

    private File basedir;
    private File outputDir;

    @Before
    public void setUp() throws Exception {
        basedir = tmpFolder.newFolder("project");
        outputDir = tmpFolder.newFolder("output");
    }

    /**
     * Creates a mojo instance with fields set via reflection (matching how Maven injects parameters).
     */
    private GenerateVendoredJsBomMojo createMojo() throws Exception {
        GenerateVendoredJsBomMojo mojo = new GenerateVendoredJsBomMojo();
        setField(mojo, "basedir", basedir);
        setField(mojo, "outputDirectory", outputDir);
        setField(mojo, "manifestFile", "vendored-js.cdx.json");
        setField(mojo, "outputFileName", "vendored-js-bom.cdx");
        setField(mojo, "skipIfMissing", true);
        setField(mojo, "projectGroupId", "org.jahia.modules");
        setField(mojo, "projectArtifactId", "legacy-module");
        setField(mojo, "projectVersion", "1.2.3");
        setField(mojo, "pluginVersion", "6.15.0");
        return mojo;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testSkipsExecutionWhenManifestMissing() throws Exception {
        createMojo().execute();

        assertFalse(new File(outputDir, "vendored-js-bom.cdx.json").exists());
    }

    @Test
    public void testExampleManifestIsValidCycloneDxAndProducesValidBom() throws Exception {
        try (InputStream example = getClass().getResourceAsStream("vendored-js.cdx.json")) {
            Files.copy(example, new File(basedir, "vendored-js.cdx.json").toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        assertSchemaValid(new File(basedir, "vendored-js.cdx.json"));
        writeFile("src/main/resources/javascript/jquery/jquery.min.js", "/* jquery */");
        writeFile("src/main/resources/javascript/moment/moment.js", "/* moment */");
        writeFile("src/main/resources/javascript/moment/locale/fr.js", "/* fr */");

        JsonNode root = generate();

        assertSchemaValid(new File(outputDir, "vendored-js-bom.cdx.json"));
        JsonNode jquery = root.get("components").get(0);
        assertEquals("Copyright JS Foundation and other contributors", jquery.get("copyright").asText());
        assertEquals("pkg:npm/jquery@3.6.0", jquery.get("bom-ref").asText());
        JsonNode moment = root.get("components").get(1);
        assertEquals("Fields the plugin does not know are passed through",
                "pkg:npm/moment@2.29.1", moment.get("pedigree").get("ancestors").get(0).get("purl").asText());
        assertEquals(2, moment.get("components").size());
    }

    @Test
    public void testSchemaCheckDetectsInvalidBom() throws Exception {
        writeManifest("{'bomFormat':'CycloneDX','specVersion':'1.4','version':1,'components':["
                + "{'type':'script','name':'lib','hashes':[{'alg':'SHA256','content':'abc'}]}]}");

        List<ParseException> errors = new JsonParser().validate(new File(basedir, "vendored-js.cdx.json"), Version.VERSION_14);
        assertFalse("The schema check used by these tests must catch invalid BOMs", errors.isEmpty());
    }

    @Test
    public void testSingleFileIsHashedOnFileAndLibrary() throws Exception {
        writeFile("js/app.js", "console.log('hello');");
        writeManifest("{'bomFormat':'CycloneDX','specVersion':'1.4','version':1,'components':["
                + "{'type':'library','name':'app','version':'1.0','components':[{'type':'file','name':'js/app.js'}]}]}");

        JsonNode library = generate().get("components").get(0);

        JsonNode fileHashes = library.get("components").get(0).get("hashes");
        assertEquals(1, fileHashes.size());
        assertEquals("SHA-256", fileHashes.get(0).get("alg").asText());
        assertEquals(64, fileHashes.get(0).get("content").asText().length());
        assertEquals("A library made of one file is that file", fileHashes, library.get("hashes"));
        assertEquals("vendored-js:app@1.0", library.get("bom-ref").asText());
        assertEquals("vendored-js:app@1.0#js/app.js", library.get("components").get(0).get("bom-ref").asText());
    }

    @Test
    public void testFolderProducesOneFileComponentPerJsFile() throws Exception {
        writeFile("vendor/moment/moment.js", "/* moment */");
        writeFile("vendor/moment/locale/fr.js", "/* fr */");
        writeFile("vendor/moment/README.md", "not javascript");
        writeManifest("{'bomFormat':'CycloneDX','specVersion':'1.4','version':1,'components':["
                + "{'type':'library','name':'moment','version':'2.29.1',"
                + "'properties':[{'name':'jahia:source-folder','value':'vendor/moment/'}]}]}");

        JsonNode library = generate().get("components").get(0);

        assertNull("Several files must not share one hashes array", library.get("hashes"));
        JsonNode files = library.get("components");
        assertEquals("Only .js files are covered, like the validator does", 2, files.size());
        assertEquals("vendor/moment/locale/fr.js", files.get(0).get("name").asText());
        assertEquals("vendor/moment/moment.js", files.get(1).get("name").asText());
        for (JsonNode file : files) {
            assertEquals("file", file.get("type").asText());
            assertEquals(1, file.get("hashes").size());
            assertTrue(file.get("bom-ref").asText().startsWith("vendored-js:moment@2.29.1#vendor/moment/"));
        }
        assertSchemaValid(new File(outputDir, "vendored-js-bom.cdx.json"));
    }

    @Test
    public void testFileListedAndInFolderIsHashedOnce() throws Exception {
        writeFile("js/a.js", "a");
        writeFile("js/b.js", "b");
        writeManifest("{'bomFormat':'CycloneDX','specVersion':'1.4','version':1,'components':["
                + "{'type':'library','name':'ab','version':'1.0','purl':'pkg:npm/ab@1.0',"
                + "'components':[{'type':'file','name':'js/a.js','description':'kept'}],"
                + "'properties':[{'name':'jahia:source-folder','value':'js'}]}]}");

        JsonNode library = generate().get("components").get(0);

        assertEquals("pkg:npm/ab@1.0", library.get("bom-ref").asText());
        JsonNode files = library.get("components");
        assertEquals(2, files.size());
        assertEquals("kept", files.get(0).get("description").asText());
        assertEquals("js/b.js", files.get(1).get("name").asText());
    }

    @Test
    public void testMatchingDeclaredHashIsAccepted() throws Exception {
        writeFile("js/app.js", "pinned");
        String sha256 = org.apache.commons.codec.digest.DigestUtils.sha256Hex("pinned").toUpperCase();
        writeManifest("{'bomFormat':'CycloneDX','specVersion':'1.4','version':1,'components':["
                + "{'type':'library','name':'app','components':[{'type':'file','name':'js/app.js',"
                + "'hashes':[{'alg':'SHA-256','content':'" + sha256 + "'}]}]}]}");

        JsonNode file = generate().get("components").get(0).get("components").get(0);
        assertEquals(sha256.toLowerCase(), file.get("hashes").get(0).get("content").asText());
    }

    @Test
    public void testChangedFileWithDeclaredHashFails() throws Exception {
        writeFile("js/app.js", "tampered");
        writeManifest("{'bomFormat':'CycloneDX','specVersion':'1.4','version':1,'components':["
                + "{'type':'library','name':'app','components':[{'type':'file','name':'js/app.js',"
                + "'hashes':[{'alg':'SHA-256','content':'" + repeat('0', 64) + "'}]}]}]}");

        assertGenerationFails("does not match");
    }

    @Test
    public void testHashesOnLibraryAreRejected() throws Exception {
        writeManifest("{'bomFormat':'CycloneDX','specVersion':'1.4','version':1,'components':["
                + "{'type':'library','name':'app','hashes':[{'alg':'SHA-256','content':'" + repeat('0', 64) + "'}]}]}");

        assertGenerationFails("must not declare \"hashes\"");
    }

    @Test
    public void testMissingFileFails() throws Exception {
        writeManifest("{'bomFormat':'CycloneDX','specVersion':'1.4','version':1,'components':["
                + "{'type':'library','name':'app','components':[{'type':'file','name':'js/missing.js'}]}]}");

        assertGenerationFails("is not an existing file");
    }

    @Test
    public void testFolderDeclaredAsFileComponentFails() throws Exception {
        writeFile("js/app.js", "app");
        writeManifest("{'bomFormat':'CycloneDX','specVersion':'1.4','version':1,'components':["
                + "{'type':'library','name':'app','components':[{'type':'file','name':'js/'}]}]}");

        assertGenerationFails("jahia:source-folder");
    }

    @Test
    public void testFileListedTwiceFails() throws Exception {
        writeFile("js/app.js", "app");
        writeManifest("{'bomFormat':'CycloneDX','specVersion':'1.4','version':1,'components':["
                + "{'type':'library','name':'app','components':[{'type':'file','name':'js/app.js'},"
                + "{'type':'file','name':'./js/app.js'}]}]}");

        assertGenerationFails("listed more than once");
    }

    @Test
    public void testBomContainsSerialNumberMetadataAndKeepsDeclaredTools() throws Exception {
        writeManifest("{'bomFormat':'CycloneDX','specVersion':'1.4','version':3,"
                + "'metadata':{'tools':[{'name':'hand-written'}]},"
                + "'components':[{'type':'library','name':'lib','version':'1.0'}]}");

        JsonNode root = generate();

        assertTrue(root.get("serialNumber").asText().matches("urn:uuid:[0-9a-f-]{36}"));
        assertEquals("Declared BOM version is kept", 3, root.get("version").asInt());
        JsonNode metadata = root.get("metadata");
        assertNotNull(metadata.get("timestamp"));
        assertEquals(2, metadata.get("tools").size());
        assertEquals("hand-written", metadata.get("tools").get(0).get("name").asText());
        assertEquals("vendored-js-bom-maven-plugin", metadata.get("tools").get(1).get("name").asText());
        assertEquals("6.15.0", metadata.get("tools").get(1).get("version").asText());
        assertEquals("pkg:maven/org.jahia.modules/legacy-module@1.2.3", metadata.get("component").get("purl").asText());
        JsonNode properties = root.get("components").get(0).get("properties");
        assertEquals("jahia:component-source", properties.get(0).get("name").asText());
        assertEquals("vendored-javascript", properties.get(0).get("value").asText());
        assertSchemaValid(new File(outputDir, "vendored-js-bom.cdx.json"));
    }

    @Test
    public void testPathEscapingBasedirFails() throws Exception {
        Files.write(new File(tmpFolder.getRoot(), "outside.js").toPath(), "secret".getBytes(StandardCharsets.UTF_8));
        writeManifest("{'bomFormat':'CycloneDX','specVersion':'1.4','version':1,'components':["
                + "{'type':'library','name':'evil','components':[{'type':'file','name':'../outside.js'}]}]}");

        assertGenerationFails("escapes project basedir");
    }

    @Test
    public void testSymlinkInFolderEscapingBasedirFails() throws Exception {
        File outside = new File(tmpFolder.getRoot(), "outside.js");
        Files.write(outside.toPath(), "secret".getBytes(StandardCharsets.UTF_8));
        File vendorDir = new File(basedir, "vendor");
        vendorDir.mkdirs();
        try {
            Files.createSymbolicLink(new File(vendorDir, "link.js").toPath(), outside.toPath());
        } catch (IOException | UnsupportedOperationException e) {
            Assume.assumeNoException("Symbolic links not supported here", e);
        }
        writeManifest("{'bomFormat':'CycloneDX','specVersion':'1.4','version':1,'components':["
                + "{'type':'library','name':'evil','properties':[{'name':'jahia:source-folder','value':'vendor/'}]}]}");

        assertGenerationFails("escapes project basedir");
    }

    private JsonNode generate() throws Exception {
        createMojo().execute();
        return new ObjectMapper().readTree(new File(outputDir, "vendored-js-bom.cdx.json"));
    }

    private void assertGenerationFails(String expectedMessage) throws Exception {
        try {
            createMojo().execute();
            fail("Generation should fail with: " + expectedMessage);
        } catch (MojoExecutionException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(expectedMessage));
        }
    }

    static void assertSchemaValid(File bom) throws IOException {
        List<ParseException> errors = new JsonParser().validate(bom, Version.VERSION_14);
        assertTrue("Not valid CycloneDX 1.4: " + errors, errors.isEmpty());
    }

    /**
     * Writes a manifest given with single quotes instead of double quotes, for readability.
     */
    private void writeManifest(String json) throws IOException {
        writeFile("vendored-js.cdx.json", json.replace('\'', '"'));
    }

    private void writeFile(String path, String content) throws IOException {
        File file = new File(basedir, path);
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
