package org.jahia.utils.maven.plugin.configurators;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Attribute;
import org.jdom.Element;

import java.net.URL;
import java.io.File;

/**
 * Test unit for indexation policy XML configurator.
 *
 * @author loom
 *         Date: Nov 9, 2009
 *         Time: 11:17:41 AM
 */
public class IndexationPolicyConfiguratorTest extends AbstractXMLConfiguratorTestCase {
    public void testUpdateConfiguration() throws Exception {
        URL indexationPolicyURL = this.getClass().getClassLoader().getResource("configurators/WEB-INF/etc/spring/applicationcontext-indexationpolicy.xml");
        File indexationPolicyFile = new File(indexationPolicyURL.getFile());
        String indexationPolicyFileParentPath = indexationPolicyFile.getParentFile().getPath() + File.separator;

        IndexationPolicyConfigurator websphereOracleConfigurator = new IndexationPolicyConfigurator(oracleDBProperties, websphereOraclePropertiesBean);
        websphereOracleConfigurator.updateConfiguration(indexationPolicyFile.toString(), indexationPolicyFileParentPath + "applicationcontext-indexationpolicy2.xml");

        SAXBuilder saxBuilder = new SAXBuilder();
        Document jdomDocument = saxBuilder.build(indexationPolicyFileParentPath + "applicationcontext-indexationpolicy2.xml");
        String prefix = "xp";

        assertEquals("1", ((Element) getNode(jdomDocument, "/xp:beans/xp:bean[@id=\"indexationConfig\"]/xp:property[@name=\"properties\"]/xp:props/xp:prop[@key=\"multipleIndexingServer\"]", prefix)).getText());

        IndexationPolicyConfigurator tomcatMySQLConfigurator = new IndexationPolicyConfigurator(mysqlDBProperties, tomcatMySQLPropertiesBean);
        tomcatMySQLConfigurator.updateConfiguration(indexationPolicyFileParentPath + "applicationcontext-indexationpolicy2.xml", indexationPolicyFileParentPath + "applicationcontext-indexationpolicy3.xml");

        jdomDocument = saxBuilder.build(indexationPolicyFileParentPath + "applicationcontext-indexationpolicy3.xml");

        assertEquals("0", ((Element) getNode(jdomDocument, "/xp:beans/xp:bean[@id=\"indexationConfig\"]/xp:property[@name=\"properties\"]/xp:props/xp:prop[@key=\"multipleIndexingServer\"]", prefix)).getText());

    }
}
