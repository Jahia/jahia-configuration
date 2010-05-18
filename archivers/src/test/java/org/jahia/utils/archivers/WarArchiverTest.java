package org.jahia.utils.archivers;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Test unit for the WAR archiver
 *
 * @author loom
 *         Date: May 18, 2010
 *         Time: 3:08:58 PM
 */
public class WarArchiverTest extends TestCase {

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
}
