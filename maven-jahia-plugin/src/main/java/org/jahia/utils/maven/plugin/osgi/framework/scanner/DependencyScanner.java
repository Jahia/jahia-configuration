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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.jahia.utils.maven.plugin.osgi.framework.filter.ExclusionFilter;
import org.jahia.utils.maven.plugin.osgi.framework.models.ScanSource;

import java.io.IOException;
import java.util.Set;

/**
 * Scans Maven project dependencies for OSGi package information.
 *
 * <p>This class is responsible for:
 * <ul>
 *   <li>Filtering artifacts based on exclusion patterns (artifact-level filtering)</li>
 *   <li>Checking artifact scope (only compile, provided, runtime)</li>
 *   <li>Delegating JAR scanning to JarScanner for package discovery</li>
 *   <li>Passing ExclusionFilter to JarScanner for early package-level filtering</li>
 * </ul>
 *
 * <p><b>Performance Note:</b> The ExclusionFilter is passed down to all scanners,
 * enabling packages to be filtered immediately when discovered rather than at the end.
 * This significantly reduces memory usage and processing time.
 */
public class DependencyScanner {

    private final ExclusionFilter exclusionFilter;
    private final JarScanner jarScanner;
    private final Log log;

    /**
     * Creates a new DependencyScanner.
     *
     * @param exclusionFilter filter for artifact exclusions
     * @param log Maven logger for output messages
     */
    public DependencyScanner(ExclusionFilter exclusionFilter, Log log) {
        this.exclusionFilter = exclusionFilter;
        this.log = log;
        this.jarScanner = new JarScanner(exclusionFilter, log);
    }

    /**
     * Scans project artifacts for package information.
     *
     * @param artifacts the project artifacts to scan
     * @param scanContext the context to store discovered packages
     * @throws IOException if an error occurs reading artifacts
     */
    public void scanArtifacts(Set<Artifact> artifacts, PackageScanContext scanContext) throws IOException {
        log.info("Scanning project dependencies...");

        for (Artifact artifact : artifacts) {
            if (shouldSkipArtifact(artifact, scanContext)) {
                continue;
            }

            scanArtifact(artifact, scanContext);
        }
    }

    /**
     * Determines if an artifact should be skipped based on exclusions and scope.
     */
    private boolean shouldSkipArtifact(Artifact artifact, PackageScanContext scanContext) {
        // Check exclusion patterns
        String exclusionPattern = exclusionFilter.getArtifactExclusionPattern(
            artifact.getGroupId(), artifact.getArtifactId());

        if (exclusionPattern != null) {
            String artifactCoords = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
            log.info("Matched exclusion " + artifact.getGroupId() + ":" +
                     artifact.getArtifactId() + ", ignoring artifact.");
            scanContext.trackExcludedArtifact(artifactCoords, exclusionPattern);
            return true;
        }

        // Check scope
        String scope = artifact.getScope();
        if (!Artifact.SCOPE_PROVIDED.equals(scope) &&
            !Artifact.SCOPE_COMPILE.equals(scope) &&
            !Artifact.SCOPE_RUNTIME.equals(scope)) {
            return true;
        }

        // Check type
        if (!"jar".equals(artifact.getType())) {
            log.warn("Ignoring artifact " + artifact.getFile() +
                     " since it is of type " + artifact.getType());
            return true;
        }

        return false;
    }

    /**
     * Scans a single artifact.
     */
    private void scanArtifact(Artifact artifact, PackageScanContext scanContext) throws IOException {
        log.debug("Scanning dependency " + artifact.getFile());

        String artifactCoordinates = artifact.getGroupId() + ":" +
                                    artifact.getArtifactId() + ":" +
                                    artifact.getBaseVersion();

        // Register artifact for coordinate lookup
        try {
            String canonicalPath = artifact.getFile().getCanonicalPath();
            scanContext.registerArtifact(canonicalPath, artifactCoordinates);
        } catch (IOException e) {
            log.warn("Could not get canonical path for " + artifact.getFile() + ": " + e.getMessage());
            scanContext.registerArtifact(artifact.getFile().getAbsolutePath(), artifactCoordinates);
        }

        // Scan the JAR file
        jarScanner.scanJar(
            artifact.getFile(),
            artifact.getBaseVersion(),
            artifactCoordinates,
            new JarScanner.PackageCallback() {
                @Override
                public void onPackageFound(String packageName, String version, String specificationVersion,
                                         String artifactCoords, ScanSource scanSource, String parentPackage) {
                    try {
                        String canonicalPath = artifact.getFile().getCanonicalPath();
                        scanContext.updateVersionLocationCounts(canonicalPath, version,
                                                               specificationVersion, packageName);
                        scanContext.trackPackage(packageName, version, artifactCoords,
                                               scanSource, parentPackage);
                    } catch (IOException e) {
                        log.warn("Error updating version location counts: " + e.getMessage());
                    }
                }

                @Override
                public void onPackageExcluded(String packageName, String exclusionPattern, String artifactCoords) {
                    // Track the excluded package for reporting
                    scanContext.markPackageExcluded(packageName, exclusionPattern);
                }
            }
        );
    }
}

