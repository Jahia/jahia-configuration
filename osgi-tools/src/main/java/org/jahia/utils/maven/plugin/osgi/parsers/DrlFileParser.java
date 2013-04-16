package org.jahia.utils.maven.plugin.osgi.parsers;

import org.codehaus.plexus.util.FileUtils;
import org.jahia.utils.maven.plugin.osgi.PackageUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Drools rule definition file parser
 */
public class DrlFileParser extends AbstractFileParser {

    public static final Pattern RULE_IMPORT_PATTERN = Pattern.compile("^\\s*import\\s*([\\w.\\*]*)\\s*$");

    public boolean canParse(String fileName) {
        String ext = FileUtils.getExtension(fileName).toLowerCase();
        return "drl".equals(ext);
    }

    public boolean parse(String fileName, InputStream inputStream, ParsingContext parsingContext, boolean externalDependency) throws IOException {
        getLogger().debug("Processing Drools Rule file " + fileName + "...");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            Matcher ruleImportMatcher = RULE_IMPORT_PATTERN.matcher(line);
            if (ruleImportMatcher.matches()) {
                String ruleImport = ruleImportMatcher.group(1);
                getLogger().debug(fileName + ": found rule import " + ruleImport + " package=" + PackageUtils.getPackageFromClass(ruleImport));
                parsingContext.addPackageImport(PackageUtils.getPackageFromClass(ruleImport));
            }
        }
        return true;
    }
}
