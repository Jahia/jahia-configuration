package org.jahia.utils.maven.plugin.configurators;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.util.Map;
import java.io.FileWriter;

/**
 * Configurator to modify the applicationcontext-services.xml file, notably the file sync port.
 *
 * @author loom
 *         Date: Oct 27, 2009
 *         Time: 2:45:50 PM
 */
public class SpringServicesConfigurator extends AbstractXMLConfigurator {

    public SpringServicesConfigurator(Map dbProps, JahiaConfigInterface jahiaConfigInterface) {
        super(dbProps, jahiaConfigInterface);
    }

    public void updateConfiguration(String sourceFileName, String destFileName) throws Exception {
        // Note : from Crazy Jane on the FileList sync uses Spring properties and is now in applicationcontext-cluster.xml

        SAXBuilder saxBuilder = new SAXBuilder();
        Document jdomDocument = saxBuilder.build(sourceFileName);
        Element beansElement = jdomDocument.getRootElement();
        Element syncURLPropertyElement = getElement(beansElement, "/xp:beans/xp:bean[@id=\"FileListSync\"]/xp:property[@name=\"syncUrl\"]");
        if ((syncURLPropertyElement != null) && (syncURLPropertyElement.getAttributeValue("value").indexOf(":${localPort}") < 0)) {
            // port was found in setting, let's change it !
            setElementAttribute(beansElement, "/xp:beans/xp:bean[@id=\"FileListSync\"]/xp:property[@name=\"syncUrl\"]", "value", "http://${localIp}:" + jahiaConfigInterface.getLocalPort());
        }

        Format customFormat = Format.getPrettyFormat();
        customFormat.setLineSeparator(System.getProperty("line.separator"));
        XMLOutputter xmlOutputter = new XMLOutputter(customFormat);
        xmlOutputter.output(jdomDocument, new FileWriter(destFileName));
    }
}
