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
package org.jahia.utils.maven.plugin.osgi;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.jahia.utils.osgi.BundleUtils;
import org.jahia.utils.osgi.ManifestValueClause;
import org.jahia.utils.osgi.parsers.PackageInfo;
import org.jahia.utils.osgi.parsers.ParsingContext;

import java.io.*;
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

    /**
     * Classifier type of the bundle to be installed.  For example, "jdk14".
     * Defaults to none which means this is the project's main bundle.
     *
     * @parameter
     */
    protected String classifier;
    /**
     * @parameter default-value="true"
     */
    protected boolean failBuildOnSplitPackages = true;
    /**
     * The directory for the generated JAR.
     *
     * @parameter default-value="false" expression="${jahia.modules.skipCheckDependencies}"
     */
    protected boolean skipCheckDependencies;
    /**
     * @component
     */
    private MavenProjectHelper mavenProjectHelper;
    /**
     * @component
     */
    private ArtifactHandlerManager artifactHandlerManager;
    /**
     * @component
     */
    private ArchiverManager archiverManager;

    @Override
    public void execute() throws MojoExecutionException {
        long timer = System.currentTimeMillis();
        if (skipCheckDependencies || !isValidPackaging()) {
            return;
        }

        initialize();

        List<PackageInfo> bundlePluginExplicitPackages = null;
        try {
            bundlePluginExplicitPackages = new ArrayList<>();
            getBundlePluginExplicitPackageImports(projectParsingContext, bundlePluginExplicitPackages, originalInstructions);
        } catch (IOException e) {
            e.printStackTrace();
        }


        int scanned;
        try {
            scanClassesBuildDirectory(projectParsingContext);

            getLog().info("Scanned classes directory in " + (System.currentTimeMillis() - timer) + " ms. Found " + projectParsingContext.getLocalPackages().size() + " project packages.");

            scanned = scanDependencies(projectParsingContext);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while scanning dependencies", e);
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error while scanning project packages", e);
        }

        getLog().info("Scanned " + scanned + " project dependencies in " + (System.currentTimeMillis() - timer) + " ms. Currently we have " + projectParsingContext.getLocalPackages().size() + " project packages.");

        projectParsingContext.postProcess();

        if (!projectParsingContext.getSplitPackages().isEmpty() && failBuildOnSplitPackages) {
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
            String importPackageHeaderValue = manifest.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
            Set<String> visitedPackageImports = new TreeSet<>();
            if (importPackageHeaderValue != null) {
                List<ManifestValueClause> importPackageClauses = BundleUtils.getHeaderClauses(Constants.IMPORT_PACKAGE, importPackageHeaderValue);
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
                                String bndVersion = importPackageClause.getAttributes().get(Constants.VERSION_ATTRIBUTE);
                                if (!bndVersion.equals(info.getVersion())) {
                                    getLog().info("Explicit package configuration version" + info.getVersion() + " is different than BND one " + bndVersion + " for " + importPackagePath + ".");
                                    modifiedImportPackageClauses = true;
                                }
                                importPackageClause.getAttributes().put(Constants.VERSION_ATTRIBUTE, info.getVersion());
                            }
                            for (Map.Entry<Object, Object> entry : info.getOtherDirectives().entrySet()) {
                                importPackageClause.getDirectives().put((String) entry.getKey(), (String) entry.getValue());
                            }
                        } else if (!"mandatory".equals(importPackageClause.getDirectives().get(Constants.RESOLUTION)) && !allPackages.contains(importPackagePath)) {
                            importPackageClause.getDirectives().put(Constants.RESOLUTION, Constants.OPTIONAL);
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
                    updateBundle(manifest, importPackageClauses, artifactFile);
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error reading OSGi bundle manifest data from artifact " + artifactFile, e);
        }
    }

    public void getBundlePluginExplicitPackageImports(ParsingContext projectParsingContext, List<PackageInfo> bundlePluginExplicitPackages, Map<String, String> originalInstructions) throws IOException {
        String importPackageInstruction = originalInstructions.get(Constants.IMPORT_PACKAGE);
        // now let's remove all variable substitutions
        importPackageInstruction = importPackageInstruction.replaceAll(",(\\s)*\\$\\{.*\\}", "");
        List<ManifestValueClause> existingImportValueClauses = BundleUtils.getHeaderClauses(Constants.IMPORT_PACKAGE, importPackageInstruction);
        for (ManifestValueClause existingImportValueClause : existingImportValueClauses) {
            String clauseVersion = existingImportValueClause.getAttributes().get(Constants.VERSION_ATTRIBUTE);
            String clauseResolution = existingImportValueClause.getDirectives().get(Constants.RESOLUTION);
            boolean optionalClause = false;
            if (Constants.OPTIONAL.equals(clauseResolution)) {
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

    private void updateBundle(Manifest manifest, List<ManifestValueClause> importPackageClauses, File artifactFile) {
        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (ManifestValueClause importPackageClause : importPackageClauses) {
            sb.append(separator);
            sb.append(importPackageClause.toString());
            separator = ",";
        }
        manifest.getMainAttributes().putValue(Constants.IMPORT_PACKAGE, sb.toString());
        File expandedJarDirectory = unpackBundle(artifactFile);
        getLog().info("Extract JAR " + artifactFile + " contents to directory " + expandedJarDirectory);
        File manifestFile = new File(expandedJarDirectory, "META-INF/MANIFEST.MF");
        if (manifestFile.exists()) {
            getLog().info("Overwriting existing META-INF/MANIFEST file");
        } else {
            getLog().warn("Missing META-INF/MANIFEST.MF file in bundle, how did that happen ?");
        }

        try (FileOutputStream manifestFileOutputStream = new FileOutputStream(manifestFile)) {
            manifest.write(manifestFileOutputStream);
        } catch (IOException e) {
            getLog().error("Error writing new META-INF/MANIFEST.MF file", e);
            return;
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

    private File unpackBundle(File jarFile) {
        File outputDir = new File(projectBuildDirectory, jarFile.getName() + "-" + System.currentTimeMillis());
        try {
            outputDir.mkdirs();
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

            String directDep = artifact.getDependencyTrail().get(1);

            try (Jar jar = new Jar(artifact.getArtifactId(), file)) {
                Set<String> packages = new HashSet<>(jar.getPackages());
                if (directDep.startsWith("org.jahia.server:jahia-impl:jar")) {
                    allPackages.addAll(packages);
                } else if ("provided".equals(artifact.getScope())) {
                    if (jar.getManifest() != null) {
                        String value = jar.getManifest().getMainAttributes().getValue(Constants.EXPORT_PACKAGE);
                        if (value != null) {
                            List<ManifestValueClause> l = BundleUtils.getHeaderClauses(Constants.EXPORT_PACKAGE, value);
                            packages.retainAll(l.stream().flatMap(c -> c.getPaths().stream()).collect(Collectors.toSet()));
                            allPackages.addAll(packages);
                        }
                    }
                } else {
//                    allPackages.addAll(packages);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return allPackages;
    }
}
