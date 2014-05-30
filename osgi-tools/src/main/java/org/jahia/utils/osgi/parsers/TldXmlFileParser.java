package org.jahia.utils.osgi.parsers;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.utils.osgi.PackageUtils;
import org.jdom2.Comment;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.util.*;

/**
 * A TLD (Java Tag Library definition) file parser
 * @todo This parser doesn't support tag files for the moment, but we really should as we are missing
 * dependencies because of this.
 */
public class TldXmlFileParser extends AbstractXmlFileParser {
    
    private static Map<String, List<String>> knownTransitiveImportPackages;
    
    static {
        knownTransitiveImportPackages = new HashMap<String, List<String>>();
        knownTransitiveImportPackages.put("org.springframework.web.servlet.tags.form",
                Arrays.asList(new String[] { "org.springframework.web.servlet.tags" }));
    }

    public static String getTaglibUri(Element tldRootElement) throws JDOMException {
        boolean hasDefaultNamespace = !StringUtils.isEmpty(tldRootElement.getNamespaceURI());

        Element uriElement = null;
        if (hasDefaultNamespace) {
            uriElement = getElement(tldRootElement, "/xp:taglib/xp:uri");
        } else {
            uriElement = getElement(tldRootElement, "/taglib/uri");
        }
        return uriElement != null ? uriElement.getTextTrim() : null;
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

        String uri = getTaglibUri(rootElement);
        if (uri == null) {
            getLogger().warn("Couldn't find /taglib/uri tag in " + fileName + ", aborting TLD parsing !");
            return;
        }
        getLogger().debug("Taglib URI=" + uri);
        Set<String> taglibPackageSet = parsingContext.getTaglibPackages().get(uri);
        if (taglibPackageSet == null) {
            taglibPackageSet = new TreeSet<String>();
        }

        List<Element> tagClassElements = new LinkedList<Element>();
        tagClassElements.addAll(getElements(rootElement, hasDefaultNamespace ? "//xp:tag/xp:tag-class" : "//tag/tag-class"));
        tagClassElements.addAll(getElements(rootElement, hasDefaultNamespace ? "//xp:tag/xp:tei-class" : "//tag/tei-class"));
        tagClassElements.addAll(getElements(rootElement, hasDefaultNamespace ? "//xp:tag/xp:attribute/xp:type" : "//tag/attribute/type"));
        tagClassElements.addAll(getElements(rootElement, hasDefaultNamespace ? "//xp:tag/xp:tagclass" : "//tag/tagclass"));
        tagClassElements.addAll(getElements(rootElement, hasDefaultNamespace ? "//xp:tag/xp:teiclass" : "//tag/teiclass"));
        for (Element tagClassElement : tagClassElements) {
            getLogger().debug(
                    fileName + " Found tag class " + tagClassElement.getTextTrim() + " package="
                            + PackageUtils.getPackagesFromClass(tagClassElement.getTextTrim()).toString());
            taglibPackageSet.addAll(PackageUtils.getPackagesFromClass(tagClassElement.getTextTrim()));
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
                            + PackageUtils.getPackagesFromClass(functionClassElement.getTextTrim()).toString());
            taglibPackageSet.addAll(PackageUtils.getPackagesFromClass(functionClassElement.getTextTrim()));
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

        // Parse tag files
        for (Element tagFilePathElement : getElements(rootElement, hasDefaultNamespace ? "//xp:tag-file/xp:path" : "//tag-file/path")) {
            String tagFilePath = tagFilePathElement.getTextTrim();
            getLogger().debug("Adding tag file to be parsed later in the process: " + tagFilePath);
            parsingContext.addAdditionalFileToParse(tagFilePath);
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
                pkgs.addAll(PackageUtils.getPackagesFromClass(token));
            }
        }

        return pkgs;
    }
}
