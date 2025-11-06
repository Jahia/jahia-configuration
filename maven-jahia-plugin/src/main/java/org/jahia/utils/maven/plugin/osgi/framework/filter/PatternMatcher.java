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
package org.jahia.utils.maven.plugin.osgi.framework.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for building and applying exclusion patterns for artifacts and packages.
 * Supports wildcard patterns that are converted to regular expressions.
 */
public class PatternMatcher {

    private final List<Pattern> patterns;

    /**
     * Creates a new PatternMatcher with the specified patterns.
     *
     * @param patterns the list of compiled patterns
     */
    private PatternMatcher(List<Pattern> patterns) {
        this.patterns = patterns;
    }

    /**
     * Tests if the given value matches any of the exclusion patterns.
     *
     * @param value the value to test
     * @return true if the value matches at least one pattern, false otherwise
     */
    public boolean matches(String value) {
        if (value == null) {
            return false;
        }

        for (Pattern pattern : patterns) {
            if (pattern.matcher(value).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the first pattern that matches the given value.
     *
     * @param value the value to test
     * @return the pattern string that matched, or null if no match
     */
    public String getMatchingPattern(String value) {
        if (value == null) {
            return null;
        }

        for (Pattern pattern : patterns) {
            if (pattern.matcher(value).matches()) {
                return pattern.pattern();
            }
        }
        return null;
    }

    /**
     * Gets all patterns.
     *
     * @return the list of compiled patterns
     */
    public List<Pattern> getPatterns() {
        return new ArrayList<>(patterns);
    }

    /**
     * Builder for creating PatternMatcher instances for artifact exclusions.
     */
    public static class ArtifactPatternBuilder {

        private final List<Pattern> patterns = new ArrayList<>();

        /**
         * Adds an artifact exclusion pattern.
         * Pattern format: "groupId:artifactId" where both can contain wildcards (*).
         * If no colon is present, the pattern is applied to the artifact ID only.
         *
         * @param pattern the artifact pattern (e.g., "org.jahia.modules:*")
         * @return this builder for method chaining
         */
        public ArtifactPatternBuilder addPattern(String pattern) {
            if (pattern == null || pattern.trim().isEmpty()) {
                return this;
            }

            int colonPos = pattern.indexOf(":");
            String groupPattern;
            String artifactPattern;

            if (colonPos > -1) {
                groupPattern = pattern.substring(0, colonPos);
                artifactPattern = pattern.substring(colonPos + 1);
            } else {
                groupPattern = ".*";
                artifactPattern = pattern;
            }

            String regex = convertToRegex(groupPattern) + ":" + convertToRegex(artifactPattern);
            patterns.add(Pattern.compile(regex));

            return this;
        }

        /**
         * Adds multiple artifact exclusion patterns.
         *
         * @param patterns the list of patterns to add
         * @return this builder for method chaining
         */
        public ArtifactPatternBuilder addPatterns(List<String> patterns) {
            if (patterns != null) {
                for (String pattern : patterns) {
                    addPattern(pattern);
                }
            }
            return this;
        }

        /**
         * Builds the PatternMatcher.
         *
         * @return a new PatternMatcher instance
         */
        public PatternMatcher build() {
            return new PatternMatcher(patterns);
        }
    }

    /**
     * Builder for creating PatternMatcher instances for package exclusions.
     */
    public static class PackagePatternBuilder {

        private final List<Pattern> patterns = new ArrayList<>();

        /**
         * Adds a package exclusion pattern.
         * Pattern format: fully qualified package name with wildcard support (*).
         * Example: "org.jahia.taglibs*" matches "org.jahia.taglibs" and all sub-packages.
         *
         * @param pattern the package pattern
         * @return this builder for method chaining
         */
        public PackagePatternBuilder addPattern(String pattern) {
            if (pattern == null || pattern.trim().isEmpty()) {
                return this;
            }

            String regex = convertToRegex(pattern);
            patterns.add(Pattern.compile(regex));

            return this;
        }

        /**
         * Adds multiple package exclusion patterns.
         *
         * @param patterns the list of patterns to add
         * @return this builder for method chaining
         */
        public PackagePatternBuilder addPatterns(List<String> patterns) {
            if (patterns != null) {
                for (String pattern : patterns) {
                    addPattern(pattern);
                }
            }
            return this;
        }

        /**
         * Builds the PatternMatcher.
         *
         * @return a new PatternMatcher instance
         */
        public PatternMatcher build() {
            return new PatternMatcher(patterns);
        }
    }

    /**
     * Converts a wildcard pattern to a regular expression.
     * Escapes dots and converts asterisks to ".*".
     *
     * @param pattern the wildcard pattern
     * @return the regular expression string
     */
    private static String convertToRegex(String pattern) {
        return pattern
                .replaceAll("\\.", "\\\\.")
                .replaceAll("\\*", ".*");
    }
}

