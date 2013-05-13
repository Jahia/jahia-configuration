package org.jahia.utils.osgi.parsers;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jahia.utils.osgi.PackageUtils;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPath;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic file parser for different XML file types
 */
public abstract class AbstractXmlFileParser extends AbstractFileParser {

    public static final Pattern XPATH_PREFIX_PATTERN = Pattern.compile("(\\w+):[\\w-]+");

    public abstract boolean canParse(String fileName, Element rootElement);

    public abstract void parse(String fileName, Element rootElement, ParsingContext parsingContext, boolean externalDependency)
            throws JDOMException;

    public boolean canParse(String fileName) {
        String ext = FilenameUtils.getExtension(fileName).toLowerCase();
        return "xml".equals(ext);
    }

    public boolean parse(String fileName, InputStream inputStream, ParsingContext parsingContext, boolean externalDependency) {
        boolean processed = true;
        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setValidation(false);
        saxBuilder.setFeature("http://xml.org/sax/features/validation", false);
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        InputStream inputStreamCopy = null;
        try {
            ByteArrayOutputStream entryOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, entryOutputStream);
            inputStreamCopy = new ByteArrayInputStream(entryOutputStream.toByteArray());
            InputStreamReader fileReader = new InputStreamReader(inputStreamCopy);
            org.jdom2.Document jdomDocument = saxBuilder.build(fileReader);
            Element rootElement = jdomDocument.getRootElement();
            // getLog().debug("Parsed XML file" + fileName + " successfully.");

            if (canParse(fileName, rootElement)) {
                parse(fileName, rootElement, parsingContext, externalDependency);
            } else {
                processed = false;
            }
        } catch (JDOMException e) {
            getLogger().warn("Error parsing XML file " + fileName + ": " + e.getMessage() + " enable debug mode (-X) for more detailed exception");
            getLogger().debug("Detailed exception", e);
        } catch (IOException e) {
            getLogger().warn("Error parsing XML file " + fileName + ": " + e.getMessage() + " enable debug mode (-X) for more detailed exception");
            getLogger().debug("Detailed exception", e);
        } finally {
            IOUtils.closeQuietly(inputStreamCopy);
        }
        return processed;
    }

    public boolean hasNamespaceURI(Element element, String namespaceURI) {
        //getLog().debug("Main namespace URI=" + element.getNamespace().getURI());
        if (element.getNamespace().getURI().equals(namespaceURI)) {
            return true;
        }
        @SuppressWarnings("unchecked")
        List<Namespace> additionalNamespaces = (List<Namespace>) element.getAdditionalNamespaces();
        for (Namespace additionalNamespace : additionalNamespaces) {
            //getLog().debug("Additional namespace URI=" + additionalNamespace.getURI());
            if (additionalNamespace.getURI().equals(namespaceURI)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Utility method to retrieve an XML element using an XPath expression. Note that this method is
     * namespace aware and will require you to use the "xp" prefix in your XPath queries. For example, an XPath query
     * for a Spring XML configuration will look like this :
     * /xp:beans/xp:bean[@id="FileListSync"]/xp:property[@name="syncUrl"]
     * Currently there is no way to rename the prefix.
     *
     * @param scopeElement    the scope in which to execute the XPath query
     * @param xPathExpression the XPath query to select the element we wish to retrieve. In the case where multiple
     *                        elements match, only the first one will be returned.
     * @return the first element that matches the XPath expression, or null if no element matches.
     * @throws JDOMException raised if there was a problem navigating the JDOM structure.
     */
    @SuppressWarnings("unchecked")
    public Element getElement(Element scopeElement, String xPathExpression) throws JDOMException {
        XPath xPath = XPath.newInstance(xPathExpression);
        String namespaceURI = scopeElement.getDocument().getRootElement().getNamespaceURI();
        if ((namespaceURI != null) && (!"".equals(namespaceURI))) {
            xPath.addNamespace("xp", namespaceURI);
        }
        for (Namespace additionalNamespace : (List<Namespace>) scopeElement.getDocument().getRootElement().getAdditionalNamespaces()) {
            xPath.addNamespace(additionalNamespace);
        }
        return (Element) xPath.selectSingleNode(scopeElement);
    }

    @SuppressWarnings("unchecked")
    public List<Element> getElements(Element scopeElement, String xPathExpression) throws JDOMException {
        List<Element> elems = new LinkedList<Element>();
        XPath xPath = XPath.newInstance(xPathExpression);
        String namespaceURI = scopeElement.getDocument().getRootElement().getNamespaceURI();
        if ((namespaceURI != null) && (!"".equals(namespaceURI))) {
            xPath.addNamespace("xp", namespaceURI);
        }
        for (Namespace additionalNamespace : (List<Namespace>) scopeElement.getDocument().getRootElement().getAdditionalNamespaces()) {
            xPath.addNamespace(additionalNamespace);
        }
        for (Object obj : xPath.selectNodes(scopeElement)) {
            if (obj instanceof Element) {
                elems.add((Element) obj);
            }
        }

        return elems;
    }

    @SuppressWarnings("unchecked")
    public List<Attribute> getAttributes(Element scopeElement, String xPathExpression) throws JDOMException {
        List<Attribute> elems = new LinkedList<Attribute>();
        XPath xPath = XPath.newInstance(xPathExpression);
        String namespaceURI = scopeElement.getDocument().getRootElement().getNamespaceURI();
        if ((namespaceURI != null) && (!"".equals(namespaceURI))) {
            xPath.addNamespace("xp", namespaceURI);
        }
        for (Namespace additionalNamespace : (List<Namespace>) scopeElement.getDocument().getRootElement().getAdditionalNamespaces()) {
            xPath.addNamespace(additionalNamespace);
        }
        for (Object obj : xPath.selectNodes(scopeElement)) {
            if (obj instanceof Attribute) {
                elems.add((Attribute) obj);
            }
        }

        return elems;
    }

    private Set<String> getPrefixesInXPath(String xPathQuery) {
        Set<String> prefixes = new TreeSet<String>();
        Matcher xPathPrefixMatcher = XPATH_PREFIX_PATTERN.matcher(xPathQuery);
        while (xPathPrefixMatcher.find()) {
            prefixes.add(xPathPrefixMatcher.group(1));
        }
        return prefixes;
    }

    public Set<String> getMissingQueryPrefixes(Element root, String xPathQuery) {
        Set<String> xPathQueryPrefixes = getPrefixesInXPath(xPathQuery);
        Set<String> elementPrefixes = new HashSet<String>();
        for (Namespace additionalNamespaces : (List<Namespace>) root.getAdditionalNamespaces()) {
            elementPrefixes.add(additionalNamespaces.getPrefix());
        }
        elementPrefixes.add("beans");
        Set<String> missingPrefixes = new TreeSet<String>();
        for (String xPathQueryPrefix : xPathQueryPrefixes) {
            if (!elementPrefixes.contains(xPathQueryPrefix)) {
                missingPrefixes.add(xPathQueryPrefix);
            }
        }
        return missingPrefixes;
    }

    public List<Object> getNodes(Element scopeElement, String xPathExpression, String defaultPrefix) throws JDOMException {
        List<Object> nodes = new LinkedList<Object>();
        XPath xPath = XPath.newInstance(xPathExpression);
        String namespaceURI = scopeElement.getDocument().getRootElement().getNamespaceURI();
        if ((namespaceURI != null) && (!"".equals(namespaceURI))) {
            xPath.addNamespace(defaultPrefix, namespaceURI);
        }
        for (Namespace additionalNamespace : (List<Namespace>) scopeElement.getDocument().getRootElement().getAdditionalNamespaces()) {
            xPath.addNamespace(additionalNamespace);
        }
        for (Object obj : xPath.selectNodes(scopeElement)) {
            nodes.add(obj);
        }
        return nodes;
    }

    public void getRefsUsingXPathQueries(String fileName, Element root,
                                          boolean packageReferences,
                                          String[] xPathQueries, String defaultNamespacePrefix,
                                          ParsingContext parsingContext) throws JDOMException {
        for (String xPathQuery : xPathQueries) {
            Set<String> missingPrefixes = getMissingQueryPrefixes(root, xPathQuery);
            if (missingPrefixes.size() > 0) {
                getLogger().debug(fileName + ": xPath query " + xPathQuery + " cannot be executed on this file since it has prefixes not declared in the file: " + missingPrefixes);
                continue;
            }
            List<Object> classObjects = getNodes(root, xPathQuery, defaultNamespacePrefix);
            for (Object classObject : classObjects) {
                String referenceValue = null;
                if (classObject instanceof Attribute) {
                    referenceValue = ((Attribute) classObject).getValue();
                } else if (classObject instanceof Element) {
                    referenceValue = ((Element) classObject).getTextTrim();
                } else {
                    getLogger().warn(fileName + ": xPath query" + xPathQuery + " return unknown XML node type " + classObject + "...");
                }
                if (referenceValue != null) {
                        if (packageReferences) {
                            getLogger().debug(fileName + " Found class " + referenceValue + " package=" + PackageUtils.getPackageFromClass(referenceValue));
                            parsingContext.addPackageImport(PackageUtils.getPackageFromClass(referenceValue));
                        } else {
                            if (referenceValue.contains(" ")) {
                                getLogger().debug(fileName + "Found multi-valued reference: " + referenceValue);
                                String[] referenceValueArray = referenceValue.split(" ");
                                for (String reference : referenceValueArray) {
                                    getLogger().debug(fileName + " Found content type " + referenceValue + " reference");
                                    parsingContext.getContentTypeReferences().add(reference);
                                }
                            } else if (referenceValue.contains(",")) {
                                getLogger().debug(fileName + "Found multi-valued reference: " + referenceValue);
                                String[] referenceValueArray = referenceValue.split(",");
                                for (String reference : referenceValueArray) {
                                    getLogger().debug(fileName + " Found content type " + referenceValue + " reference");
                                    parsingContext.getContentTypeReferences().add(reference);
                                }
                            } else {
                                getLogger().debug(fileName + " Found content type " + referenceValue + " reference");
                                parsingContext.getContentTypeReferences().add(referenceValue);
                            }
                        }
                }
            }
        }
    }

    public void dumpElementNamespaces(Element element) {
        Namespace mainNamespace = element.getNamespace();
        getLogger().debug("Main namespace prefix=[" + mainNamespace.getPrefix() + "] uri=[" + mainNamespace.getURI() + "] getNamespaceURI=[" + element.getNamespaceURI() + "]");
        for (Namespace additionalNamespace : (List<Namespace>) element.getAdditionalNamespaces()) {
            getLogger().debug("Additional namespace prefix=" + additionalNamespace.getPrefix() + " uri=" + additionalNamespace.getURI());
        }
    }


}
