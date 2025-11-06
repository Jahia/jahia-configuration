/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2025 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.osgi.framework.version;

/**
 * Represents a package version override rule.
 *
 * <p>Format: "packagePattern:version"</p>
 * <ul>
 *   <li>packagePattern - Package name with optional wildcard (*) suffix</li>
 *   <li>version - The version to force for matching packages</li>
 * </ul>
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>"javax.transaction:1.1.1" - Exact match only</li>
 *   <li>"org.apache.xalan*:2.7.3" - Matches org.apache.xalan and all sub-packages</li>
 * </ul>
 */
public class PackageVersionOverride {

    private final String pattern;
    private final String version;
    private final boolean isWildcard;
    private final String packagePrefix;

    /**
     * Parses a version override string in format "pattern:version".
     *
     * @param overrideString the override string
     * @return parsed PackageVersionOverride
     * @throws IllegalArgumentException if format is invalid
     */
    public static PackageVersionOverride parse(String overrideString) {
        if (overrideString == null || overrideString.trim().isEmpty()) {
            throw new IllegalArgumentException("Override string cannot be null or empty");
        }

        int colonIndex = overrideString.lastIndexOf(':');
        if (colonIndex <= 0 || colonIndex == overrideString.length() - 1) {
            throw new IllegalArgumentException(
                "Invalid override format: '" + overrideString + "'. " +
                "Expected format: 'packagePattern:version' (e.g., 'org.apache.xalan*:2.7.3')"
            );
        }

        String pattern = overrideString.substring(0, colonIndex).trim();
        String version = overrideString.substring(colonIndex + 1).trim();

        if (pattern.isEmpty() || version.isEmpty()) {
            throw new IllegalArgumentException(
                "Invalid override format: '" + overrideString + "'. " +
                "Both pattern and version must be non-empty"
            );
        }

        return new PackageVersionOverride(pattern, version);
    }

    /**
     * Creates a new PackageVersionOverride.
     *
     * @param pattern the package pattern (with optional * suffix)
     * @param version the version to apply
     */
    public PackageVersionOverride(String pattern, String version) {
        this.pattern = pattern;
        this.version = version;
        this.isWildcard = pattern.endsWith("*");
        this.packagePrefix = isWildcard ? pattern.substring(0, pattern.length() - 1) : pattern;
    }

    /**
     * Checks if this override matches the given package name.
     *
     * @param packageName the package name to check
     * @return true if this override should be applied to the package
     */
    public boolean matches(String packageName) {
        if (isWildcard) {
            // Wildcard match: package name must start with the prefix
            return packageName.startsWith(packagePrefix);
        } else {
            // Exact match: package name must equal the pattern
            return packageName.equals(pattern);
        }
    }

    /**
     * Gets the original pattern string.
     *
     * @return the pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Gets the override version.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Checks if this is a wildcard pattern.
     *
     * @return true if pattern ends with *
     */
    public boolean isWildcard() {
        return isWildcard;
    }

    @Override
    public String toString() {
        return pattern + ":" + version;
    }
}

