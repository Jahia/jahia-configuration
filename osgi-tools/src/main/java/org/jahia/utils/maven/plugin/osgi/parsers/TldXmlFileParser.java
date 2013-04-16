package org.jahia.utils.maven.plugin.osgi.parsers;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.FileUtils;
import org.jahia.utils.maven.plugin.osgi.PackageUtils;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * A TLD (Java Tag Library definition) file parser
 *
 */
public class TldXmlFileParser extends AbstractXmlFileParser {

    @Override
    public boolean canParse(String fileName) {
        String ext = FileUtils.getExtension(fileName).toLowerCase();
        return "tld".equals(ext);
    }

    @Override
    public boolean canParse(String fileName, Element rootElement) {
        return true;
    }

    @Override
    public void parse(String fileName, Element rootElement, ParsingContext parsingContext, boolean externalDependency) throws JDOMException {
        dumpElementNamespaces(rootElement);
        boolean hasDefaultNamespace = !StringUtils.isEmpty(rootElement.getNamespaceURI());
        if (hasDefaultNamespace) {
            getLogger().debug("Using default namespace XPath queries");
        }

        Element uriElement = null;
        if (hasDefaultNamespace) {
            uriElement = getElement(rootElement, "/xp:taglib/xp:uri");
        } else {
            uriElement = getElement(rootElement, "/taglib/uri");
        }
        if (uriElement == null) {
            getLogger().warn("Couldn't find /taglib/uri tag in " + fileName + ", aborting TLD parsing !");
            return;
        }
        String uri = uriElement.getTextTrim();
        getLogger().debug("Taglib URI=" + uri);
        Set<String> taglibPackageSet = parsingContext.getTaglibPackages().get(uri);
        if (taglibPackageSet == null) {
            taglibPackageSet = new TreeSet<String>();
        }

        List<Element> tagClassElements = null;
        if (hasDefaultNamespace) {
            tagClassElements = getElements(rootElement, "//xp:tag/xp:tag-class");
        } else {
            tagClassElements = getElements(rootElement, "//tag/tag-class");
        }
        for (Element tagClassElement : tagClassElements) {
            getLogger().debug(fileName + " Found tag class " + tagClassElement.getTextTrim() + " package=" + PackageUtils.getPackageFromClass(tagClassElement.getTextTrim()));
            taglibPackageSet.add(PackageUtils.getPackageFromClass(tagClassElement.getTextTrim()));
        }
        List<Element> functionClassElements = null;
        if (hasDefaultNamespace) {
            functionClassElements = getElements(rootElement, "//xp:function/xp:function-class");
        } else {
            functionClassElements = getElements(rootElement, "//function/function-class");
        }
        for (Element functionClassElement : functionClassElements) {
            getLogger().debug(fileName + " Found function class " + functionClassElement.getTextTrim() + " package=" + PackageUtils.getPackageFromClass(functionClassElement.getTextTrim()));
            taglibPackageSet.add(PackageUtils.getPackageFromClass(functionClassElement.getTextTrim()));
        }
        parsingContext.getTaglibPackages().put(uri, taglibPackageSet);
        parsingContext.getExternalTaglibs().put(uri, externalDependency);
    }
}
