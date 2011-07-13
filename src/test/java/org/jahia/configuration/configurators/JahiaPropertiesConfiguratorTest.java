package org.jahia.configuration.configurators;

import java.io.InputStream;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.jahia.configuration.configurators.JahiaPropertiesConfigurator;
import org.jahia.configuration.logging.SLF4JLogger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for jahia.properties configurator
 *
 * @author loom
 *         Date: Oct 27, 2009
 *         Time: 9:02:29 AM
 */
public class JahiaPropertiesConfiguratorTest extends AbstractConfiguratorTestCase {

    public void testUpdateConfiguration() throws IOException {

        URL jahiaDefaultConfigJARURL = this.getClass().getClassLoader().getResource("jahia-default-config.jar");

        // Locate the Jar file
        FileSystemManager fsManager = VFS.getManager();
        ConfigFile jahiaPropertiesConfigFile = new VFSConfigFile(fsManager.resolveFile("jar:" + jahiaDefaultConfigJARURL.toExternalForm()), "org/jahia/defaults/config/properties/jahia.properties");

        File jahiaDefaultConfigFile = new File(jahiaDefaultConfigJARURL.getFile());
        String jahiaDefaultConfigFileParentPath = jahiaDefaultConfigFile.getParentFile().getPath() + File.separator;
        String targetJahiaPropertiesFile = jahiaDefaultConfigFileParentPath + "jahia.properties";
        String secondTargetJahiaPropertiesFile = jahiaDefaultConfigFileParentPath + "jahia2.properties";

        SLF4JLogger logger = new SLF4JLogger(LoggerFactory.getLogger(JahiaPropertiesConfiguratorTest.class));

        JahiaPropertiesConfigurator websphereOracleConfigurator = new JahiaPropertiesConfigurator(oracleDBProperties, websphereOracleConfigBean);
        websphereOracleConfigurator.updateConfiguration(jahiaPropertiesConfigFile, targetJahiaPropertiesFile);
        new JahiaAdvancedPropertiesConfigurator(logger, websphereOracleConfigBean).updateConfiguration(jahiaPropertiesConfigFile, targetJahiaPropertiesFile);
        Properties websphereOracleProperties = new Properties();
        websphereOracleProperties.load(new FileInputStream(jahiaDefaultConfigFileParentPath + "jahia.properties"));
        assertEquals("was", websphereOracleProperties.getProperty("server"));
        assertEquals("4", websphereOracleProperties.getProperty("cluster.tcp.num_initial_members"));
        assertEquals("1.2.3.4[7860],2.3.4.5[8860],3.4.5.6[9860],4.5.6.7[10860]", websphereOracleProperties.getProperty("cluster.tcp.ehcache.hibernate.nodes.ip_address"));
        assertEquals("1.2.3.4[7870],2.3.4.5[8870],3.4.5.6[9870],4.5.6.7[10870]", websphereOracleProperties.getProperty("cluster.tcp.ehcache.jahia.nodes.ip_address"));

        JahiaPropertiesConfigurator tomcatMySQLConfigurator = new JahiaPropertiesConfigurator(mysqlDBProperties, tomcatMySQLConfigBean);
        tomcatMySQLConfigurator.updateConfiguration(new VFSConfigFile(fsManager, targetJahiaPropertiesFile), secondTargetJahiaPropertiesFile);
        new JahiaAdvancedPropertiesConfigurator(logger, tomcatMySQLConfigBean).updateConfiguration(new VFSConfigFile(fsManager, secondTargetJahiaPropertiesFile), secondTargetJahiaPropertiesFile);
        Properties tomcatMySQLProperties = new Properties();
        tomcatMySQLProperties.load(new FileInputStream(secondTargetJahiaPropertiesFile));
        assertEquals("tomcat", tomcatMySQLProperties.getProperty("server"));
    }
}
