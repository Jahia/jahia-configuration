package org.jahia.utils.maven.plugin.configurators;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Attribute;
import org.jdom.Element;

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
        URL rootJahia6XmlUrl = this.getClass().getClassLoader().getResource("configurators/WEB-INF/etc/repository/root-jahia6.xml");
        File rootJahia6XmlFile = new File(rootJahia6XmlUrl.getFile());
        String rootJahia6XmlFileParentPath = rootJahia6XmlFile.getParentFile().getPath() + File.separator;

        RootUserConfigurator jahia6RootUserConfigurator = new RootUserConfigurator(oracleDBProperties, websphereOracleConfigBean, "root1234");
        jahia6RootUserConfigurator.updateConfiguration(rootJahia6XmlFile.toString(), rootJahia6XmlFileParentPath + "root-jahia6-modified.xml");

        SAXBuilder saxBuilder = new SAXBuilder();
        Document jdomDocument = saxBuilder.build(rootJahia6XmlFileParentPath + "root-jahia6-modified.xml");
        String prefix = "";
        assertNull(getNode(jdomDocument, "/content/users/root/@j:password", prefix));

        URL rootXmlUrl = this.getClass().getClassLoader().getResource("configurators/WEB-INF/etc/repository/root-jahia65.xml");
        File rootXmlFile = new File(rootXmlUrl.getFile());
        String rootXmlFileParentPath = rootXmlFile.getParentFile().getPath() + File.separator;

        RootUserConfigurator websphereOracleConfigurator = new RootUserConfigurator(oracleDBProperties, websphereOracleConfigBean, "root1234");
        websphereOracleConfigurator.updateConfiguration(rootXmlFile.toString(), rootXmlFileParentPath + "root-jahia65-modified.xml");

        jdomDocument = saxBuilder.build(rootXmlFileParentPath + "root-jahia65-modified.xml");

        assertEquals("root", ((Element) getNode(jdomDocument, "/content/users/*", prefix)).getName());
        assertEquals("root1234", ((Attribute) getNode(jdomDocument, "/content/users/root/@j:password", prefix)).getValue());

        RootUserConfigurator tomcatMySQLConfigurator = new RootUserConfigurator(mysqlDBProperties, tomcatMySQLConfigBean, "1234root");
        tomcatMySQLConfigurator.updateConfiguration(rootXmlFileParentPath + "root-jahia65-modified.xml", rootXmlFileParentPath + "root-jahia65-modified2.xml");

        jdomDocument = saxBuilder.build(rootXmlFileParentPath + "root-jahia65-modified2.xml");

        assertEquals("root", ((Element) getNode(jdomDocument, "/content/users/*", prefix)).getName());
        assertEquals("1234root", ((Attribute) getNode(jdomDocument, "/content/users/root/@j:password", prefix)).getValue());

    }
}
