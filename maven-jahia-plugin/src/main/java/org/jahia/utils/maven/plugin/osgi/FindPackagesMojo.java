package org.jahia.utils.maven.plugin.osgi;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.graph.Exclusion;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * A little utility goal to locate a package inside the project's dependencies, including optional or provided ones.
 *
 * @goal find-packages
 * @requiresDependencyResolution test
 */

public class FindPackagesMojo extends AbstractMojo {

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
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of plugins and their dependencies.
     *
     * @parameter default-value="${project.remotePluginRepositories}"
     * @readonly
     */
    private List<RemoteRepository> remoteRepos;

    public class PackagerFinderDependencyVisitor implements DependencyVisitor {

        private String packageName;
        private Map<String,List<String>> foundPackages;
        private boolean excludedDependency = false;
        private List<String> dependencyTrail = null;

        public PackagerFinderDependencyVisitor(String packageName, Map<String,List<String>> foundPackages, boolean excludedDependency, List<String> dependencyTrail) {
            this.packageName = packageName;
            this.foundPackages = foundPackages;
            this.excludedDependency = excludedDependency;
            this.dependencyTrail = new ArrayList(dependencyTrail);
        }

        @Override
        public boolean visitEnter(DependencyNode node) {
            if (node.getDependency().getArtifact().getFile() == null) {
                getLog().warn("No local file for artifact " + node.getDependency().getArtifact());
                return true;
            }
            if (node.getDependency().isOptional()) {
                getLog().debug("Processing optional file " + node.getDependency().getArtifact().getFile() + "...");
            }
            List<String> curTrail = new ArrayList(dependencyTrail) ;
            String trailSuffix="";
            if (!excludedDependency) {
                if (node.getDependency().isOptional()) {
                    trailSuffix="[optional]";
                }
            } else {
                if (node.getDependency().isOptional()) {
                    trailSuffix="[excluded+optional]";
                } else {
                    trailSuffix="[excluded]";
                }
            }
            curTrail.add(node.toString()+trailSuffix);
            String trail = getTrail(dependencyTrail);
            if (doesJarHavePackageName(node.getDependency().getArtifact().getFile(), packageName)) {
                if (!excludedDependency) {
                    if (node.getDependency().isOptional()) {
                        getLog().info(trail +": Found package " + packageName + " in optional artifact " + node.getDependency().getArtifact().getFile());
                    } else {
                        getLog().info(trail +": Found package " + packageName + " in artifact " + node.getDependency().getArtifact().getFile());
                    }
                } else {
                    if (node.getDependency().isOptional()) {
                        getLog().warn(trail + ": Found package " + packageName + " in optional excluded artifact " + node.getDependency().getArtifact().getFile());
                    } else {
                        getLog().warn(trail + ": Found package " + packageName + " in excluded artifact " + node.getDependency().getArtifact().getFile());
                    }
                }
                foundPackages.put(packageName, curTrail);
            } else {
                for (Exclusion exclusion : node.getDependency().getExclusions()) {
                    getLog().debug(trail + ": Processing exclusion " + exclusion + " of artifact " + node.getDependency().getArtifact());
                    DependencyNode exclusionNode = resolveExclusion(node, exclusion);
                    if (exclusionNode != null) {
                        exclusionNode.accept(new PackagerFinderDependencyVisitor(packageName, foundPackages, true, new ArrayList<String>(curTrail)));
                    }
                }
            }
            return true;
        }

        @Override
        public boolean visitLeave(DependencyNode node) {
            return true;
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (packageNames == null || packageNames.size() == 0) {
            getLog().warn("No package names specified, will abort now !");
            return;
        }
        getLog().info("Scanning project dependencies...");
        final Map<String, List<String>> foundPackages = new HashMap<String, List<String>>();
        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.isOptional()) {
                getLog().debug("Processing optional dependency " + artifact + "...");
            }
            if (!artifact.getType().equals("jar")) {
                getLog().warn("Found non JAR artifact " + artifact);
            }
            for (String packageName : packageNames) {
                if (doesJarHavePackageName(artifact.getFile(), packageName)) {
                    List<String> trail = new ArrayList<String>(artifact.getDependencyTrail());
                    if (artifact.isOptional()) {
                        trail.add("[optional]");
                    }
                    getLog().info("Found package " + packageName + " in " + getTrail(trail));
                    foundPackages.put(packageName, trail);
                }
            }
        }
        for (String packageName : packageNames) {
            if (!foundPackages.containsKey(packageName)) {
                getLog().warn("Couldn't find " + packageName + " in normal project dependencies, will now search optional (and excluded) dependencies");
                for (Artifact artifact : project.getArtifacts()) {
                    if (artifact.isOptional()) {
                        getLog().debug("Processing optional artifact " + artifact + "...");
                    }
                    DependencyNode dependencyNode = getDependencyNode(artifact);
                    if (dependencyNode != null) {
                        List<String> trail = new ArrayList<String>(artifact.getDependencyTrail());
                        dependencyNode.accept(new PackagerFinderDependencyVisitor(packageName, foundPackages, false, trail));
                    }
                }
            }
        }
        getLog().info("=================================================================================");
        getLog().info("SEARCH RESULTS");
        getLog().info("---------------------------------------------------------------------------------");
        for (String packageName : packageNames) {
            if (!foundPackages.containsKey(packageName)) {
                getLog().warn("Couldn't find " + packageName + " anywhere !");
            } else {
                getLog().info("Found package " + packageName + " in " + getTrail(foundPackages.get(packageName)));
            }
        }
    }

    private DependencyNode getDependencyNode(Artifact artifact) {
        String artifactCoords = artifact.getGroupId() + ":" + artifact.getArtifactId();
        if (StringUtils.isNotEmpty(artifact.getType()) && !("*".equals(artifact.getType())) ) {
            artifactCoords += ":" + artifact.getType();
        }
        if (StringUtils.isNotEmpty(artifact.getBaseVersion()) && !("*".equals(artifact.getBaseVersion())) ) {
            artifactCoords += ":" + artifact.getBaseVersion();
        }

        return getDependencyNode(artifactCoords);
    }

    private DependencyNode resolveExclusion(DependencyNode dependencyNode, Exclusion exclusion) {
        if (dependencyNode.getDependency().getArtifact().getGroupId().equals(exclusion.getGroupId()) &&
                dependencyNode.getDependency().getArtifact().getArtifactId().equals(exclusion.getArtifactId())) {
            return dependencyNode;
        }
        for (DependencyNode childNode : dependencyNode.getChildren()) {
            DependencyNode childDependency = resolveExclusion(childNode, exclusion);
            if (childDependency != null) {
                return childDependency;
            }
        }
        return null;
    }

    private DependencyNode getDependencyNode(String artifactCoords) {
        ArtifactRequest request = new ArtifactRequest();
        DefaultArtifact aetherArtifact = new DefaultArtifact(artifactCoords);
        request.setArtifact(aetherArtifact);
        request.setRepositories(remoteRepos);

        Dependency dependency =
                new Dependency(aetherArtifact, "compile");

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        collectRequest.setRepositories(remoteRepos);

        DependencyNode dependencyNode = null;
        try {
            dependencyNode = repoSystem.collectDependencies(repoSession, collectRequest).getRoot();
            DependencyRequest dependencyRequest = new DependencyRequest(dependencyNode, null);

            repoSystem.resolveDependencies(repoSession, dependencyRequest);

        } catch (DependencyCollectionException e) {
            getLog().error("Error collecting dependencies for " + artifactCoords + ": " + e.getMessage());
        } catch (DependencyResolutionException e) {
            getLog().error("Error resolving dependencies for " + artifactCoords + ": " + e.getMessage());
        }
        return dependencyNode;
    }

    private boolean doesJarHavePackageName(File jarFile, String packageName) {
        JarInputStream jarInputStream = null;
        if (jarFile == null) {
            getLog().warn("File is null !");
            return false;
        }
        if (!jarFile.exists()) {
            getLog().warn("File " + jarFile + " does not exist !");
            return false;
        }
        getLog().debug("Scanning JAR " + jarFile + "...");
        try {
            jarInputStream = new JarInputStream(new FileInputStream(jarFile));
            JarEntry jarEntry = null;
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                String jarPackageName = jarEntry.getName().replaceAll("/", ".");
                if (jarPackageName.endsWith(".")) {
                    jarPackageName = jarPackageName.substring(0, jarPackageName.length()-1);
                }
                if (jarPackageName.equals(packageName)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            IOUtils.closeQuietly(jarInputStream);
        }
        return false;
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
