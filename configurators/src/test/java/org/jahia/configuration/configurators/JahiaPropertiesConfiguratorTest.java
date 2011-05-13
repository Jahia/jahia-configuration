package org.jahia.configuration.configurators;

import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.Properties;

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
        URL jahiaSkeletonURL = this.getClass().getClassLoader().getResource("configurators/WEB-INF/etc/config/jahia.skeleton");
        File jahiaSkeletonFile = new File(jahiaSkeletonURL.getFile());
        String jahiaSkeletonFileParentPath = jahiaSkeletonFile.getParentFile().getPath() + File.separator;

        SLF4JLogger logger = new SLF4JLogger(LoggerFactory.getLogger(JahiaPropertiesConfiguratorTest.class));

        JahiaPropertiesConfigurator websphereOracleConfigurator = new JahiaPropertiesConfigurator(oracleDBProperties, websphereOracleConfigBean);
        websphereOracleConfigurator.updateConfiguration(jahiaSkeletonFile.toString(), jahiaSkeletonFileParentPath + "jahia.properties");
        new JahiaAdvancedPropertiesConfigurator(logger, websphereOracleConfigBean).updateConfiguration(jahiaSkeletonFileParentPath + "jahia.properties", jahiaSkeletonFileParentPath + "jahia.properties");
        Properties websphereOracleProperties = new Properties();
        websphereOracleProperties.load(new FileInputStream(jahiaSkeletonFileParentPath + "jahia.properties"));
        assertEquals("was", websphereOracleProperties.getProperty("server"));
        assertEquals("4", websphereOracleProperties.getProperty("cluster.tcp.num_initial_members"));
        assertEquals("1.2.3.4[7860],2.3.4.5[8860],3.4.5.6[9860],4.5.6.7[10860]", websphereOracleProperties.getProperty("cluster.tcp.ehcache.hibernate.nodes.ip_address"));
        assertEquals("1.2.3.4[7870],2.3.4.5[8870],3.4.5.6[9870],4.5.6.7[10870]", websphereOracleProperties.getProperty("cluster.tcp.ehcache.jahia.nodes.ip_address"));

        JahiaPropertiesConfigurator tomcatMySQLConfigurator = new JahiaPropertiesConfigurator(mysqlDBProperties, tomcatMySQLConfigBean);
        tomcatMySQLConfigurator.updateConfiguration(jahiaSkeletonFileParentPath + "jahia.properties", jahiaSkeletonFileParentPath + "jahia2.properties");
        new JahiaAdvancedPropertiesConfigurator(logger, tomcatMySQLConfigBean).updateConfiguration(jahiaSkeletonFileParentPath + "jahia2.properties", jahiaSkeletonFileParentPath + "jahia2.properties");
        Properties tomcatMySQLProperties = new Properties();
        tomcatMySQLProperties.load(new FileInputStream(jahiaSkeletonFileParentPath + "jahia2.properties"));
        assertEquals("tomcat", tomcatMySQLProperties.getProperty("server"));
    }
}
