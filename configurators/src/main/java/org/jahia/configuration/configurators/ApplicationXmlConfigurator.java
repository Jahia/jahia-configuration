package org.jahia.configuration.configurators;

import org.codehaus.plexus.util.StringUtils;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.FileWriter;
import java.io.InputStreamReader;

/**
 * JEE application.xml configurator.
 */
public class ApplicationXmlConfigurator extends AbstractXMLConfigurator {

    private String jeeApplicationModuleList;

    public ApplicationXmlConfigurator(JahiaConfigInterface jahiaConfigInterface, String jeeApplicationModuleListOverride) {
        super(null, jahiaConfigInterface);
        this.jeeApplicationModuleList = StringUtils.defaultString(jeeApplicationModuleListOverride, jahiaConfigInterface.getJeeApplicationModuleList());
    }

    @Override
    public void updateConfiguration(ConfigFile sourceConfigFile, String destFileName) throws Exception {
        if (jeeApplicationModuleList == null) {
            return;
        }

        String[] moduleConfigList = jeeApplicationModuleList.split(",");

        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        InputStreamReader fileReader = new InputStreamReader(sourceConfigFile.getInputStream());
        org.jdom.Document jdomDocument = saxBuilder.build(fileReader);
        Element root = jdomDocument.getRootElement();
        Namespace ns = root.getNamespace();

        for (String moduleConfig : moduleConfigList) {
            String[] moduleParams = moduleConfig.split(":");
            Element moduleElement = getElement(root, "//xp:module[@id=\"" + moduleParams[0] + "\"]");
            Element moduleType = null;
            if (moduleElement != null) {
                moduleType = moduleElement.getChild(moduleParams[1], ns);
                if (moduleType == null) {
                    moduleElement.removeContent();
                }
            } else {
                moduleElement = new Element("module", ns);
                moduleElement.setAttribute("id", moduleParams[0]);
                root.addContent(moduleElement);
            }
            if (moduleType == null) {
                moduleType = new Element(moduleParams[1], ns);
                createModuleTypeContent(moduleParams, moduleType);
                moduleElement.addContent(moduleType);
            } else {
                if ("web".equals(moduleParams[1])) {
                    Element webUri = moduleType.getChild("web-uri", ns);
                    webUri.setText(moduleParams[2]);
                    Element contextRoot = moduleType.getChild("context-root", ns);
                    if ((moduleParams.length < 4) || StringUtils.isEmpty(moduleParams[3])) {
                        contextRoot.setText("/");
                    } else {
                        contextRoot.setText(moduleParams[3]);
                    }
                } else {
                    moduleType.setText(moduleParams[2]);
                }
            }
        }

        Format customFormat = Format.getPrettyFormat();
        customFormat.setLineSeparator(System.getProperty("line.separator"));
        XMLOutputter xmlOutputter = new XMLOutputter(customFormat);
        xmlOutputter.output(jdomDocument, new FileWriter(destFileName));

    }

    private void createModuleTypeContent(String[] moduleParams, Element moduleType) {
        if ("web".equals(moduleParams[1])) {
            Element webUri = new Element("web-uri", moduleType.getNamespace());
            webUri.setText(moduleParams[2]);
            Element contextRoot = new Element("context-root", moduleType.getNamespace());
            if ((moduleParams.length < 4) || StringUtils.isEmpty(moduleParams[3])) {
                contextRoot.setText("/");
            } else {
                contextRoot.setText(moduleParams[3]);
            }
            moduleType.addContent(webUri);
            moduleType.addContent(contextRoot);
        } else {
            // for all other types we simply assume we will set the text directly.
            moduleType.setText(moduleParams[2]);
        }
    }
}
