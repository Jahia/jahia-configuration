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
package org.jahia.utils.maven.plugin.osgi.framework.filter;

import org.jahia.utils.maven.plugin.osgi.framework.models.PackageInfo;

/**
 * Filters packages and artifacts based on user-configured exclusion patterns.
 *
 * <p>This class provides centralized exclusion logic used throughout the scanning process.
 * It encapsulates two types of filtering:</p>
 *
 * <h3>1. Artifact-Level Filtering</h3>
 * <p>Excludes entire Maven artifacts (JARs) from being scanned.
 * Format: "groupId:artifactId" with wildcard support (*)</p>
 * <p>Example: "org.osgi:*" excludes all OSGi framework artifacts</p>
 * <p>Applied in: DependencyScanner before scanning a JAR</p>
 *
 * <h3>2. Package-Level Filtering</h3>
 * <p>Excludes specific Java packages from the final export list.
 * Format: Package names with optional wildcard suffix (*)</p>
 * <p>Example: "org.example.internal.*" excludes all internal packages</p>
 * <p><b>Applied EARLY during scanning</b> (PERFORMANCE OPTIMIZATION)</p>
 * <p>Applied in:</p>
 * <ul>
 *   <li>ManifestScanner - when parsing Export-Package headers</li>
 *   <li>JarScanner.scanManifestEntries() - when processing manifest packages</li>
 *   <li>JarScanner.scanJarEntries() - when discovering packages from file structure</li>
 * </ul>
 *
 * <h3>Why Early Filtering Matters</h3>
 * <p>By checking exclusions during scanning rather than at the end:
 * <ul>
 *   <li><b>Memory Efficiency:</b> Excluded packages never stored in data structures</li>
 *   <li><b>CPU Efficiency:</b> Excluded packages never processed in version resolution</li>
 *   <li><b>Cleaner Code:</b> Only relevant packages in internal structures</li>
 * </ul>
 *
 * <p>For a typical project with 500 excluded packages out of 5000 total:
 * <ul>
 *   <li>Before: All 5000 packages scanned, stored, processed, then 500 filtered</li>
 *   <li>After: Only 4500 packages scanned, stored, and processed</li>
 *   <li>Result: ~10% memory savings, ~10% faster processing</li>
 * </ul>
 */
public class ExclusionFilter {

    private final PatternMatcher artifactMatcher;
    private final PatternMatcher packageMatcher;

    /**
     * Creates a new ExclusionFilter with the specified matchers.
     *
     * @param artifactMatcher matcher for artifact exclusions
     * @param packageMatcher matcher for package exclusions
     */
    public ExclusionFilter(PatternMatcher artifactMatcher, PatternMatcher packageMatcher) {
        this.artifactMatcher = artifactMatcher;
        this.packageMatcher = packageMatcher;
    }

    /**
     * Checks if an artifact should be excluded based on its group and artifact ID.
     *
     * @param groupId the artifact's group ID
     * @param artifactId the artifact's artifact ID
     * @return true if the artifact should be excluded, false otherwise
     */
    public boolean isArtifactExcluded(String groupId, String artifactId) {
        String coordinates = groupId + ":" + artifactId;
        return artifactMatcher.matches(coordinates);
    }

    /**
     * Gets the pattern that matched for artifact exclusion.
     *
     * @param groupId the artifact's group ID
     * @param artifactId the artifact's artifact ID
     * @return the matching pattern, or null if not excluded
     */
    public String getArtifactExclusionPattern(String groupId, String artifactId) {
        String coordinates = groupId + ":" + artifactId;
        return artifactMatcher.getMatchingPattern(coordinates);
    }

    /**
     * Checks if a package should be excluded.
     * Tests both the full package export instruction and the package name alone.
     *
     * @param packageName the package name to test
     * @return true if the package should be excluded, false otherwise
     */
    public boolean isPackageExcluded(String packageName) {
        return packageMatcher.matches(packageName);
    }

    /**
     * Gets the pattern that matched for package exclusion.
     *
     * @param packageName the package name
     * @return the matching pattern, or null if not excluded
     */
    public String getPackageExclusionPattern(String packageName) {
        return packageMatcher.getMatchingPattern(packageName);
    }

    /**
     * Marks a PackageInfo as excluded if it matches the exclusion patterns.
     *
     * @param packageInfo the package info to check and potentially mark as excluded
     * @return true if the package was marked as excluded, false otherwise
     */
    public boolean applyExclusionToPackageInfo(PackageInfo packageInfo) {
        String exclusionPattern = getPackageExclusionPattern(packageInfo.getPackageName());
        if (exclusionPattern != null) {
            packageInfo.setExcluded(exclusionPattern);
            return true;
        }
        return false;
    }

    public PatternMatcher getArtifactMatcher() {
        return artifactMatcher;
    }

    public PatternMatcher getPackageMatcher() {
        return packageMatcher;
    }
}

