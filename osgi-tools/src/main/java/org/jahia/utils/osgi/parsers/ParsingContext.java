package org.jahia.utils.osgi.parsers;

import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * Context for parsing files
 */
public class ParsingContext {

    private Set<String> packageImports = new TreeSet<String>();
    private Set<String> taglibUris = new TreeSet<String>();
    private Map<String, Set<String>> taglibPackages = new HashMap<String, Set<String>>();
    private Map<String, Boolean> externalTaglibs = new HashMap<String, Boolean>();
    private Set<String> contentTypeDefinitions = new TreeSet<String>();
    private Set<String> contentTypeReferences = new TreeSet<String>();
    private Set<String> projectPackages = new TreeSet<String>();

    public void addPackageImport(String packageName) {
        if (StringUtils.isEmpty(packageName)) {
            return;
        }
        if (!packageName.startsWith("java.") &&
            !packageImports.contains(packageName) &&
            !packageImports.contains(packageName + ";resolution:=optional")) {
            packageImports.add(packageName);
        }
    }

    public void addAllPackageImports(Collection<String> packageNames) {
        for (String packageName : packageNames) {
            addPackageImport(packageName);
        }
    }

    public Set<String> getTaglibUris() {
        return taglibUris;
    }

    public Map<String, Set<String>> getTaglibPackages() {
        return taglibPackages;
    }

    public Map<String, Boolean> getExternalTaglibs() {
        return externalTaglibs;
    }

    public Set<String> getContentTypeDefinitions() {
        return contentTypeDefinitions;
    }

    public Set<String> getContentTypeReferences() {
        return contentTypeReferences;
    }

    public Set<String> getPackageImports() {
        return packageImports;
    }

    public Set<String> getProjectPackages() {
        return projectPackages;
    }

    public void postProcess() {
        // now let's remove all the project packages from the imports, we assume we will not import split packages.
        packageImports.removeAll(projectPackages);

        contentTypeReferences.removeAll(contentTypeDefinitions);

    }
}
