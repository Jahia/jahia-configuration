package org.jahia.utils.archivers;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Test unit for the WAR archiver
 *
 * @author loom
 *         Date: May 18, 2010
 *         Time: 3:08:58 PM
 */
public class WarArchiverTest extends TestCase {

    private static final Logger logger = LoggerFactory.getLogger(WarArchiverTest.class);

    @Override
    protected void setUp() throws Exception {
        super.setUp();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public void testCreateJarArchive() throws IOException, URISyntaxException {
        WarArchiver warArchiver = new WarArchiver();

        boolean succeeded = true;

        File targetDirectory = new File("target");
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }

        File newWar = new File("target/directoryArchive.war");
        if (newWar.exists()) {
            newWar.delete();
        }

        URL packagersURL = this.getClass().getClassLoader().getResource("packagers");
        File packagersFile = new File(packagersURL.toURI());

        try {
            warArchiver.createJarArchive(newWar, new File("nonExistantDirectory"));
        } catch (IOException e) {
            succeeded = false;
        }
        if (!newWar.exists()) {
            succeeded = false;
        }
        assertFalse("Compressing non existant directory was created !", succeeded);
        newWar.delete();
                
        warArchiver.createJarArchive(newWar, packagersFile);

        validateJarArchive(newWar, packagersFile);

        newWar.delete();

    }

    private void validateJarArchive(File archive, File directory) throws IOException {
        JarInputStream jarInputStream = new JarInputStream(new FileInputStream(archive));
        JarEntry jarEntry = null;
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            logger.info("Testing entry " + jarEntry.getName());
            File testFile = new File(directory, jarEntry.getName());
            assertTrue("File " + testFile.toString() + " could not be found in location " + testFile.getAbsolutePath(), testFile.exists());
            assertEquals("File " + testFile.toString() + " and JAR entry type differs (isDirectory) ", testFile.isDirectory(), jarEntry.isDirectory());
            if (!jarEntry.isDirectory()) {
                // assertEquals("File " + testFile.toString() + " and JAR entry differ in size", testFile.length(), jarEntry.getSize());
            }
        }
    }
}
