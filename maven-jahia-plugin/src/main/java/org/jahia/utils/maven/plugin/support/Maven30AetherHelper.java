/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
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
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 */

package org.jahia.utils.maven.plugin.support;

import static org.jahia.utils.maven.plugin.support.MavenAetherHelperUtils.doesJarHavePackageName;
import static org.jahia.utils.maven.plugin.support.MavenAetherHelperUtils.getCoords;
import static org.jahia.utils.maven.plugin.support.MavenAetherHelperUtils.getTrail;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
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
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * Artifact and dependency resolution helper which will be used for Maven 3.0.x execution environment.
 * 
 * @author Sergiy Shyrkov
 */
public class Maven30AetherHelper implements AetherHelper {

    class PackagerFinderDependencyVisitor implements DependencyVisitor {

        private List<String> dependencyTrail = null;
        private boolean excludedDependency = false;
        private Map<String, List<String>> foundPackages;
        private String packageName;

        public PackagerFinderDependencyVisitor(String packageName, Map<String, List<String>> foundPackages,
                boolean excludedDependency, List<String> dependencyTrail) {
            this.packageName = packageName;
            this.foundPackages = foundPackages;
            this.excludedDependency = excludedDependency;
            this.dependencyTrail = new LinkedList<String>(dependencyTrail);
        }

        @Override
        public boolean visitEnter(DependencyNode node) {
            if (node.getDependency().getArtifact().getFile() == null) {
                log.warn("No local file for artifact " + node.getDependency().getArtifact());
                return true;
            }
            if (node.getDependency().isOptional()) {
                log.debug("Processing optional file " + node.getDependency().getArtifact().getFile() + "...");
            }
            List<String> curTrail = new LinkedList<String>(dependencyTrail);
            String trailSuffix = "";
            if (!excludedDependency) {
                if (node.getDependency().isOptional()) {
                    trailSuffix = "[optional]";
                }
            } else {
                if (node.getDependency().isOptional()) {
                    trailSuffix = "[excluded+optional]";
                } else {
                    trailSuffix = "[excluded]";
                }
            }
            curTrail.add(node.toString() + trailSuffix);
            String trail = getTrail(dependencyTrail);
            if (doesJarHavePackageName(node.getDependency().getArtifact().getFile(), packageName, log)) {
                if (!excludedDependency) {
                    if (node.getDependency().isOptional()) {
                        log.info(trail + ": Found package " + packageName + " in optional artifact "
                                + node.getDependency().getArtifact().getFile());
                    } else {
                        log.info(trail + ": Found package " + packageName + " in artifact "
                                + node.getDependency().getArtifact().getFile());
                    }
                } else {
                    if (node.getDependency().isOptional()) {
                        log.warn(trail + ": Found package " + packageName + " in optional excluded artifact "
                                + node.getDependency().getArtifact().getFile());
                    } else {
                        log.warn(trail + ": Found package " + packageName + " in excluded artifact "
                                + node.getDependency().getArtifact().getFile());
                    }
                }
                foundPackages.put(packageName, curTrail);
            } else {
                for (Exclusion exclusion : node.getDependency().getExclusions()) {
                    log.debug(trail + ": Processing exclusion " + exclusion + " of artifact "
                            + node.getDependency().getArtifact());
                    DependencyNode exclusionNode = resolveExclusion(node, exclusion);
                    if (exclusionNode != null) {
                        exclusionNode.accept(new PackagerFinderDependencyVisitor(packageName, foundPackages, true,
                                new LinkedList<String>(curTrail)));
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

    private Log log;

    private List<RemoteRepository> remoteRepos;

    private RepositorySystemSession repoSession;

    private RepositorySystem repoSystem;

    private Map<String, DependencyNode> resolvedDependencyNodes = new HashMap<String, DependencyNode>();

    public Maven30AetherHelper(RepositorySystem repoSystem, RepositorySystemSession repoSession,
            List<RemoteRepository> remoteRepos, Log log) {
        this.repoSystem = repoSystem;
        this.repoSession = repoSession;
        this.remoteRepos = remoteRepos;
        this.log = log;
    }

    private Set<String> findInWarDependencies(Artifact warArtifact, final String artifactId) {
        final Set<String> versions = new LinkedHashSet<String>();
        String artifactCoords = warArtifact.getGroupId() + ":" + warArtifact.getArtifactId() + ":"
                + warArtifact.getType() + ":" + warArtifact.getBaseVersion();
        DependencyNode node = null;
        if (resolvedDependencyNodes.containsKey(artifactCoords)) {
            node = resolvedDependencyNodes.get(artifactCoords);
        }

        if (node == null) {
            try {

                log.info("Resolving artifact " + artifactCoords + "...");
                ArtifactRequest request = new ArtifactRequest();
                request.setArtifact(new DefaultArtifact(artifactCoords));
                request.setRepositories(remoteRepos);

                Dependency dependency = new Dependency(new DefaultArtifact(artifactCoords), "compile");

                CollectRequest collectRequest = new CollectRequest();
                collectRequest.setRoot(dependency);
                collectRequest.setRepositories(remoteRepos);

                node = repoSystem.collectDependencies(repoSession, collectRequest).getRoot();

                DependencyRequest dependencyRequest = new DependencyRequest(node, null);

                repoSystem.resolveDependencies(repoSession, dependencyRequest);

                resolvedDependencyNodes.put(artifactCoords, node);

            } catch (DependencyCollectionException e) {
                log.error(e);
            } catch (DependencyResolutionException e) {
                log.error(e);
            }
        }

        if (node != null) {
            node.accept(new DependencyVisitor() {
                @Override
                public boolean visitEnter(DependencyNode node) {
                    if (node.getDependency().getArtifact().getFile().getName().equals(artifactId)) {
                        versions.add(node.getDependency().getArtifact().getBaseVersion());
                    }
                    return true;
                }

                @Override
                public boolean visitLeave(DependencyNode node) {
                    return true;
                }
            });

        }
        return versions;

    }

    @Override
    public Map<String, List<String>> findPackages(MavenProject project, List<String> packageNames)
            throws MojoExecutionException {
        final Map<String, List<String>> foundPackages = new HashMap<String, List<String>>();
        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.isOptional()) {
                log.debug("Processing optional dependency " + artifact + "...");
            }
            if (!artifact.getType().equals("jar")) {
                log.warn("Found non JAR artifact " + artifact);
            }
            for (String packageName : packageNames) {
                if (doesJarHavePackageName(artifact.getFile(), packageName, log)) {
                    List<String> trail = new LinkedList<String>(artifact.getDependencyTrail());
                    if (artifact.isOptional()) {
                        trail.add("[optional]");
                    }
                    log.info("Found package " + packageName + " in " + getTrail(trail));
                    foundPackages.put(packageName, trail);
                }
            }
        }
        for (String packageName : packageNames) {
            if (!foundPackages.containsKey(packageName)) {
                log.warn("Couldn't find " + packageName
                        + " in normal project dependencies, will now search optional (and excluded) dependencies");
                for (Artifact artifact : project.getArtifacts()) {
                    if (artifact.isOptional()) {
                        log.debug("Processing optional artifact " + artifact + "...");
                    }
                    DependencyNode dependencyNode = getDependencyNode(getCoords(artifact));
                    if (dependencyNode != null) {
                        List<String> trail = new LinkedList<String>(artifact.getDependencyTrail());
                        dependencyNode.accept(new PackagerFinderDependencyVisitor(packageName, foundPackages, false,
                                trail));
                    }
                }
            }
        }
        return foundPackages;
    }

    private DependencyNode getDependencyNode(String artifactCoords) {
        ArtifactRequest request = new ArtifactRequest();
        DefaultArtifact aetherArtifact = new DefaultArtifact(artifactCoords);
        request.setArtifact(aetherArtifact);
        request.setRepositories(remoteRepos);

        Dependency dependency = new Dependency(aetherArtifact, "compile");

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        collectRequest.setRepositories(remoteRepos);

        DependencyNode dependencyNode = null;
        try {
            dependencyNode = repoSystem.collectDependencies(repoSession, collectRequest).getRoot();
            DependencyRequest dependencyRequest = new DependencyRequest(dependencyNode, null);

            repoSystem.resolveDependencies(repoSession, dependencyRequest);

        } catch (DependencyCollectionException e) {
            log.error("Error collecting dependencies for " + artifactCoords + ": " + e.getMessage());
        } catch (DependencyResolutionException e) {
            log.error("Error resolving dependencies for " + artifactCoords + ": " + e.getMessage());
        }
        return dependencyNode;
    }

    @Override
    public List<String> getDependencyVersion(MavenProject project, String artifactFileName)
            throws MojoExecutionException {
        if (project == null || StringUtils.isEmpty(artifactFileName)) {
            return Collections.emptyList();
        }

        Set<String> versions = new LinkedHashSet<String>();

        Set<Artifact> artifacts = project.getArtifacts();
        for (Artifact artifact : artifacts) {
            if (artifact.getType().equals("war")) {
                // we have a WAR dependency, we will look in that project dependencies seperately since it is not
                // directly transitive.
                versions.addAll(findInWarDependencies(artifact, artifactFileName));
            } else if (artifact.getFile().getName().equals(artifactFileName)) {
                versions.add(artifact.getBaseVersion());
            }
        }

        return new LinkedList<String>(versions);
    }

    @Override
    public File resolveArtifactFile(String coords) throws MojoExecutionException {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(new DefaultArtifact(coords));
        request.setRepositories(remoteRepos);
        org.sonatype.aether.artifact.Artifact artifact = null;
        try {
            artifact = repoSystem.resolveArtifact(repoSession, request).getArtifact();
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        return artifact != null ? artifact.getFile() : null;
    }

    private DependencyNode resolveExclusion(DependencyNode dependencyNode, Exclusion exclusion) {
        if (dependencyNode.getDependency().getArtifact().getGroupId().equals(exclusion.getGroupId())
                && dependencyNode.getDependency().getArtifact().getArtifactId().equals(exclusion.getArtifactId())) {
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

}
