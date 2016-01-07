/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.utils.osgi.PackageUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSP file parser
 */
public class JspFileParser extends AbstractFileParser {

    public static final Pattern JSP_PAGE_IMPORT_PATTERN = Pattern.compile("<%@\\s*page[^%]*\\simport=\"([^%\"]*)\"[^%]*%>");
    public static final Pattern JSP_TAGLIB_PATTERN = Pattern.compile("<%@\\s*taglib[^%]*\\suri=\"([^%\"]*)\"[^%]*%>");
    public static final Pattern IDEA_TYPE_HINT_PATTERN = Pattern.compile("<%\\s*--@elvariable.*type=\\\"(.*)\\\"\\s*--\\s*%>");
    public static final Pattern JSP_USEBEAN_TAG_PATTERN = Pattern.compile("<jsp:useBean(.*)\\/>");
    public static final Pattern TAG_ATTRIBUTES_PATTERN = Pattern.compile("((?:\\S:)?\\S*)\\s*=\\s*(?:\\\"|\\')([^\\\"\\']*)(?:\\\"|\\')");

    public boolean canParse(String fileName) {
        String ext = FilenameUtils.getExtension(fileName).toLowerCase();
        return "jsp".equals(ext) || "jspf".equals(ext) || "tag".equals(ext) || "tagf".equals(ext);
    }

    public boolean parse(String fileName, InputStream inputStream, String fileParent, boolean externalDependency, boolean optionalDependency, String version, ParsingContext parsingContext) throws IOException {
        getLogger().debug("Processing JSP " + fileParent + " / " + fileName + "...");
        String jspFileContent = IOUtils.toString(inputStream);

        parsePageImport(parsingContext, jspFileContent, fileParent + " / " + fileName, optionalDependency, version);
        parseIdeaTypeHint(parsingContext, jspFileContent, fileParent + " / " + fileName, optionalDependency, version);
        parseJspUseBean(parsingContext, jspFileContent, fileParent + " / " + fileName, optionalDependency, version);
        parseTaglib(fileName, parsingContext, jspFileContent);
        return true;
    }

    private void parseJspUseBean(ParsingContext parsingContext, String jspFileContent, String sourceLocation, boolean optionalDependency, String version) {
        Matcher jspUseBeanTagMatcher = JSP_USEBEAN_TAG_PATTERN.matcher(jspFileContent);
        while (jspUseBeanTagMatcher.find()) {
            String useBeanAttributes = jspUseBeanTagMatcher.group(1);
            Matcher tagAttributesMatcher = TAG_ATTRIBUTES_PATTERN.matcher(useBeanAttributes);
            while (tagAttributesMatcher.find()) {
                String attributeName = tagAttributesMatcher.group(1);
                String attributeValue = tagAttributesMatcher.group(2);
                if ("class".equals(attributeName) || "type".equals(attributeName)) {
                    parsingContext.addAllPackageImports(PackageUtils.getPackagesFromClass(attributeValue, optionalDependency, version, sourceLocation, parsingContext));
                }
            }
        }
    }

    private void parseTaglib(String fileName, ParsingContext parsingContext, String jspFileContent) {
        Matcher taglibUriMatcher = JSP_TAGLIB_PATTERN.matcher(jspFileContent);
        while (taglibUriMatcher.find()) {
            String taglibUri = taglibUriMatcher.group(1);
            parsingContext.addTaglibUri(taglibUri);
            if (!parsingContext.getTaglibPackages().containsKey(taglibUri)) {
                Set<String> unresolvedUrisForJsp = parsingContext.getUnresolvedTaglibUris().get(fileName);
                if (unresolvedUrisForJsp == null) {
                    unresolvedUrisForJsp = new TreeSet<String>();
                }
                unresolvedUrisForJsp.add(taglibUri);
                parsingContext.putUnresolvedTaglibUris(fileName, unresolvedUrisForJsp);
            } else {
                Set<PackageInfo> taglibPackageSet = parsingContext.getTaglibPackages().get(taglibUri);
                boolean externalTagLib = parsingContext.getExternalTaglibs().get(taglibUri);
                if (externalTagLib) {
                    parsingContext.addAllPackageImports(taglibPackageSet);
                }
            }
        }
    }

    private void parseIdeaTypeHint(ParsingContext parsingContext, String jspFileContent, String sourceLocation, boolean optionalDependency, String version) {
        Matcher ideaTypeHintMatcher = IDEA_TYPE_HINT_PATTERN.matcher(jspFileContent);
        while (ideaTypeHintMatcher.find()) {
            String classImportString = ideaTypeHintMatcher.group(1);
            parsingContext.addAllPackageImports(PackageUtils.getPackagesFromClass(classImportString, optionalDependency, version, sourceLocation, parsingContext));
        }
    }

    private void parsePageImport(ParsingContext parsingContext, String jspFileContent, String sourceLocation, boolean optionalDependency, String version) {
        Matcher pageImportMatcher = JSP_PAGE_IMPORT_PATTERN.matcher(jspFileContent);
        while (pageImportMatcher.find()) {
            String classImportString = pageImportMatcher.group(1);
            if (classImportString.contains(",")) {
                getLogger().debug("Multiple imports in a single JSP page import statement detected: " + classImportString);
                String[] classImports = StringUtils.split(classImportString, ",");
                Set<PackageInfo> jspPackageImports = new TreeSet<PackageInfo>();
                for (String classImport : classImports) {
                    jspPackageImports.addAll(PackageUtils.getPackagesFromClass(classImport.trim(), optionalDependency, version, sourceLocation, parsingContext));
                }
                parsingContext.addAllPackageImports(jspPackageImports);
            } else {
                parsingContext.addAllPackageImports(PackageUtils.getPackagesFromClass(classImportString, optionalDependency, version, sourceLocation, parsingContext));
            }
        }
    }
}
