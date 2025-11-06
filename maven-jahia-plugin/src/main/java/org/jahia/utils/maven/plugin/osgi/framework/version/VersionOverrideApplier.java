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

import org.apache.maven.plugin.logging.Log;

import java.util.*;

/**
 * Applies version overrides to packages based on configured rules.
 *
 * <p>This class handles:
 * <ul>
 *   <li>Parsing override rules from configuration</li>
 *   <li>Applying overrides to package versions</li>
 *   <li>Tracking which packages were overridden for reporting</li>
 * </ul>
 */
public class VersionOverrideApplier {

    private final List<PackageVersionOverride> overrides;
    private final Map<String, VersionOverrideInfo> appliedOverrides;
    private final Log log;

    /**
     * Creates a new VersionOverrideApplier.
     *
     * @param overrideStrings list of override strings in format "pattern:version"
     * @param log Maven logger
     */
    public VersionOverrideApplier(List<String> overrideStrings, Log log) {
        this.log = log;
        this.overrides = new ArrayList<>();
        this.appliedOverrides = new TreeMap<>();

        if (overrideStrings != null && !overrideStrings.isEmpty()) {
            parseOverrides(overrideStrings);
        }
    }

    /**
     * Parses override strings and validates them.
     */
    private void parseOverrides(List<String> overrideStrings) {
        for (String overrideString : overrideStrings) {
            try {
                PackageVersionOverride override = PackageVersionOverride.parse(overrideString);
                overrides.add(override);
                log.info("Loaded version override: " + override);
            } catch (IllegalArgumentException e) {
                log.warn("Skipping invalid version override: " + e.getMessage());
            }
        }

        if (!overrides.isEmpty()) {
            log.info("Configured " + overrides.size() + " package version override(s)");
        }
    }

    /**
     * Applies version overrides to the package versions map.
     *
     * <p>For each package, checks if any override rule matches.
     * If matched, replaces all versions with the override version.</p>
     *
     * @param packageVersions map of package name to set of versions (modified in place)
     */
    public void applyOverrides(Map<String, Set<String>> packageVersions) {
        if (overrides.isEmpty()) {
            return;
        }

        int totalOverridden = 0;

        for (Map.Entry<String, Set<String>> entry : packageVersions.entrySet()) {
            String packageName = entry.getKey();
            Set<String> versions = entry.getValue();

            // Check each override rule
            for (PackageVersionOverride override : overrides) {
                if (override.matches(packageName)) {
                    // Store original versions for reporting
                    Set<String> originalVersions = new HashSet<>(versions);

                    // Replace all versions with the override version
                    versions.clear();
                    versions.add(override.getVersion());

                    // Track this override for reporting
                    appliedOverrides.put(packageName, new VersionOverrideInfo(
                        packageName,
                        originalVersions,
                        override.getVersion(),
                        override.getPattern()
                    ));

                    totalOverridden++;
                    log.debug("Applied version override to " + packageName + ": " +
                             originalVersions + " → " + override.getVersion() +
                             " (matched by: " + override.getPattern() + ")");

                    // Only apply first matching override
                    break;
                }
            }
        }

        if (totalOverridden > 0) {
            log.info("Applied version overrides to " + totalOverridden + " package(s)");
        } else {
            log.warn("No packages matched the configured version override patterns");
        }
    }

    /**
     * Gets the map of applied overrides for reporting.
     *
     * @return map of package name to override information
     */
    public Map<String, VersionOverrideInfo> getAppliedOverrides() {
        return Collections.unmodifiableMap(appliedOverrides);
    }

    /**
     * Checks if any overrides were applied.
     *
     * @return true if at least one package was overridden
     */
    public boolean hasAppliedOverrides() {
        return !appliedOverrides.isEmpty();
    }

    /**
     * Information about an applied version override.
     */
    public static class VersionOverrideInfo {
        private final String packageName;
        private final Set<String> originalVersions;
        private final String overriddenVersion;
        private final String matchedPattern;

        public VersionOverrideInfo(String packageName, Set<String> originalVersions,
                                   String overriddenVersion, String matchedPattern) {
            this.packageName = packageName;
            this.originalVersions = new HashSet<>(originalVersions);
            this.overriddenVersion = overriddenVersion;
            this.matchedPattern = matchedPattern;
        }

        public String getPackageName() {
            return packageName;
        }

        public Set<String> getOriginalVersions() {
            return Collections.unmodifiableSet(originalVersions);
        }

        public String getOverriddenVersion() {
            return overriddenVersion;
        }

        public String getMatchedPattern() {
            return matchedPattern;
        }
    }
}

