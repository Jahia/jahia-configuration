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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.jahia.utils.maven.plugin.osgi.framework.filter.ExclusionFilter;
import org.jahia.utils.maven.plugin.osgi.framework.models.ScanSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Scans JAR files to extract package information.
 *
 * <p>This class discovers packages from three sources (in priority order):
 * <ol>
 *   <li><b>Export-Package Header</b> (highest priority)
 *     <ul>
 *       <li>Standard OSGi manifest header with explicit package exports</li>
 *       <li>Includes version information and other directives</li>
 *       <li>If present, other sources are skipped for this JAR</li>
 *       <li>Handled by ManifestScanner</li>
 *     </ul>
 *   </li>
 *   <li><b>Manifest Package Entries</b>
 *     <ul>
 *       <li>Named sections in MANIFEST.MF with Specification-Version or Implementation-Version</li>
 *       <li>Example: [org/apache/commons/lang/] section with version attributes</li>
 *       <li>Child packages can inherit versions from parent package entries</li>
 *     </ul>
 *   </li>
 *   <li><b>JAR File Structure</b> (fallback)
 *     <ul>
 *       <li>Discovers packages by analyzing directory structure in JAR</li>
 *       <li>Uses artifact version or inherited parent package version</li>
 *       <li>Skips META-INF, OSGI-INF, WEB-INF, and org.osgi.* packages</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p><b>IMPORTANT - Early Filtering Optimization:</b></p>
 * <p>For each package discovered from ANY source, the ExclusionFilter is checked
 * immediately. If the package matches an exclusion pattern, it is skipped and
 * never added to the scan context. This prevents excluded packages from consuming
 * memory or being processed in downstream operations (version resolution, etc.).
 *
 * <p><b>Filtering happens at:</b></p>
 * <ul>
 *   <li>ManifestScanner.scanExportPackageHeader() - filters Export-Package entries</li>
 *   <li>scanManifestEntries() - filters manifest package entries</li>
 *   <li>scanJarEntries() - filters packages discovered from file structure</li>
 * </ul>
 */
public class JarScanner {

    private final ExclusionFilter exclusionFilter;
    private final Log log;
    private final ManifestScanner manifestScanner;

    /**
     * Creates a new JarScanner with logging.
     *
     * @param exclusionFilter filter for package exclusions
     * @param log Maven logger for output messages
     */
    public JarScanner(ExclusionFilter exclusionFilter, Log log) {
        this.exclusionFilter = exclusionFilter;
        this.log = log;
        this.manifestScanner = new ManifestScanner(exclusionFilter, log);
    }

    /**
     * Scans a JAR file and extracts package information.
     *
     * @param jarFile the JAR file to scan
     * @param defaultVersion the default version to use if no version is found
     * @param artifactCoordinates the Maven coordinates of this artifact
     * @param packageCallback callback invoked for each discovered package
     * @throws IOException if an error occurs reading the JAR file
     */
    public void scanJar(File jarFile, String defaultVersion, String artifactCoordinates,
                       PackageCallback packageCallback) throws IOException {

        try (JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile))) {
            Manifest manifest = jarInputStream.getManifest();
            String specificationVersion = null;
            Map<String, String> manifestPackageVersions = new HashMap<>();

            if (manifest == null) {
                log.warn("No MANIFEST.MF file found for dependency " + jarFile);
            } else {
                specificationVersion = extractSpecificationVersion(manifest, jarFile, defaultVersion);

                // First check for Export-Package header - if found, we're done
                if (manifestScanner.scanExportPackageHeader(manifest, jarFile, artifactCoordinates, packageCallback)) {
                    return; // Export-Package found and processed, no need to scan further
                }

                // Collect manifest package entries
                manifestPackageVersions = scanManifestEntries(manifest, jarFile, specificationVersion,
                                                             artifactCoordinates, packageCallback);
            }

            // Scan JAR contents
            scanJarEntries(jarInputStream, jarFile, defaultVersion, specificationVersion,
                          manifestPackageVersions, artifactCoordinates, packageCallback);
        }
    }

    /**
     * Extracts the specification version from the manifest and updates default version if needed.
     */
    private String extractSpecificationVersion(Manifest manifest, File jarFile, String defaultVersion) {
        Attributes mainAttributes = manifest.getMainAttributes();
        if (mainAttributes == null) {
            log.warn("No main attributes found in MANIFEST.MF file found for dependency " + jarFile);
            return null;
        }

        String specificationVersion = mainAttributes.getValue("Specification-Version");

        // Update default version if not set
        if (defaultVersion == null) {
            String bundleVersion = mainAttributes.getValue("Bundle-Version");
            if (bundleVersion != null) {
                defaultVersion = bundleVersion;
            } else if (specificationVersion != null) {
                defaultVersion = specificationVersion;
            } else {
                defaultVersion = mainAttributes.getValue("Implementation-Version");
            }
        }

        return specificationVersion;
    }

    /**
     * Scans manifest entries for package version information.
     */
    private Map<String, String> scanManifestEntries(Manifest manifest, File jarFile, String specificationVersion,
                                                     String artifactCoordinates, PackageCallback packageCallback) {
        Map<String, String> manifestPackageVersions = new HashMap<>();

        for (Map.Entry<String, Attributes> manifestEntry : manifest.getEntries().entrySet()) {
            String packageName = normalizePackageName(manifestEntry.getKey());

            if (shouldSkipPackage(packageName)) {
                continue;
            }

            String packageVersion = extractPackageVersion(manifestEntry.getValue());
            if (packageVersion != null) {
                log.info("Found package version in " + jarFile.getName() +
                         " MANIFEST : " + packageName + " v" + packageVersion);
                manifestPackageVersions.put(packageName, packageVersion);

                // Check if package is excluded
                if (exclusionFilter != null && exclusionFilter.isPackageExcluded(packageName)) {
                    String pattern = exclusionFilter.getPackageExclusionPattern(packageName);
                    log.debug("Package " + packageName + " matched exclusion pattern '" + pattern +
                              "', skipping from " + jarFile.getName());
                    // Still track for reporting but mark as excluded
                    packageCallback.onPackageFound(packageName, packageVersion, specificationVersion,
                                                  artifactCoordinates, ScanSource.MANIFEST_ENTRY, null);
                    packageCallback.onPackageExcluded(packageName, pattern, artifactCoordinates);
                    continue;
                }

                packageCallback.onPackageFound(packageName, packageVersion, specificationVersion,
                                              artifactCoordinates, ScanSource.MANIFEST_ENTRY, null);
            }
        }

        return manifestPackageVersions;
    }

    /**
     * Scans the actual JAR entries (files) for package structure.
     */
    private void scanJarEntries(JarInputStream jarInputStream, File jarFile, String defaultVersion,
                               String specificationVersion, Map<String, String> manifestPackageVersions,
                               String artifactCoordinates, PackageCallback packageCallback) throws IOException {
        JarEntry jarEntry;

        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            if (jarEntry.isDirectory()) {
                continue;
            }

            String entryName = jarEntry.getName();
            int lastSlash = entryName.lastIndexOf("/");

            if (lastSlash <= 0) {
                continue; // No package
            }

            String packageName = entryName.substring(0, lastSlash).replace("/", ".");

            if (shouldSkipPackage(packageName)) {
                continue;
            }

            // Look for parent package version in manifest
            String version = findParentPackageVersion(packageName, manifestPackageVersions);
            String parentPackage = null;
            ScanSource scanSource = ScanSource.JAR_SCAN;

            if (version != null) {
                scanSource = ScanSource.PARENT_PACKAGE_INHERITANCE;
                parentPackage = findActualParentPackage(packageName, manifestPackageVersions);
            } else {
                version = defaultVersion;
            }

            // Check if package is excluded
            if (exclusionFilter != null && exclusionFilter.isPackageExcluded(packageName)) {
                // Track for reporting but don't process further
                packageCallback.onPackageFound(packageName, version, specificationVersion,
                                              artifactCoordinates, scanSource, parentPackage);
                String pattern = exclusionFilter.getPackageExclusionPattern(packageName);
                packageCallback.onPackageExcluded(packageName, pattern, artifactCoordinates);
                continue;
            }

            packageCallback.onPackageFound(packageName, version, specificationVersion,
                                          artifactCoordinates, scanSource, parentPackage);
        }
    }

    /**
     * Normalizes a manifest entry name to a package name.
     */
    private String normalizePackageName(String entryName) {
        String packageName = entryName.replace("/", ".");

        if (packageName.endsWith(".class")) {
            return null; // Skip class files
        }

        if (packageName.endsWith(".")) {
            packageName = packageName.substring(0, packageName.length() - 1);
        }

        if (packageName.endsWith(".*")) {
            packageName = packageName.substring(0, packageName.length() - 1);
        }

        // Check if last component starts with uppercase (likely a class, not a package)
        int lastDotPos = packageName.lastIndexOf(".");
        if (lastDotPos > -1) {
            String lastComponent = packageName.substring(lastDotPos + 1);
            if (!lastComponent.isEmpty() && Character.isUpperCase(lastComponent.charAt(0))) {
                return null; // Skip, likely a class
            }
        }

        return packageName;
    }

    /**
     * Determines if a package should be skipped based on naming conventions.
     */
    private boolean shouldSkipPackage(String packageName) {
        return StringUtils.isEmpty(packageName) ||
               packageName.startsWith("META-INF") ||
               packageName.startsWith("OSGI-INF") ||
               packageName.startsWith("OSGI-OPT") ||
               packageName.startsWith("WEB-INF") ||
               packageName.startsWith("org.osgi");
    }

    /**
     * Extracts package version from manifest attributes.
     */
    private String extractPackageVersion(Attributes attributes) {
        String version = attributes.getValue("Specification-Version");
        if (version == null) {
            version = attributes.getValue("Implementation-Version");
        }
        return version;
    }

    /**
     * Finds the version for a package by looking up parent packages in manifest entries.
     */
    private String findParentPackageVersion(String packageName, Map<String, String> manifestPackageVersions) {
        if (manifestPackageVersions.containsKey(packageName)) {
            return manifestPackageVersions.get(packageName);
        }

        String currentPackage = packageName;
        while (currentPackage.contains(".")) {
            int lastDotPos = currentPackage.lastIndexOf(".");
            currentPackage = currentPackage.substring(0, lastDotPos);

            if (manifestPackageVersions.containsKey(currentPackage)) {
                log.debug("Found parent package " + currentPackage + " for " + packageName);
                return manifestPackageVersions.get(currentPackage);
            }
        }

        return null;
    }

    /**
     * Finds the actual parent package name (not just version).
     */
    private String findActualParentPackage(String packageName, Map<String, String> manifestPackageVersions) {
        if (manifestPackageVersions.containsKey(packageName)) {
            return packageName;
        }

        String currentPackage = packageName;
        while (currentPackage.contains(".")) {
            int lastDotPos = currentPackage.lastIndexOf(".");
            currentPackage = currentPackage.substring(0, lastDotPos);

            if (manifestPackageVersions.containsKey(currentPackage)) {
                return currentPackage;
            }
        }

        return null;
    }

    /**
     * Callback interface for package discovery events.
     */
    public interface PackageCallback {
        /**
         * Called when a package is discovered during scanning.
         *
         * @param packageName the package name
         * @param version the package version
         * @param specificationVersion the specification version from the manifest
         * @param artifactCoordinates the Maven coordinates
         * @param scanSource how the package was discovered
         * @param parentPackage the parent package if version was inherited
         */
        void onPackageFound(String packageName, String version, String specificationVersion,
                           String artifactCoordinates, ScanSource scanSource, String parentPackage);

        /**
         * Called when a package is excluded by a filter during scanning.
         *
         * @param packageName the package name
         * @param exclusionPattern the pattern that matched and caused exclusion
         * @param artifactCoordinates the Maven coordinates of the source artifact
         */
        void onPackageExcluded(String packageName, String exclusionPattern, String artifactCoordinates);
    }
}

