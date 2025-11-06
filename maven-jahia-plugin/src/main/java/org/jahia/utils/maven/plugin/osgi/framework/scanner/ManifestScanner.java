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
package org.jahia.utils.maven.plugin.osgi.framework.scanner;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.osgi.util.ManifestElement;
import org.jahia.utils.maven.plugin.osgi.framework.filter.ExclusionFilter;
import org.jahia.utils.maven.plugin.osgi.framework.models.ScanSource;
import org.jahia.utils.maven.plugin.osgi.framework.scanner.JarScanner.PackageCallback;
import org.osgi.framework.BundleException;

import java.io.File;
import java.util.jar.Manifest;

/**
 * Handles parsing of OSGi Export-Package manifest headers.
 *
 * <p>The Export-Package header is the standard OSGi way to declare which packages
 * a bundle exports. It has the highest priority - if found, other package discovery
 * methods are skipped for that JAR.</p>
 *
 * <p><b>Header Format Examples:</b></p>
 * <pre>
 * Export-Package: org.example.api;version="1.0.0",
 *                 org.example.util;version="1.2.3"
 * </pre>
 *
 * <p><b>Parsing:</b></p>
 * <p>Uses Eclipse OSGi's ManifestElement parser to properly handle:
 * <ul>
 *   <li>Multiple package declarations separated by commas</li>
 *   <li>Version attributes and directives</li>
 *   <li>Quoted strings and escaped characters</li>
 *   <li>Complex OSGi header syntax</li>
 * </ul>
 *
 * <p><b>IMPORTANT - Early Filtering:</b></p>
 * <p>Each package from the Export-Package header is immediately checked against
 * the ExclusionFilter. If it matches an exclusion pattern, it is skipped and
 * the callback is never invoked. This prevents excluded packages from being
 * stored in the scan context, saving memory and processing time.</p>
 *
 * <p><b>Version Defaults:</b></p>
 * <p>If a package in Export-Package has no version attribute, "0.0.0" is used
 * as the default version (OSGi specification default).</p>
 */
public class ManifestScanner {

    private static final String DEFAULT_VERSION = "0.0.0";

    private final ExclusionFilter exclusionFilter;
    private final Log log;

    /**
     * Creates a new ManifestScanner with logging.
     *
     * @param exclusionFilter filter for package exclusions
     * @param log Maven logger for output messages
     */
    public ManifestScanner(ExclusionFilter exclusionFilter, Log log) {
        this.exclusionFilter = exclusionFilter;
        this.log = log;
    }

    /**
     * Scans the Export-Package header in the manifest and extracts package information.
     *
     * @param manifest the JAR manifest
     * @param jarFile the JAR file (for logging)
     * @param artifactCoordinates the Maven coordinates
     * @param packageCallback callback for discovered packages
     * @return true if Export-Package was found and processed, false otherwise
     */
    public boolean scanExportPackageHeader(Manifest manifest, File jarFile, String artifactCoordinates,
                                          PackageCallback packageCallback) {
        if (manifest == null || manifest.getMainAttributes() == null) {
            return false;
        }

        String exportPackageHeaderValue = manifest.getMainAttributes().getValue("Export-Package");
        if (exportPackageHeaderValue == null) {
            return false;
        }

        ManifestElement[] manifestElements;
        try {
            manifestElements = ManifestElement.parseHeader("Export-Package", exportPackageHeaderValue);
        } catch (BundleException e) {
            log.warn("Error while parsing Export-Package header value for jar " + jarFile + ": " + e.getMessage());
            return false;
        }

        if (manifestElements == null || manifestElements.length == 0) {
            return false;
        }

        for (ManifestElement manifestElement : manifestElements) {
            String[] packageNames = manifestElement.getValueComponents();
            String version = manifestElement.getAttribute("version");

            // If no version is specified in Export-Package, use 0.0.0 as default
            if (version == null || version.trim().isEmpty()) {
                version = DEFAULT_VERSION;
                log.debug("Package from Export-Package header has no version, using default " +
                          DEFAULT_VERSION + " for packages in " + jarFile.getName());
            }

            for (String packageName : packageNames) {
                // Check if package is excluded
                if (exclusionFilter != null && exclusionFilter.isPackageExcluded(packageName)) {
                    String pattern = exclusionFilter.getPackageExclusionPattern(packageName);
                    log.debug("Package " + packageName + " matched exclusion pattern '" + pattern +
                              "', skipping from " + jarFile.getName());
                    // Still track the package but mark it as excluded for reporting
                    packageCallback.onPackageFound(packageName, version, version, artifactCoordinates,
                                                  ScanSource.EXPORT_PACKAGE_HEADER, null);
                    packageCallback.onPackageExcluded(packageName, pattern, artifactCoordinates);
                    continue;
                }

                packageCallback.onPackageFound(packageName, version, version, artifactCoordinates,
                                              ScanSource.EXPORT_PACKAGE_HEADER, null);
            }
        }

        return true; // Export-Package was found and processed
    }
}

