package org.jahia.configuration.configurators;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Test unit for LDAP configurator
 * User: loom
 * Date: 27.04.11
 * Time: 17:22
 */
public class LDAPConfiguratorTest extends AbstractConfiguratorTestCase {
    @Override
    public void testUpdateConfiguration() throws Exception {
        File targetFolder = new File(FileUtils.getTempDirectory(), "ldap-config-test-" + System.currentTimeMillis());
        FileInputStream inStream = null;
        try {
            LDAPConfigurator ldapConfigurator = new LDAPConfigurator(oracleDBProperties, websphereOracleConfigBean);
            ldapConfigurator.updateConfiguration(null, targetFolder.getPath());
    
            Properties ldapProperties = new Properties();
            inStream = new FileInputStream(new File(targetFolder, "org.jahia.services.usermanager.ldap-config.cfg"));
            ldapProperties.load(inStream);
            assertEquals("ldap://10.8.37.17:389/", ldapProperties.getProperty("user.url"));
            assertEquals("dc=jahia", ldapProperties.getProperty("group.search.name"));
            assertEquals("givenName", ldapProperties.getProperty("user.j:firstName.attribute.map"));
        } finally {
            IOUtils.closeQuietly(inStream);
            FileUtils.deleteDirectory(targetFolder);
        }
    }
}
