package org.jahia.utils.maven.plugin.osgi;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.tika.io.IOUtils;
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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (packageNames == null || packageNames.size() == 0) {
            getLog().warn("No package names specified, will abort now !");
            return;
        }
        getLog().info("Scanning project dependencies...");
        final Map<String, Map<String, Artifact>> packageResults = new TreeMap<String, Map<String, Artifact>>();
        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.isOptional()) {
                getLog().debug("Processing optional dependency " + artifact + "...");
            }
            if (!artifact.getType().equals("jar")) {
                getLog().warn("Found non JAR artifact " + artifact);
            }
            boolean currentTrailWasDisplayed = false;
            int trailDepth = artifact.getDependencyTrail().size();
            for (String packageName : packageNames) {
                Map<String, Artifact> foundClasses = packageResults.get(packageName);
                if (foundClasses == null) {
                    foundClasses = new TreeMap<String, Artifact>();
                }
                Set<String> classesThatHaveDependency = findClassesThatUsePackage(artifact.getFile(), packageName);
                if (classesThatHaveDependency != null & classesThatHaveDependency.size() > 0) {
                    List<String> trail = new ArrayList<String>(artifact.getDependencyTrail());
                    if (artifact.isOptional()) {
                        trail.add("[optional]");
                    }
                    for (String classThatHasDependency : classesThatHaveDependency) {
                        if (!currentTrailWasDisplayed) {
                            displayTrailTree(project, artifact);
                            currentTrailWasDisplayed = true;
                        }
                        getLog().info(getPaddingString(trailDepth) + "+--> Found class " + classThatHasDependency + " that uses package " + packageName);
                        foundClasses.put(classThatHasDependency, artifact);
                    }
                    packageResults.put(packageName, foundClasses);
                }
            }
        }
        getLog().info("=================================================================================");
        getLog().info("SEARCH RESULTS SUMMARY");
        getLog().info("---------------------------------------------------------------------------------");
        for (String packageName : packageNames) {
            if (!packageResults.containsKey(packageName)) {
                getLog().warn("Couldn't find " + packageName + " uses anywhere !");
            } else {
                getLog().info("Package " + packageName + " used in classes :");
                Map<String, Artifact> foundClasses = packageResults.get(packageName);
                for (Map.Entry<String, Artifact> foundClass : foundClasses.entrySet()) {
                    getLog().info("  " + foundClass.getKey() + " ( " + foundClass.getValue().getFile() + ")");
                }
            }
        }

    }

    private Set<String> findClassesThatUsePackage(File jarFile, String packageName) {
        Set<String> classesThatHaveDependency = new TreeSet<String>();
        JarInputStream jarInputStream = null;
        if (jarFile == null) {
            getLog().warn("File is null !");
            return classesThatHaveDependency;
        }
        if (!jarFile.exists()) {
            getLog().warn("File " + jarFile + " does not exist !");
            return classesThatHaveDependency;
        }
        getLog().debug("Scanning JAR " + jarFile + "...");
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

    private void displayTrailTree(MavenProject project, Artifact artifact) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (String trailEntry : artifact.getDependencyTrail()) {
            builder.append(trailEntry);
            Artifact dependencyArtifact = findArtifactInProject(project, trailEntry);
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
                getLog().info(builder.toString());
                builder = new StringBuilder();
                builder.append(getPaddingString(i));
                builder.append("+- ");
            }
            i++;
        }
        builder.append(" (" + artifact.getFile() + ") : ");
        getLog().info(builder.toString());
    }

    private Artifact findArtifactInProject(MavenProject project, String artifactIdentifier) {
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
            getLog().warn("Found more than one matching dependency for identifier " + artifactIdentifier + ":");
            for (Artifact resultArtifact : results) {
                getLog().warn(" --> " + resultArtifact.toString());
            }
            return null;
        } else {
            if (results.size() == 1) {
                return results.get(0);
            } else {
                getLog().warn("Couldn't find project dependency for identifier " + artifactIdentifier + "!");
                return null;
            }
        }
    }

    private boolean artifactMatches(Artifact artifact, String artifactIdentifier) {
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

    private String getPaddingString(int i) {
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
