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
package org.jahia.configuration.configurators;

import difflib.DiffUtils;
import difflib.Patch;
import junit.framework.TestCase;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Test case for Properties Manager
 */
public class PropertiesManagerTest extends TestCase {

    private File jahiaPropertiesFile;
    private File jahiaTargetPropertiesFile;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Use the properties file directly from test resources
        URL propertiesUrl = this.getClass().getClassLoader().getResource("jahia.properties");
        jahiaPropertiesFile = new File(propertiesUrl.getFile());
        jahiaTargetPropertiesFile = new File(jahiaPropertiesFile.getParentFile(), "jahia-test.properties");
        if (jahiaTargetPropertiesFile.exists()) {
            jahiaTargetPropertiesFile.delete();
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (jahiaTargetPropertiesFile.exists()) {
            jahiaTargetPropertiesFile.delete();
        }
    }

    public void testLoadBasicCase() throws IOException {
        try (InputStream is = new FileInputStream(jahiaPropertiesFile)) {
            PropertiesManager propertiesManager = new PropertiesManager(is);
            propertiesManager.setReplaceTabsWithSpaces(false);
            propertiesManager.setSanitizeValue(false);

            propertiesManager.setProperty("testPropertyName\u2126", "testPropertyValue\u2126");
            propertiesManager.setProperty("backslashTest", "c:\\Program Files (x86)\\SWFTools\\pdf2swf.exe");

            // test some loaded values for validity
            assertEquals("Property with colon separator is invalid", "value1value2value3", propertiesManager.getProperty("colonSeparatorKey"));
            assertEquals("Whitespace separated propertiy is invalid", "value", propertiesManager.getProperty("whiteSpaceKey"));
            assertEquals("No value property is invalid", "", propertiesManager.getProperty("noValueKey"));
            assertEquals("Special characters in key property is invalid", "value", propertiesManager.getProperty("key_with_special_characters_:=_in_it."));
            assertEquals("Long text property is invalid", "This is an example \nof a long text with carriage returns \nembedded.", propertiesManager.getProperty("longTextProperty"));
            assertEquals("Comma separated property is invalid", "test1,test2", propertiesManager.getProperty("commaSeparatedProperty"));

            // test storage
            try (InputStream templateIs = new FileInputStream(jahiaPropertiesFile)) {
                propertiesManager.storeProperties(templateIs, jahiaTargetPropertiesFile.getPath());
            }
        }

        // Test the result file
        try (InputStream compareIs = new FileInputStream(jahiaPropertiesFile)) {
            Patch patch = getFileDiffs(compareIs, jahiaTargetPropertiesFile);
            assertEquals("Only one line should be modified", 1, patch.getDeltas().size());
        }

        Properties testProperties = new Properties();
        try (InputStream propIs = new FileInputStream(jahiaTargetPropertiesFile)) {
            testProperties.load(propIs);
        }
        assertEquals("Test property does not have proper value", "testPropertyValue\u2126", testProperties.getProperty("testPropertyName\u2126"));
        assertEquals("Backslash property does not have proper value", "c:\\Program Files (x86)\\SWFTools\\pdf2swf.exe", testProperties.getProperty("backslashTest"));
        assertEquals("Pattern property does not have a proper value", "[0-9a-z_A-Z\\-\\{\\}]+", testProperties.getProperty("userManagementGroupNamePattern"));
        assertEquals("Pattern property does not have a proper value", "[0-9a-z_A-Z\\-\\{\\}\\.@]+", testProperties.getProperty("userManagementUserNamePattern"));
    }

    public void testCommentingCase() throws IOException {
        try (InputStream is = new FileInputStream(jahiaPropertiesFile)) {
            PropertiesManager propertiesManager = new PropertiesManager(is);
            propertiesManager.setReplaceTabsWithSpaces(false);
            propertiesManager.setSanitizeValue(false);
            propertiesManager.setUnmodifiedCommentingActivated(true);

            propertiesManager.setProperty("server", "testServerValue");

            try (InputStream templateIs = new FileInputStream(jahiaPropertiesFile)) {
                propertiesManager.storeProperties(templateIs, jahiaTargetPropertiesFile.getPath());
            }
        }

        try (InputStream compareIs = new FileInputStream(jahiaPropertiesFile)) {
            Patch patch = getFileDiffs(compareIs, jahiaTargetPropertiesFile);
            // Adjust the expected delta count based on your actual file content
            assertEquals("Several lines should be modified", 82, patch.getDeltas().size());
        }

        Properties testProperties = new Properties();
        try (InputStream propIs = new FileInputStream(jahiaTargetPropertiesFile)) {
            testProperties.load(propIs);
        }
        assertEquals("Server property doesn't have proper value", "testServerValue", testProperties.getProperty("server"));
        assertNull("Operating mode property should be commented out", testProperties.getProperty("operatingMode"));

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(jahiaTargetPropertiesFile))) {
            String currentLine;
            boolean foundCommentedServerVersion = false;
            while ((currentLine = bufferedReader.readLine()) != null) {
                if (!currentLine.startsWith("#")) {
                    continue;
                }
                int equalPosition = currentLine.indexOf("=");
                if (equalPosition != -1) {
                    String currentPropertyName = currentLine.substring(1, equalPosition).trim();
                    if ("operatingMode".equals(currentPropertyName)) {
                        foundCommentedServerVersion = true;
                    }
                }
            }
            assertTrue("Server version property (serverVersion) should have been commented out!", foundCommentedServerVersion);
        }
    }

    public void testAdditionalProperties() throws IOException {
        try (InputStream is = new FileInputStream(jahiaPropertiesFile)) {
            PropertiesManager propertiesManager = new PropertiesManager(is);
            propertiesManager.setReplaceTabsWithSpaces(false);
            propertiesManager.setSanitizeValue(false);
            propertiesManager.setAdditionalPropertiesMessage("###ADDITIONALPROPERTIES###");
            propertiesManager.setProperty("testPropertyName", "testPropertyValue");

            try (InputStream templateIs = new FileInputStream(jahiaPropertiesFile)) {
                propertiesManager.storeProperties(templateIs, jahiaTargetPropertiesFile.getPath());
            }
        }

        try (InputStream compareIs = new FileInputStream(jahiaPropertiesFile)) {
            Patch patch = getFileDiffs(compareIs, jahiaTargetPropertiesFile);
            assertEquals("Only one line should be modified", 1, patch.getDeltas().size());
        }

        List<String> fileContents = getFileContents(jahiaTargetPropertiesFile);
        int additionalPropertiesIndex = indexOfInStringList(fileContents, "# ###ADDITIONALPROPERTIES###" );
        assertTrue("Couldn't find additional properties message in file", additionalPropertiesIndex > -1);

        int propertyIndex = indexOfPropertyInStringList(fileContents, "testPropertyName");
        assertTrue("Property should be after additional properties message in file", propertyIndex > additionalPropertiesIndex);
    }

    public void testRemoveProperty() throws IOException {
        try (InputStream is = new FileInputStream(jahiaPropertiesFile)) {
            PropertiesManager propertiesManager = new PropertiesManager(is);
            propertiesManager.setReplaceTabsWithSpaces(false);
            propertiesManager.setSanitizeValue(false);
            propertiesManager.removeProperty("operatingMode");

            try (InputStream templateIs = new FileInputStream(jahiaPropertiesFile)) {
                propertiesManager.storeProperties(templateIs, jahiaTargetPropertiesFile.getPath());
            }
        }

        try (InputStream compareIs = new FileInputStream(jahiaPropertiesFile)) {
            Patch patch = getFileDiffs(compareIs, jahiaTargetPropertiesFile);
            assertEquals("Only one line should be modified", 1, patch.getDeltas().size());
        }

        Properties testProperties = new Properties();
        try (InputStream propIs = new FileInputStream(jahiaTargetPropertiesFile)) {
            testProperties.load(propIs);
        }
        assertNull("Property operatingMode should have been removed from file !", testProperties.getProperty("operatingMode"));
    }

    public void testMultiValuedProperties() throws IOException {
        try (InputStream is = new FileInputStream(jahiaPropertiesFile)) {
            PropertiesManager propertiesManager = new PropertiesManager(is);
            propertiesManager.setReplaceTabsWithSpaces(false);
            propertiesManager.setSanitizeValue(false);
            propertiesManager.setProperty("multiValuedProperty", new String[] { "value1", "value2"} );
            Object multilinePropertyValue = propertiesManager.getRawProperty("multilineProperty");
            assertTrue("Multi line property is not of correct type", multilinePropertyValue instanceof String);
            assertEquals("Multi line property has incorrect number of values", "value1, value2", multilinePropertyValue);

            try (InputStream templateIs = new FileInputStream(jahiaPropertiesFile)) {
                propertiesManager.storeProperties(templateIs, jahiaTargetPropertiesFile.getPath());
            }
        }

        try (InputStream compareIs = new FileInputStream(jahiaPropertiesFile)) {
            Patch patch = getFileDiffs(compareIs, jahiaTargetPropertiesFile);
            assertEquals("Only one line should be modified", 1, patch.getDeltas().size());
        }

        Properties testProperties = new Properties();
        try (InputStream propIs = new FileInputStream(jahiaTargetPropertiesFile)) {
            testProperties.load(propIs);
        }
        assertEquals("Multi-valued property not saved properly", "value1value2", testProperties.getProperty("multiValuedProperty"));

        try (InputStream finalIs = new FileInputStream(jahiaTargetPropertiesFile)) {
            PropertiesManager propertiesManager = new PropertiesManager(finalIs);
            // Additional tests with the loaded properties manager if needed
        }
    }

    public void testSanitize() throws IOException {
        try (InputStream is = new FileInputStream(jahiaPropertiesFile)) {
            PropertiesManager propertiesManager = new PropertiesManager(is);

            try (InputStream templateIs = new FileInputStream(jahiaPropertiesFile)) {
                propertiesManager.storeProperties(templateIs, jahiaTargetPropertiesFile.getPath());
            }
        }

        String out = Files.readString(jahiaTargetPropertiesFile.toPath());

        assertFalse("Tabs were not replaced with spaces", out.contains("\t"));
        assertFalse("Values were not sanitized", out.contains("=  "));
    }

    private List<String> getFileContents(File inputFile) throws IOException {
        List<String> fileContents = new ArrayList<String>();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(jahiaTargetPropertiesFile));
        String currentLine;
        while ((currentLine = bufferedReader.readLine()) != null) {
            fileContents.add(currentLine);
        }
        bufferedReader.close();
        return fileContents;
    }

    private List<String> getFileContents(InputStream inputStream) throws IOException {
        List<String> fileContents = new ArrayList<String>();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String currentLine;
        while ((currentLine = bufferedReader.readLine()) != null) {
            fileContents.add(currentLine);
        }
        bufferedReader.close();
        return fileContents;
    }

    private int indexOfInStringList(List<String> stringList, String searchString) {
        int i=0;
        for (String currentString : stringList) {
            if (currentString.equals(searchString)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private int indexOfPropertyInStringList(List<String> stringList, String propertyName) {
        int i=0;
        for (String currentString : stringList) {
            if (currentString.startsWith("#")) {
                i++;
                continue;
            }
            int equalPosition = currentString.indexOf("=");
            if (equalPosition != -1) {
                String currentPropertyName = currentString.substring(0, equalPosition).trim();
                if (propertyName.equals(currentPropertyName)) {
                    return i;
                }
            }
            i++;
        }
        return -1;
    }

    public Patch getFileDiffs(InputStream originalFileInputStream, File revisedFile) throws IOException {
        List<String> original = getFileContents(originalFileInputStream);
        List<String> revised  = getFileContents(revisedFile);

        // Compute diff. Get the Patch object. Patch is the container for computed deltas.
        Patch patch = DiffUtils.diff(original, revised);

        System.out.println("File comparison between original and modified file:");
        List deltas = patch.getDeltas();
        for (Object delta : deltas) {
            System.out.println(delta);
        }
        return patch;
    }


}

