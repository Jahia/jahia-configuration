/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2013 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
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

        assertTrue(content.contains("<driver name=\"jahia.oracle\" module=\"org.jahia.jdbc.oracle\" />"));
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
        assertTrue(content.contains("<driver name=\"jahia.mysql\" module=\"org.jahia.jdbc.mysql\" />"));
        assertTrue(content.contains("<connection-url>jdbc:mysql"));
        assertFalse(content.contains("<connection-url>jdbc:oracle"));

    }

}
