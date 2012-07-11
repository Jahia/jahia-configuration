package org.jahia.configuration.configurators;

import org.codehaus.plexus.util.StringUtils;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * JEE application.xml configurator
 */
public class ApplicationXmlConfigurator extends AbstractXMLConfigurator {

    public ApplicationXmlConfigurator(Map dbProps, JahiaConfigInterface jahiaConfigInterface) {
        super(dbProps, jahiaConfigInterface);
    }

    @Override
    public void updateConfiguration(ConfigFile sourceConfigFile, String destFileName) throws Exception {
        if (StringUtils.isEmpty(jahiaConfigInterface.getJeeApplicationLocation())) {
            return;
        }
        String jeeApplicationModuleListStr = jahiaConfigInterface.getJeeApplicationModuleList();
        if (jeeApplicationModuleListStr == null) {
            return;
        }

        String[] moduleConfigList = jeeApplicationModuleListStr.split(",");

        SAXBuilder saxBuilder = new SAXBuilder();
        InputStreamReader fileReader = new InputStreamReader(sourceConfigFile.getInputStream());
        org.jdom.Document jdomDocument = saxBuilder.build(fileReader);
        Element root = jdomDocument.getRootElement();

        for (String moduleConfig : moduleConfigList) {
            String[] moduleParams = moduleConfig.split(":");
            Element moduleElement = getElement(root, "//module[@id=\"" + moduleParams[0] + "\"]");
            if (moduleElement == null) {
                moduleElement = new Element("module");
                moduleElement.setAttribute("id", moduleParams[0]);
                Element moduleType = new Element(moduleParams[1]);
                createModuleTypeContent(moduleParams, moduleType);
                moduleElement.addContent(moduleType);
                root.addContent(moduleElement);
            } else {
                Element moduleType = moduleElement.getChild(moduleParams[1]);
                if (moduleType == null) {
                    moduleType.removeContent();
                    createModuleTypeContent(moduleParams, moduleType);
                } else {
                    if ("web".equals(moduleParams[1])) {
                        Element webUri = moduleType.getChild("web-uri");
                        webUri.setText(moduleParams[2]);
                        Element contextRoot = moduleType.getChild("context-root");
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
        }

        Format customFormat = Format.getPrettyFormat();
        customFormat.setLineSeparator(System.getProperty("line.separator"));
        XMLOutputter xmlOutputter = new XMLOutputter(customFormat);
        xmlOutputter.output(jdomDocument, new FileWriter(destFileName));

    }

    private void createModuleTypeContent(String[] moduleParams, Element moduleType) {
        if ("web".equals(moduleParams[1])) {
            Element webUri = new Element("web-uri");
            webUri.setText(moduleParams[2]);
            Element contextRoot = new Element("context-root");
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
