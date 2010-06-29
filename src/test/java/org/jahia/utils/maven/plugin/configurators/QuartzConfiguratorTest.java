package org.jahia.utils.maven.plugin.configurators;

import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * Simple test unit to validate Quartz configurator
 *
 * @author loom
 *         Date: Oct 26, 2009
 *         Time: 11:22:04 AM
 */
public class QuartzConfiguratorTest extends AbstractConfiguratorTestCase {

    public void testUpdateConfiguration() throws IOException {
//        URL quartzConfigurationURL = this.getClass().getClassLoader().getResource("configurators/WEB-INF/etc/config/quartz.properties");
//        File quartzConfigurationFile = new File(quartzConfigurationURL.getFile());
//        String quartzConfigurationFileParentPath = quartzConfigurationFile.getParentFile().getPath() + File.separator;
//
//        QuartzConfigurator websphereOracleQuartzConfigurator = new QuartzConfigurator(oracleDBProperties, websphereOracleConfigBean);
//        websphereOracleQuartzConfigurator.updateConfiguration(quartzConfigurationFile.toString(), quartzConfigurationFileParentPath + "quartz-modified.properties");
//
//        Properties websphereProperties = new Properties();
//        websphereProperties.load(new FileInputStream(quartzConfigurationFileParentPath + "quartz-modified.properties"));
//        assertEquals("java:comp/env/jdbc/jahiaNonTx", websphereProperties.getProperty("org.quartz.dataSource.jahiaNonTxDS.jndiURL"));
//        if (oracleDBProperties.getProperty("jahia.quartz.selectWithLockSQL") != null) {
//            assertEquals(oracleDBProperties.getProperty("jahia.quartz.selectWithLockSQL"), websphereProperties.getProperty("org.quartz.jobStore.selectWithLockSQL"));            
//        } else {
//            assertEquals("", websphereProperties.getProperty("org.quartz.jobStore.selectWithLockSQL"));                        
//        }
//        // TODO complete validation checks.
//
//        QuartzConfigurator  tomcatMySQLQuartzConfigurator = new QuartzConfigurator(mysqlDBProperties, tomcatMySQLConfigBean);
//        tomcatMySQLQuartzConfigurator.updateConfiguration(quartzConfigurationFileParentPath + "quartz-modified.properties", quartzConfigurationFileParentPath + "quartz-modified2.properties");
//
//        // TODO implement validation checks.
//        Properties tomcatProperties = new Properties();
//        tomcatProperties.load(new FileInputStream(quartzConfigurationFileParentPath + "quartz-modified2.properties"));
//        assertNull(tomcatProperties.getProperty("org.quartz.dataSource.jahiaNonTxDS.jndiURL"));
//        if (mysqlDBProperties.getProperty("jahia.quartz.selectWithLockSQL") != null) {
//            assertEquals(mysqlDBProperties.getProperty("jahia.quartz.selectWithLockSQL"), tomcatProperties.getProperty("org.quartz.jobStore.selectWithLockSQL"));
//        } else {
//            assertEquals("", tomcatProperties.getProperty("org.quartz.jobStore.selectWithLockSQL"));            
//        }
    }
}
