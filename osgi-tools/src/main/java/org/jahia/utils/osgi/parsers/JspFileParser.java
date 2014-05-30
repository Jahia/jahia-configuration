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

    public boolean parse(String fileName, InputStream inputStream, ParsingContext parsingContext, boolean externalDependency) throws IOException {
        getLogger().debug("Processing JSP " + fileName + "...");
        String jspFileContent = IOUtils.toString(inputStream);

        parsePageImport(parsingContext, jspFileContent);
        parseIdeaTypeHint(parsingContext, jspFileContent);
        parseJspUseBean(parsingContext, jspFileContent);
        parseTaglib(fileName, parsingContext, jspFileContent);
        return true;
    }

    private void parseJspUseBean(ParsingContext parsingContext, String jspFileContent) {
        Matcher jspUseBeanTagMatcher = JSP_USEBEAN_TAG_PATTERN.matcher(jspFileContent);
        while (jspUseBeanTagMatcher.find()) {
            String useBeanAttributes = jspUseBeanTagMatcher.group(1);
            Matcher tagAttributesMatcher = TAG_ATTRIBUTES_PATTERN.matcher(useBeanAttributes);
            while (tagAttributesMatcher.find()) {
                String attributeName = tagAttributesMatcher.group(1);
                String attributeValue = tagAttributesMatcher.group(2);
                if ("class".equals(attributeName) || "type".equals(attributeName)) {
                    parsingContext.addAllPackageImports(PackageUtils.getPackagesFromClass(attributeValue));
                }
            }
        }
    }

    private void parseTaglib(String fileName, ParsingContext parsingContext, String jspFileContent) {
        Matcher taglibUriMatcher = JSP_TAGLIB_PATTERN.matcher(jspFileContent);
        while (taglibUriMatcher.find()) {
            String taglibUri = taglibUriMatcher.group(1);
            parsingContext.getTaglibUris().add(taglibUri);
            if (!parsingContext.getTaglibPackages().containsKey(taglibUri)) {
                Set<String> unresolvedUrisForJsp = parsingContext.getUnresolvedTaglibUris().get(fileName);
                if (unresolvedUrisForJsp == null) {
                    unresolvedUrisForJsp = new TreeSet<String>();
                }
                unresolvedUrisForJsp.add(taglibUri);
                parsingContext.getUnresolvedTaglibUris().put(fileName, unresolvedUrisForJsp);
            } else {
                Set<String> taglibPackageSet = parsingContext.getTaglibPackages().get(taglibUri);
                boolean externalTagLib = parsingContext.getExternalTaglibs().get(taglibUri);
                if (externalTagLib) {
                    parsingContext.addAllPackageImports(taglibPackageSet);
                }
            }
        }
    }

    private void parseIdeaTypeHint(ParsingContext parsingContext, String jspFileContent) {
        Matcher ideaTypeHintMatcher = IDEA_TYPE_HINT_PATTERN.matcher(jspFileContent);
        while (ideaTypeHintMatcher.find()) {
            String classImportString = ideaTypeHintMatcher.group(1);
            parsingContext.addAllPackageImports(PackageUtils.getPackagesFromClass(classImportString));
        }
    }

    private void parsePageImport(ParsingContext parsingContext, String jspFileContent) {
        Matcher pageImportMatcher = JSP_PAGE_IMPORT_PATTERN.matcher(jspFileContent);
        while (pageImportMatcher.find()) {
            String classImportString = pageImportMatcher.group(1);
            if (classImportString.contains(",")) {
                getLogger().debug("Multiple imports in a single JSP page import statement detected: " + classImportString);
                String[] classImports = StringUtils.split(classImportString, ",");
                Set<String> jspPackageImports = new TreeSet<String>();
                for (String classImport : classImports) {
                    jspPackageImports.addAll(PackageUtils.getPackagesFromClass(classImport.trim()));
                }
                parsingContext.addAllPackageImports(jspPackageImports);
            } else {
                parsingContext.addAllPackageImports(PackageUtils.getPackagesFromClass(classImportString));
            }
        }
    }
}
