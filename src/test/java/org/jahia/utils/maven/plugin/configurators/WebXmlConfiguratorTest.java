package org.jahia.utils.maven.plugin.configurators;

import java.net.URL;
import java.io.File;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;

/**
 * Test class for WebXmlConfigurator configuration updater.
 *
 * @author loom
 *         Date: Oct 20, 2009
 *         Time: 8:03:23 AM
 */
public class WebXmlConfiguratorTest extends AbstractXMLConfiguratorTestCase {

    public void testUpdateConfiguration() throws Exception {
        URL webXmlURL = this.getClass().getClassLoader().getResource("configurators/WEB-INF/web.xml");
        File webXmlFile = new File(webXmlURL.getFile());
        String webXmlParentPath = webXmlFile.getParentFile().getPath() + File.separator;

        // the following tests the modifications required to the web.xml file when deploying under Websphere.
        WebXmlConfigurator oracleWebsphereWebXmlConfigurator = new WebXmlConfigurator(oracleDBProperties, websphereOracleConfigBean);
        oracleWebsphereWebXmlConfigurator.updateConfiguration(webXmlFile.toString(), webXmlParentPath + "web-modified.xml");

        // ok the file is modified now, let's validate it.

        SAXBuilder saxBuilder = new SAXBuilder();
        Document jdomDocument = saxBuilder.build(webXmlParentPath + "web-modified.xml");
        String prefix = "xp";
        String tagPre = ("".equals(prefix) ? "" : prefix + ":");

        assertNull(getNode(jdomDocument, "/"+tagPre+"web-app//"+tagPre+"servlet[contains("+tagPre+"servlet-class,\"org.jahia.bin.TestServlet\")]", prefix));
        assertNull(getNode(jdomDocument, "/"+tagPre+"web-app//"+tagPre+"servlet-mapping[contains("+tagPre+"servlet-name,\"Test\")]", prefix));
        checkOnlyOneElement(jdomDocument, "/"+tagPre+"web-app//"+tagPre+"context-param[contains("+tagPre+"param-name,\"com.ibm.websphere.portletcontainer.PortletDeploymentEnabled\")]", prefix );

        WebXmlConfigurator tomcatMySQLWebXmlConfigurator = new WebXmlConfigurator(mysqlDBProperties, tomcatMySQLConfigBean);
        tomcatMySQLWebXmlConfigurator.updateConfiguration(webXmlParentPath + "web-modified.xml", webXmlParentPath + "web-modified2.xml");

        saxBuilder = new SAXBuilder();
        jdomDocument = saxBuilder.build(webXmlParentPath + "web-modified2.xml");
        // prefix = "xp";
        // tagPre = ("".equals(prefix) ? "" : prefix + ":");

        assertNull(getNode(jdomDocument, "/"+tagPre+"web-app//"+tagPre+"context-param[contains("+tagPre+"param-name,\"com.ibm.websphere.portletcontainer.PortletDeploymentEnabled\")]", prefix) );
        checkOnlyOneElement(jdomDocument, "/"+tagPre+"web-app//"+tagPre+"servlet[contains("+tagPre+"servlet-class,\"org.jahia.bin.TestServlet\")]", prefix);
        checkOnlyOneElement(jdomDocument, "/"+tagPre+"web-app//"+tagPre+"servlet-mapping[contains("+tagPre+"servlet-name,\"Test\")]", prefix);

    }

}