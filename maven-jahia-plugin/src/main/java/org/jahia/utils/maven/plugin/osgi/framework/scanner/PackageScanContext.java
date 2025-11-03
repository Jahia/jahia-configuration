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
package org.jahia.utils.maven.plugin.osgi.framework.scanner;

import org.jahia.utils.maven.plugin.osgi.framework.models.PackageInfo;
import org.jahia.utils.maven.plugin.osgi.framework.models.ScanSource;
import org.jahia.utils.maven.plugin.osgi.framework.models.VersionLocation;
import org.jahia.utils.maven.plugin.osgi.framework.version.VersionCleaner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Manages the state during package scanning, tracking all discovered packages
 * and their version information across multiple locations.
 *
 * This class serves as the central data store for the scanning process.
 */
public class PackageScanContext {

    // Detailed tracking for reporting
    private final Map<String, PackageInfo> packageTracking;

    // Legacy structure for split-package resolution
    private final Map<String, Map<String, Map<String, VersionLocation>>> packageVersionCounts;

    // Maps file paths to artifact coordinates for better reporting
    private final Map<String, String> artifactCoordinatesByPath;

    // Tracks excluded artifacts with their exclusion pattern
    private final Map<String, String> excludedArtifacts;

    /**
     * Creates a new empty PackageScanContext.
     */
    public PackageScanContext() {
        this.packageTracking = new TreeMap<>();
        this.packageVersionCounts = new TreeMap<>();
        this.artifactCoordinatesByPath = new HashMap<>();
        this.excludedArtifacts = new TreeMap<>();
    }

    /**
     * Registers an artifact with its file path for coordinate lookup.
     *
     * @param filePath the canonical file path
     * @param coordinates the Maven coordinates
     */
    public void registerArtifact(String filePath, String coordinates) {
        artifactCoordinatesByPath.put(filePath, coordinates);
    }

    /**
     * Tracks a package that was discovered during scanning.
     *
     * @param packageName the package name
     * @param version the package version
     * @param artifactCoordinates the Maven coordinates of the source artifact
     * @param scanSource how the package was discovered
     * @param parentPackage the parent package if version was inherited
     */
    public void trackPackage(String packageName, String version, String artifactCoordinates,
                            ScanSource scanSource, String parentPackage) {
        PackageInfo packageInfo = packageTracking.get(packageName);
        if (packageInfo == null) {
            packageInfo = new PackageInfo(packageName);
            packageTracking.put(packageName, packageInfo);
        }

        packageInfo.addVersion(version);
        packageInfo.addSourceArtifact(artifactCoordinates, scanSource);

        if (parentPackage != null) {
            packageInfo.setParentPackage(parentPackage);
        }
    }

    /**
     * Updates the version location counts for split-package resolution.
     * This maintains backward compatibility with the original data structure.
     *
     * @param originLocation the file path where the package was found
     * @param newVersion the version to record
     * @param specificationVersion the specification version from manifest
     * @param packageName the package name
     * @throws IOException if an error occurs
     */
    public void updateVersionLocationCounts(String originLocation, String newVersion,
                                           String specificationVersion, String packageName) throws IOException {
        Map<String, Map<String, VersionLocation>> versionLocations =
            packageVersionCounts.computeIfAbsent(packageName, k -> new HashMap<>());

        Map<String, VersionLocation> existingVersionLocations = versionLocations.get(originLocation);

        if (existingVersionLocations != null && existingVersionLocations.containsKey(newVersion)) {
            VersionLocation existingVersionLocation = existingVersionLocations.get(newVersion);
            existingVersionLocation.incrementCounter();
        } else {
            if (existingVersionLocations == null) {
                existingVersionLocations = new HashMap<>();
                versionLocations.put(originLocation, existingVersionLocations);
            }

            VersionLocation versionLocation = new VersionLocation(originLocation,
                                                                  VersionCleaner.cleanupVersion(newVersion),
                                                                  specificationVersion);
            versionLocation.incrementCounter();
            existingVersionLocations.put(newVersion, versionLocation);
        }
    }

    /**
     * Marks a package as excluded.
     *
     * @param packageName the package name
     * @param pattern the exclusion pattern that matched
     */
    public void markPackageExcluded(String packageName, String pattern) {
        PackageInfo packageInfo = packageTracking.get(packageName);
        if (packageInfo == null) {
            packageInfo = new PackageInfo(packageName);
            packageTracking.put(packageName, packageInfo);
        }
        packageInfo.setExcluded(pattern);
    }

    /**
     * Gets the artifact coordinates for a file path.
     *
     * @param filePath the file path
     * @return the coordinates, or null if not found
     */
    public String getArtifactCoordinates(String filePath) {
        return artifactCoordinatesByPath.get(filePath);
    }

    public Map<String, PackageInfo> getPackageTracking() {
        return packageTracking;
    }

    public Map<String, Map<String, Map<String, VersionLocation>>> getPackageVersionCounts() {
        return packageVersionCounts;
    }

    public Map<String, String> getArtifactCoordinatesByPath() {
        return artifactCoordinatesByPath;
    }

    /**
     * Tracks an excluded artifact.
     *
     * @param artifactCoordinates the Maven coordinates (groupId:artifactId:version)
     * @param exclusionPattern the pattern that caused the exclusion
     */
    public void trackExcludedArtifact(String artifactCoordinates, String exclusionPattern) {
        excludedArtifacts.put(artifactCoordinates, exclusionPattern);
    }

    /**
     * Gets the map of excluded artifacts and their exclusion patterns.
     *
     * @return map of artifact coordinates to exclusion pattern
     */
    public Map<String, String> getExcludedArtifacts() {
        return excludedArtifacts;
    }
}

