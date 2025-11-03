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
package org.jahia.utils.maven.plugin.osgi.framework.generator;

import org.apache.maven.plugin.logging.Log;
import org.jahia.utils.maven.plugin.osgi.framework.filter.ExclusionFilter;
import org.jahia.utils.maven.plugin.osgi.framework.version.VersionCleaner;

import java.util.*;

/**
 * Generates the OSGi framework system package list.
 * Formats packages with versions into the proper OSGi export syntax.
 */
public class PackageListGenerator {

    private final ExclusionFilter exclusionFilter;
    private final Log log;

    /**
     * Creates a new PackageListGenerator.
     *
     * @param exclusionFilter filter for excluded packages
     * @param log Maven logger for output messages
     */
    public PackageListGenerator(ExclusionFilter exclusionFilter, Log log) {
        this.exclusionFilter = exclusionFilter;
        this.log = log;
    }

    /**
     * Result of package list generation.
     */
    public static class GenerationResult {
        private final List<String> packageList;
        private final String concatenatedList;

        public GenerationResult(List<String> packageList, String concatenatedList) {
            this.packageList = packageList;
            this.concatenatedList = concatenatedList;
        }

        public List<String> getPackageList() {
            return packageList;
        }

        public String getConcatenatedList() {
            return concatenatedList;
        }
    }

    /**
     * Generates the formatted package list from resolved package versions.
     *
     * @param packageVersions map of package name to set of versions
     * @return generation result containing the formatted package list
     */
    public GenerationResult generatePackageList(Map<String, Set<String>> packageVersions) {
        // Use TreeSet for sorted output
        Set<String> sortedPackageInstructions = new TreeSet<>();

        // Build complete package:version instructions
        for (Map.Entry<String, Set<String>> packageEntry : packageVersions.entrySet()) {
            String packageName = packageEntry.getKey();
            Set<String> versions = packageEntry.getValue();

            if (versions == null) {
                continue;
            }

            for (String versionString : versions) {
                if (versionString == null) {
                    continue;
                }

                String cleanedVersion = VersionCleaner.cleanupVersion(versionString);
                String packageInstruction = packageName + ";version=\"" + cleanedVersion + "\"";

                if (shouldIncludePackage(packageInstruction, packageName)) {
                    sortedPackageInstructions.add(packageInstruction);
                } else {
                    log.info("Package " + packageInstruction + " matched exclusion list, will not be included!");
                }
            }
        }

        return formatPackageList(sortedPackageInstructions);
    }

    /**
     * Checks if a package should be included (not excluded).
     */
    private boolean shouldIncludePackage(String packageInstruction, String packageName) {
        // Check both the full instruction and the package name
        return !exclusionFilter.isPackageExcluded(packageInstruction) &&
               !exclusionFilter.isPackageExcluded(packageName);
    }

    /**
     * Formats the sorted package instructions into final output format.
     */
    private GenerationResult formatPackageList(Set<String> sortedPackageInstructions) {
        List<String> packageList = new ArrayList<>();
        StringBuilder concatenatedBuffer = new StringBuilder();

        for (String packageInstruction : sortedPackageInstructions) {
            String packageInstructionWithComma = packageInstruction + ",";

            if (packageList.contains(packageInstructionWithComma)) {
                log.warn("Package export " + packageInstructionWithComma +
                         " already present in list, will not add again!");
            } else {
                packageList.add(packageInstructionWithComma);
                concatenatedBuffer.append(packageInstructionWithComma);
            }
        }

        // Build final concatenated string
        String concatenatedList = "";
        if (concatenatedBuffer.length() > 0) {
            concatenatedList = concatenatedBuffer.toString();
            // Remove the last comma
            concatenatedList = concatenatedList.substring(0, concatenatedList.length() - 1);
        }

        // Update packageList to remove the last comma from the last element
        if (!packageList.isEmpty()) {
            String lastPackage = packageList.remove(packageList.size() - 1);
            // Remove the last comma
            packageList.add(lastPackage.substring(0, lastPackage.length() - 1));
        }

        return new GenerationResult(Collections.unmodifiableList(packageList), concatenatedList);
    }
}

