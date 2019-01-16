/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
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

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.jahia.configuration.logging.AbstractLogger;
import org.jahia.configuration.logging.ConsoleLogger;

/**
 * Unit test for the database configuration in JBoss.
 * 
 * @author Sergiy Shyrkov
 */
public class JBossConfiguratorTest extends AbstractXMLConfiguratorTestCase {

    @Override
    public void testUpdateConfiguration() throws Exception {
        FileSystemManager fsManager = VFS.getManager();
        AbstractLogger logger = new ConsoleLogger(ConsoleLogger.LEVEL_INFO);

        URL cfgFileUrl = this.getClass().getClassLoader().getResource("configurators/jboss/standalone.xml");
        File sourceCfgFile = new File(cfgFileUrl.getFile());

        JBossConfigurator configurator = new JBossConfigurator(oracleDBProperties,
                websphereOracleConfigBean, null, logger);
        String destFileName = sourceCfgFile.getParent() + "/standalone-modified.xml";
        configurator.updateConfiguration(new VFSConfigFile(fsManager, cfgFileUrl.toExternalForm()), destFileName);

        String content = FileUtils.readFileToString(new File(destFileName));

        assertTrue(content.contains("<driver name=\"jahia.oracle\" module=\"org.jahia.jdbc.oracle\">"));
        assertTrue(content.contains("<driver-class>oracle.jdbc.OracleDriver</driver-class>"));
        assertTrue(content.contains("<datasource jndi-name=\"java:/jahiaDS\""));
        assertTrue(content.contains("<connection-url>jdbc:oracle"));
        
        assertTrue(content.contains("enable-welcome-root=\"false\""));

        String unchangedDestFileName = sourceCfgFile.getParent() + "/standalone-modified-unchanged.xml";
        configurator.updateConfiguration(new VFSConfigFile(fsManager, destFileName), unchangedDestFileName);

        assertTrue(FileUtils.contentEquals(new File(destFileName), new File(unchangedDestFileName)));

        configurator = new JBossConfigurator(mysqlDBProperties, tomcatMySQLConfigBean, null, logger);
        String updatedDestFileName = sourceCfgFile.getParent() + "/standalone-modified-updated.xml";
        configurator.updateConfiguration(new VFSConfigFile(fsManager, unchangedDestFileName), updatedDestFileName);

        assertFalse(FileUtils.contentEquals(new File(unchangedDestFileName), new File(updatedDestFileName)));
        
        content = FileUtils.readFileToString(new File(updatedDestFileName));
        assertTrue(content.contains("<driver name=\"jahia.mysql\" module=\"org.jahia.jdbc.mysql\">"));
        assertTrue(content.contains("<driver-class>com.mysql.jdbc.Driver</driver-class>"));
        assertTrue(content.contains("<connection-url>jdbc:mysql"));
        assertFalse(content.contains("<connection-url>jdbc:oracle"));
        assertTrue(content.contains("<connector name=\"http\" protocol=\"org.apache.coyote.http11.Http11NioProtocol\" scheme=\"http\" socket-binding=\"http\" />"));
    }

}
