/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
    
    private static Map<String, List<PackageInfo>> knownTransitiveImportPackages;
    
    static {
        knownTransitiveImportPackages = new HashMap<String, List<PackageInfo>>();
        List<PackageInfo> springServletFormTagTransitiveImport = new ArrayList<PackageInfo>();
        springServletFormTagTransitiveImport.add(new PackageInfo("org.springframework.web.servlet.tags", "Jahia Maven plugin built-in hardcoded transitive import list", null));
        knownTransitiveImportPackages.put("org.springframework.web.servlet.tags.form",
                springServletFormTagTransitiveImport);
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
    public void parse(String fileName, Element rootElement, String fileParent, boolean externalDependency, boolean optionalDependency, String version, ParsingContext parsingContext)
            throws JDOMException {
        dumpElementNamespaces(rootElement);
        boolean hasDefaultNamespace = !StringUtils.isEmpty(rootElement.getNamespaceURI());
        if (hasDefaultNamespace) {
            getLogger().debug("Using default namespace XPath queries");
        }

        String uri = getTaglibUri(rootElement);
        if (uri == null) {
            getLogger().warn("Couldn't find /taglib/uri tag in " + fileParent + " / " + fileName + ", aborting TLD parsing !");
            return;
        }
        getLogger().debug("Taglib URI=" + uri);
        Set<PackageInfo> taglibPackageSet = parsingContext.getTaglibPackages().get(uri);
        if (taglibPackageSet == null) {
            taglibPackageSet = new TreeSet<PackageInfo>();
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
                            + PackageUtils.getPackagesFromClass(tagClassElement.getTextTrim(), optionalDependency, version, fileParent + "/" + fileName, parsingContext).toString());
            taglibPackageSet.addAll(PackageUtils.getPackagesFromClass(tagClassElement.getTextTrim(), optionalDependency, version, fileParent + "/" + fileName, parsingContext));
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
                            + PackageUtils.getPackagesFromClass(functionClassElement.getTextTrim(), optionalDependency, version , fileParent + "/" + fileName, parsingContext).toString());
            taglibPackageSet.addAll(PackageUtils.getPackagesFromClass(functionClassElement.getTextTrim(), optionalDependency, version , fileParent + "/" + fileName, parsingContext));
        }

        // Parsing function signature
        for (Element functionSignatureElement : getElements(rootElement,
                hasDefaultNamespace ? "//xp:function/xp:function-signature" : "//function/function-signature")) {
            List<PackageInfo> pkgs = getPackagesFromFunctionSignature(functionSignatureElement.getTextTrim(), fileParent + "/" + fileName, optionalDependency, version, parsingContext);
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
                List<PackageInfo> pkgs = getPackagesFromTagFileComment(text, fileParent + "/" + fileName, parsingContext);
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
            if (tagFilePath.startsWith("/")) {
                tagFilePath = tagFilePath.substring(1);
            }
            getLogger().debug("Adding tag file to be parsed later in the process: " + tagFilePath);
            parsingContext.addAdditionalFileToParse(tagFilePath);
        }
        
        Set<PackageInfo> effectivePackages = new TreeSet<PackageInfo>(taglibPackageSet);
        for (PackageInfo pkg : taglibPackageSet) {
            if (knownTransitiveImportPackages.containsKey(pkg.getName())) {
                effectivePackages.addAll(knownTransitiveImportPackages.get(pkg.getName()));
            }
        }

        parsingContext.putTaglibPackages(uri, effectivePackages);
        parsingContext.putExternalTaglib(uri, externalDependency);
    }

    private List<PackageInfo> getPackagesFromTagFileComment(String text, String sourceLocation, ParsingContext parsingContext) {
        if (text == null || !text.contains("Import-Package:")) {
            return null;
        }

        List<PackageInfo> pkgs = null;
        String[] tokens = StringUtils.split(StringUtils.replace(text, "Import-Package:", ""), "\n\r \t,");
        for (String token : tokens) {
            if (token.indexOf('.') != -1 && !token.startsWith("java.lang")) {
                if (pkgs == null) {
                    pkgs = new LinkedList<PackageInfo>();
                }
                pkgs.add(new PackageInfo(token, sourceLocation, parsingContext));
            }
        }

        return pkgs;
    }

    private List<PackageInfo> getPackagesFromFunctionSignature(String signature, String sourceLocation, boolean optionalDependency, String version, ParsingContext parsingContext) {
        List<PackageInfo> pkgs = null;
        String[] tokens = StringUtils.split(signature, "\n\r \t,()");
        for (String token : tokens) {
            if (token.indexOf('.') != -1 && !token.startsWith("java.lang")) {
                if (pkgs == null) {
                    pkgs = new LinkedList<PackageInfo>();
                }
                pkgs.addAll(PackageUtils.getPackagesFromClass(token, optionalDependency, version, sourceLocation, parsingContext));
            }
        }

        return pkgs;
    }
}
