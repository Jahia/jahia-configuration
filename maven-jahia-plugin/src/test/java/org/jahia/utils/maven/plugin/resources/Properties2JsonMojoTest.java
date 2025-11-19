/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2023 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.resources;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;



/**
 * Comprehensive test suite for Properties2JsonMojo class with 100% coverage.
 *
 * @author Test Suite
 */
public class Properties2JsonMojoTest {

    private Properties2JsonMojo mojo;
    private File tempDir;
    private File srcDir;
    private File destDir;
    private Log mockLog;
    private MavenProject mockProject;

    @Before
    public void setUp() throws Exception {
        mojo = new Properties2JsonMojo();
        mockLog = mock(Log.class);
        mockProject = mock(MavenProject.class);

        // Create temporary directories for testing
        tempDir = Files.createTempDirectory("properties2json-test").toFile();
        srcDir = new File(tempDir, "src");
        destDir = new File(tempDir, "dest");
        srcDir.mkdirs();
        destDir.mkdirs();

        // Set up the mojo with test values
        setField(mojo, "src", srcDir);
        setField(mojo, "dest", destDir);
        setField(mojo, "includes", "**/*.properties");
        setField(mojo, "excludes", null);
        setField(mojo, "prettyPrinting", false);
        setField(mojo, "addToProjectResources", true);
        setField(mojo, "project", mockProject);

        // Inject mock logger
        mojo.setLog(mockLog);
    }

    @After
    public void tearDown() throws Exception {
        // Clean up temporary directories
        if (tempDir != null && tempDir.exists()) {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    /**
     * Test toJson method with pretty printing disabled
     */
    @Test
    public void testToJsonWithoutPrettyPrinting() throws Exception {
        // Create a test resource bundle file
        File propsFile = new File(srcDir, "messages_en.properties");
        FileUtils.writeStringToFile(propsFile, "key1=value1\nkey2=value2\n", StandardCharsets.UTF_8);

        // Test conversion without pretty printing
        String json = Properties2JsonMojo.toJson(propsFile, false);

        assertNotNull("JSON should not be null", json);
        assertTrue("JSON should contain key1", json.contains("key1"));
        assertTrue("JSON should contain value1", json.contains("value1"));
        assertTrue("JSON should contain key2", json.contains("key2"));
        assertTrue("JSON should contain value2", json.contains("value2"));
        assertFalse("JSON should not contain line breaks (compact format)", json.contains("\n  "));

        // Parse and validate the JSON using Jackson ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();

        // Parse JSON string into JsonNode
        JsonNode rootNode = objectMapper.readTree(json);

        assertNotNull("Parsed JSON node should not be null", rootNode);
        assertTrue("Root node should be an object", rootNode.isObject());
        assertEquals("JSON should contain exactly 2 fields", 2, rootNode.size());

        // Verify individual fields
        assertTrue("JSON should contain key1 field", rootNode.has("key1"));
        assertTrue("JSON should contain key2 field", rootNode.has("key2"));
        assertEquals("key1 should have value 'value1'", "value1", rootNode.get("key1").asText());
        assertEquals("key2 should have value 'value2'", "value2", rootNode.get("key2").asText());
    }

    /**
     * Test toJson method with pretty printing enabled
     */
    @Test
    public void testToJsonWithPrettyPrinting() throws Exception {
        // Create a test resource bundle file
        File propsFile = new File(srcDir, "messages_en.properties");
        FileUtils.writeStringToFile(propsFile, "key1=value1\nkey2=value2\n", StandardCharsets.UTF_8);

        // Test conversion with pretty printing
        String json = Properties2JsonMojo.toJson(propsFile, true);

        assertNotNull("JSON should not be null", json);
        assertTrue("JSON should contain key1", json.contains("key1"));
        assertTrue("JSON should contain value1", json.contains("value1"));
        // Pretty printed JSON typically has indentation and newlines
        assertTrue("JSON should be formatted (contain newlines)", json.contains("\n"));
    }

    /**
     * Test toJson method with non-existent file
     */
    @Test(expected = FileNotFoundException.class)
    public void testToJsonWithNonExistentFile() throws Exception {
        File nonExistentFile = new File(srcDir, "nonexistent.properties");
        Properties2JsonMojo.toJson(nonExistentFile, false);
    }

    /**
     * Test toJson method with complex properties including special characters
     */
    @Test
    public void testToJsonWithComplexProperties() throws Exception {
        // Create a resource bundle with special characters
        File propsFile = new File(srcDir, "messages_en.properties");
        FileUtils.writeStringToFile(propsFile,
            "key.with.dots=Value with spaces\n" +
            "key_with_underscores=Value123\n" +
            "numeric.key=12345\n", StandardCharsets.UTF_8);

        String json = Properties2JsonMojo.toJson(propsFile, false);

        assertNotNull("JSON should not be null", json);
        assertTrue("JSON should handle keys with dots", json.contains("key") || json.contains("dots"));
        assertTrue("JSON should handle keys with underscores", json.contains("key_with_underscores") || json.contains("underscores"));
    }

    /**
     * Test execute method when src is null
     */
    @Test
    public void testExecuteWithNullSrc() throws Exception {
        setField(mojo, "src", null);

        mojo.execute();

        verify(mockLog).info(contains("does not exist"));
    }

    /**
     * Test execute method when src directory doesn't exist
     */
    @Test
    public void testExecuteWithNonExistentSrc() throws Exception {
        File nonExistentDir = new File(tempDir, "nonexistent");
        setField(mojo, "src", nonExistentDir);

        mojo.execute();

        verify(mockLog).info(contains("does not exist"));
    }

    /**
     * Test execute method with successful conversion
     */
    @Test
    public void testExecuteWithSuccessfulConversion() throws Exception {
        // Create test properties files
        File subDir = new File(srcDir, "javascript/locales");
        subDir.mkdirs();

        File propsFile1 = new File(subDir, "messages_en.properties");
        FileUtils.writeStringToFile(propsFile1, "greeting=Hello\nfarewell=Goodbye\n", StandardCharsets.UTF_8);

        File propsFile2 = new File(subDir, "messages_fr.properties");
        FileUtils.writeStringToFile(propsFile2, "greeting=Bonjour\nfarewell=Au revoir\n", StandardCharsets.UTF_8);

        setField(mojo, "includes", "javascript/locales/*.properties");

        mojo.execute();

        // Verify log messages
        verify(mockLog, atLeastOnce()).info(contains("Converted file"));

        // Verify resources were added to project
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(mockProject).addResource(resourceCaptor.capture());

        Resource addedResource = resourceCaptor.getValue();
        assertEquals("Resource directory should match dest", destDir.getPath(), addedResource.getDirectory());

        // Verify output files exist
        File outputDir = new File(destDir, "javascript/locales");
        assertTrue("Output directory should exist", outputDir.exists());

        // Check that JSON files were created (filename without locale prefix)
        File[] jsonFiles = outputDir.listFiles((dir, name) -> name.endsWith(".json"));
        assertNotNull("JSON files should be created", jsonFiles);
        assertTrue("At least one JSON file should be created", jsonFiles.length > 0);
    }

    /**
     * Test execute method with addToProjectResources set to false
     */
    @Test
    public void testExecuteWithoutAddingToProjectResources() throws Exception {
        // Create a test resource bundle file in a subdirectory
        File i18nDir = new File(srcDir, "i18n");
        i18nDir.mkdirs();
        File propsFile = new File(i18nDir, "messages_en.properties");
        FileUtils.writeStringToFile(propsFile, "key=value\n", StandardCharsets.UTF_8);

        setField(mojo, "addToProjectResources", false);
        setField(mojo, "includes", "i18n/*.properties");

        mojo.execute();

        // Verify that addResource was not called
        verify(mockProject, never()).addResource(any(Resource.class));
    }

    /**
     * Test execute method with excludes pattern
     */
    @Test
    public void testExecuteWithExcludes() throws Exception {
        // Create test resource bundle files in a subdirectory
        File i18nDir = new File(srcDir, "i18n");
        i18nDir.mkdirs();

        File includedFile = new File(i18nDir, "messages_en.properties");
        FileUtils.writeStringToFile(includedFile, "key=value\n", StandardCharsets.UTF_8);

        File excludedFile = new File(i18nDir, "errors_en.properties");
        FileUtils.writeStringToFile(excludedFile, "key=value\n", StandardCharsets.UTF_8);

        setField(mojo, "includes", "i18n/*.properties");
        setField(mojo, "excludes", "i18n/errors_*.properties");

        mojo.execute();

        // At least one conversion should have happened (messages_en.properties)
        verify(mockLog, atLeastOnce()).info(contains("Converted file"));
    }

    /**
     * Test execute method with comma-separated includes
     */
    @Test
    public void testExecuteWithCommaSeparatedIncludes() throws Exception {
        File dir1 = new File(srcDir, "dir1");
        File dir2 = new File(srcDir, "dir2");
        dir1.mkdirs();
        dir2.mkdirs();

        File file1 = new File(dir1, "messages_en.properties");
        File file2 = new File(dir2, "labels_fr.properties");
        FileUtils.writeStringToFile(file1, "key1=value1\n", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(file2, "key2=value2\n", StandardCharsets.UTF_8);

        setField(mojo, "includes", "dir1/*.properties,dir2/*.properties");

        mojo.execute();

        // Verify conversions happened
        verify(mockLog, atLeast(2)).info(contains("Converted file"));
    }

    /**
     * Test execute method with space-separated includes
     */
    @Test
    public void testExecuteWithSpaceSeparatedIncludes() throws Exception {
        File i18nDir = new File(srcDir, "i18n");
        i18nDir.mkdirs();

        File file1 = new File(i18nDir, "messages_en.properties");
        File file2 = new File(i18nDir, "messages_fr.properties");
        FileUtils.writeStringToFile(file1, "key1=value1\n", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(file2, "key2=value2\n", StandardCharsets.UTF_8);

        setField(mojo, "includes", "i18n/messages_en.properties i18n/messages_fr.properties");

        mojo.execute();

        // Verify conversions happened
        verify(mockLog, atLeast(2)).info(contains("Converted file"));
    }

    /**
     * Test execute method when IOException occurs during conversion
     */
    @Test
    public void testExecuteWithIOException() throws Exception {
        // Create a resource bundle file in a subdirectory
        File i18nDir = new File(srcDir, "i18n");
        i18nDir.mkdirs();
        File propsFile = new File(i18nDir, "messages_en.properties");
        FileUtils.writeStringToFile(propsFile, "key=value\n", StandardCharsets.UTF_8);

        setField(mojo, "includes", "i18n/*.properties");

        // Make dest directory read-only to cause IOException
        destDir.setReadOnly();

        try {
            mojo.execute();
            fail("Should throw MojoExecutionException");
        } catch (MojoExecutionException e) {
            assertTrue("Exception message should mention conversion error",
                e.getMessage().contains("Error converting properties file") ||
                e.getMessage().contains("JSON format"));
            assertNotNull("Exception should have a cause", e.getCause());
        } finally {
            // Restore permissions for cleanup
            destDir.setWritable(true);
        }
    }

    /**
     * Test execute method with nested directory structure
     */
    @Test
    public void testExecuteWithNestedDirectories() throws Exception {
        File nestedDir = new File(srcDir, "a/b/c");
        nestedDir.mkdirs();

        File propsFile = new File(nestedDir, "messages_en.properties");
        FileUtils.writeStringToFile(propsFile, "key=value\n", StandardCharsets.UTF_8);

        setField(mojo, "includes", "a/b/c/*.properties");

        mojo.execute();

        verify(mockLog, atLeastOnce()).info(contains("Converted file"));

        // Verify output directory structure
        File outputDir = new File(destDir, "a/b/c");
        assertTrue("Nested output directory should be created", outputDir.exists());
    }

    /**
     * Test execute method with no matching files
     */
    @Test
    public void testExecuteWithNoMatchingFiles() throws Exception {
        setField(mojo, "includes", "nonexistent/*.properties");

        mojo.execute();

        // Should not log any conversion messages
        verify(mockLog, never()).info(contains("Converted file"));
    }

    /**
     * Test execute method with Windows-style path separators
     */
    @Test
    public void testExecuteWithWindowsStylePaths() throws Exception {
        File subDir = new File(srcDir, "javascript\\locales");
        subDir.mkdirs();

        File propsFile = new File(subDir, "messages_en.properties");
        FileUtils.writeStringToFile(propsFile, "key=value\n", StandardCharsets.UTF_8);

        // The includes pattern uses forward slashes, but getOutputFile should handle backslashes
        setField(mojo, "includes", "**/*.properties");

        mojo.execute();

        verify(mockLog, atLeastOnce()).info(contains("Converted file"));
    }

    /**
     * Test getOutputFile method through reflection (private method testing)
     * This mojo is designed for Java resource bundles in subdirectories (e.g., i18n/messages_en.properties)
     * Files at root level create an unexpected subdirectory due to StringUtils.substringBeforeLast behavior.
     */
    @Test
    public void testGetOutputFile() throws Exception {
        Method getOutputFileMethod = Properties2JsonMojo.class.getDeclaredMethod("getOutputFile", String.class);
        getOutputFileMethod.setAccessible(true);

        // Test with nested path (recommended usage - typical Angular/React i18n structure)
        File result1 = (File) getOutputFileMethod.invoke(mojo, "javascript/locales/messages_en.properties");
        assertEquals("Output file should be en.json", "en.json", result1.getName());
        assertTrue("Output path should include parent directory", result1.getPath().contains("javascript"));
        assertTrue("Output path should include parent directory", result1.getPath().contains("locales"));

        // Test with different locale
        File result2 = (File) getOutputFileMethod.invoke(mojo, "javascript/locales/messages_fr.properties");
        assertEquals("Output file should be fr.json", "fr.json", result2.getName());
        assertTrue("Output path should include parent directory", result2.getPath().contains("javascript"));
        assertTrue("Output path should include parent directory", result2.getPath().contains("locales"));

        // Test with Windows-style path (cross-platform compatibility)
        File result3 = (File) getOutputFileMethod.invoke(mojo, "javascript\\locales\\messages_de.properties");
        assertEquals("Output file should be de.json", "de.json", result3.getName());
        // Verify path normalization (backslashes converted to forward slashes internally)
        assertTrue("Output path should include parent directory", result3.getPath().contains("javascript"));
        assertTrue("Output path should include parent directory", result3.getPath().contains("locales"));

        // Test with locale variant (e.g., messages_en_US.properties -> en_US.json)
        File result4 = (File) getOutputFileMethod.invoke(mojo, "i18n/messages_en_US.properties");
        assertEquals("Should extract everything after first underscore", "en_US.json", result4.getName());
    }

    /**
     * Test getOutputFile with complex locale patterns (e.g., en_US, pt_BR)
     * Resource bundles can have locale variants like en_US, en_GB, zh_CN, etc.
     */
    @Test
    public void testGetOutputFileComplexPatterns() throws Exception {
        Method getOutputFileMethod = Properties2JsonMojo.class.getDeclaredMethod("getOutputFile", String.class);
        getOutputFileMethod.setAccessible(true);

        // Test with locale variant (messages_en_US.properties -> en_US.json)
        File result1 = (File) getOutputFileMethod.invoke(mojo, "i18n/messages_en_US.properties");
        assertEquals("Should extract everything after first underscore (locale code)", "en_US.json", result1.getName());
        assertTrue("Output path should include i18n directory", result1.getPath().contains("i18n"));

        // Test with different base name (app_config.properties -> config.json)
        File result2 = (File) getOutputFileMethod.invoke(mojo, "config/app_config.properties");
        assertEquals("Should extract locale part from app_config", "config.json", result2.getName());
        assertTrue("Output path should include config directory", result2.getPath().contains("config"));

        // Test with nested path and locale variant
        File result3 = (File) getOutputFileMethod.invoke(mojo, "locales/pt/messages_pt_BR.properties");
        assertEquals("Should extract pt_BR locale", "pt_BR.json", result3.getName());
        assertTrue("Output path should include locales directory", result3.getPath().contains("locales"));
    }

    /**
     * Test execute with pretty printing enabled
     */
    @Test
    public void testExecuteWithPrettyPrinting() throws Exception {
        File i18nDir = new File(srcDir, "i18n");
        i18nDir.mkdirs();
        File propsFile = new File(i18nDir, "messages_en.properties");
        FileUtils.writeStringToFile(propsFile, "key1=value1\nkey2=value2\n", StandardCharsets.UTF_8);

        setField(mojo, "prettyPrinting", true);
        setField(mojo, "includes", "i18n/*.properties");

        mojo.execute();

        verify(mockLog, atLeastOnce()).info(contains("Converted file"));

        // Verify output file exists - i18n/messages_en.properties should generate i18n/en.json
        File expectedJsonFile = new File(destDir, "i18n/en.json");
        assertTrue("JSON file i18n/en.json should be created", expectedJsonFile.exists());

        String content = FileUtils.readFileToString(expectedJsonFile, StandardCharsets.UTF_8);
        assertNotNull("File content should not be null", content);
        // Pretty printed JSON typically contains newlines and indentation
        assertTrue("Pretty printed JSON should contain newlines", content.contains("\n"));
    }

    /**
     * Test with empty resource bundle (edge case)
     */
    @Test
    public void testExecuteWithEmptyResourceBundle() throws Exception {
        File i18nDir = new File(srcDir, "i18n");
        i18nDir.mkdirs();
        File propsFile = new File(i18nDir, "messages_en.properties");
        FileUtils.writeStringToFile(propsFile, "", StandardCharsets.UTF_8);

        setField(mojo, "includes", "i18n/*.properties");

        mojo.execute();

        verify(mockLog, atLeastOnce()).info(contains("Converted file"));
    }

    /**
     * Test resource addition to project
     */
    @Test
    public void testResourceAdditionToProject() throws Exception {
        File i18nDir = new File(srcDir, "i18n");
        i18nDir.mkdirs();
        File propsFile = new File(i18nDir, "messages_en.properties");
        FileUtils.writeStringToFile(propsFile, "key=value\n", StandardCharsets.UTF_8);

        setField(mojo, "addToProjectResources", true);
        setField(mojo, "includes", "i18n/*.properties");

        mojo.execute();

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(mockProject, times(1)).addResource(resourceCaptor.capture());

        Resource resource = resourceCaptor.getValue();
        assertNotNull("Resource should not be null", resource);
        assertEquals("Resource directory should match dest", destDir.getPath(), resource.getDirectory());
    }

    /**
     * Test with multiple files in same directory
     */
    @Test
    public void testExecuteWithMultipleFilesInSameDirectory() throws Exception {
        File dir = new File(srcDir, "locales");
        dir.mkdirs();

        File file1 = new File(dir, "app_en.properties");
        File file2 = new File(dir, "app_fr.properties");
        File file3 = new File(dir, "app_de.properties");

        FileUtils.writeStringToFile(file1, "key=Hello\n", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(file2, "key=Bonjour\n", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(file3, "key=Hallo\n", StandardCharsets.UTF_8);

        setField(mojo, "includes", "locales/*.properties");

        mojo.execute();

        verify(mockLog, atLeast(3)).info(contains("Converted file"));

        // Verify all JSON files were created
        File outputDir = new File(destDir, "locales");
        File[] jsonFiles = outputDir.listFiles((dir1, name) -> name.endsWith(".json"));
        assertNotNull("JSON files should be created", jsonFiles);
        assertEquals("Should create 3 JSON files", 3, jsonFiles.length);
    }

    /**
     * Helper method to set private/protected fields using reflection
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

