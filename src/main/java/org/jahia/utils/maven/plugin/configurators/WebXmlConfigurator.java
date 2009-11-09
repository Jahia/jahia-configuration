package org.jahia.utils.maven.plugin.configurators;

import org.jdom.input.SAXBuilder;
import org.jdom.*;
import org.jdom.xpath.XPath;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jahia.utils.maven.plugin.buildautomation.JahiaPropertiesBean;

import java.util.Map;
import java.util.List;
import java.io.FileWriter;

/**
 * Configurator for web.xml file. It will active/deactivate parts of the web.xml file depending on the target
 * application server for deployment. For example under Websphere it will deactivate the Test servlet that is not
 * compatible with this server.
 *
 * @author loom
 *         Date: Oct 19, 2009
 *         Time: 4:40:55 PM
 */
public class WebXmlConfigurator extends AbstractXMLConfigurator {

    public WebXmlConfigurator(Map dbProperties, JahiaPropertiesBean jahiaPropertiesBean) {
        super(dbProperties, jahiaPropertiesBean);
    }

    public void updateConfiguration(String sourceFileName, String destFileName) throws Exception {
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            Document jdomDocument = saxBuilder.build(sourceFileName);
            Element webAppElement = jdomDocument.getRootElement();
            String namespaceURI = webAppElement.getNamespaceURI();
            Namespace namespace = webAppElement.getNamespace();

            boolean removeTestServlet = false;
            boolean usingWebsphere = false;
            if ("was".equals(jahiaPropertiesBean.getServer())) {
                removeTestServlet = true;
                usingWebsphere = true;
            }

            // Deactivate the test servlet under Websphere, as it doesn't work (seems like JUnit is never available in the production class loaders)

            XPath servletXPath = XPath.newInstance("/xp:web-app//xp:servlet[contains(xp:servlet-class,\"org.jahia.bin.TestServlet\")]");
            servletXPath.addNamespace("xp", namespaceURI);
            Element servletElement = (Element) (servletXPath.selectSingleNode(jdomDocument));
            if (servletElement != null) {
                if (removeTestServlet) {
                    servletElement.getParent().removeContent(servletElement);
                }
            } else {
                if (!removeTestServlet) {
                    // servlet is missing, we must add it.
                    servletElement = new Element("servlet", namespace);
                    servletElement.addContent(new Element("servlet-name", namespace).setText("Test"));
                    servletElement.addContent(new Element("servlet-class", namespace).setText("org.jahia.bin.TestServlet"));
                    servletElement.addContent(new Element("load-on-startup", namespace).setText("99"));
                    insertElementAfter(jdomDocument, "/xp:web-app//xp:servlet[contains(xp:servlet-class,\"org.jahia.bin.FilesServlet\")]", servletElement);
                }
            }
            XPath servletMappingXPath = XPath.newInstance("/xp:web-app//xp:servlet-mapping[contains(xp:servlet-name,\"Test\")]");
            servletMappingXPath.addNamespace("xp", namespaceURI);
            Element servletMappingElement = (Element) servletMappingXPath.selectSingleNode(jdomDocument);
            if (servletMappingElement != null) {
                if (removeTestServlet) {
                    servletMappingElement.getParent().removeContent(servletMappingElement);
                }
            } else {
                if (!removeTestServlet) {
                    // servlet mapping is missing, let's add it.
                    servletMappingElement = new Element("servlet-mapping", namespace);
                    servletMappingElement.addContent(new Element("servlet-name", namespace).setText("Test"));
                    servletMappingElement.addContent(new Element("url-pattern", namespace).setText("/test/*"));
                    insertElementAfter(jdomDocument, "/xp:web-app//xp:servlet-mapping[contains(xp:servlet-name,\"Files\")]", servletMappingElement);
                }
            }
            // Now let's add the context parameter specific to Websphere.
            XPath contextParamXPath = XPath.newInstance("/xp:web-app//xp:context-param[contains(xp:param-name,\"com.ibm.websphere.portletcontainer.PortletDeploymentEnabled\")]");
            contextParamXPath.addNamespace("xp", namespaceURI);
            Element contextParamElement = (Element) contextParamXPath.selectSingleNode(jdomDocument);
            if (contextParamElement == null) {
                if (usingWebsphere) {
                    contextParamElement = new Element("context-param", namespace);
                    List contextParamElementChildren = contextParamElement.getContent();
                    contextParamElementChildren.add(new Element("param-name", namespace).setText("com.ibm.websphere.portletcontainer.PortletDeploymentEnabled"));
                    contextParamElementChildren.add(new Element("param-value", namespace).setText("false"));
                    int firstContextParamIndex = webAppElement.indexOf(webAppElement.getChild("context-param", namespace));
                    webAppElement.getContent().add(firstContextParamIndex, contextParamElement);
                    webAppElement.getContent().add(firstContextParamIndex + 1, new Text("\n\n    "));
                }
            } else {
                if (!usingWebsphere) {
                    // the element exists, we must remove it.
                    contextParamElement.getParent().removeContent(contextParamElement);
                }
            }

            Format customFormat = Format.getPrettyFormat();
            customFormat.setLineSeparator(System.getProperty("line.separator"));
            XMLOutputter xmlOutputter = new XMLOutputter(customFormat);
            xmlOutputter.output(jdomDocument, new FileWriter(destFileName));
        } catch (JDOMException jdome) {
            throw new Exception("Error while modifying web descriptor file " + sourceFileName, jdome);
        }
    }


}
