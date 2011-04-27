package org.jahia.configuration.configurators;

import java.io.File;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Test unit for LDAP configurator
 * User: loom
 * Date: 27.04.11
 * Time: 17:22
 * To change this template use File | Settings | File Templates.
 */
public class LDAPConfiguratorTest extends AbstractXMLConfiguratorTestCase {
    @Override
    public void testUpdateConfiguration() throws Exception {
        URL indexationPolicyURL = this.getClass().getClassLoader().getResource("configurators/WEB-INF/etc/spring/applicationcontext-indexationpolicy.xml");
        File indexationPolicyFile = new File(indexationPolicyURL.getFile());
        String indexationPolicyFileParentPath = indexationPolicyFile.getParentFile().getPath() + File.separator;

        LDAPConfigurator websphereOracleConfigurator = new LDAPConfigurator(oracleDBProperties, websphereOracleConfigBean);
        websphereOracleConfigurator.updateConfiguration(indexationPolicyFile.toString(), indexationPolicyFileParentPath);

        // @todo we should validate the generated WAR file here.
        JarFile jarFile = new JarFile(indexationPolicyFileParentPath + "ldap-config.war");
        Manifest manifest = jarFile.getManifest();
        assertEquals("LDAP configuration WAR should depend on LDAP connector module", manifest.getMainAttributes().getValue("depends"), "Jahia LDAP connector");
    }
}
