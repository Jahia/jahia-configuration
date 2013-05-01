package org.jahia.utils.osgi.parsers;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.FileUtils;
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

    public static final Pattern JSP_PAGE_IMPORT_PATTERN = Pattern.compile("<%@.*page.*import=\\\"(.*?)\\\".*%>");
    public static final Pattern JSP_TAGLIB_PATTERN = Pattern.compile("<%@.*taglib.*uri=\\\"(.*?)\\\".*%>");

    public boolean canParse(String fileName) {
        String ext = FileUtils.getExtension(fileName).toLowerCase();
        return "jsp".equals(ext) || "jspf".equals(ext);
    }

    public boolean parse(String fileName, InputStream inputStream, ParsingContext parsingContext, boolean externalDependency) throws IOException {
        getLogger().debug("Processing JSP " + fileName + "...");
        String jspFileContent = IOUtils.toString(inputStream);
        Matcher pageImportMatcher = JSP_PAGE_IMPORT_PATTERN.matcher(jspFileContent);
        while (pageImportMatcher.find()) {
            String classImportString = pageImportMatcher.group(1);
            if (classImportString.contains(",")) {
                getLogger().debug("Multiple imports in a single JSP page import statement detected: " + classImportString);
                String[] classImports = StringUtils.split(classImportString, ",");
                Set<String> jspPackageImports = new TreeSet<String>();
                for (String classImport : classImports) {
                    jspPackageImports.add(PackageUtils.getPackageFromClass(classImport.trim()));
                }
                parsingContext.addAllPackageImports(jspPackageImports);
            } else {
                parsingContext.addPackageImport(PackageUtils.getPackageFromClass(classImportString));
            }
        }
        Matcher taglibUriMatcher = JSP_TAGLIB_PATTERN.matcher(jspFileContent);
        while (taglibUriMatcher.find()) {
            String taglibUri = taglibUriMatcher.group(1);
            parsingContext.getTaglibUris().add(taglibUri);
            if (!parsingContext.getTaglibPackages().containsKey(taglibUri)) {
                getLogger().warn("JSP " + fileName + " has a reference to taglib " + taglibUri + " that is not in the project's dependencies !");
            } else {
                Set<String> taglibPackageSet = parsingContext.getTaglibPackages().get(taglibUri);
                boolean externalTagLib = parsingContext.getExternalTaglibs().get(taglibUri);
                if (externalTagLib) {
                    parsingContext.addAllPackageImports(taglibPackageSet);
                }
            }
        }
        return true;
    }
}
