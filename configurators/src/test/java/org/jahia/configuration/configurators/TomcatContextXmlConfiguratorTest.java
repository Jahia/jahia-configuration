package org.jahia.configuration.configurators;

import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.net.URL;

/**
 * Test unit for Tomcat's context deployment configurator.
 *
 * @author loom
 *         Date: Nov 3, 2009
 *         Time: 2:59:53 PM
 */
public class TomcatContextXmlConfiguratorTest extends AbstractXMLConfiguratorTestCase {
    
    public void testUpdateConfiguration() throws Exception {
        FileSystemManager fsManager = VFS.getManager();
        URL contextXmlUrl = this.getClass().getClassLoader().getResource("configurators/META-INF/context.xml");
        File contextXmlFile = new File(contextXmlUrl.getFile());
        String contextXmlParentPath = contextXmlFile.getParentFile().getPath() + File.separator;

        TomcatContextXmlConfigurator tomcatOracleConfigurator = new TomcatContextXmlConfigurator(oracleDBProperties, tomcatMySQLConfigBean);
        tomcatOracleConfigurator.updateConfiguration(new VFSConfigFile(fsManager, contextXmlUrl.toExternalForm()), contextXmlParentPath + "context-modified.xml");

        // The following tests are NOT exhaustive
        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Document jdomDocument = saxBuilder.build(contextXmlParentPath + "context-modified.xml");
        String prefix = "";

        assertAllTextEquals(jdomDocument, "//Resource/@username", prefix, oracleDBProperties.getProperty("jahia.database.user"));
        assertAllTextEquals(jdomDocument, "//Resource/@password", prefix, oracleDBProperties.getProperty("jahia.database.pass"));
        assertAllTextEquals(jdomDocument, "//Resource/@url", prefix, oracleDBProperties.getProperty("jahia.database.url"));
        assertAllTextEquals(jdomDocument, "//Resource/@driverClassName", prefix, oracleDBProperties.getProperty("jahia.database.driver"));

        TomcatContextXmlConfigurator tomcatMySQLContextXmlConfigurator = new TomcatContextXmlConfigurator(mysqlDBProperties, tomcatMySQLConfigBean);
        tomcatMySQLContextXmlConfigurator.updateConfiguration(new VFSConfigFile(fsManager, contextXmlParentPath + "context-modified.xml"), contextXmlParentPath + "context-modified2.xml");

        // The following tests are NOT exhaustive
        saxBuilder = new SAXBuilder();
        jdomDocument = saxBuilder.build(contextXmlParentPath + "context-modified2.xml");
        assertAllTextEquals(jdomDocument, "//Resource/@username", prefix, mysqlDBProperties.getProperty("jahia.database.user"));
        assertAllTextEquals(jdomDocument, "//Resource/@password", prefix, mysqlDBProperties.getProperty("jahia.database.pass"));
        assertAllTextEquals(jdomDocument, "//Resource/@url", prefix, mysqlDBProperties.getProperty("jahia.database.url"));
        assertAllTextEquals(jdomDocument, "//Resource/@driverClassName", prefix, mysqlDBProperties.getProperty("jahia.database.driver"));

    }
}
