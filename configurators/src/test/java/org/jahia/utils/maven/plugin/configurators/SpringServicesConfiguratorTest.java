package org.jahia.utils.maven.plugin.configurators;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Attribute;

import java.net.URL;
import java.io.File;

/**
 * Unit test for the applicationcontext-services configurator.
 *
 * @author loom
 *         Date: Oct 27, 2009
 *         Time: 2:56:10 PM
 */
public class SpringServicesConfiguratorTest extends AbstractXMLConfiguratorTestCase {

    public void testUpdateConfiguration() throws Exception {
        URL applicationContextServicesURL = this.getClass().getClassLoader().getResource("configurators/WEB-INF/etc/spring/applicationcontext-services.xml");
        File applicationContextServicesFile = new File(applicationContextServicesURL.getFile());
        String applicationContextServicesFileParentPath = applicationContextServicesFile.getParentFile().getPath() + File.separator;

        SpringServicesConfigurator websphereOracleConfigurator = new SpringServicesConfigurator(oracleDBProperties, websphereOracleConfigBean);
        websphereOracleConfigurator.updateConfiguration(applicationContextServicesFile.toString(), applicationContextServicesFileParentPath + "applicationcontext-services2.xml");

        SAXBuilder saxBuilder = new SAXBuilder();
        Document jdomDocument = saxBuilder.build(applicationContextServicesFileParentPath + "applicationcontext-services2.xml");
        String prefix = "xp";

        assertEquals("http://${localIp}:9080", ((Attribute) getNode(jdomDocument, "/xp:beans/xp:bean[@id=\"FileListSync\"]/xp:property[@name=\"syncUrl\"]/@value", prefix)).getValue());

        SpringServicesConfigurator tomcatMySQLConfigurator = new SpringServicesConfigurator(mysqlDBProperties, tomcatMySQLConfigBean);
        tomcatMySQLConfigurator.updateConfiguration(applicationContextServicesFileParentPath + "applicationcontext-services2.xml", applicationContextServicesFileParentPath + "applicationcontext-services3.xml");

        saxBuilder = new SAXBuilder();
        jdomDocument = saxBuilder.build(applicationContextServicesFileParentPath + "applicationcontext-services3.xml");

        assertEquals("http://${localIp}:8080", ((Attribute) getNode(jdomDocument, "/xp:beans/xp:bean[@id=\"FileListSync\"]/xp:property[@name=\"syncUrl\"]/@value", prefix)).getValue());
    }
}
