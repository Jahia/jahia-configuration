/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
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

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.jahia.utils.osgi.ClassDependencyTracker;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarInputStream;

/**
 * A little utility goal to locate a package usage inside the project's dependencies, using BND to make sure we
 * scan the same way.
 *
 * @goal find-package-uses
 * @requiresDependencyResolution test
 */
public class FindPackageUsesMojo extends AbstractMojo {

    /**
     * @parameter default-value="${packageNames}"
     */
    protected List<String> packageNames = new ArrayList<String>();

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    /**
     * The directory for the generated bundles.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File outputDirectory;

    /**
     * @parameter default-value="true"
     */
    private boolean searchInDependencies = true;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (packageNames == null || packageNames.size() == 0) {
            getLog().warn("No package names specified, will abort now !");
            return;
        }
        final Map<String, Map<String, Artifact>> packageResults = findPackageUses(packageNames, project.getArtifacts(), project, outputDirectory, searchInDependencies, getLog());
        getLog().info("=================================================================================");
        getLog().info("SEARCH RESULTS SUMMARY");
        getLog().info("---------------------------------------------------------------------------------");
        for (String packageName : packageNames) {
            if (!packageResults.containsKey(packageName)) {
                getLog().warn("Couldn't find " + packageName + " uses anywhere !");
            } else {
                getLog().info("Package " + packageName + " used in :");
                Map<String, Artifact> foundClasses = packageResults.get(packageName);
                for (Map.Entry<String, Artifact> foundClass : foundClasses.entrySet()) {
                    getLog().info("  " + foundClass.getKey() + " ( " + foundClass.getValue().getFile() + ")");
                }
            }
        }
    }

    public static Map<String, Map<String, Artifact>> findPackageUses(List<String> packageNames, Set<Artifact> artifacts, MavenProject project, File buildOutputDirectory, boolean searchInDependencies, Log log) {
        final Map<String, Map<String, Artifact>> packageResults = new TreeMap<String, Map<String, Artifact>>();

        log.info("Scanning project build directory...");

        if (buildOutputDirectory.exists()) {
            findPackageUsesInDirectory(packageNames, project, log, packageResults, buildOutputDirectory);
        }

        if (!searchInDependencies) {
            return packageResults;
        }

        log.info("Scanning project dependencies...");

        for (Artifact artifact : artifacts) {
            if (artifact.isOptional()) {
                log.debug("Processing optional dependency " + artifact + "...");
            }
            if (artifact.getType().equals("pom")) {
                log.warn("Skipping POM artifact " + artifact);
                continue;
            }
            if (!artifact.getType().equals("jar")) {
                log.warn("Found non JAR artifact " + artifact);
            }
            findPackageUsesInArtifact(packageNames, project, log, packageResults, artifact);
        }
        return packageResults;
    }

    public static void findPackageUsesInArtifact(List<String> packageNames, MavenProject project, Log log, Map<String, Map<String, Artifact>> packageResults, Artifact artifact) {
        boolean currentTrailWasDisplayed = false;
        int trailDepth = artifact.getDependencyTrail().size();
        for (String packageName : packageNames) {
            Map<String, Artifact> foundClasses = packageResults.get(packageName);
            if (foundClasses == null) {
                foundClasses = new TreeMap<String, Artifact>();
            }
            Set<String> classesThatHaveDependency = findClassesThatUsePackage(artifact.getFile(), packageName, project, log);
            if (classesThatHaveDependency != null & classesThatHaveDependency.size() > 0) {
                List<String> trail = new ArrayList<String>(artifact.getDependencyTrail());
                if (artifact.isOptional()) {
                    trail.add("[optional]");
                }
                for (String classThatHasDependency : classesThatHaveDependency) {
                    if (!currentTrailWasDisplayed) {
                        displayTrailTree(project, artifact, log);
                        currentTrailWasDisplayed = true;
                    }
                    log.info(getPaddingString(trailDepth) + "+--> Found class " + classThatHasDependency + " that uses package " + packageName);
                    foundClasses.put(classThatHasDependency, artifact);
                }
                packageResults.put(packageName, foundClasses);
            }
        }
    }

    public static void findPackageUsesInDirectory(List<String> packageNames, MavenProject project, Log log, Map<String, Map<String, Artifact>> packageResults, File directory) {
        for (String packageName : packageNames) {
            Map<String, Artifact> foundClasses = packageResults.get(packageName);
            if (foundClasses == null) {
                foundClasses = new TreeMap<String, Artifact>();
            }
            Set<String> classesThatHaveDependency = findClassesThatUsePackage(directory, packageName, project, log);
            if (classesThatHaveDependency != null & classesThatHaveDependency.size() > 0) {
                for (String classThatHasDependency : classesThatHaveDependency) {
                    log.info( "+--> Found class " + classThatHasDependency + " that uses package " + packageName);
                    foundClasses.put(classThatHasDependency, project.getArtifact());
                }
                packageResults.put(packageName, foundClasses);
            }
        }
    }

    private static Set<String> findClassesThatUsePackage(File jarFile, String packageName, MavenProject project, Log log) {
        Set<String> classesThatHaveDependency = new TreeSet<String>();
        JarInputStream jarInputStream = null;
        if (jarFile == null) {
            log.warn("File is null !");
            return classesThatHaveDependency;
        }
        if (!jarFile.exists()) {
            log.warn("File " + jarFile + " does not exist !");
            return classesThatHaveDependency;
        }
        log.debug("Scanning JAR " + jarFile + "...");
        try {
            classesThatHaveDependency = ClassDependencyTracker.findDependencyInJar(jarFile, packageName, project.getTestClasspathElements());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (DependencyResolutionRequiredException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            IOUtils.closeQuietly(jarInputStream);
        }
        return classesThatHaveDependency;
    }

    private static void displayTrailTree(MavenProject project, Artifact artifact, Log log) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (String trailEntry : artifact.getDependencyTrail()) {
            builder.append(trailEntry);
            Artifact dependencyArtifact = findArtifactInProject(project, trailEntry, log);
            if (dependencyArtifact != null) {
                if (dependencyArtifact.isOptional()) {
                    builder.append(" [OPTIONAL]");
                }
                if (dependencyArtifact.getScope() != null &&
                        dependencyArtifact.getScope().contains(Artifact.SCOPE_PROVIDED)) {
                    builder.append(" [PROVIDED]");
                }
            }
            if (i < artifact.getDependencyTrail().size() - 1) {
                log.info(builder.toString());
                builder = new StringBuilder();
                builder.append(getPaddingString(i));
                builder.append("+- ");
            }
            i++;
        }
        builder.append(" (" + artifact.getFile() + ") : ");
        log.info(builder.toString());
    }

    private static Artifact findArtifactInProject(MavenProject project, String artifactIdentifier, Log log) {
        List<Artifact> results = new ArrayList<Artifact>();
        if (artifactMatches(project.getArtifact(), artifactIdentifier)) {
            results.add(project.getArtifact());
        }
        for (Artifact artifact : project.getArtifacts()) {
            if (artifactMatches(artifact, artifactIdentifier)) {
                results.add(artifact);
            }
        }
        if (results.size() > 1) {
            log.warn("Found more than one matching dependency for identifier " + artifactIdentifier + ":");
            for (Artifact resultArtifact : results) {
                log.warn(" --> " + resultArtifact.toString());
            }
            return null;
        } else {
            if (results.size() == 1) {
                return results.get(0);
            } else {
                log.warn("Couldn't find project dependency for identifier " + artifactIdentifier + "!");
                return null;
            }
        }
    }

    private static boolean artifactMatches(Artifact artifact, String artifactIdentifier) {
        String[] artifactIdentifierParts = artifactIdentifier.split(":");
        String artifactGroupId = null;
        String artifactId = null;
        String artifactType = null;
        String artifactClassifier = null;
        String artifactVersion = null;
        artifactGroupId = artifactIdentifierParts[0];
        artifactId = artifactIdentifierParts[1];
        if (artifactIdentifierParts.length >= 5) {
            artifactType = artifactIdentifierParts[2];
            artifactClassifier = artifactIdentifierParts[3];
            artifactVersion = artifactIdentifierParts[4];
        } else {
            if (artifactIdentifierParts.length > 2) {
                artifactType = artifactIdentifierParts[2];
            }
            if (artifactIdentifierParts.length > 3) {
                artifactVersion = artifactIdentifierParts[3];
            }
        }
        if (!artifact.getGroupId().equals(artifactGroupId)) {
            return false;
        }
        if (!artifact.getArtifactId().equals(artifactId)) {
            return false;
        }
        if (artifactType != null) {
            if (!artifact.getType().equals(artifactType)) {
                System.out.print(artifact.toString() + " == " + artifactIdentifier + " ? ");
                System.out.println("Type didn't match : " + artifact.getType() + " != " + artifactType);
                return false;
            }
        }
        if (artifactClassifier != null) {
            if (!artifactClassifier.equals(artifact.getClassifier())) {
                System.out.print(artifact.toString() + " == " + artifactIdentifier + " ? ");
                System.out.println("Classifier didn't match : " + artifact.getClassifier() + " != " + artifactClassifier);
                return false;
            }

        }
        if (artifactVersion != null) {
            if (!artifact.getBaseVersion().equals(artifactVersion)) {
                System.out.print(artifact.toString() + " == " + artifactIdentifier + " ? ");
                System.out.println("Version didn't match : " + artifact.getVersion() + " != " + artifactVersion);
                return false;
            }
        }
        return true;
    }

    private static String getPaddingString(int i) {
        StringBuilder builder = new StringBuilder();
        for (int j = 0; j < i; j++) {
            builder.append("  ");
        }
        return builder.toString();
    }

    private String getTrail(Artifact artifact) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (String trailEntry : artifact.getDependencyTrail()) {
            builder.append(trailEntry);
            if (i < artifact.getDependencyTrail().size() - 1) {
                builder.append(" -> ");
            }
            i++;
        }
        builder.append(" (" + artifact.getFile() + ") ");
        return builder.toString();
    }

}
