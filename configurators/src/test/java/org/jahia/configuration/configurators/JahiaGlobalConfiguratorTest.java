package org.jahia.configuration.configurators;

import junit.framework.TestCase;
import org.apache.commons.vfs.FileObject;
import org.jahia.configuration.logging.SLF4JLogger;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Unit test to validate proper configuration generation.
 *
 * @author loom
 *         Date: May 11, 2010
 *         Time: 2:26:38 PM
 */
public class JahiaGlobalConfiguratorTest extends TestCase {

    public void testFindVFSFile() throws URISyntaxException {
        JahiaConfigBean websphereDerbyConfigBean;

        Logger logger = LoggerFactory.getLogger(JahiaGlobalConfiguratorTest.class);

        URL configuratorsResourceURL = this.getClass().getClassLoader().getResource("configurators");
        File configuratorsFile = new File(configuratorsResourceURL.toURI());

        websphereDerbyConfigBean = new JahiaConfigBean();
        websphereDerbyConfigBean.setDatabaseType("derby_embedded");
        websphereDerbyConfigBean.setTargetServerType("was");
        websphereDerbyConfigBean.setTargetServerVersion("6.1.0.25");
        websphereDerbyConfigBean.setTargetConfigurationDirectory(configuratorsFile.toString());
        websphereDerbyConfigBean.setCluster_activated("true");
        websphereDerbyConfigBean.setCluster_node_serverId("jahiaServer1");
        websphereDerbyConfigBean.setProcessingServer("true");
        websphereDerbyConfigBean.setLdapActivated("true");
        websphereDerbyConfigBean.setExternalizedConfigActivated(true);
        websphereDerbyConfigBean.setExternalizedConfigTargetPath(configuratorsFile.getPath());
        websphereDerbyConfigBean.setExternalizedConfigClassifier("jahiaServer1");
        websphereDerbyConfigBean.setExternalizedConfigFinalName("jahia-externalized-config");

        JahiaGlobalConfigurator jahiaGlobalConfigurator = new JahiaGlobalConfigurator(new SLF4JLogger(logger), websphereDerbyConfigBean);
        FileObject webXmlFileObject = jahiaGlobalConfigurator.findVFSFile(configuratorsFile.getPath() + "/WEB-INF", "web\\.xml");
        assertNotNull("Couldn't find web.xml using full matching pattern", webXmlFileObject);
        webXmlFileObject = jahiaGlobalConfigurator.findVFSFile(configuratorsFile.getPath() + "/WEB-INF", "w.*\\.xml");
        assertNotNull("Couldn't find web.xml using basic pattern", webXmlFileObject);
    }

    public void testGlobalConfiguration() throws Exception {

        JahiaConfigBean websphereDerbyConfigBean;

        Logger logger = LoggerFactory.getLogger(JahiaGlobalConfiguratorTest.class);

        URL configuratorsResourceURL = this.getClass().getClassLoader().getResource("configurators");
        File configuratorsFile = new File(configuratorsResourceURL.toURI());

        websphereDerbyConfigBean = new JahiaConfigBean();
        websphereDerbyConfigBean.setDatabaseType("derby_embedded");
        websphereDerbyConfigBean.setTargetServerType("was");
        websphereDerbyConfigBean.setTargetServerVersion("6.1.0.25");
        websphereDerbyConfigBean.setTargetConfigurationDirectory(configuratorsFile.toString());
        websphereDerbyConfigBean.setCluster_activated("true");
        websphereDerbyConfigBean.setCluster_node_serverId("jahiaServer1");
        websphereDerbyConfigBean.setProcessingServer("true");
        websphereDerbyConfigBean.setLdapActivated("true");
        websphereDerbyConfigBean.setExternalizedConfigActivated(true);
        websphereDerbyConfigBean.setExternalizedConfigTargetPath(configuratorsFile.getPath());
        websphereDerbyConfigBean.setExternalizedConfigClassifier("jahiaServer1");
        websphereDerbyConfigBean.setExternalizedConfigFinalName("jahia-externalized-config");
        websphereDerbyConfigBean.setJeeApplicationLocation(configuratorsFile.toString());
        websphereDerbyConfigBean.setJeeApplicationModuleList("jahia-war:web:jahia.war:jahia,portlet-testsuite:web:websphere-testsuite.war:testsuite,java-example:java:somecode.jar");

        JahiaGlobalConfigurator jahiaGlobalConfigurator = new JahiaGlobalConfigurator(new SLF4JLogger(logger), websphereDerbyConfigBean);
        jahiaGlobalConfigurator.execute();

        File configFile = new File(configuratorsFile, "jahia-externalized-config-jahiaServer1.jar");
        JarFile configJarFile = new JarFile(configFile);
        // assertNotNull("Missing LDAP configuration file in jahia-config.jar file!", configJarFile.getEntry("org/jahia/config/applicationcontext-ldap-config.xml"));
        JarEntry licenseJarEntry = configJarFile.getJarEntry("org/jahia/config/license.xml");
        assertNotNull("Missing license file in jahia-externalized-config-jahiaServer1.jar file!", licenseJarEntry);
        JarEntry jahiaPropertiesJarEntry = configJarFile.getJarEntry("org/jahia/config/jahia.jahiaServer1.properties");
        assertNotNull("Missing jahia.jahiaServer1.properties file in jahia-externalized-config-jahiaServer1.jar file!", jahiaPropertiesJarEntry);
        JarEntry jahiaAdvancedPropertiesJarEntry = configJarFile.getJarEntry("org/jahia/config/jahia.advanced.jahiaServer1.properties");
        assertNotNull("Missing jahia.advanced.jahiaServer1.properties file in jahia-externalized-config-jahiaServer1.jar file!", jahiaAdvancedPropertiesJarEntry);

        InputStream jahiaPropsInputStream = configJarFile.getInputStream(jahiaPropertiesJarEntry);
        Properties jahiaProperties = new Properties();
        jahiaProperties.load(jahiaPropsInputStream);
        assertEquals("Server value not correct", "was", jahiaProperties.get("server"));
        assertEquals("Server version value not correct", "6.1.0.25", jahiaProperties.get("serverVersion"));

        // The following tests are NOT exhaustive
        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Document jdomDocument = saxBuilder.build(configuratorsFile.toString() + "/META-INF/application.xml");
        String prefix = "";

        assertAllTextEquals(jdomDocument, "//application/module[@id=\"jahia-war\"]/web/web-uri/text()", prefix, "jahia.war");
        assertAllTextEquals(jdomDocument, "//application/module[@id=\"jahia-war\"]/web/context-root/text()", prefix, "jahia");
        assertAllTextEquals(jdomDocument, "//application/module[@id=\"portlet-testsuite\"]/web/web-uri/text()", prefix, "websphere-testsuite.war");
        assertAllTextEquals(jdomDocument, "//application/module[@id=\"portlet-testsuite\"]/web/context-root/text()", prefix, "testsuite");
        assertAllTextEquals(jdomDocument, "//application/module[@id=\"java-example\"]/java/text()", prefix, "somecode.jar");

    }

    public void assertAllTextEquals(Document jdomDocument, String xPathExpression, String prefix, String value) throws JDOMException {
        Element rootElement = jdomDocument.getRootElement();
        String namespaceURI = rootElement.getNamespaceURI();
        XPath contextParamXPath = XPath.newInstance(xPathExpression);
        contextParamXPath.addNamespace(prefix, namespaceURI);
        List resultList = contextParamXPath.selectNodes(jdomDocument);
        for (Object currentObject : resultList) {
            if (currentObject instanceof Attribute) {
                assertEquals(value, ((Attribute) currentObject).getValue());
            } else if (currentObject instanceof Element) {
                assertEquals(value, ((Element) currentObject).getText());
            } else if (currentObject instanceof Text) {
                assertEquals(value, ((Text) currentObject).getValue());
            } else if (currentObject instanceof Comment) {
                assertEquals(value, ((Comment) currentObject).getText());
            } else if (currentObject instanceof CDATA) {
                assertEquals(value, ((CDATA) currentObject).getText());
            } else if (currentObject instanceof ProcessingInstruction) {
                assertEquals(value, ((ProcessingInstruction) currentObject).getValue());
            } else {
                // default fall-back comparison, should rarely be useful.
                assertEquals(value, currentObject.toString());
            }
        }
    }

}
