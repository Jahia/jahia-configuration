/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.StringUtils;
import org.jahia.utils.osgi.PackageUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Context for parsing files
 */
public class ParsingContext implements Serializable {

    protected String mavenCoords;
    protected long lastModified;
    protected long fileSize;
    protected String fileName;
    protected String filePath;
    protected String version;
    protected boolean inCache = false;
    private Set<String> taglibUris = new TreeSet<String>();
    private Map<String, Set<String>> unresolvedTaglibUris = new TreeMap<String,Set<String>>();
    private Map<String, Set<PackageInfo>> taglibPackages = new HashMap<String, Set<PackageInfo>>();
    private Map<String, Boolean> externalTaglibs = new HashMap<String, Boolean>();
    private Set<String> contentTypeDefinitions = new TreeSet<String>();
    private Set<String> contentTypeReferences = new TreeSet<String>();
    private Set<String> additionalFilesToParse = new TreeSet<String>();
    private Set<PackageInfo> splitPackages = new TreeSet<PackageInfo>();
    private boolean osgiBundle = false;
    private List<String> bundleClassPath = new ArrayList<String>();
    private Set<PackageInfo> localPackages = new TreeSet<PackageInfo>();
    private Set<PackageInfo> packageImports = new TreeSet<PackageInfo>();
    private Set<PackageInfo> packageExports = new TreeSet<PackageInfo>();
    private Set<PackageInfo> packageIgnores = new TreeSet<PackageInfo>();

    @JsonIgnore protected ParsingContext parentParsingContext;
    @JsonIgnore protected List<ParsingContext> children = new ArrayList<ParsingContext>();
    @JsonIgnore Boolean optional = null;
    @JsonIgnore Boolean external = null;

    public ParsingContext() {
    }

    public ParsingContext(String mavenCoords, long lastModified, long fileSize, String fileName, String filePath, String version, ParsingContext parentParsingContext) {
        this.mavenCoords = mavenCoords;
        this.lastModified = lastModified;
        this.fileSize = fileSize;
        this.fileName = fileName;
        this.filePath = filePath;
        this.version = version;
        this.inCache = false;
        this.parentParsingContext = parentParsingContext;
    }

    public void addPackageImport(PackageInfo packageInfo, Boolean forceOptional) {
        if (StringUtils.isEmpty(packageInfo.getName())) {
            return;
        }
        if (!packageInfo.getName().startsWith("java.")) {
            if (!localPackages.contains(packageInfo)) {
                if (!packageImports.contains(packageInfo)) {
                    packageImports.add(packageInfo);
                } else {
                    PackageInfo existingPackageInfo = null;
                    // we must add the new source locations to the existing entry
                    for (PackageInfo packageImport : packageImports) {
                        if (packageImport.equals(packageImport)) {
                            existingPackageInfo = packageImport;
                            break;
                        }
                    }
                    if (existingPackageInfo != null) {
                        Set<String> existingSourceLocations = existingPackageInfo.getSourceLocations();
                        Set<String> newSourceLocations = new TreeSet<String>(existingSourceLocations);
                        newSourceLocations.addAll(packageInfo.getSourceLocations());
                        boolean optional = packageInfo.isOptional();
                        if (forceOptional != null && forceOptional) {
                            optional = true;
                        }
                        PackageInfo newPackageInfo = new PackageInfo(existingPackageInfo.getName(),
                                existingPackageInfo.getVersion(), optional, newSourceLocations, this);
                        packageImports.remove(existingPackageInfo);
                        packageImports.add(newPackageInfo);
                    }
                }
            }
        }
    }

    public void addPackageImport(PackageInfo packageInfo) {
        addPackageImport(packageInfo, null);
    }

    public void addAllPackageImports(Collection<PackageInfo> packageInfos, Boolean forceOptional) {
        for (PackageInfo packageInfo : packageInfos) {
            addPackageImport(packageInfo, forceOptional);
        }
    }

    public void addAllPackageImports(Collection<PackageInfo> packageInfos) {
        addAllPackageImports(packageInfos, null);
    }

    public Set<String> getTaglibUris() {
        return taglibUris;
    }

    public boolean addTaglibUri(String taglibUri) {
        return taglibUris.add(taglibUri);
    }

    public boolean addAllTaglibUris(Set<String> taglibUrisToAdd) {
        return taglibUris.addAll(taglibUrisToAdd);
    }

    public Map<String, Set<PackageInfo>> getTaglibPackages() {
        return taglibPackages;
    }

    public Set<PackageInfo> putTaglibPackages(String uri, Set<PackageInfo> packages) {
        return taglibPackages.put(uri, packages);
    }

    public Map<String, Boolean> getExternalTaglibs() {
        return externalTaglibs;
    }

    public Boolean putExternalTaglib(String uri, Boolean external) {
        return externalTaglibs.put(uri, external);
    }

    public Set<String> getContentTypeDefinitions() {
        return contentTypeDefinitions;
    }

    public boolean addAllContentTypeDefinitions(Set<String> newContentTypeDefinitions) {
        return contentTypeDefinitions.addAll(newContentTypeDefinitions);
    }

    public Set<String> getContentTypeReferences() {
        return contentTypeReferences;
    }

    public boolean addContentTypeReference(String contentTypeReference) {
        return contentTypeReferences.add(contentTypeReference);
    }

    public boolean addAllContentTypeReferences(Set<String> newContentTypeReferences) {
        return contentTypeReferences.addAll(newContentTypeReferences);
    }

    public Set<PackageInfo> getPackageImports() {
        return packageImports;
    }


    public Map<String,Set<String>> getUnresolvedTaglibUris() {
        return unresolvedTaglibUris;
    }

    public Set<String> putUnresolvedTaglibUris(String fileName, Set<String> unresolvedUrisForJsp) {
        return unresolvedTaglibUris.put(fileName, unresolvedUrisForJsp);
    }

    public void addAdditionalFileToParse(String filePath) {
        additionalFilesToParse.add(filePath);
    }

    public void clearAdditionalFilesToParse() {
        additionalFilesToParse.clear();
    }
    public Set<String> getAdditionalFilesToParse() {
        return additionalFilesToParse;
    }

    public Set<PackageInfo> getSplitPackages() {
        return splitPackages;
    }

    public void removeLocalPackagesFromImports() {

        // now let's remove all the project packages from the imports, we assume we will not import split packages.

        packageImports.removeAll(localPackages);
    }

    public SortedSet<PackageInfo> getChildrenLocalPackagesToRemoveFromImports() {
        SortedSet<PackageInfo> childLocalPackagesToRemove = new TreeSet<PackageInfo>();
        if (children != null && children.size() > 0) {
            for (ParsingContext childParsingContext : children) {
                // remove all child local packages that satisfy package imports, since we are embedding them.
                if (!childParsingContext.isExternal() && !childParsingContext.isOptional()) {
                    childLocalPackagesToRemove.addAll(childParsingContext.getLocalPackages());
                    childLocalPackagesToRemove.addAll(childParsingContext.getChildrenLocalPackagesToRemoveFromImports());
                }
            }
        }
        return childLocalPackagesToRemove;
    }

    public String getMavenCoords() {
        return mavenCoords;
    }

    public long getLastModified() {
        return lastModified;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean addLocalPackage(PackageInfo packageInfo) {
        return localPackages.add(packageInfo);
    }

    public Set<PackageInfo> getLocalPackages() {
        return localPackages;
    }

    public void addChildJarParsingContext(ParsingContext childParsingContext) {
        this.children.add(childParsingContext);
    }

    public Set<PackageInfo> getPackageExports() {
        return packageExports;
    }

    public boolean addPackageExport(PackageInfo packageInfo) {
        return packageExports.add(packageInfo);
    }

    public Set<PackageInfo> getPackageIgnores() {
        return packageIgnores;
    }

    public boolean addPackageIgnore(PackageInfo packageInfo) {
        return packageIgnores.add(packageInfo);
    }

    public List<ParsingContext> getChildren() {
        return children;
    }

    public boolean isInCache() {
        return inCache;
    }

    public void setInCache(boolean inCache) {
        this.inCache = inCache;
    }

    public boolean isOptional() {
        if (optional == null) {
            return false;
        }
        return optional;
    }

    /**
     * This method will only set the optional state if it was not set before or if the new state is not optional
     * @param optional
     */
    public void setOptional(boolean optional) {
        if (this.optional == null) {
            this.optional = optional;
        }
        if (this.optional && !optional) {
            this.optional = optional;
        }
    }

    public boolean isExternal() {
        if (this.external == null) {
            return false;
        }
        return external;
    }

    public void setExternal(boolean external) {
        if (this.external != null) {

        }
        this.external = external;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ParsingContext getParentParsingContext() {
        return parentParsingContext;
    }

    public void setParentParsingContext(ParsingContext parentParsingContext) {
        this.parentParsingContext = parentParsingContext;
    }

    public boolean isOsgiBundle() {
        return osgiBundle;
    }

    public void setOsgiBundle(boolean osgiBundle) {
        this.osgiBundle = osgiBundle;
    }

    public List<String> getBundleClassPath() {
        return bundleClassPath;
    }

    public void setBundleClassPath(List<String> bundleClassPath) {
        this.bundleClassPath = bundleClassPath;
    }

    public void postProcess() {

        if (children != null && children.size() > 0) {
            for (ParsingContext childParsingContext : children) {
                addAllTaglibUris(childParsingContext.getTaglibUris());
                getTaglibPackages().putAll(childParsingContext.getTaglibPackages());
                Set<String> externalTaglibUris = new TreeSet<String>();
                Set<String> internalTaglibUris = new TreeSet<String>();
                for (Map.Entry<String,Boolean> taglibUriExternalEntry : childParsingContext.getExternalTaglibs().entrySet()) {
                    if (taglibUriExternalEntry.getValue()) {
                        externalTaglibUris.add(taglibUriExternalEntry.getKey());
                    } else {
                        internalTaglibUris.add(taglibUriExternalEntry.getKey());
                    }
                }

                if (childParsingContext.isExternal()) {

                    if (childParsingContext.isOptional()) {
                        addAllPackageImports(childParsingContext.getPackageExports(), true);
                        // mark all child taglib uris as external
                        for (String externalTaglibUri : externalTaglibUris) {
                            getExternalTaglibs().put(externalTaglibUri, true);
                        }
                        for (String internalTaglibUri : internalTaglibUris) {
                            getExternalTaglibs().put(internalTaglibUri, true);
                        }
                    }

                    // let's do split-package detection

                    for (PackageInfo localPackage : localPackages) {
                        if (PackageUtils.containsIgnoreVersion(childParsingContext.getPackageExports(), localPackage)) {
                            PackageInfo splitPackageInfo = new PackageInfo(localPackage);
                            for (PackageInfo childPackageExport : childParsingContext.getPackageExports()) {
                                if (childPackageExport.equalsIgnoreVersion(localPackage)) {
                                    if (childPackageExport.getSourceLocations() != null &&
                                            childPackageExport.getSourceLocations().size() > 0) {
                                        splitPackageInfo.getSourceLocations().addAll(childPackageExport.getSourceLocations());
                                    } else {
                                        splitPackageInfo.getSourceLocations().add(childPackageExport.getOrigin().getFilePath());
                                    }
                                }
                            }
                            splitPackages.add(splitPackageInfo);
                        }
                    }

                    // mark all child taglib uris as external
                    for (String externalTaglibUri : externalTaglibUris) {
                        getExternalTaglibs().put(externalTaglibUri, true);
                    }
                    for (String internalTaglibUri : internalTaglibUris) {
                        getExternalTaglibs().put(internalTaglibUri, true);
                    }
                } else {
                    // child context is internal
                    addAllPackageImports(childParsingContext.getPackageImports());

                    addAllContentTypeDefinitions(childParsingContext.getContentTypeDefinitions());
                    addAllContentTypeReferences(childParsingContext.getContentTypeReferences());

                    // simply keep the internal/external as it was originally.
                    for (String externalTaglibUri : externalTaglibUris) {
                        getExternalTaglibs().put(externalTaglibUri, true);
                    }
                    for (String internalTaglibUri : internalTaglibUris) {
                        getExternalTaglibs().put(internalTaglibUri, false);
                    }
                }
                // add all package ignores as optional packages
                addAllPackageImports(childParsingContext.getPackageIgnores(), true);
            }
        }

        // first let's detect split packages between the imports and the local packages.
        for (PackageInfo localPackage : localPackages) {
            if (PackageUtils.containsIgnoreVersion(packageImports, localPackage)) {
                PackageInfo splitPackageInfo = new PackageInfo(localPackage);
                for (PackageInfo packageImport : packageImports) {
                    if (packageImport.equalsIgnoreVersion(localPackage)) {
                        if (packageImport.getSourceLocations() != null &&
                                packageImport.getSourceLocations().size() > 0) {
                            splitPackageInfo.getSourceLocations().addAll(packageImport.getSourceLocations());
                        } else {
                            splitPackageInfo.getSourceLocations().add(packageImport.getOrigin().getFilePath());
                        }
                    }
                }
                splitPackages.add(splitPackageInfo);
            }
        }

        removeLocalPackagesFromImports();

        contentTypeReferences.removeAll(contentTypeDefinitions);

        // we now resolve the tag lib URIs that we found earlier
        Map<String,Set<String>> resolvedUris = new TreeMap<String,Set<String>>();
        for (Map.Entry<String,Set<String>> unresolvedTaglibUri : unresolvedTaglibUris.entrySet()) {
            for (String singleUri : unresolvedTaglibUri.getValue()) {
                if (taglibUris.contains(singleUri)) {
                    Set<PackageInfo> taglibPackageSet = getTaglibPackages().get(singleUri);
                    if (taglibPackageSet != null) {
                        boolean externalTagLib = getExternalTaglibs().get(singleUri);
                        if (externalTagLib) {
                            addAllPackageImports(taglibPackageSet, null);
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
        }

        // we now remove all the resolved URIs from the unresolved map
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

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ParsingContext{");
        sb.append("mavenCoords='").append(mavenCoords).append('\'');
        sb.append(", lastModified=").append(lastModified);
        sb.append(", fileSize=").append(fileSize);
        sb.append(", fileName='").append(fileName).append('\'');
        sb.append(", filePath='").append(filePath).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", inCache=").append(inCache);
        sb.append(", optional=").append(optional);
        sb.append(", external=").append(external);
        sb.append(", osgiBundle=").append(osgiBundle);
        sb.append(", bundleClassPath=").append(bundleClassPath);
        sb.append('}');
        return sb.toString();
    }

    public void reconnectPackageInfos() {
        reconnectPackageInfos(localPackages);
        reconnectPackageInfos(packageExports);
        reconnectPackageInfos(packageIgnores);
        reconnectPackageInfos(getPackageImports());
        for (Map.Entry<String,Set<PackageInfo>> taglibPackage : getTaglibPackages().entrySet()) {
            reconnectPackageInfos(taglibPackage.getValue());
        }
    }

    private void reconnectPackageInfos(Collection<PackageInfo> packageInfos) {
        for (PackageInfo packageInfo : packageInfos) {
            packageInfo.setOrigin(this);
        }
    }

}
