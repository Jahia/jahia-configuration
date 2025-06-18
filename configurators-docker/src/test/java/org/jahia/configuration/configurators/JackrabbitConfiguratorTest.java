/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2023 Jahia Solutions Group SA. All rights reserved.
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

import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.jahia.configuration.logging.AbstractLogger;
import org.jahia.configuration.logging.ConsoleLogger;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPath;

import java.io.File;
import java.net.URL;

/**
 * Test unit for the Jackrabbit configurator
 *
 * @author loom
 *         Date: Oct 20, 2009
 *         Time: 3:11:30 PM
 */
public class JackrabbitConfiguratorTest extends AbstractXMLConfiguratorTestCase {

    public void testUpdateConfiguration () throws Exception {
        URL repositoryURL = this.getClass().getClassLoader().getResource("configurators/WEB-INF/etc/repository/jackrabbit/repository.xml");
        FileSystemManager fsManager = VFS.getManager();
        AbstractLogger logger = new ConsoleLogger(ConsoleLogger.LEVEL_INFO);

        File repositoryFile = new File(repositoryURL.getFile());
        String repositoryFileParentPath = repositoryFile.getParentFile().getPath() + File.separator;

        oracleDBProperties.setProperty("storeFilesInDB", "true");
        JackrabbitConfigurator websphereOracleJackrabbitConfigurator = new JackrabbitConfigurator(oracleDBProperties, websphereOracleConfigBean, logger);
        websphereOracleJackrabbitConfigurator.updateConfiguration(new VFSConfigFile(fsManager, repositoryURL.toExternalForm()), repositoryFileParentPath + "repository-modified.xml");

        // The following tests are NOT exhaustive
        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Document jdomDocument = saxBuilder.build(repositoryFileParentPath + "repository-modified.xml");
        String prefix = "";
        String tagPre = ("".equals(prefix) ? "" : prefix + ":");
        assertAllTextEquals(jdomDocument, "//param[@name=\"databaseType\"]/@value", prefix, oracleDBProperties.getProperty("jahia.jackrabbit.schema"));
        checkOnlyOneElement(jdomDocument, "//Cluster/Journal", prefix);
        checkOnlyOneElement(jdomDocument, "/Repository/Cluster/Journal/param[@name=\"janitorEnabled\"]", prefix);

        assertAllTextEquals(jdomDocument, "//param[@name=\"externalBLOBs\"]/@value", prefix, "false");

        assertAllTextEquals(jdomDocument, "/Repository/FileSystem/@class", prefix, oracleDBProperties.getProperty("jahia.jackrabbit.filesystem"));
        assertAllTextEquals(jdomDocument, "//PersistenceManager/@class", prefix, oracleDBProperties.getProperty("jahia.jackrabbit.persistence"));

        mysqlDBProperties.setProperty("storeFilesInDB", "false");
        JackrabbitConfigurator tomcatMySQLJackrabbitConfigurator = new JackrabbitConfigurator(mysqlDBProperties, tomcatMySQLConfigBean, logger);
        tomcatMySQLJackrabbitConfigurator.updateConfiguration(new VFSConfigFile(fsManager, repositoryFileParentPath + "repository-modified.xml"), repositoryFileParentPath + "repository-modified2.xml");

        // The following tests are NOT exhaustive
        saxBuilder = new SAXBuilder();
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        jdomDocument = saxBuilder.build(repositoryFileParentPath + "repository-modified2.xml");
        assertAllTextEquals(jdomDocument, "//param[@name=\"databaseType\"]/@value", prefix, mysqlDBProperties.getProperty("jahia.jackrabbit.schema"));
        assertNotNull(getNode(jdomDocument, "//Cluster/Journal", prefix));

        assertAllTextEquals(jdomDocument, "/Repository/FileSystem/@class", prefix, mysqlDBProperties.getProperty("jahia.jackrabbit.filesystem"));
        assertAllTextEquals(jdomDocument, "//PersistenceManager/@class", prefix, mysqlDBProperties.getProperty("jahia.jackrabbit.persistence"));

        new JackrabbitConfigurator(mysqlDBProperties, tomcatMySQLConfigBean, logger).updateConfiguration(new VFSConfigFile(fsManager, repositoryFileParentPath + "repository-modified2.xml"), repositoryFileParentPath + "repository-modified-store.xml");
        saxBuilder = new SAXBuilder();
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        jdomDocument = saxBuilder.build(repositoryFileParentPath + "repository-modified-store.xml");
        assertTrue(XPath.selectNodes(jdomDocument, "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.db.DbDataStore\"]").isEmpty());
        assertNotNull(XPath.selectSingleNode(jdomDocument, "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.FileDataStore\"]"));

        mysqlDBProperties.setProperty("storeFilesInDB", "true");
        new JackrabbitConfigurator(mysqlDBProperties, tomcatMySQLConfigBean, logger).updateConfiguration(new VFSConfigFile(fsManager, repositoryFileParentPath + "repository-modified-store.xml"), repositoryFileParentPath + "repository-modified-store-db.xml");
        saxBuilder = new SAXBuilder();
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        jdomDocument = saxBuilder.build(repositoryFileParentPath + "repository-modified-store-db.xml");
        assertTrue(XPath.selectNodes(jdomDocument, "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.FileDataStore\"]").isEmpty());
        assertNotNull(XPath.selectSingleNode(jdomDocument, "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.db.DbDataStore\"]"));
    }
}
