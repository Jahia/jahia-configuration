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
package org.jahia.utils.maven.plugin.osgi.framework.version;

import org.apache.maven.plugin.logging.Log;
import org.jahia.utils.maven.plugin.osgi.framework.models.VersionLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resolves split-package scenarios where the same package appears in multiple JARs.
 * Detects and reports version conflicts, then consolidates versions for final export.
 */
public class VersionResolver {

    private final Log log;

    /**
     * Creates a new VersionResolver with a Maven logger.
     *
     * @param log Maven logger for output messages
     */
    public VersionResolver(Log log) {
        this.log = log;
    }

    /**
     * Resolves package versions from the collected version location data.
     * Detects split packages with different versions and logs warnings.
     *
     * @param packageVersionCounts map of package -> location -> version -> VersionLocation
     * @return map of package name to set of versions to export
     */
    public Map<String, Set<String>> resolveSplitPackages(
            Map<String, Map<String, Map<String, VersionLocation>>> packageVersionCounts) {

        Map<String, Set<String>> packageVersions = new HashMap<>();

        for (Map.Entry<String, Map<String, Map<String, VersionLocation>>> packageEntry :
                packageVersionCounts.entrySet()) {

            String packageName = packageEntry.getKey();
            Map<String, Map<String, VersionLocation>> locationVersions = packageEntry.getValue();

            boolean allVersionsEqual = checkVersionConsistency(locationVersions);

            if (locationVersions.size() > 1 && !allVersionsEqual) {
                warnSplitPackage(packageName, locationVersions);
            }

            Set<String> versions = collectVersions(locationVersions);
            packageVersions.put(packageName, versions);
        }

        return packageVersions;
    }

    /**
     * Checks if all locations have the same set of versions.
     */
    private boolean checkVersionConsistency(Map<String, Map<String, VersionLocation>> locationVersions) {
        Set<String> previousVersions = null;

        for (Map<String, VersionLocation> versionLocations : locationVersions.values()) {
            Set<String> currentVersions = versionLocations.keySet();

            if (previousVersions != null && !previousVersions.equals(currentVersions)) {
                return false;
            }

            previousVersions = currentVersions;
        }

        return true;
    }

    /**
     * Logs a warning about split packages with version conflicts.
     */
    private void warnSplitPackage(String packageName, Map<String, Map<String, VersionLocation>> locationVersions) {
        log.warn("Split-package with different versions detected for package " + packageName + ":");

        for (Map.Entry<String, Map<String, VersionLocation>> locationEntry : locationVersions.entrySet()) {
            String location = locationEntry.getKey();

            for (Map.Entry<String, VersionLocation> versionEntry : locationEntry.getValue().entrySet()) {
                VersionLocation versionLocation = versionEntry.getValue();
                log.warn("  - " + location + " v" + versionLocation.getVersion() +
                         " count=" + versionLocation.getCounter() +
                         " Specification-Version=" + versionLocation.getSpecificationVersion());
            }
        }
    }

    /**
     * Collects all unique versions from all locations.
     */
    private Set<String> collectVersions(Map<String, Map<String, VersionLocation>> locationVersions) {
        Set<String> versions = new HashSet<>();

        for (Map<String, VersionLocation> versionLocations : locationVersions.values()) {
            if (versionLocations != null) {
                versions.addAll(versionLocations.keySet());
            }
        }

        return versions;
    }
}

