package org.jahia.utils.maven.plugin.configurators;

import java.net.URL;
import java.io.File;
import java.util.List;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.xpath.XPath;

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
        File repositoryFile = new File(repositoryURL.getFile());
        String repositoryFileParentPath = repositoryFile.getParentFile().getPath() + File.separator;

        oracleDBProperties.setProperty("storeFilesInDB", "true");
        JackrabbitConfigurator websphereOracleJackrabbitConfigurator = new JackrabbitConfigurator(oracleDBProperties, websphereOraclePropertiesBean);
        websphereOracleJackrabbitConfigurator.updateConfiguration(repositoryFile.toString(), repositoryFileParentPath + "repository-modified.xml");

        // The following tests are NOT exhaustive
        SAXBuilder saxBuilder = new SAXBuilder();
        Document jdomDocument = saxBuilder.build(repositoryFileParentPath + "repository-modified.xml");
        String prefix = "";
        String tagPre = ("".equals(prefix) ? "" : prefix + ":");
        assertAllTextEquals(jdomDocument, "//param[@name=\"user\"]/@value", prefix, oracleDBProperties.getProperty("jahia.database.user"));
        assertAllTextEquals(jdomDocument, "//param[@name=\"password\"]/@value", prefix, oracleDBProperties.getProperty("jahia.database.pass"));
        assertAllTextEquals(jdomDocument, "//param[@name=\"url\"]/@value", prefix, oracleDBProperties.getProperty("jahia.database.url"));
        assertAllTextEquals(jdomDocument, "//param[@name=\"schema\"]/@value", prefix, oracleDBProperties.getProperty("jahia.jackrabbit.schema"));
        checkOnlyOneElement(jdomDocument, "//Cluster/Journal", prefix);
        checkOnlyOneElement(jdomDocument, "/Repository/Cluster/Journal/param[@name=\"janitorEnabled\"]", prefix);

        assertAllTextEquals(jdomDocument, "//param[@name=\"externalBLOBs\"]/@value", prefix, "false");

        assertAllTextEquals(jdomDocument, "//FileSystem/@class", prefix, oracleDBProperties.getProperty("jahia.jackrabbit.filesystem"));
        assertAllTextEquals(jdomDocument, "//PersistenceManager/@class", prefix, oracleDBProperties.getProperty("jahia.jackrabbit.persistence"));

        mysqlDBProperties.setProperty("storeFilesInDB", "false");
        JackrabbitConfigurator tomcatMySQLJackrabbitConfigurator = new JackrabbitConfigurator(mysqlDBProperties, tomcatMySQLPropertiesBean);
        tomcatMySQLJackrabbitConfigurator.updateConfiguration(repositoryFileParentPath + "repository-modified.xml", repositoryFileParentPath + "repository-modified2.xml");

        // The following tests are NOT exhaustive
        saxBuilder = new SAXBuilder();
        jdomDocument = saxBuilder.build(repositoryFileParentPath + "repository-modified2.xml");
        assertNull(getNode(jdomDocument, "//param[@name=\"user\"]/@value", prefix));
        assertAllTextEquals(jdomDocument, "//param[@name=\"driver\"]/@value", prefix, "javax.naming.InitialContext");
        assertAllTextEquals(jdomDocument, "//param[@name=\"url\"]/@value", prefix, "java:comp/env/jdbc/jahia");
        assertAllTextEquals(jdomDocument, "//param[@name=\"schema\"]/@value", prefix, mysqlDBProperties.getProperty("jahia.jackrabbit.schema"));
        assertNotNull(getNode(jdomDocument, "//Cluster/Journal", prefix));

        assertAllTextEquals(jdomDocument, "//param[@name=\"externalBLOBs\"]/@value", prefix, "true");

        assertAllTextEquals(jdomDocument, "//FileSystem/@class", prefix, mysqlDBProperties.getProperty("jahia.jackrabbit.filesystem"));
        assertAllTextEquals(jdomDocument, "//PersistenceManager/@class", prefix, mysqlDBProperties.getProperty("jahia.jackrabbit.persistence"));

    }
}
