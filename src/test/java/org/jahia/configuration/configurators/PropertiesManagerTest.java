package org.jahia.configuration.configurators;

import junit.framework.TestCase;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Test case for Properties Manager
 */
public class PropertiesManagerTest extends TestCase {

    private URL jahiaDefaultConfigJARURL;
    private JarEntry jahiaPropertiesJarEntry;
    private JarFile jahiaDefaultConfigJarFile;
    private String jahiaDefaultConfigFileParentPath;
    private File jahiaTargetPropertiesFile;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        jahiaDefaultConfigJARURL = this.getClass().getClassLoader().getResource("jahia-default-config.jar");
        File jahiaDefaultConfigFile = new File(jahiaDefaultConfigJARURL.getFile());
        jahiaDefaultConfigJarFile = new JarFile(jahiaDefaultConfigFile);
        jahiaPropertiesJarEntry = jahiaDefaultConfigJarFile.getJarEntry("org/jahia/defaults/config/properties/jahia.properties");
        jahiaDefaultConfigFileParentPath = jahiaDefaultConfigFile.getParentFile().getPath() + File.separator;
        jahiaTargetPropertiesFile = new File(jahiaDefaultConfigFileParentPath + "/jahia.properties");
        if (jahiaTargetPropertiesFile.exists()) {
            jahiaTargetPropertiesFile.delete();
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        jahiaTargetPropertiesFile.delete();
    }

    public void testLoadBasicCase() throws IOException {
        PropertiesManager propertiesManager = new PropertiesManager(jahiaDefaultConfigJarFile.getInputStream(jahiaPropertiesJarEntry));

//        propertiesManager.setProperty("server", "testServerValue");
        propertiesManager.setProperty("testPropertyName", "testPropertyValue");
        propertiesManager.storeProperties(jahiaDefaultConfigJarFile.getInputStream(jahiaPropertiesJarEntry), jahiaTargetPropertiesFile.getPath());

        Properties testProperties = new Properties();
        testProperties.load(new FileInputStream(jahiaTargetPropertiesFile));
//        assertEquals("Server property does not have proper value", "testServerValue", testProperties.getProperty("server"));
        assertEquals("Test property does not have proper value", "testPropertyValue", testProperties.getProperty("testPropertyName"));
        //assertEquals("Server version property (serverVersion) doesn't have default value !", "", testProperties.getProperty("serverVersion"));
    }

    public void testCommentingCase() throws IOException {

//        PropertiesManager propertiesManager = new PropertiesManager(jahiaDefaultConfigJarFile.getInputStream(jahiaPropertiesJarEntry));
//        propertiesManager.setUnmodifiedCommentingActivated(true);
//
////        propertiesManager.setProperty("server", "testServerValue");
//        propertiesManager.storeProperties(jahiaDefaultConfigJarFile.getInputStream(jahiaPropertiesJarEntry), jahiaTargetPropertiesFile.getPath());
//
//        Properties testProperties = new Properties();
//        testProperties.load(new FileInputStream(jahiaTargetPropertiesFile));
////        assertEquals("Server property doesn't have proper value", "testServerValue", testProperties.getProperty("server"));
//
//        BufferedReader bufferedReader = new BufferedReader(new FileReader(jahiaTargetPropertiesFile));
//
//        String currentLine;
//        boolean foundCommentedServerVersion = false;
//        while ((currentLine = bufferedReader.readLine()) != null) {
//            if (!currentLine.startsWith("#")) {
//                continue;
//            }
//            int equalPosition = currentLine.indexOf("=");
//            if (equalPosition != -1) {
//                String currentPropertyName = currentLine.substring(1, equalPosition).trim();
//                if ("serverVersion".equals(currentPropertyName)) {
//                    foundCommentedServerVersion = true;
//                }
//            }
//        }
//        bufferedReader.close();
//        assertTrue("Server version property (serverVersion) should have been commented out!", foundCommentedServerVersion);

    }

    public void testAdditionalProperties() throws IOException {
        PropertiesManager propertiesManager = new PropertiesManager(jahiaDefaultConfigJarFile.getInputStream(jahiaPropertiesJarEntry));
        propertiesManager.setProperty("testPropertyName", "testPropertyValue");
        propertiesManager.setAdditionalPropertiesMessage("###ADDITIONALPROPERTIES###");
        propertiesManager.storeProperties(jahiaDefaultConfigJarFile.getInputStream(jahiaPropertiesJarEntry), jahiaTargetPropertiesFile.getPath());

        List<String> fileContents = getFileContents(jahiaTargetPropertiesFile);
        int additionalPropertiesIndex = indexOfInStringList(fileContents, "# ###ADDITIONALPROPERTIES###" );
        assertTrue("Couldn't find additional properties message in file", additionalPropertiesIndex > -1);

        int propertyIndex = indexOfPropertyInStringList(fileContents, "testPropertyName");
        assertTrue("Property should be after additional properties message in file", propertyIndex > additionalPropertiesIndex);
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

}
