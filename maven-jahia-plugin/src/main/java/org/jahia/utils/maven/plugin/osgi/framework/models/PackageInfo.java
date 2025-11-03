/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2023 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.osgi.framework.models;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Represents a Java package with its version information, source artifacts, and exclusion status.
 * This class is used to track packages during the OSGi framework package list generation process.
 */
public class PackageInfo {

    private final String packageName;
    private final Set<String> versions;
    private final Map<String, ScanSource> sourceArtifacts;
    private String excludedByPattern;
    private boolean isExcluded;
    private String parentPackage;

    /**
     * Creates a new PackageInfo instance for the specified package name.
     *
     * @param packageName the fully qualified package name (e.g., "org.apache.commons.lang")
     */
    public PackageInfo(String packageName) {
        this.packageName = packageName;
        this.versions = new TreeSet<>();
        this.sourceArtifacts = new TreeMap<>();
        this.isExcluded = false;
        this.parentPackage = null;
    }

    /**
     * Adds a version to this package. Versions are stored in a sorted set.
     *
     * @param version the version string to add (e.g., "1.2.3")
     */
    public void addVersion(String version) {
        if (version != null) {
            versions.add(version);
        }
    }

    /**
     * Adds a source artifact that contains this package.
     *
     * @param artifact the artifact identifier (e.g., Maven coordinates)
     * @param scanSource the method used to discover this package
     */
    public void addSourceArtifact(String artifact, ScanSource scanSource) {
        sourceArtifacts.put(artifact, scanSource);
    }

    /**
     * Marks this package as excluded from the export list.
     *
     * @param pattern the exclusion pattern that matched this package
     */
    public void setExcluded(String pattern) {
        this.isExcluded = true;
        this.excludedByPattern = pattern;
    }

    /**
     * Sets the parent package from which this package inherited its version.
     *
     * @param parentPackage the parent package name
     */
    public void setParentPackage(String parentPackage) {
        this.parentPackage = parentPackage;
    }

    public String getPackageName() {
        return packageName;
    }

    public Set<String> getVersions() {
        return versions;
    }

    public Map<String, ScanSource> getSourceArtifacts() {
        return sourceArtifacts;
    }

    public boolean isExcluded() {
        return isExcluded;
    }

    public String getExcludedByPattern() {
        return excludedByPattern;
    }

    public String getParentPackage() {
        return parentPackage;
    }
}

