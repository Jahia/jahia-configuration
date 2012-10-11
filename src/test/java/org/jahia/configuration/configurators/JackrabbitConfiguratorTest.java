package org.jahia.configuration.configurators;

import java.net.URL;
import java.io.File;

import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.codehaus.plexus.util.FileUtils;
import org.jahia.configuration.configurators.JackrabbitConfigurator;
import org.jahia.configuration.logging.AbstractLogger;
import org.jahia.configuration.logging.ConsoleLogger;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.jdom.Document;

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
        jdomDocument = saxBuilder.build(repositoryFileParentPath + "repository-modified2.xml");
        assertAllTextEquals(jdomDocument, "//param[@name=\"databaseType\"]/@value", prefix, mysqlDBProperties.getProperty("jahia.jackrabbit.schema"));
        assertNotNull(getNode(jdomDocument, "//Cluster/Journal", prefix));

        assertAllTextEquals(jdomDocument, "/Repository/FileSystem/@class", prefix, mysqlDBProperties.getProperty("jahia.jackrabbit.filesystem"));
        assertAllTextEquals(jdomDocument, "//PersistenceManager/@class", prefix, mysqlDBProperties.getProperty("jahia.jackrabbit.persistence"));

        new JackrabbitConfigurator(mysqlDBProperties, tomcatMySQLConfigBean, logger).updateConfiguration(new VFSConfigFile(fsManager, repositoryFileParentPath + "repository-modified2.xml"), repositoryFileParentPath + "repository-modified-store.xml");
        saxBuilder = new SAXBuilder();
        jdomDocument = saxBuilder.build(repositoryFileParentPath + "repository-modified-store.xml");
        assertTrue(XPath.selectNodes(jdomDocument, "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.db.DbDataStore\"]").isEmpty());
        assertNotNull(XPath.selectSingleNode(jdomDocument, "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.FileDataStore\"]"));
        
        mysqlDBProperties.setProperty("storeFilesInDB", "true");
        new JackrabbitConfigurator(mysqlDBProperties, tomcatMySQLConfigBean, logger).updateConfiguration(new VFSConfigFile(fsManager, repositoryFileParentPath + "repository-modified-store.xml"), repositoryFileParentPath + "repository-modified-store-db.xml");
        saxBuilder = new SAXBuilder();
        jdomDocument = saxBuilder.build(repositoryFileParentPath + "repository-modified-store-db.xml");
        assertTrue(XPath.selectNodes(jdomDocument, "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.FileDataStore\"]").isEmpty());
        assertNotNull(XPath.selectSingleNode(jdomDocument, "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.db.DbDataStore\"]"));
    }
}
