package org.jahia.configuration.configurators;

import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
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

        jahiaPropertiesConfigFile = new VFSConfigFile(fsManager.resolveFile("jar:" + jahiaDefaultConfigJARURL.toExternalForm()), "org/jahia/defaults/config/properties/jahia.advanced.properties");
        targetJahiaPropertiesFile = jahiaDefaultConfigFileParentPath + "jahia.advanced.properties";
        JahiaPropertiesConfigurator websphereOracleConfigurator = new JahiaPropertiesConfigurator(oracleDBProperties, websphereOracleConfigBean);
        websphereOracleConfigurator.updateConfiguration(jahiaPropertiesConfigFile, targetJahiaPropertiesFile);
        new JahiaAdvancedPropertiesConfigurator(logger, websphereOracleConfigBean).updateConfiguration(jahiaPropertiesConfigFile, targetJahiaPropertiesFile);
        Properties websphereOracleProperties = new Properties();
        FileInputStream inStream = new FileInputStream(jahiaDefaultConfigFileParentPath + "jahia.advanced.properties");
        try {
            websphereOracleProperties.load(inStream);
        } finally {
            IOUtils.closeQuietly(inStream);
        }
        //assertEquals("was", websphereOracleProperties.getProperty("server"));
        assertEquals("4", websphereOracleProperties.getProperty("cluster.tcp.num_initial_members"));
        assertEquals("1.2.3.4[7860],2.3.4.5[8860],3.4.5.6[9860],4.5.6.7[10860]", websphereOracleProperties.getProperty("cluster.tcp.ehcache.hibernate.nodes.ip_address"));
        assertEquals("1.2.3.4[7870],2.3.4.5[8870],3.4.5.6[9870],4.5.6.7[10870]", websphereOracleProperties.getProperty("cluster.tcp.ehcache.jahia.nodes.ip_address"));
        
        // test for the additional properties
        assertEquals("true", websphereOracleProperties.getProperty("jahia.dm.viewer.enabled"));
        assertEquals("c:\\Program Files (x86)\\SWFTools\\pdf2swf.exe", websphereOracleProperties.getProperty("jahia.dm.viewer.pdf2swf"));
        assertNotNull(websphereOracleProperties.getProperty("auth.spnego.bypassForUrls"));

        JahiaPropertiesConfigurator tomcatMySQLConfigurator = new JahiaPropertiesConfigurator(mysqlDBProperties, tomcatMySQLConfigBean);
        tomcatMySQLConfigurator.updateConfiguration(new VFSConfigFile(fsManager, targetJahiaPropertiesFile), secondTargetJahiaPropertiesFile);
        new JahiaAdvancedPropertiesConfigurator(logger, tomcatMySQLConfigBean).updateConfiguration(new VFSConfigFile(fsManager, secondTargetJahiaPropertiesFile), secondTargetJahiaPropertiesFile);
        Properties tomcatMySQLProperties = new Properties();
        inStream = new FileInputStream(secondTargetJahiaPropertiesFile);
        try {
            tomcatMySQLProperties.load(inStream);
        } finally {
            IOUtils.closeQuietly(inStream);
        }
        //assertEquals("tomcat", tomcatMySQLProperties.getProperty("server"));
    }
}
