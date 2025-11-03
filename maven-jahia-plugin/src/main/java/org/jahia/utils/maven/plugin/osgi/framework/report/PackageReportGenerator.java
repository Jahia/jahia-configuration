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
package org.jahia.utils.maven.plugin.osgi.framework.report;

import org.apache.maven.plugin.logging.Log;
import org.jahia.utils.maven.plugin.osgi.framework.models.PackageInfo;
import org.jahia.utils.maven.plugin.osgi.framework.models.ScanSource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Generates comprehensive reports about package scanning and export decisions.
 * Helps developers understand which packages are included/excluded and why.
 */
public class PackageReportGenerator {

    private final Log log;

    /**
     * Creates a new PackageReportGenerator with logging.
     *
     * @param log Maven logger for output messages
     */
    public PackageReportGenerator(Log log) {
        this.log = log;
    }

    /**
     * Generates a comprehensive package analysis report.
     *
     * @param packageTracking map of all tracked packages
     * @param packageExclusionPatterns patterns used for exclusion
     * @param excludedArtifacts map of excluded artifacts to their exclusion patterns
     * @param versionOverrides map of packages with overridden versions
     * @param outputDirectory directory where the report will be written
     */
    public void generateReport(Map<String, PackageInfo> packageTracking,
                              List<Pattern> packageExclusionPatterns,
                              Map<String, String> excludedArtifacts,
                              Map<String, ?> versionOverrides,
                              File outputDirectory) {
        List<PackageInfo> includedPackages = new ArrayList<>();
        List<PackageInfo> excludedPackages = new ArrayList<>();

        // Separate packages
        for (PackageInfo packageInfo : packageTracking.values()) {
            if (packageInfo.isExcluded()) {
                excludedPackages.add(packageInfo);
            } else {
                includedPackages.add(packageInfo);
            }
        }

        File reportFile = new File(outputDirectory, "jahia-system-packages-check/report.txt");
        reportFile.getParentFile().mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
            writeReportHeader(writer, packageTracking.size(), includedPackages.size(), excludedPackages.size());
            writeExcludedArtifacts(writer, excludedArtifacts);
            writeVersionOverrides(writer, versionOverrides);
            writeIncludedPackages(writer, includedPackages, versionOverrides);
            writeExcludedPackages(writer, excludedPackages, packageExclusionPatterns);
            writePackagesWithInheritance(writer, includedPackages, packageTracking);
            writeScanSourceStatistics(writer, includedPackages);
            writeReportFooter(writer);

            log.info("Package export analysis report generated: " + reportFile.getAbsolutePath());
            log.info("  Total packages scanned: " + packageTracking.size() +
                     ", included: " + includedPackages.size() +
                     ", excluded: " + excludedPackages.size());

        } catch (IOException e) {
            log.error("Failed to generate package export analysis report: " + e.getMessage());
        }
    }

    private void writeReportHeader(PrintWriter writer, int total, int included, int excluded) {
        writer.println("################################################################################");
        writer.println("###                                                                          ###");
        writer.println("###           OSGi FRAMEWORK SYSTEM PACKAGES - ANALYSIS REPORT               ###");
        writer.println("###                                                                          ###");
        writer.println("################################################################################");
        writer.println("");
        writer.println("Generated: " + new Date());
        writer.println("");
        writer.println("================================================================================");
        writer.println(" SUMMARY");
        writer.println("================================================================================");
        writer.println("");
        writer.println("  Total packages discovered:  " + total);
        writer.println("  ✓ Included (exported):      " + included + " packages");
        writer.println("  ✗ Excluded (filtered):      " + excluded + " packages");
        writer.println("");
        writer.println("This report helps you understand:");
        writer.println("  • Which packages will be exported as OSGi system packages");
        writer.println("  • Why certain packages are excluded");
        writer.println("  • Where each package comes from (artifact coordinates)");
        writer.println("  • How package versions were determined");
        writer.println("");
    }

    private void writeExcludedArtifacts(PrintWriter writer, Map<String, String> excludedArtifacts) {
        if (excludedArtifacts == null || excludedArtifacts.isEmpty()) {
            writer.println("================================================================================");
            writer.println(" ℹ NO ARTIFACTS WERE EXCLUDED");
            writer.println("================================================================================");
            writer.println("");
            writer.println("All dependency artifacts were scanned for packages.");
            writer.println("Your artifact exclusion patterns didn't match any dependencies.");
            writer.println("");
            return;
        }

        writer.println("================================================================================");
        writer.println(" ✗ EXCLUDED ARTIFACTS (entire JARs skipped, not scanned)");
        writer.println("================================================================================");
        writer.println("");
        writer.println("These artifacts were excluded by your filter configuration.");
        writer.println("No packages from these artifacts were scanned or included.");
        writer.println("");

        // Group by exclusion pattern
        Map<String, List<String>> artifactsByPattern = new TreeMap<>();
        for (Map.Entry<String, String> entry : excludedArtifacts.entrySet()) {
            String artifact = entry.getKey();
            String pattern = entry.getValue();
            artifactsByPattern.computeIfAbsent(pattern, k -> new ArrayList<>()).add(artifact);
        }

        int patternCount = 0;
        for (Map.Entry<String, List<String>> entry : artifactsByPattern.entrySet()) {
            String pattern = entry.getKey();
            List<String> artifacts = entry.getValue();
            patternCount++;

            writer.println("┌─ Exclusion Rule #" + patternCount + ": \"" + pattern + "\"");
            writer.println("│  Matched " + artifacts.size() + " artifact(s)");
            writer.println("│");

            for (int i = 0; i < artifacts.size(); i++) {
                String artifact = artifacts.get(i);
                boolean isLast = (i == artifacts.size() - 1);
                String prefix = isLast ? "└──" : "├──";

                writer.println("│  " + prefix + " " + artifact);
            }
            writer.println("└─────────────────────────────────────────────────────────────────────────────");
            writer.println("");
        }
    }

    private void writeVersionOverrides(PrintWriter writer, Map<String, ?> versionOverrides) {
        if (versionOverrides == null || versionOverrides.isEmpty()) {
            writer.println("================================================================================");
            writer.println(" ℹ NO VERSION OVERRIDES CONFIGURED");
            writer.println("================================================================================");
            writer.println("");
            writer.println("All package versions are using automatically detected values.");
            writer.println("To override versions, use <packageVersionOverrides> in your POM.");
            writer.println("");
            return;
        }

        writer.println("================================================================================");
        writer.println(" ⚡ VERSION OVERRIDES (configured versions forced)");
        writer.println("================================================================================");
        writer.println("");
        writer.println("These packages had their versions overridden by configuration.");
        writer.println("The detected versions were replaced with configured values.");
        writer.println("");

        // Group by pattern
        Map<String, List<Object>> byPattern = new TreeMap<>();
        for (Map.Entry<String, ?> entry : versionOverrides.entrySet()) {
            Object info = entry.getValue();
            // Access via reflection to avoid type issues
            try {
                String pattern = (String) info.getClass().getMethod("getMatchedPattern").invoke(info);
                byPattern.computeIfAbsent(pattern, k -> new ArrayList<>()).add(info);
            } catch (Exception e) {
                // Fallback
                byPattern.computeIfAbsent("unknown", k -> new ArrayList<>()).add(info);
            }
        }

        int patternCount = 0;
        for (Map.Entry<String, List<Object>> entry : byPattern.entrySet()) {
            String pattern = entry.getKey();
            List<Object> infos = entry.getValue();
            patternCount++;

            writer.println("┌─ Override Rule #" + patternCount + ": \"" + pattern + "\"");
            writer.println("│  Matched " + infos.size() + " package(s)");
            writer.println("│");

            for (int i = 0; i < infos.size(); i++) {
                Object info = infos.get(i);
                boolean isLast = (i == infos.size() - 1);
                String prefix = isLast ? "└──" : "├──";

                try {
                    String packageName = (String) info.getClass().getMethod("getPackageName").invoke(info);
                    @SuppressWarnings("unchecked")
                    Set<String> originalVersions = (Set<String>) info.getClass().getMethod("getOriginalVersions").invoke(info);
                    String overriddenVersion = (String) info.getClass().getMethod("getOverriddenVersion").invoke(info);

                    writer.println("│  " + prefix + " Package: " + packageName);
                    writer.println("│      " + (isLast ? "   " : "│  ") + " Original: " + String.join(", ", originalVersions));
                    writer.println("│      " + (isLast ? "   " : "│  ") + " Override: " + overriddenVersion);

                    if (!isLast) {
                        writer.println("│      │");
                    }
                } catch (Exception e) {
                    writer.println("│  " + prefix + " " + info.toString());
                }
            }
            writer.println("└─────────────────────────────────────────────────────────────────────────────");
            writer.println("");
        }
    }

    private void writeIncludedPackages(PrintWriter writer, List<PackageInfo> includedPackages,
                                      Map<String, ?> versionOverrides) {
        if (includedPackages.isEmpty()) {
            writer.println("================================================================================");
            writer.println(" NO PACKAGES TO EXPORT");
            writer.println("================================================================================");
            writer.println("All packages were filtered by exclusion rules.");
            writer.println("");
            return;
        }

        writer.println("================================================================================");
        writer.println(" ✓ INCLUDED PACKAGES (will be exported as system packages)");
        writer.println("================================================================================");
        writer.println("");

        // Group by artifact
        Map<String, List<PackageInfo>> packagesByArtifact = groupByArtifact(includedPackages);

        int artifactCount = 0;
        for (Map.Entry<String, List<PackageInfo>> entry : packagesByArtifact.entrySet()) {
            String artifact = entry.getKey();
            List<PackageInfo> packages = entry.getValue();
            artifactCount++;

            writer.println("┌─ Artifact #" + artifactCount + ": " + artifact);
            writer.println("│  Exports " + packages.size() + " package(s)");
            writer.println("│");

            for (int i = 0; i < packages.size(); i++) {
                PackageInfo packageInfo = packages.get(i);
                ScanSource scanSource = packageInfo.getSourceArtifacts().get(artifact);
                String versions = formatVersions(packageInfo.getVersions());
                boolean isLast = (i == packages.size() - 1);
                String prefix = isLast ? "└──" : "├──";

                // Check if this package has a version override
                boolean hasOverride = versionOverrides != null && versionOverrides.containsKey(packageInfo.getPackageName());
                String versionLabel = hasOverride ? " Version: " + versions + " ⚡ OVERRIDDEN" : " Version: " + versions;

                writer.println("│  " + prefix + " Package: " + packageInfo.getPackageName());
                writer.println("│      " + (isLast ? "   " : "│  ") + versionLabel);
                writer.println("│      " + (isLast ? "   " : "│  ") + " Discovery: " + scanSource.getDescription());

                if (packageInfo.getParentPackage() != null) {
                    writer.println("│      " + (isLast ? "   " : "│  ") + " Inherited from: " + packageInfo.getParentPackage());
                }
                if (!isLast) {
                    writer.println("│      │");
                }
            }
            writer.println("└─────────────────────────────────────────────────────────────────────────────");
            writer.println("");
        }
    }

    private void writeExcludedPackages(PrintWriter writer, List<PackageInfo> excludedPackages,
                                      List<Pattern> packageExclusionPatterns) {
        if (excludedPackages.isEmpty()) {
            writer.println("================================================================================");
            writer.println(" ℹ NO PACKAGES WERE EXCLUDED");
            writer.println("================================================================================");
            writer.println("");
            writer.println("All discovered packages passed the exclusion filters.");
            writer.println("This means your exclusion patterns didn't match any packages.");
            writer.println("");
            writeUnusedExclusionPatterns(writer, new TreeSet<>(), packageExclusionPatterns);
            return;
        }

        writer.println("================================================================================");
        writer.println(" ✗ EXCLUDED PACKAGES (filtered out, will NOT be exported)");
        writer.println("================================================================================");
        writer.println("");
        writer.println("These packages were discovered but excluded by your filter configuration.");
        writer.println("");

        // Group by exclusion pattern
        Map<String, List<PackageInfo>> exclusionsByPattern = groupByExclusionPattern(excludedPackages);

        int patternCount = 0;
        for (Map.Entry<String, List<PackageInfo>> entry : exclusionsByPattern.entrySet()) {
            String pattern = entry.getKey();
            List<PackageInfo> packages = entry.getValue();
            patternCount++;

            writer.println("┌─ Exclusion Rule #" + patternCount + ": \"" + pattern + "\"");
            writer.println("│  Matched " + packages.size() + " package(s)");
            writer.println("│");

            for (int i = 0; i < packages.size(); i++) {
                PackageInfo packageInfo = packages.get(i);
                String versions = formatVersions(packageInfo.getVersions());
                boolean isLast = (i == packages.size() - 1);
                String prefix = isLast ? "└──" : "├──";

                writer.println("│  " + prefix + " Package: " + packageInfo.getPackageName());
                writer.println("│      " + (isLast ? "   " : "│  ") + " Version: " + versions);

                if (!packageInfo.getSourceArtifacts().isEmpty()) {
                    String artifacts = String.join(", ", packageInfo.getSourceArtifacts().keySet());
                    writer.println("│      " + (isLast ? "   " : "│  ") + " From: " + artifacts);
                }
                if (!isLast) {
                    writer.println("│      │");
                }
            }
            writer.println("└─────────────────────────────────────────────────────────────────────────────");
            writer.println("");
        }

        // Report unused exclusion patterns
        writer.println("");
        writeUnusedExclusionPatterns(writer, exclusionsByPattern.keySet(), packageExclusionPatterns);
    }

    private void writeUnusedExclusionPatterns(PrintWriter writer, Set<String> usedPatterns,
                                             List<Pattern> allPatterns) {
        Set<String> allPatternStrings = new TreeSet<>();
        for (Pattern pattern : allPatterns) {
            allPatternStrings.add(pattern.pattern());
        }

        Set<String> unusedPatterns = new TreeSet<>(allPatternStrings);
        unusedPatterns.removeAll(usedPatterns);

        if (!unusedPatterns.isEmpty()) {
            writer.println("UNUSED EXCLUSION PATTERNS (consider removing):");
            for (String pattern : unusedPatterns) {
                writer.println("  - " + pattern);
            }
            writer.println("");
        }
    }

    private void writePackagesWithInheritance(PrintWriter writer, List<PackageInfo> includedPackages,
                                             Map<String, PackageInfo> packageTracking) {
        List<PackageInfo> inheritedPackages = new ArrayList<>();
        for (PackageInfo packageInfo : includedPackages) {
            if (packageInfo.getParentPackage() != null) {
                inheritedPackages.add(packageInfo);
            }
        }

        if (inheritedPackages.isEmpty()) {
            return;
        }

        writer.println("================================================================================");
        writer.println("PACKAGES WITH VERSION INHERITANCE:");
        writer.println("================================================================================");
        writer.println("");
        writer.println("These packages inherited their version from a parent package in the manifest:");

        // Group by parent package
        Map<String, List<PackageInfo>> byParent = groupByParentPackage(inheritedPackages);

        for (Map.Entry<String, List<PackageInfo>> entry : byParent.entrySet()) {
            String parent = entry.getKey();
            List<PackageInfo> packages = entry.getValue();

            // Get parent version
            PackageInfo parentInfo = packageTracking.get(parent);
            String parentVersion = (parentInfo != null && !parentInfo.getVersions().isEmpty())
                ? formatVersions(parentInfo.getVersions())
                : "unknown";

            writer.println("");
            writer.println("Parent: " + parent + ";version=\"" + parentVersion + "\"");
            writer.println("  Child packages (" + packages.size() + "):");

            for (PackageInfo packageInfo : packages) {
                writer.println("    - " + packageInfo.getPackageName());
            }
        }
        writer.println("");
    }

    private void writeScanSourceStatistics(PrintWriter writer, List<PackageInfo> includedPackages) {
        writer.println("================================================================================");
        writer.println("SCAN SOURCE STATISTICS:");
        writer.println("================================================================================");

        Map<ScanSource, Integer> sourceCounts = new TreeMap<>();
        for (ScanSource source : ScanSource.values()) {
            sourceCounts.put(source, 0);
        }

        for (PackageInfo packageInfo : includedPackages) {
            for (ScanSource source : packageInfo.getSourceArtifacts().values()) {
                sourceCounts.put(source, sourceCounts.get(source) + 1);
            }
        }

        writer.println("");
        for (Map.Entry<ScanSource, Integer> entry : sourceCounts.entrySet()) {
            writer.println("  " + entry.getKey().getDescription() + ": " + entry.getValue() + " packages");
        }
        writer.println("");
    }

    private void writeReportFooter(PrintWriter writer) {
        writer.println("================================================================================");
        writer.println("END OF PACKAGE EXPORT ANALYSIS REPORT");
        writer.println("================================================================================");
    }

    private Map<String, List<PackageInfo>> groupByArtifact(List<PackageInfo> packages) {
        Map<String, List<PackageInfo>> result = new TreeMap<>();

        for (PackageInfo packageInfo : packages) {
            for (String artifact : packageInfo.getSourceArtifacts().keySet()) {
                result.computeIfAbsent(artifact, k -> new ArrayList<>()).add(packageInfo);
            }
        }

        return result;
    }

    private Map<String, List<PackageInfo>> groupByExclusionPattern(List<PackageInfo> packages) {
        Map<String, List<PackageInfo>> result = new TreeMap<>();

        for (PackageInfo packageInfo : packages) {
            String pattern = packageInfo.getExcludedByPattern();
            if (pattern == null) {
                pattern = "Unknown pattern";
            }
            result.computeIfAbsent(pattern, k -> new ArrayList<>()).add(packageInfo);
        }

        return result;
    }

    private Map<String, List<PackageInfo>> groupByParentPackage(List<PackageInfo> packages) {
        Map<String, List<PackageInfo>> result = new TreeMap<>();

        for (PackageInfo packageInfo : packages) {
            String parent = packageInfo.getParentPackage();
            result.computeIfAbsent(parent, k -> new ArrayList<>()).add(packageInfo);
        }

        return result;
    }

    private String formatVersions(Set<String> versions) {
        return versions.isEmpty() ? "no version" : String.join(", ", versions);
    }
}

