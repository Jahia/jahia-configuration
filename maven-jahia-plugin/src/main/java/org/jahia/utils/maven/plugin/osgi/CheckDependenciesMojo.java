/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.osgi;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.bundleplugin.DependencyExcluder;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.utils.xml.Xpp3Dom;
import org.apache.tika.io.IOUtils;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.glassfish.jersey.client.ClientProperties;
import org.jahia.utils.maven.plugin.support.MavenAetherHelperUtils;
import org.jahia.utils.osgi.BundleUtils;
import org.jahia.utils.osgi.ManifestValueClause;
import org.jahia.utils.osgi.PackageUtils;
import org.jahia.utils.osgi.parsers.PackageInfo;
import org.jahia.utils.osgi.parsers.ParsingContext;

import javax.net.ssl.*;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * A goal that checks the dependencies of a generated OSGi bundle JAR against the project dependencies, and reports
 * any missing packages that weren't found in any dependency export.
 *
 * @goal check-dependencies
 * @phase package
 * @requiresDependencyResolution test
 * @requiresDependencyCollection test
 */
public class CheckDependenciesMojo extends DependenciesMojo {

    Set<PackageInfo> systemPackages = new TreeSet<PackageInfo>();

    /**
     * @component
     */
    private MavenProjectHelper mavenProjectHelper;

    /**
     * @component
     */
    private ArtifactHandlerManager artifactHandlerManager;

    /**
     * Classifier type of the bundle to be installed.  For example, "jdk14".
     * Defaults to none which means this is the project's main bundle.
     *
     * @parameter
     */
    protected String classifier;

    /**
     * @component
     */
    private ArchiverManager archiverManager;

    /**
     * The directory for the generated bundles.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File outputDirectory;

    /**
     * The directory for the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String buildDirectory;

    /**
     * @parameter default-value="true"
     */
    protected boolean failBuildOnSplitPackages = true;


    /**
     * @parameter default-value="false"
     */
    protected boolean failBuildOnMissingPackageExports = false;

    /**
     * @parameter default-value="false"
     */
    private boolean searchInDependencies = false;

    /**
     * The directory for the generated JAR.
     *
     * @parameter default-value="false" expression="${jahia.modules.skipCheckDependencies}"
     */
    protected boolean skipCheckDependencies;

    @Override
    public void execute() throws MojoExecutionException {
        if (skipCheckDependencies) {
            return;
        }
        setBuildDirectory(projectBuildDirectory);
        setOutputDirectory(new File(projectOutputDirectory));

        if (!"jar".equals(project.getPackaging()) && !"bundle".equals(project.getPackaging()) && !"war".equals(project.getPackaging())) {
            getLog().info("Not a JAR/WAR/Bundle project, will do nothing.");
            return;
        }
        loadSystemPackages();

        buildExclusionPatterns();

        Set<PackageInfo> explicitPackageImports = new TreeSet<PackageInfo>();

        final ParsingContext projectParsingContext = new ParsingContext(MavenAetherHelperUtils.getCoords(project.getArtifact()), 0, 0, project.getArtifactId(), project.getBasedir().getPath(), project.getVersion(), null);

        List<PackageInfo> bundlePluginExplicitPackages = new ArrayList<PackageInfo>();
        Map<String, String> originalInstructions = new LinkedHashMap<String, String>();
        try {
            Xpp3Dom felixBundlePluginConfiguration = (Xpp3Dom) project.getPlugin("org.apache.felix:maven-bundle-plugin")
                    .getConfiguration();
            if (felixBundlePluginConfiguration != null) {
                Xpp3Dom instructionsDom = felixBundlePluginConfiguration.getChild("instructions");
                for (Xpp3Dom instructionChild : instructionsDom.getChildren()) {
                    originalInstructions.put(instructionChild.getName(), instructionChild.getValue());
                }
                if (felixBundlePluginConfiguration.getChild("excludeDependencies") != null) {
                    excludeDependencies = felixBundlePluginConfiguration.getChild("excludeDependencies").getValue();
                }
                getBundlePluginExplicitPackageImports(projectParsingContext, bundlePluginExplicitPackages, originalInstructions);
            }
        } catch (Exception e) {
            // no overrides
            getLog().info("No maven-bundle-plugin found, will not use dependency exclude or deal with explicit Import-Package configurations. (" + e.getMessage() + ")");
        }

        try {
            Builder builder = getOSGiBuilder(project, originalInstructions, getClasspath(project));
            resolveEmbeddedDependencies(project, builder);
        } catch (Exception e) {
            throw new MojoExecutionException("Error trying to process bundle plugin instructions", e);
        }

        List<PackageInfo> existingPackageImports = getExistingImportPackages(projectParsingContext);
        explicitPackageImports.addAll(existingPackageImports);

        parsingContextCache = new ParsingContextCache(new File(dependencyParsingCacheDirectory), null);

        long timer = System.currentTimeMillis();
        int scanned = 0;
        try {
            scanClassesBuildDirectory(projectParsingContext);

            getLog().info(
                    "Scanned classes directory in " + (System.currentTimeMillis() - timer) + " ms. Found "
                            + projectParsingContext.getLocalPackages().size() + " project packages.");

            scanned = scanDependencies(projectParsingContext);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while scanning dependencies", e);
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error while scanning project packages", e);
        }

        getLog().info(
                "Scanned " + scanned + " project dependencies in " + (System.currentTimeMillis() - timer)
                        + " ms. Currently we have " + projectParsingContext.getLocalPackages().size() + " project packages.");

        projectParsingContext.postProcess();

        if (projectParsingContext.getSplitPackages().size() > 0) {
            if (failBuildOnSplitPackages) {
                StringBuilder splitPackageList = new StringBuilder();
                for (PackageInfo packageInfo : projectParsingContext.getSplitPackages()) {
                    splitPackageList.append("  ");
                    splitPackageList.append(packageInfo.toString());
                    splitPackageList.append(" from locations:\n    ");
                    splitPackageList.append(StringUtils.join(packageInfo.getSourceLocations(), "\n    "));
                    splitPackageList.append("\n");
                }
                throw new MojoExecutionException("Detected split packages:\n" + splitPackageList.toString());
            }
        }

        String extension = project.getPackaging();
        if ("bundle".equals(extension)) {
            extension = "jar";
        }
        String artifactFilePath = project.getBuild().getDirectory() + "/" + project.getBuild().getFinalName() + "." + extension;

        File artifactFile = new File(artifactFilePath);
        if (!artifactFile.exists()) {
            throw new MojoExecutionException("No artifact generated for project, was the goal called in the proper phase (should be verify) ?");
        }

        Set<String> allPackages = getAvailablePackages();
        try (Jar jarFile = new Jar(artifactFile)) {
            // Include all local packages
            allPackages.addAll(jarFile.getPackages());

            Manifest manifest = jarFile.getManifest();
            if (manifest.getMainAttributes() == null) {
                throw new MojoExecutionException("Error reading OSGi bundle manifest data from artifact " + artifactFile);
            }
            String importPackageHeaderValue = manifest.getMainAttributes().getValue("Import-Package");
            Set<String> visitedPackageImports = new TreeSet<>();
            if (importPackageHeaderValue != null) {
                List<ManifestValueClause> importPackageClauses = BundleUtils.getHeaderClauses("Import-Package", importPackageHeaderValue);
                List<ManifestValueClause> clausesToRemove = new ArrayList<>();
                boolean modifiedImportPackageClauses = false;
                for (ManifestValueClause importPackageClause : importPackageClauses) {
                    for (String importPackagePath : importPackageClause.getPaths()) {
                        PackageInfo info = getPackageInfo(existingPackageImports, importPackagePath);
                        if (info == null) {
                            info = getPackageInfo(bundlePluginExplicitPackages, importPackagePath);
                        }
                        if (info != null) {
                            // the package was explicitely configured either through Maven properties or through
                            // explicit configuration in the bundle plugin, in this case we will not touch the
                            // package's resolution directive
                            getLog().info("Explicit package configuration found for " + importPackagePath + ".");
                            if (info.getVersion() != null) {
                                importPackageClause.getAttributes().put("version", info.getVersion());
                            }
                            for (Map.Entry<Object, Object> entry : info.getOtherDirectives().entrySet()) {
                                importPackageClause.getDirectives().put((String) entry.getKey(), (String) entry.getValue());
                            }
                        } else if (!"mandatory".equals(importPackageClause.getDirectives().get("resolution")) && !allPackages.contains(importPackagePath)) {
                            importPackageClause.getDirectives().put("resolution", "optional");
                            modifiedImportPackageClauses = true;
                        }
                        if (visitedPackageImports.contains(importPackagePath)) {
                            getLog().warn("Duplicate import detected on package " + importPackagePath + ", will remove duplicate. To remove this warning remove the duplicate import (possibly coming from a explicit import in the maven-bundle-plugin instructions)");
                            clausesToRemove.add(importPackageClause);
                            modifiedImportPackageClauses = true;
                        }

                        visitedPackageImports.add(importPackagePath);
                    }
                }
                if (modifiedImportPackageClauses) {
                    for (ManifestValueClause clauseToRemove : clausesToRemove) {
                        boolean removeSuccessful = importPackageClauses.remove(clauseToRemove);
                        if (!removeSuccessful) {
                            getLog().warn("Removal of clause " + clauseToRemove + " was not successful, duplicates may still remain in Manifest !");
                        }
                    }
                    updateBundle(manifest, importPackageClauses, artifactFile, buildDirectory);
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error reading OSGi bundle manifest data from artifact " + artifactFile, e);
        }
    }

    public void getBundlePluginExplicitPackageImports(ParsingContext projectParsingContext, List<PackageInfo> bundlePluginExplicitPackages, Map<String, String> originalInstructions) throws IOException {
        String importPackageInstruction = originalInstructions.get("Import-Package");
        // now let's remove all variable substitutions
        importPackageInstruction = importPackageInstruction.replaceAll(",(\\s)*\\$\\{.*\\}", "");
        List<ManifestValueClause> existingImportValueClauses = BundleUtils.getHeaderClauses("Import-Package", importPackageInstruction);
        for (ManifestValueClause existingImportValueClause : existingImportValueClauses) {
            String clauseVersion = existingImportValueClause.getAttributes().get("version");
            String clauseResolution = existingImportValueClause.getDirectives().get("resolution");
            boolean optionalClause = false;
            if ("optional".equals(clauseResolution)) {
                optionalClause = true;
            }
            for (String existingImportPath : existingImportValueClause.getPaths()) {
                bundlePluginExplicitPackages.add(new PackageInfo(existingImportPath, clauseVersion, optionalClause, "Maven plugin configuration", projectParsingContext));
            }
        }
    }

    private PackageInfo getPackageInfo(List<PackageInfo> packages, String packageName) {
        for (PackageInfo packageInfo : packages) {
            if (packageInfo.getName().equals(packageName)) {
                return packageInfo;
            }
        }
        return null;
    }

    private void updateBundle(Manifest manifest, List<ManifestValueClause> importPackageClauses, File artifactFile, String buildDirectory) {
        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (ManifestValueClause importPackageClause : importPackageClauses) {
            sb.append(separator);
            sb.append(importPackageClause.toString());
            separator = ",";
        }
        manifest.getMainAttributes().putValue("Import-Package", sb.toString());
        File expandedJarDirectory = unpackBundle(artifactFile);
        getLog().info("Extract JAR " + artifactFile + " contents to directory " + expandedJarDirectory);
        if (expandedJarDirectory == null) {
            getLog().error("Error unpacking artifact " + artifactFile + " aborting bundle update");
            return;
        }
        File manifestFile = new File(expandedJarDirectory, "META-INF/MANIFEST.MF");
        if (manifestFile.exists()) {
            getLog().info("Overwriting existing META-INF/MANIFEST file");
        } else {
            getLog().warn("Missing META-INF/MANIFEST.MF file in bundle, how did that happen ?");
        }
        FileOutputStream manifestFileOutputStream = null;
        try {
            manifestFileOutputStream = new FileOutputStream(manifestFile);
            manifest.write(manifestFileOutputStream);
        } catch (FileNotFoundException e) {
            getLog().error("Error writing new META-INF/MANIFEST.MF file", e);
            return;
        } catch (IOException e) {
            getLog().error("Error writing new META-INF/MANIFEST.MF file", e);
            return;
        } finally {
            IOUtils.closeQuietly(manifestFileOutputStream);
        }
        packBundle(artifactFile, manifestFile, expandedJarDirectory);

        try {
            FileUtils.deleteDirectory(expandedJarDirectory);
            getLog().info("Deleted temporary JAR extraction directory " + expandedJarDirectory);
        } catch (IOException e) {
            getLog().error("Error purging temporary extracted JAR directory " + expandedJarDirectory, e);
        }

        Artifact mainArtifact = project.getArtifact();

        if ("bundle".equals(mainArtifact.getType())) {
            // workaround for MNG-1682: force maven to install artifact using the "jar" handler
            mainArtifact.setArtifactHandler(artifactHandlerManager.getArtifactHandler("jar"));
        }

        if (null == classifier || classifier.trim().length() == 0) {
            mainArtifact.setFile(artifactFile);
        } else {
            mavenProjectHelper.attachArtifact(project, artifactFile, classifier);
        }

    }

    private static final String DELIM_START = "${";
    private static final String DELIM_STOP = "}";

    /**
     * <p>
     * This method performs property variable substitution on the
     * specified value. If the specified value contains the syntax
     * <tt>${&lt;prop-name&gt;}</tt>, where <tt>&lt;prop-name&gt;</tt>
     * refers to either a configuration property or a system property,
     * then the corresponding property value is substituted for the variable
     * placeholder. Multiple variable placeholders may exist in the
     * specified value as well as nested variable placeholders, which
     * are substituted from inner most to outer most. Configuration
     * properties override system properties.
     * </p>
     *
     * @param val         The string on which to perform property substitution.
     * @param currentKey  The key of the property being evaluated used to
     *                    detect cycles.
     * @param cycleMap    Map of variable references used to detect nested cycles.
     * @param configProps Set of configuration properties.
     * @return The value of the specified string after system property substitution.
     * @throws IllegalArgumentException If there was a syntax error in the
     *                                  property placeholder syntax or a recursive variable reference.
     **/
    public static String substVars(String val, String currentKey,
                                   Map cycleMap, Properties configProps)
            throws IllegalArgumentException {
        // If there is currently no cycle map, then create
        // one for detecting cycles for this invocation.
        if (cycleMap == null) {
            cycleMap = new HashMap();
        }

        // Put the current key in the cycle map.
        cycleMap.put(currentKey, currentKey);

        // Assume we have a value that is something like:
        // "leading ${foo.${bar}} middle ${baz} trailing"

        // Find the first ending '}' variable delimiter, which
        // will correspond to the first deepest nested variable
        // placeholder.
        int stopDelim = -1;
        int startDelim = -1;

        do {
            stopDelim = val.indexOf(DELIM_STOP, stopDelim + 1);
            // If there is no stopping delimiter, then just return
            // the value since there is no variable declared.
            if (stopDelim < 0) {
                return val;
            }
            // Try to find the matching start delimiter by
            // looping until we find a start delimiter that is
            // greater than the stop delimiter we have found.
            startDelim = val.indexOf(DELIM_START);
            // If there is no starting delimiter, then just return
            // the value since there is no variable declared.
            if (startDelim < 0) {
                return val;
            }
            while (stopDelim >= 0) {
                int idx = val.indexOf(DELIM_START, startDelim + DELIM_START.length());
                if ((idx < 0) || (idx > stopDelim)) {
                    break;
                } else if (idx < stopDelim) {
                    startDelim = idx;
                }
            }
        }
        while ((startDelim > stopDelim) && (stopDelim >= 0));

        // At this point, we have found a variable placeholder so
        // we must perform a variable substitution on it.
        // Using the start and stop delimiter indices, extract
        // the first, deepest nested variable placeholder.
        String variable =
                val.substring(startDelim + DELIM_START.length(), stopDelim);

        // Verify that this is not a recursive variable reference.
        if (cycleMap.get(variable) != null) {
            throw new IllegalArgumentException(
                    "recursive variable reference: " + variable);
        }

        // Get the value of the deepest nested variable placeholder.
        // Try to configuration properties first.
        String substValue = (configProps != null)
                ? configProps.getProperty(variable, null)
                : null;
        if (substValue == null) {
            // Ignore unknown property values.
            substValue = System.getProperty(variable, "");
        }

        // Remove the found variable from the cycle map, since
        // it may appear more than once in the value and we don't
        // want such situations to appear as a recursive reference.
        cycleMap.remove(variable);

        // Append the leading characters, the substituted value of
        // the variable, and the trailing characters to get the new
        // value.
        val = val.substring(0, startDelim)
                + substValue
                + val.substring(stopDelim + DELIM_STOP.length(), val.length());

        // Now perform substitution again, since there could still
        // be substitutions to make.
        val = substVars(val, currentKey, cycleMap, configProps);

        // Return the value.
        return val;
    }

    private void loadSystemPackages() throws MojoExecutionException {
        Properties dependenciesProperties = new Properties();
        InputStream dependenciesPropertiesStream = this.getClass().getClassLoader().getResourceAsStream("org/jahia/utils/maven/plugin/osgi/dependencies.properties");
        try {
            dependenciesProperties.load(dependenciesPropertiesStream);
            dependenciesProperties.setProperty("dollar", "$");
            String propertyName = "org.osgi.framework.system.packages";
            String systemPackagesValue = dependenciesProperties.getProperty(propertyName);
            systemPackagesValue = (systemPackagesValue != null)
                    ? substVars(systemPackagesValue, propertyName, null, dependenciesProperties)
                    : null;
            List<ManifestValueClause> systemPackageClauses = BundleUtils.getHeaderClauses("Export-Package", systemPackagesValue);
            for (ManifestValueClause systemPackageClause : systemPackageClauses) {
                for (String systemPackagePath : systemPackageClause.getPaths()) {
                    String clauseVersion = systemPackageClause.getAttributes().get("version");
                    PackageInfo systemPackageInfo = new PackageInfo(systemPackagePath, clauseVersion, false, "System packages", null);
                    systemPackages.add(systemPackageInfo);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error loading system exports", e);
        }
    }

    private File unpackBundle(File jarFile) {
        File outputDir = new File(buildDirectory, jarFile.getName() + "-" + System.currentTimeMillis());
        if (outputDir.exists()) {
            getLog().error("Problem unpacking " + jarFile + " to " + outputDir + " : directory already exists !");
            return null;
        }

        try {
            /*
             * this directory must exist before unpacking, otherwise the plexus
             * unarchiver decides to use the current working directory instead!
             */
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            UnArchiver unArchiver = archiverManager.getUnArchiver("jar");
            unArchiver.setDestDirectory(outputDir);
            unArchiver.setSourceFile(jarFile);
            unArchiver.extract();
        } catch (Exception e) {
            getLog().error("Problem unpacking " + jarFile + " to " + outputDir, e);
            return null;
        }
        return outputDir;
    }

    private void packBundle(File jarFile, File manifestFile, File contentDirectory) {
        try {
            JarArchiver archiver = (JarArchiver) archiverManager.getArchiver("jar");

            archiver.setManifest(manifestFile);
            archiver.setDestFile(jarFile);

            archiver.addDirectory(contentDirectory, null, null);
            archiver.createArchive();
        } catch (Exception e) {
            getLog().error("Problem packing " + jarFile + " with contents from  " + contentDirectory, e);
        }

    }

    private Set<String> getAvailablePackages() throws MojoExecutionException {
        Set<String> allPackages = new HashSet<>();
        ArrayList<Artifact> inscope = new ArrayList<>();
        final Collection<Artifact> artifacts = getSelectedDependencies(project.getArtifacts());
        for (Iterator<Artifact> it = artifacts.iterator(); it.hasNext(); ) {
            Artifact artifact = it.next();
            if (artifact.getArtifactHandler().isAddedToClasspath() && !artifact.getScope().equals("system")) {
                inscope.add(artifact);
            }
        }

        for (Artifact artifact : inscope) {
            File file = artifact.getFile();
            if (file == null) {
                continue;
            }

            try (Jar jar = new Jar(artifact.getArtifactId(), file)) {
                Set<String> packages = new HashSet<>(jar.getPackages());

                System.out.println(artifact.getArtifactId() + ":" + artifact.getScope());

                if ("provided".equals(artifact.getScope())) {
                    if (jar.getManifest() != null) {
                        String value = jar.getManifest().getMainAttributes().getValue("Export-Package");
                        if (value != null) {
                            List<ManifestValueClause> l = BundleUtils.getHeaderClauses("Export-Package", value);
                            packages.retainAll(l.stream().flatMap(c -> c.getPaths().stream()).collect(Collectors.toSet()));
                            allPackages.addAll(packages);
                        }
                    }
                } else {
                    allPackages.addAll(packages);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return allPackages;
    }

    private Collection<Artifact> getSelectedDependencies(Collection<Artifact> artifacts) throws MojoExecutionException {
        if (null == excludeDependencies || excludeDependencies.isEmpty()) {
            return artifacts;
        } else if ("true".equalsIgnoreCase(excludeDependencies)) {
            return Collections.emptyList();
        }

        Collection<Artifact> selectedDependencies = new LinkedHashSet<>(artifacts);
        DependencyExcluder excluder = new DependencyExcluder(artifacts);
        excluder.processHeaders(excludeDependencies);
        selectedDependencies.removeAll(excluder.getExcludedArtifacts());

        return selectedDependencies;
    }

}
