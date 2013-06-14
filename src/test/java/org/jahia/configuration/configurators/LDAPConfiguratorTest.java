package org.jahia.configuration.configurators;

import java.io.File;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;

/**
 * Test unit for LDAP configurator
 * User: loom
 * Date: 27.04.11
 * Time: 17:22
 */
public class LDAPConfiguratorTest extends AbstractXMLConfiguratorTestCase {
    @Override
    public void testUpdateConfiguration() throws Exception {
        File targetFolder = new File(FileUtils.getTempDirectory(), "ldap-config-test-" + System.currentTimeMillis());

        try {
            LDAPConfigurator ldapConfigurator = new LDAPConfigurator(oracleDBProperties, websphereOracleConfigBean);
            ldapConfigurator.updateConfiguration(null, targetFolder.getPath());
    
            // @todo we should validate the generated WAR file here.
            JarFile jarFile = new JarFile(new File(targetFolder, "ldap-config.jar"));
            try {
                Manifest manifest = jarFile.getManifest();
                assertEquals("LDAP configuration WAR should depend on LDAP connector module", manifest.getMainAttributes().getValue("Jahia-Depends"), "ldap");
            } finally {
                jarFile.close();
            }
        } finally {
            FileUtils.deleteDirectory(targetFolder);
        }
    }
}
