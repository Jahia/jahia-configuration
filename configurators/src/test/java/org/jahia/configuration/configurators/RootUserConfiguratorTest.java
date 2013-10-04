package org.jahia.configuration.configurators;

import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.jahia.configuration.configurators.RootUserConfigurator;
import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Attribute;
import org.jdom.Element;

import java.io.FileInputStream;
import java.net.URL;
import java.io.File;

/**
 * Test unit for the root user configurator
 *
 * @author loom
 *         Date: Nov 3, 2009
 *         Time: 4:15:35 PM
 */
public class RootUserConfiguratorTest extends AbstractXMLConfiguratorTestCase {
    public void testUpdateConfiguration() throws Exception {
        FileSystemManager fsManager = VFS.getManager();
        URL rootXmlUrl = this.getClass().getClassLoader().getResource("configurators/WEB-INF/etc/repository/root-test.xml");
        File rootXmlFile = new File(rootXmlUrl.getFile());
        String rootXmlFileParentPath = rootXmlFile.getParentFile().getPath() + File.separator;

        RootUserConfigurator websphereOracleConfigurator = new RootUserConfigurator(oracleDBProperties, websphereOracleConfigBean, "password");
        websphereOracleConfigurator.updateConfiguration(new VFSConfigFile(fsManager, rootXmlUrl.toExternalForm()), rootXmlFileParentPath + "root-modified.xml");

        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Document jdomDocument = saxBuilder.build(rootXmlFileParentPath + "root-modified.xml");
        String prefix = "";

        Element el = (Element) getNode(jdomDocument, "/content/users/*", prefix);
        assertEquals(websphereOracleConfigBean.getJahiaRootUsername(), el.getName());
        assertEquals("password", ((Attribute) getNode(jdomDocument, "/content/users/"
                + websphereOracleConfigBean.getJahiaRootUsername() + "/@j:password", prefix)).getValue());
        assertEquals(websphereOracleConfigBean.getJahiaRootFirstname(), ((Attribute) getNode(jdomDocument, "/content/users/"
                + websphereOracleConfigBean.getJahiaRootUsername() + "/@j:firstName", prefix)).getValue());
        assertEquals(websphereOracleConfigBean.getJahiaRootLastname(), ((Attribute) getNode(jdomDocument, "/content/users/"
                + websphereOracleConfigBean.getJahiaRootUsername() + "/@j:lastName", prefix)).getValue());
        assertEquals(websphereOracleConfigBean.getJahiaRootEmail(), ((Attribute) getNode(jdomDocument, "/content/users/"
                + websphereOracleConfigBean.getJahiaRootUsername() + "/@j:email", prefix)).getValue());

        RootUserConfigurator tomcatMySQLConfigurator = new RootUserConfigurator(mysqlDBProperties, tomcatMySQLConfigBean, "1234root");
        tomcatMySQLConfigurator.updateConfiguration(new VFSConfigFile(fsManager, rootXmlFileParentPath + "root-test.xml"), rootXmlFileParentPath + "root-modified2.xml");

        jdomDocument = saxBuilder.build(rootXmlFileParentPath + "root-modified2.xml");

        assertEquals("root", ((Element) getNode(jdomDocument, "/content/users/*", prefix)).getName());
        assertEquals("1234root", ((Attribute) getNode(jdomDocument, "/content/users/root/@j:password", prefix)).getValue());

    }
}
