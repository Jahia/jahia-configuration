package org.jahia.utils.osgi.parsers;

import org.apache.commons.io.FilenameUtils;
import org.jahia.utils.osgi.PackageUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Groovy file parser
 */
public class GroovyFileParser extends AbstractFileParser {

    public static final Pattern GROOVY_IMPORT_PATTERN = Pattern.compile("^\\s*import\\s*(?:static)?\\s*([\\w\\.\\*]*)\\s*(?:as\\s*(\\w*)\\s*)?");

    public boolean canParse(String fileName) {
        String ext = FilenameUtils.getExtension(fileName).toLowerCase();
        return "groovy".equals(ext);
    }

    public boolean parse(String fileName, InputStream inputStream, ParsingContext parsingContext, boolean externalDependency) throws IOException {
        getLogger().debug("Processing Groovy file " + fileName + "...");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            Matcher groovyImportMatcher = GROOVY_IMPORT_PATTERN.matcher(line);
            if (groovyImportMatcher.matches()) {
                String groovyImport = groovyImportMatcher.group(1);
                getLogger().debug(fileName + ": found Groovy import " + groovyImport + " package=" + PackageUtils.getPackagesFromClass(groovyImport).toString());
                parsingContext.addAllPackageImports(PackageUtils.getPackagesFromClass(groovyImport));
            }
        }
        return true;
    }
}
