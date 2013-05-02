package org.jahia.utils.osgi.parsers;

import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * Context for parsing files
 */
public class ParsingContext {

    private Set<String> packageImports = new TreeSet<String>();
    private Set<String> taglibUris = new TreeSet<String>();
    private Map<String, Set<String>> unresolvedTaglibUris = new TreeMap<String,Set<String>>();
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

    public Map<String,Set<String>> getUnresolvedTaglibUris() {
        return unresolvedTaglibUris;
    }

    public void postProcess() {
        // now let's remove all the project packages from the imports, we assume we will not import split packages.
        packageImports.removeAll(projectPackages);

        contentTypeReferences.removeAll(contentTypeDefinitions);

        // we now resolve the tag lib URIs that we found earlier
        Map<String,Set<String>> resolvedUris = new TreeMap<String,Set<String>>();
        for (Map.Entry<String,Set<String>> unresolvedTaglibUri : unresolvedTaglibUris.entrySet()) {
            for (String singleUri : unresolvedTaglibUri.getValue()) {
                if (taglibUris.contains(singleUri)) {
                    Set<String> taglibPackageSet = getTaglibPackages().get(singleUri);
                    boolean externalTagLib = getExternalTaglibs().get(singleUri);
                    if (externalTagLib) {
                        addAllPackageImports(taglibPackageSet);
                    }
                    Set<String> resolvedUrisForJsp = resolvedUris.get(unresolvedTaglibUri.getKey());
                    if (resolvedUrisForJsp == null) {
                        resolvedUrisForJsp = new TreeSet<String>();
                    }
                    resolvedUrisForJsp.add(singleUri);
                    resolvedUris.put(unresolvedTaglibUri.getKey(), resolvedUrisForJsp);
                }
            }
        }

        for (Map.Entry<String,Set<String>> resolvedUri : resolvedUris.entrySet()) {
            Set<String> unresolvedUrisForJsp = unresolvedTaglibUris.get(resolvedUri.getKey());
            if (unresolvedUrisForJsp != null) {
                unresolvedUrisForJsp.removeAll(resolvedUri.getValue());
                if (unresolvedUrisForJsp.size() == 0) {
                    unresolvedTaglibUris.remove(resolvedUri.getKey());
                }
            }
        }
    }
}
