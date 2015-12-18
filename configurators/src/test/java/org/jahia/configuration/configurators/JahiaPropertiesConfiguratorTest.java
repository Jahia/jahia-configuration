/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
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
        new JahiaNodePropertiesConfigurator(logger, websphereOracleConfigBean).updateConfiguration(jahiaPropertiesConfigFile, targetJahiaPropertiesFile);
        Properties websphereOracleProperties = new Properties();
        FileInputStream inStream = new FileInputStream(jahiaDefaultConfigFileParentPath + "jahia.advanced.properties");
        try {
            websphereOracleProperties.load(inStream);
        } finally {
            IOUtils.closeQuietly(inStream);
        }
        
        // test for the additional properties
        assertEquals("true", websphereOracleProperties.getProperty("jahia.dm.viewer.enabled"));
        assertEquals("c:\\Program Files (x86)\\SWFTools\\pdf2swf.exe", websphereOracleProperties.getProperty("jahia.dm.viewer.pdf2swf"));
        assertNotNull(websphereOracleProperties.getProperty("auth.spnego.bypassForUrls"));

        JahiaPropertiesConfigurator tomcatMySQLConfigurator = new JahiaPropertiesConfigurator(mysqlDBProperties, tomcatMySQLConfigBean);
        tomcatMySQLConfigurator.updateConfiguration(new VFSConfigFile(fsManager, targetJahiaPropertiesFile), secondTargetJahiaPropertiesFile);
        new JahiaNodePropertiesConfigurator(logger, tomcatMySQLConfigBean).updateConfiguration(new VFSConfigFile(fsManager, secondTargetJahiaPropertiesFile), secondTargetJahiaPropertiesFile);
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
