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
package org.jahia.utils.maven.plugin.osgi.framework;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jahia.utils.maven.plugin.AetherAwareMojo;
import org.jahia.utils.maven.plugin.osgi.framework.filter.ExclusionFilter;
import org.jahia.utils.maven.plugin.osgi.framework.filter.PatternMatcher;
import org.jahia.utils.maven.plugin.osgi.framework.generator.PackageListGenerator;
import org.jahia.utils.maven.plugin.osgi.framework.report.PackageReportGenerator;
import org.jahia.utils.maven.plugin.osgi.framework.scanner.DependencyScanner;
import org.jahia.utils.maven.plugin.osgi.framework.scanner.PackageScanContext;
import org.jahia.utils.maven.plugin.osgi.framework.validation.SystemPackagesValidator;
import org.jahia.utils.maven.plugin.osgi.framework.version.VersionOverrideApplier;
import org.jahia.utils.maven.plugin.osgi.framework.version.VersionResolver;

import java.io.*;
import java.util.*;

/**
 * Validates Jahia OSGi system packages configuration against project dependencies.
 *
 * <h2>Purpose</h2>
 * <p>This goal acts as a <b>build validation gate</b> to ensure your OSGi system packages configuration
 * stays in sync with your project dependencies. It scans dependencies, generates the expected
 * configuration, and <b>fails the build if it doesn't match your reference file</b>.</p>
 *
 * <p><b>Key Behavior:</b> This goal <u>validates</u> rather than generates. If validation fails,
 * you must explicitly update your configuration or adjust the plugin settings.</p>
 *
 * <h2>How It Works</h2>
 * <ol>
 *   <li><b>Scans</b> all project dependencies (compile, provided, runtime scopes only)</li>
 *   <li><b>Extracts</b> package names and versions from:
 *     <ul>
 *       <li>OSGi Export-Package manifest headers (highest priority)</li>
 *       <li>Manifest package entries with Specification/Implementation versions</li>
 *       <li>JAR file structure (discovers packages from directory layout)</li>
 *     </ul>
 *   </li>
 *   <li><b>Filters</b> packages based on exclusion patterns (early filtering for performance)</li>
 *   <li><b>Resolves</b> split-package conflicts (same package in multiple JARs)</li>
 *   <li><b>Applies</b> version overrides if configured</li>
 *   <li><b>Generates</b> expected system packages list</li>
 *   <li><b>Validates</b> against reference file - <b>FAILS BUILD if mismatch detected</b></li>
 * </ol>
 *
 * <h2>When Validation Fails</h2>
 * <p>Build failure typically occurs after dependency upgrades. You have three options:</p>
 * <ol>
 *   <li><b>Accept changes:</b> Copy generated file to reference location (if upgrade is correct)</li>
 *   <li><b>Adjust configuration:</b> Update packageExcludes or packageVersionOverrides</li>
 *   <li><b>Fix dependencies:</b> Revert or correct the dependency changes</li>
 * </ol>
 *
 * <h2>Configuration Parameters</h2>
 * <table border="1">
 *   <tr><th>Parameter</th><th>Type</th><th>Required</th><th>Default</th><th>Description</th></tr>
 *   <tr>
 *     <td><b>referencePropertiesFile</b></td>
 *     <td>File</td>
 *     <td>Yes</td>
 *     <td>-</td>
 *     <td>Reference file to validate against.<br/>
 *         Example: ${project.basedir}/src/main/webapp/WEB-INF/etc/config/felix-framework.properties</td>
 *   </tr>
 *   <tr>
 *     <td><b>propertyFilePropertyName</b></td>
 *     <td>String</td>
 *     <td>No</td>
 *     <td>org.osgi.framework.system.packages.extra</td>
 *     <td>Property name in the generated/reference files</td>
 *   </tr>
 *   <tr>
 *     <td><b>artifactExcludes</b></td>
 *     <td>List&lt;String&gt;</td>
 *     <td>No</td>
 *     <td>org.osgi:*</td>
 *     <td>Artifact patterns to skip (format: groupId:artifactId)</td>
 *   </tr>
 *   <tr>
 *     <td><b>packageExcludes</b></td>
 *     <td>List&lt;String&gt;</td>
 *     <td>No</td>
 *     <td>See defaults</td>
 *     <td>Package patterns to exclude from exports</td>
 *   </tr>
 *   <tr>
 *     <td><b>packageVersionOverrides</b></td>
 *     <td>List&lt;String&gt;</td>
 *     <td>No</td>
 *     <td>-</td>
 *     <td>Force specific versions (format: pattern:version)</td>
 *   </tr>
 * </table>
 *
 * <h2>Output Files</h2>
 * <p>All files generated under: <code>target/jahia-system-packages-check/</code></p>
 * <ul>
 *   <li><b>{propertyName}.properties</b> - Generated configuration for comparison</li>
 *   <li><b>report.txt</b> - Detailed analysis of packages, exclusions, and overrides</li>
 * </ul>
 *
 * <h2>Example Configuration</h2>
 * <pre>
 * &lt;plugin&gt;
 *   &lt;groupId&gt;org.jahia.utils&lt;/groupId&gt;
 *   &lt;artifactId&gt;maven-jahia-plugin&lt;/artifactId&gt;
 *   &lt;executions&gt;
 *     &lt;execution&gt;
 *       &lt;goals&gt;
 *         &lt;goal&gt;jahia-system-packages-check&lt;/goal&gt;
 *       &lt;/goals&gt;
 *       &lt;configuration&gt;
 *         &lt;!-- REQUIRED: Reference file to validate against --&gt;
 *         &lt;referencePropertiesFile&gt;
 *           ${project.basedir}/src/main/webapp/WEB-INF/etc/config/felix-framework.properties
 *         &lt;/referencePropertiesFile&gt;
 *
 *         &lt;!-- Optional: Customize property name --&gt;
 *         &lt;propertyFilePropertyName&gt;org.osgi.framework.system.packages.extra&lt;/propertyFilePropertyName&gt;
 *
 *         &lt;!-- Optional: Exclude packages --&gt;
 *         &lt;packageExcludes&gt;
 *           &lt;exclude&gt;org.jahia.taglibs.*&lt;/exclude&gt;
 *           &lt;exclude&gt;com.internal.*&lt;/exclude&gt;
 *         &lt;/packageExcludes&gt;
 *
 *         &lt;!-- Optional: Override versions --&gt;
 *         &lt;packageVersionOverrides&gt;
 *           &lt;packageVersionOverride&gt;org.apache.xalan*:2.7.3&lt;/packageVersionOverride&gt;
 *         &lt;/packageVersionOverrides&gt;
 *       &lt;/configuration&gt;
 *     &lt;/execution&gt;
 *   &lt;/executions&gt;
 * &lt;/plugin&gt;
 * </pre>
 *
 * <h2>Use in CI/CD</h2>
 * <p>This goal is designed to run in CI/CD pipelines as a validation gate.
 * When dependencies change, the build fails with clear instructions on how to resolve
 * the mismatch, preventing configuration drift.</p>
 *
 * @goal jahia-system-packages-check
 * @requiresDependencyResolution test
 */
public class JahiaSystemPackagesCheckMojo extends AetherAwareMojo {


    /**
     * List of artifact patterns to EXCLUDE from dependency scanning.
     *
     * <p>Artifacts matching these patterns will be completely skipped during the scanning phase.
     * This is useful for excluding OSGi framework bundles or other artifacts whose packages
     * should not be exported as system packages.</p>
     *
     * <p><b>Format:</b> "groupId:artifactId" with wildcard support (*)</p>
     *
     * <p><b>Examples:</b></p>
     * <ul>
     *   <li>"org.osgi:*" - Exclude all artifacts from org.osgi group</li>
     *   <li>"com.example:my-artifact" - Exclude specific artifact</li>
     *   <li>"*:test-*" - Exclude all artifacts with IDs starting with "test-"</li>
     * </ul>
     *
     * <p><b>Default:</b> "org.osgi:*" (excludes OSGi framework packages)</p>
     *
     * <p><i>Note: Due to Maven bug MNG-5440, defaults are set at runtime via ARTIFACT_EXCLUDE_DEFAULT_VALUE constant</i></p>
     *
     * @parameter
     */
    protected List<String> artifactExcludes;

    /**
     * Default artifact exclusion patterns applied if artifactExcludes is not configured in POM.
     * OSGi framework artifacts are excluded by default as their packages are already provided by the framework.
     */
    private static final String ARTIFACT_EXCLUDE_DEFAULT_VALUE = "org.osgi:*";

    /**
     * List of package patterns to EXCLUDE from the generated Export-Package list.
     *
     * <p><b>IMPORTANT PERFORMANCE OPTIMIZATION:</b> These patterns are checked DURING scanning,
     * not at the end. Packages matching these patterns are filtered immediately when discovered,
     * preventing them from being stored in memory or processed further.</p>
     *
     * <p><b>Format:</b> Package names with optional wildcard suffix (*)</p>
     *
     * <p><b>Examples:</b></p>
     * <ul>
     *   <li>"org.example.internal.*" - Exclude all packages under org.example.internal</li>
     *   <li>"javax.servlet.jsp*" - Exclude javax.servlet.jsp and all sub-packages</li>
     *   <li>"com.example.PrivateImpl" - Exclude exact package name</li>
     * </ul>
     *
     * <p><b>Default packages excluded:</b></p>
     * <ul>
     *   <li>org.jahia.taglibs* - Jahia tag libraries</li>
     *   <li>org.apache.taglibs.standard* - Apache standard tag libraries</li>
     *   <li>javax.servlet.jsp* - JSP packages (provided by container)</li>
     *   <li>org.codehaus.groovy.ast* - Groovy AST internal packages</li>
     *   <li>javax.el* - Expression Language (provided by container)</li>
     *   <li>de.odysseus.el* - JUEL EL implementation</li>
     * </ul>
     *
     * <p><b>When to exclude packages:</b></p>
     * <ul>
     *   <li>Internal implementation packages not meant for OSGi bundles</li>
     *   <li>Packages already provided by the servlet container</li>
     *   <li>Framework-specific packages that cause conflicts</li>
     * </ul>
     *
     * <p><i>Note: Due to Maven bug MNG-5440, defaults are set at runtime via PACKAGE_EXCLUDE_DEFAULT_VALUE constant</i></p>
     *
     * @parameter
     */
    protected List<String> packageExcludes;

    /**
     * Default package exclusion patterns applied if packageExcludes is not configured in POM.
     * These are common packages that should not be exported as system packages.
     */
    private static final String PACKAGE_EXCLUDE_DEFAULT_VALUE = "org.jahia.taglibs*,org.apache.taglibs.standard*,javax.servlet.jsp*,org.codehaus.groovy.ast*,javax.el*,de.odysseus.el*";

    /**
     * List of package version overrides to force specific versions for matching packages.
     *
     * <p>This allows you to override the automatically detected version for packages.
     * Useful when the detected version is incorrect or when you need to align versions
     * across multiple packages.</p>
     *
     * <p><b>Format:</b> "packagePattern:version"</p>
     *
     * <p><b>Pattern matching:</b></p>
     * <ul>
     *   <li>Exact match: "javax.transaction" matches only that package</li>
     *   <li>Wildcard suffix: "org.apache.xalan*" matches org.apache.xalan and all sub-packages</li>
     * </ul>
     *
     * <p><b>Examples:</b></p>
     * <pre>
     * &lt;packageVersionOverrides&gt;
     *   &lt;packageVersionOverride&gt;org.apache.xalan*:2.7.3&lt;/packageVersionOverride&gt;
     *   &lt;packageVersionOverride&gt;javax.transaction:1.1.1&lt;/packageVersionOverride&gt;
     *   &lt;packageVersionOverride&gt;com.example.api:2.0.0&lt;/packageVersionOverride&gt;
     * &lt;/packageVersionOverrides&gt;
     * </pre>
     *
     * <p><b>Effect:</b></p>
     * <ul>
     *   <li>org.apache.xalan → version 2.7.3</li>
     *   <li>org.apache.xalan.xsltc → version 2.7.3 (matched by wildcard)</li>
     *   <li>javax.transaction → version 1.1.1</li>
     *   <li>javax.transaction.xa → keeps original version (no wildcard, doesn't match)</li>
     * </ul>
     *
     * <p><b>Important Notes:</b></p>
     * <ul>
     *   <li>Overrides are applied AFTER scanning and version resolution</li>
     *   <li>The report will clearly mark overridden packages</li>
     *   <li>Use carefully - incorrect versions can cause runtime issues</li>
     * </ul>
     *
     * @parameter
     */
    protected List<String> packageVersionOverrides;

    /**
     * Reference properties file to validate against (the "expected" configuration).
     *
     * <p>This is the properties file in your source code that contains the expected system packages.
     * The plugin will compare the generated configuration against this file.</p>
     *
     * <p><b>If the files don't match, the build will FAIL.</b></p>
     *
     * <p><b>Example:</b> ${project.basedir}/src/main/webapp/WEB-INF/etc/config/felix-framework.properties</p>
     *
     * <p><b>When validation fails, you have three options:</b></p>
     * <ul>
     *   <li>Accept the changes: Copy generated file content to reference file</li>
     *   <li>Adjust configuration: Update exclusions/overrides in plugin config</li>
     *   <li>Fix dependencies: Review and correct dependency changes</li>
     * </ul>
     *
     * @parameter
     * @required
     */
    protected File referencePropertiesFile;

    /**
     * The property name used in the OUTPUT Felix properties file for the generated package list.
     *
     * <p>This is where the plugin writes the generated Export-Package list.
     * Felix reads this property to extend its system packages beyond the base JRE packages.</p>
     *
     * <p><b>Standard value:</b> "org.osgi.framework.system.packages.extra"</p>
     *
     * @parameter default-value="org.osgi.framework.system.packages.extra"
     */
    protected String propertyFilePropertyName = "org.osgi.framework.system.packages.extra";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            // ========================================================================
            // STEP 1: Initialize Exclusion Filter
            // ========================================================================
            // Create the exclusion filter with user-configured patterns.
            // IMPORTANT: These patterns are applied DURING scanning (not at the end)
            // for performance optimization - excluded packages never enter memory.
            ExclusionFilter exclusionFilter = createExclusionFilter();

            // ========================================================================
            // STEP 2: Initialize Scan Context
            // ========================================================================
            // The scan context stores all discovered packages, versions, and metadata
            // during the dependency scanning process.
            PackageScanContext scanContext = new PackageScanContext();

            // ========================================================================
            // STEP 3: Scan Project Dependencies
            // ========================================================================
            // Scan all compile, provided, and runtime scope dependencies.
            // For each JAR file:
            //   1. Check artifact exclusions (skip entire JARs if matched)
            //   2. Extract packages from Export-Package header (if present)
            //   3. Extract packages from manifest entries with versions
            //   4. Discover packages from JAR file structure
            //   5. For EACH discovered package, immediately check package exclusions
            //   6. If excluded: skip (don't store in memory)
            //   7. If not excluded: add to scanContext
            if (project != null) {
                DependencyScanner dependencyScanner = new DependencyScanner(exclusionFilter, getLog());
                dependencyScanner.scanArtifacts(project.getArtifacts(), scanContext);
            }

            // ========================================================================
            // STEP 4: Resolve Split Packages
            // ========================================================================
            // Handle packages that appear in multiple JARs with different versions.
            // The resolver picks the most appropriate version for each package based
            // on occurrence counts and version precedence.
            VersionResolver versionResolver = new VersionResolver(getLog());
            Map<String, Set<String>> packageVersions = versionResolver.resolveSplitPackages(
                scanContext.getPackageVersionCounts()
            );

            // ========================================================================
            // STEP 4.5: Apply Version Overrides
            // ========================================================================
            // Apply user-configured version overrides to specific packages.
            // This allows forcing specific versions for packages where the detected
            // version is incorrect or needs to be aligned.
            VersionOverrideApplier versionOverrideApplier = new VersionOverrideApplier(
                packageVersionOverrides, getLog());
            versionOverrideApplier.applyOverrides(packageVersions);

            // ========================================================================
            // STEP 5: Generate Final Package List
            // ========================================================================
            // Format the resolved packages into OSGi Export-Package syntax:
            //   "package.name;version=\"1.2.3\""
            // Applies a safety filter (should rarely filter since exclusions happen earlier).
            PackageListGenerator packageListGenerator = new PackageListGenerator(exclusionFilter, getLog());
            PackageListGenerator.GenerationResult result = packageListGenerator.generatePackageList(packageVersions);

            getLog().info("Found " + packageVersions.size() + " packages in dependencies.");

            // ========================================================================
            // STEP 6: Write Output Files to target/jahia-system-packages-check/
            // ========================================================================
            // Create output directory
            File outputDirectory = new File(project.getBuild().getDirectory(), "jahia-system-packages-check");
            outputDirectory.mkdirs();

            // Generate properties file name from property name
            // e.g., "jahia.system.packages.generated" -> "jahia.system.packages.generated.properties"
            String propertiesFileName = propertyFilePropertyName + ".properties";
            File generatedPropertiesFile = new File(outputDirectory, propertiesFileName);

            // Write properties file with header
            writePropertiesFileWithHeader(
                generatedPropertiesFile,
                propertyFilePropertyName,
                result.getPackageList().toArray(new String[0])
            );

            getLog().info("Generated properties file: " + generatedPropertiesFile.getAbsolutePath());

            // Generate report in same directory
            PackageReportGenerator reportGenerator = new PackageReportGenerator(getLog());
            reportGenerator.generateReport(
                scanContext.getPackageTracking(),
                exclusionFilter.getPackageMatcher().getPatterns(),
                scanContext.getExcludedArtifacts(),
                versionOverrideApplier.getAppliedOverrides(),
                outputDirectory
            );

            // ========================================================================
            // STEP 7: VALIDATE - Compare with Reference File
            // ========================================================================
            // This is the validation step - compares generated file with reference
            // and FAILS the build if they don't match
            SystemPackagesValidator validator = new SystemPackagesValidator(
                referencePropertiesFile,
                propertyFilePropertyName,
                getLog()
            );
            validator.validate(generatedPropertiesFile);

        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate package list", e);
        } catch (Exception e) {
            throw new MojoExecutionException("Unexpected error during package list generation", e);
        }
    }

    /**
     * Writes the properties file with header and package list.
     * The header includes placeholders that are replaced with actual file paths.
     *
     * @param outputFile the file to write
     * @param propertyName the property name
     * @param packageList the array of package strings
     * @throws IOException if file writing fails
     */
    private void writePropertiesFileWithHeader(File outputFile, String propertyName, String[] packageList)
            throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            // Write header from resource
            writer.write(loadHeaderFromResource());
            writer.write("\n");

            // Write the property with packages
            writer.write(propertyName);
            writer.write(" = ");  // Space before and after =

            if (packageList.length > 0) {
                // Note: PackageListGenerator already adds commas between packages (except the last one)
                // So we just need to write them with line continuations

                // First package - escape quotes
                writer.write(escapePropertyValue(packageList[0]));

                // Subsequent packages - each already has a trailing comma (except last)
                for (int i = 1; i < packageList.length; i++) {
                    writer.write("\\\n");  // Line continuation
                    writer.write(" ");     // Single space at start of continuation line
                    writer.write(escapePropertyValue(packageList[i]));
                }
            }

            writer.write("\n");
        }
    }

    /**
     * Escapes quotes in property values for properties file format.
     * Replaces " with \" so they are properly escaped in the properties file.
     *
     * @param value the value to escape
     * @return the escaped value
     */
    private String escapePropertyValue(String value) {
        return value.replace("\"", "\\\"");
    }

    /**
     * Loads the header from the resource file.
     *
     * @return the header content
     * @throws IOException if the header resource cannot be read
     */
    private String loadHeaderFromResource() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/system-packages-header.txt")) {
            if (is == null) {
                throw new IOException("Header resource not found: /system-packages-header.txt");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
                return reader.lines().collect(java.util.stream.Collectors.joining("\n")) + "\n";
            }
        }
    }

    /**
     * Creates the exclusion filter with configured patterns.
     * User-configured exclusion patterns from packageExcludes are applied during scanning.
     */
    private ExclusionFilter createExclusionFilter() {
        // Initialize artifact exclusion patterns
        if (artifactExcludes == null) {
            artifactExcludes = Arrays.asList(ARTIFACT_EXCLUDE_DEFAULT_VALUE.split(","));
        }
        PatternMatcher artifactMatcher = new PatternMatcher.ArtifactPatternBuilder()
            .addPatterns(artifactExcludes)
            .build();

        // Initialize package exclusion patterns
        if (packageExcludes == null) {
            packageExcludes = Arrays.asList(PACKAGE_EXCLUDE_DEFAULT_VALUE.split(","));
        }
        PatternMatcher packageMatcher = new PatternMatcher.PackagePatternBuilder()
            .addPatterns(packageExcludes)
            .build();

        return new ExclusionFilter(artifactMatcher, packageMatcher);
    }

}
