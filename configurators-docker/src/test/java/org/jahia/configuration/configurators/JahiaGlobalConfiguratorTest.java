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

import junit.framework.TestCase;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
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
        websphereDerbyConfigBean.setTargetConfigurationDirectory(configuratorsFile.toString());
        websphereDerbyConfigBean.setCluster_activated("true");
        websphereDerbyConfigBean.setCluster_node_serverId("jahiaServer1");
        websphereDerbyConfigBean.setProcessingServer("true");
        websphereDerbyConfigBean.setExternalizedConfigActivated(true);
        websphereDerbyConfigBean.setExternalizedConfigExploded(false);
        websphereDerbyConfigBean.setExternalizedConfigTargetPath(configuratorsFile.getPath());

        JahiaGlobalConfigurator jahiaGlobalConfigurator = new JahiaGlobalConfigurator(websphereDerbyConfigBean);
        File file = jahiaGlobalConfigurator.findFile(configuratorsFile.getPath() + "/WEB-INF", "web\\.xml");
        assertTrue("Couldn't find web.xml using full matching pattern", file != null && file.exists());
        file = jahiaGlobalConfigurator.findFile(configuratorsFile.getPath() + "/WEB-INF", "w.*\\.xml");
        assertTrue("Couldn't find web.xml using basic pattern", file != null && file.exists());
    }

//    public void testGlobalConfiguration() throws Exception {
//
//        JahiaConfigBean websphereDerbyConfigBean;
//
//        Logger logger = LoggerFactory.getLogger(JahiaGlobalConfiguratorTest.class);
//
//        URL configuratorsResourceURL = this.getClass().getClassLoader().getResource("configurators");
//        File configuratorsFile = new File(configuratorsResourceURL.toURI());
//
//        websphereDerbyConfigBean = new JahiaConfigBean();
//        websphereDerbyConfigBean.setDatabaseType("derby_embedded");
//        websphereDerbyConfigBean.setTargetServerType("was");
//        websphereDerbyConfigBean.setTargetServerVersion("6.1.0.25");
//        websphereDerbyConfigBean.setTargetConfigurationDirectory(configuratorsFile.toString());
//        websphereDerbyConfigBean.setCluster_activated("true");
//        websphereDerbyConfigBean.setCluster_node_serverId("jahiaServer1");
//        websphereDerbyConfigBean.setProcessingServer("true");
//        websphereDerbyConfigBean.setExternalizedConfigActivated(true);
//        websphereDerbyConfigBean.setExternalizedConfigExploded(true);
//        websphereDerbyConfigBean.setExternalizedConfigTargetPath(configuratorsFile.getPath());
//        websphereDerbyConfigBean.setExternalizedConfigClassifier("jahiaServer1");
//        websphereDerbyConfigBean.setExternalizedConfigFinalName("jahia-externalized-config");
//        websphereDerbyConfigBean.setJeeApplicationLocation(configuratorsFile.toString());
//        websphereDerbyConfigBean.setJeeApplicationModuleList("jahia-war:web:jahia.war:jahia,portlet-testsuite:web:websphere-testsuite.war:testsuite,java-example:java:somecode.jar");
//
//        JahiaGlobalConfigurator jahiaGlobalConfigurator = new JahiaGlobalConfigurator(new SLF4JLogger(logger), websphereDerbyConfigBean);
//        jahiaGlobalConfigurator.execute();
//
//
//        assertTrue("Missing license file in jahia-externalized-config-jahiaServer1.jar file!",
//                new File(configuratorsFile, "jahia/license.xml").exists());
//        assertTrue("Missing jahia.jahiaServer1.properties file in jahia-externalized-config-jahiaServer1.jar file!",
//                new File(configuratorsFile, "jahia/jahia.jahiaServer1.properties").exists());
//        assertTrue("Missing jahia.node.jahiaServer1.properties file in jahia-externalized-config-jahiaServer1.jar file!",
//                new File(configuratorsFile, "jahia/jahia.node.jahiaServer1.properties").exists());
//
//        Properties jahiaProperties = new Properties();
//        try (FileInputStream fis = new FileInputStream(new File(configuratorsFile, "jahia/jahia.jahiaServer1.properties"))) {
//            jahiaProperties.load(fis);
//        }
//        // The following tests are NOT exhaustive
//        SAXBuilder saxBuilder = new SAXBuilder();
//        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
//        Document jdomDocument = saxBuilder.build(configuratorsFile.toString() + "/META-INF/application.xml");
//        String prefix = "";
//
//        assertAllTextEquals(jdomDocument, "//application/module[@id=\"jahia-war\"]/web/web-uri/text()", prefix, "jahia.war");
//        assertAllTextEquals(jdomDocument, "//application/module[@id=\"jahia-war\"]/web/context-root/text()", prefix, "jahia");
//        assertAllTextEquals(jdomDocument, "//application/module[@id=\"portlet-testsuite\"]/web/web-uri/text()", prefix, "websphere-testsuite.war");
//        assertAllTextEquals(jdomDocument, "//application/module[@id=\"portlet-testsuite\"]/web/context-root/text()", prefix, "testsuite");
//        assertAllTextEquals(jdomDocument, "//application/module[@id=\"java-example\"]/java/text()", prefix, "somecode.jar");
//
//    }

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
