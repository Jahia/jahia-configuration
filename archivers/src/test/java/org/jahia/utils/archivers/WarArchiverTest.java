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

    public void testCreateJarArchive() throws IOException, URISyntaxException {
        WarArchiver warArchiver = new WarArchiver();

        boolean succeeded = true;
        File newWar = new File("target/directoryArchive.war");

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

    }

    private void validateJarArchive(File archive, File directory) throws IOException {
        JarInputStream jarInputStream = new JarInputStream(new FileInputStream(archive));
        JarEntry jarEntry = null;
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            logger.debug("Testing entry " + jarEntry.getName());
        }
    }
}
