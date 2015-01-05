package org.jahia.utils.osgi.parsers;

import org.jahia.utils.osgi.PackageUtils;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.util.List;

/**
 * XML file parser for JPDL file types
 */
public class JpdlXmlFileParser extends AbstractXmlFileParser {

    public boolean canParse(String fileName) {
        return fileName.toLowerCase().endsWith(".jpdl.xml");
    }

    @Override
    public boolean canParse(String fileName, Element rootElement) {
        return true;
    }

    @Override
    public void parse(String fileName, Element rootElement, String fileParent, boolean externalDependency, boolean optionalDependency, String version, ParsingContext parsingContext) throws JDOMException {
        getLogger().debug("Processing workflow definition file (JBPM JPDL) " + fileParent + " / " +fileName + "...");
        List<Attribute> classAttributes = getAttributes(rootElement, "//@class");
        for (Attribute classAttribute : classAttributes) {
            getLogger().debug(fileName + " Found class " + classAttribute.getValue() + " package=" + PackageUtils.getPackagesFromClass(classAttribute.getValue(), optionalDependency, version, fileName, parsingContext).toString());
            parsingContext.addAllPackageImports(PackageUtils.getPackagesFromClass(classAttribute.getValue(), optionalDependency, version, fileParent + "/" +fileName, parsingContext));
        }
    }
}
