package org.jahia.utils.osgi.parsers;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.utils.osgi.PackageUtils;
import org.jdom2.Comment;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A TLD (Java Tag Library definition) file parser
 *
 */
public class TldXmlFileParser extends AbstractXmlFileParser {
    
    private static Map<String, List<String>> knownTransitiveImportPackages;
    
    static {
        knownTransitiveImportPackages = new HashMap<String, List<String>>();
        knownTransitiveImportPackages.put("org.springframework.web.servlet.tags.form",
                Arrays.asList(new String[] { "org.springframework.web.servlet.tags" }));
    }

    @Override
    public boolean canParse(String fileName) {
        String ext = FilenameUtils.getExtension(fileName).toLowerCase();
        return "tld".equals(ext);
    }

    @Override
    public boolean canParse(String fileName, Element rootElement) {
        return true;
    }

    @Override
    public void parse(String fileName, Element rootElement, ParsingContext parsingContext, boolean externalDependency)
            throws JDOMException {
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
            getLogger().debug(
                    fileName + " Found tag class " + tagClassElement.getTextTrim() + " package="
                            + PackageUtils.getPackageFromClass(tagClassElement.getTextTrim()));
            taglibPackageSet.add(PackageUtils.getPackageFromClass(tagClassElement.getTextTrim()));
        }

        // Parsing function class
        List<Element> functionClassElements = null;
        if (hasDefaultNamespace) {
            functionClassElements = getElements(rootElement, "//xp:function/xp:function-class");
        } else {
            functionClassElements = getElements(rootElement, "//function/function-class");
        }
        for (Element functionClassElement : functionClassElements) {
            getLogger().debug(
                    fileName + " Found function class " + functionClassElement.getTextTrim() + " package="
                            + PackageUtils.getPackageFromClass(functionClassElement.getTextTrim()));
            taglibPackageSet.add(PackageUtils.getPackageFromClass(functionClassElement.getTextTrim()));
        }

        // Parsing function signature
        for (Element functionSignatureElement : getElements(rootElement,
                hasDefaultNamespace ? "//xp:function/xp:function-signature" : "//function/function-signature")) {
            List<String> pkgs = getPackagesFromFunctionSignature(functionSignatureElement.getTextTrim());
            if (pkgs != null && !pkgs.isEmpty()) {
                getLogger().debug(
                        fileName + " Found packages in function signature " + functionSignatureElement.getTextTrim()
                                + " packages=[" + StringUtils.join(pkgs, ", ") + "]");
                taglibPackageSet.addAll(pkgs);
            }
        }

        // Parsing a special "hint" in comments
        for (Object comment : selectNodes(rootElement, "//comment()")) {
            if (comment instanceof Comment) {
                String text = ((Comment) comment).getText();
                List<String> pkgs = getPackagesFromTagFileComment(text);
                if (pkgs != null && !pkgs.isEmpty()) {
                    getLogger().debug(
                            fileName + " Found import packages hint in comment " + text + " packages=["
                                    + StringUtils.join(pkgs, ", ") + "]");
                    taglibPackageSet.addAll(pkgs);
                }
            }
        }
        
        Set<String> effectivePackages = new TreeSet<String>(taglibPackageSet);
        for (String pkg : taglibPackageSet) {
            if (knownTransitiveImportPackages.containsKey(pkg)) {
                effectivePackages.addAll(knownTransitiveImportPackages.get(pkg));
            }
        }

        parsingContext.getTaglibPackages().put(uri, effectivePackages);
        parsingContext.getExternalTaglibs().put(uri, externalDependency);
    }

    private List<String> getPackagesFromTagFileComment(String text) {
        if (text == null || !text.contains("Import-Package:")) {
            return null;
        }

        List<String> pkgs = null;
        String[] tokens = StringUtils.split(StringUtils.replace(text, "Import-Package:", ""), "\n\r \t,");
        for (String token : tokens) {
            if (token.indexOf('.') != -1 && !token.startsWith("java.lang")) {
                if (pkgs == null) {
                    pkgs = new LinkedList<String>();
                }
                pkgs.add(token);
            }
        }

        return pkgs;
    }

    private List<String> getPackagesFromFunctionSignature(String signature) {
        List<String> pkgs = null;
        String[] tokens = StringUtils.split(signature, "\n\r \t,()");
        for (String token : tokens) {
            if (token.indexOf('.') != -1 && !token.startsWith("java.lang")) {
                if (pkgs == null) {
                    pkgs = new LinkedList<String>();
                }
                pkgs.add(PackageUtils.getPackageFromClass(token));
            }
        }

        return pkgs;
    }
}
