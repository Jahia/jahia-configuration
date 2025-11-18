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
package org.jahia.utils.maven.plugin.osgi.framework.validation;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Validates generated system packages against a reference configuration file.
 *
 * <p>This validator ensures that the generated system packages configuration matches
 * the expected reference file. If differences are detected, it provides detailed
 * information and actionable guidance for resolution.</p>
 */
public class SystemPackagesValidator {

    private final Log log;
    private final File referenceFile;
    private final String propertyName;

    /**
     * Creates a new SystemPackagesValidator.
     *
     * @param referenceFile the reference properties file to validate against
     * @param propertyName the property name to compare
     * @param log Maven logger
     */
    public SystemPackagesValidator(File referenceFile, String propertyName, Log log) {
        this.referenceFile = referenceFile;
        this.propertyName = propertyName;
        this.log = log;
    }

    /**
     * Validates the generated file against the reference file.
     *
     * @param generatedFile the generated properties file
     * @throws MojoFailureException if validation fails
     */
    public void validate(File generatedFile) throws MojoFailureException {
        log.info("================================================================================");
        log.info("VALIDATING: Comparing generated configuration with reference file");
        log.info("================================================================================");

        // Check if reference file exists
        if (!referenceFile.exists()) {
            String message = buildErrorMessage(
                "Reference properties file does not exist",
                "The reference file could not be found. This file should contain your expected system packages configuration.",
                generatedFile,
                null
            );
            log.error(message);
            throw new MojoFailureException(message);
        }

        // Read and compare
        try {
            String referenceContent = readPropertyFromFile(referenceFile, propertyName);
            String generatedContent = readPropertyFromFile(generatedFile, propertyName);

            if (referenceContent == null) {
                String message = buildErrorMessage(
                    "Reference file is missing the property: " + propertyName,
                    "The reference file exists but doesn't contain the '" + propertyName + "' property.",
                    generatedFile,
                    null
                );
                log.error(message);
                throw new MojoFailureException(message);
            }

            // Normalize and compare
            String normalizedReference = normalizePropertyValue(referenceContent);
            String normalizedGenerated = normalizePropertyValue(generatedContent);

            if (!normalizedReference.equals(normalizedGenerated)) {
                PackageDifference diff = analyzeDifferences(normalizedReference, normalizedGenerated);

                String message = buildErrorMessage(
                    "System packages configuration MISMATCH detected",
                    "The generated system packages differ from your reference configuration.\n" +
                    "This typically happens after upgrading dependencies.",
                    generatedFile,
                    diff
                );
                log.error(message);
                throw new MojoFailureException(message);
            }

            // Success!
            log.info("[OK] VALIDATION PASSED: Generated configuration matches reference file");
            log.info("  Reference: " + referenceFile.getAbsolutePath());
            log.info("  Generated: " + generatedFile.getAbsolutePath());
            log.info("================================================================================");

        } catch (IOException e) {
            String message = "Failed to read properties files during validation: " + e.getMessage();
            log.error(message);
            throw new MojoFailureException(message, e);
        }
    }

    /**
     * Reads a specific property value from a properties file.
     */
    private String readPropertyFromFile(File file, String propertyName) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
        }
        return props.getProperty(propertyName);
    }

    /**
     * Normalizes property value for comparison (removes whitespace variations).
     */
    private String normalizePropertyValue(String value) {
        if (value == null) {
            return "";
        }
        // Remove line continuations, extra whitespace, and normalize
        return value.replaceAll("\\\\\\s*\\n\\s*", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    /**
     * Analyzes the differences between reference and generated package lists.
     */
    private PackageDifference analyzeDifferences(String reference, String generated) {
        Set<String> referencePackages = extractPackageNames(reference);
        Set<String> generatedPackages = extractPackageNames(generated);

        Set<String> removed = new TreeSet<>(referencePackages);
        removed.removeAll(generatedPackages);

        Set<String> added = new TreeSet<>(generatedPackages);
        added.removeAll(referencePackages);

        return new PackageDifference(removed, added);
    }

    /**
     * Extracts package names from a property value.
     */
    private Set<String> extractPackageNames(String propertyValue) {
        Set<String> packages = new TreeSet<>();
        if (propertyValue == null || propertyValue.trim().isEmpty()) {
            return packages;
        }

        // Split by comma and extract package names (before semicolon)
        for (String entry : propertyValue.split(",")) {
            String trimmed = entry.trim();
            int semicolonIndex = trimmed.indexOf(';');
            if (semicolonIndex > 0) {
                packages.add(trimmed.substring(0, semicolonIndex).trim());
            } else if (!trimmed.isEmpty()) {
                packages.add(trimmed);
            }
        }
        return packages;
    }

    /**
     * Builds a comprehensive error message for validation failures.
     */
    private String buildErrorMessage(String title, String explanation, File generatedFile,
                                     PackageDifference diff) {
        StringBuilder msg = new StringBuilder();
        msg.append("\n");
        msg.append("╔════════════════════════════════════════════════════════════════════════════╗\n");
        msg.append("║                                                                            ║\n");
        msg.append("║  ").append(String.format("%-74s", title.toUpperCase())).append("║\n");
        msg.append("║                                                                            ║\n");
        msg.append("╚════════════════════════════════════════════════════════════════════════════╝\n");
        msg.append("\n");
        msg.append(explanation).append("\n");
        msg.append("\n");
        msg.append("FILES:\n");
        msg.append("  Reference (expected): ").append(referenceFile.getAbsolutePath()).append("\n");
        msg.append("  Generated (actual):   ").append(generatedFile.getAbsolutePath()).append("\n");
        msg.append("\n");

        // Show differences if available
        if (diff != null) {
            String outputDir = generatedFile.getParent();
            appendDifferences(msg, diff, outputDir);
        }

        msg.append("GUIDANCE:\n");
        msg.append("\n");
        msg.append("  See the generated properties file header for:\n");
        msg.append("    - Resolution options (3 ways to fix this)\n");
        msg.append("    - Best practices for system packages\n");
        msg.append("    - Commands to compare and update files\n");
        msg.append("\n");
        msg.append("  File: ").append(generatedFile.getAbsolutePath()).append("\n");
        msg.append("\n");

        msg.append("════════════════════════════════════════════════════════════════════════════\n");

        return msg.toString();
    }

    /**
     * Appends the package differences to the message.
     */
    private void appendDifferences(StringBuilder msg, PackageDifference diff, String outputDir) {
        msg.append("DIFFERENCES:\n");

        if (!diff.removed.isEmpty()) {
            msg.append("  [-] Removed (").append(diff.removed.size()).append(" packages):\n");
            diff.removed.stream().limit(10).forEach(pkg ->
                msg.append("      - ").append(pkg).append("\n")
            );
            if (diff.removed.size() > 10) {
                msg.append("      ... and ").append(diff.removed.size() - 10).append(" more\n");
            }
        }

        if (!diff.added.isEmpty()) {
            msg.append("  [+] Added (").append(diff.added.size()).append(" packages):\n");
            diff.added.stream().limit(10).forEach(pkg ->
                msg.append("      + ").append(pkg).append("\n")
            );
            if (diff.added.size() > 10) {
                msg.append("      ... and ").append(diff.added.size() - 10).append(" more\n");
            }
        }

        msg.append("\n");
        msg.append("  See ").append(new File(outputDir, "report.txt").getAbsolutePath())
           .append(" for details\n");
        msg.append("\n");
    }



    /**
     * Holds information about package differences.
     */
    private static class PackageDifference {
        final Set<String> removed;
        final Set<String> added;

        PackageDifference(Set<String> removed, Set<String> added) {
            this.removed = removed;
            this.added = added;
        }
    }
}

