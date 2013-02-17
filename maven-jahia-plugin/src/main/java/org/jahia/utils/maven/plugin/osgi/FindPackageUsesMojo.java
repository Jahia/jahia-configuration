package org.jahia.utils.maven.plugin.osgi;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.tika.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;

/**
 * A little utility goal to locate a package usage inside the project's dependencies, using BND to make sure we
 * scan the same way.
 *
 * @goal findPackageUses
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
        final Map<String, Map<String,List<String>>> packageResults = new HashMap<String, Map<String,List<String>>>();
        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.isOptional()) {
                getLog().debug("Processing optional dependency " + artifact + "...");
            }
            if (!artifact.getType().equals("jar")) {
                getLog().warn("Found non JAR artifact " + artifact);
            }
            for (String packageName : packageNames) {
                final Map<String, List<String>> foundClasses = new HashMap<String, List<String>>();
                List<String> classesThatHaveDependency = findClassesThatUsePackage(artifact.getFile(), packageName);
                if (classesThatHaveDependency != null & classesThatHaveDependency.size() > 0) {
                    List<String> trail = new ArrayList<String>(artifact.getDependencyTrail());
                    if (artifact.isOptional()) {
                        trail.add("[optional]");
                    }
                    for (String classThatHasDependency : classesThatHaveDependency) {
                        getLog().info("Found class " + classThatHasDependency + " that uses package "+packageName+" in " + getTrail(trail));
                        foundClasses.put(classThatHasDependency, trail);
                    }
                    packageResults.put(packageName, foundClasses);
                }
            }
        }
        getLog().info("=================================================================================");
        getLog().info("SEARCH RESULTS");
        getLog().info("---------------------------------------------------------------------------------");
        for (String packageName : packageNames) {
            if (!packageResults.containsKey(packageName)) {
                getLog().warn("Couldn't find " + packageName + " uses anywhere !");
            } else {
                Map<String, List<String>> foundClasses = packageResults.get(packageName);
                for (Map.Entry<String, List<String>> foundClass : foundClasses.entrySet()) {
                    getLog().info("Found package " + packageName + " used in class " + foundClass.getKey() + " from trail " + getTrail(foundClass.getValue()));
                }
            }
        }

    }

    private List<String> findClassesThatUsePackage(File jarFile, String packageName) {
        List<String> classesThatHaveDependency = new ArrayList<String>();
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
            classesThatHaveDependency = DependencyTracker.findDependencyInJar(jarFile, packageName, project.getTestClasspathElements());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (DependencyResolutionRequiredException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            IOUtils.closeQuietly(jarInputStream);
        }
        return classesThatHaveDependency;
    }

    private String getTrail(List<String> dependencyTrail) {
        StringBuilder builder = new StringBuilder();
        int i=0;
        for (String trailEntry : dependencyTrail) {
            builder.append(trailEntry);
            if (i < dependencyTrail.size()-1) {
                builder.append(" -> ");
            }
            i++;
        }
        return builder.toString();
    }

}
