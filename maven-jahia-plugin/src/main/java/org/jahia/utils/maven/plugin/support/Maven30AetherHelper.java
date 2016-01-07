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
package org.jahia.utils.maven.plugin.support;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.jahia.utils.osgi.parsers.ParsingContext;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.graph.Exclusion;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.*;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.selector.AndDependencySelector;
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector;
import org.sonatype.aether.util.graph.selector.OptionalDependencySelector;
import org.sonatype.aether.util.graph.selector.ScopeDependencySelector;

import java.io.File;
import java.util.*;

import static org.jahia.utils.maven.plugin.support.MavenAetherHelperUtils.*;

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
        private Deque<String> loopCheckTrail = null;

        public PackagerFinderDependencyVisitor(String packageName, Map<String, List<String>> foundPackages,
                boolean excludedDependency, List<String> dependencyTrail, Deque<String> loopCheckTrail) {
            this.packageName = packageName;
            this.foundPackages = foundPackages;
            this.excludedDependency = excludedDependency;
            this.dependencyTrail = new LinkedList<String>(dependencyTrail);
            if (loopCheckTrail == null) {
                this.loopCheckTrail = new ArrayDeque<String>();
            } else {
                this.loopCheckTrail = loopCheckTrail;
            }
        }

        @Override
        public boolean visitEnter(DependencyNode node) {
            if (!loopCheckTrail.contains(node.toString())) {
                loopCheckTrail.push(node.toString());
            } else {
                log.warn("Already visited dependency " + node.toString() + "!!!");
                loopCheckTrail.push(node.toString());
                return false;
            }
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
                                new LinkedList<String>(curTrail), loopCheckTrail));
                    }
                }
            }
            return true;
        }

        @Override
        public boolean visitLeave(DependencyNode node) {
            loopCheckTrail.pop();
            return true;
        }
    }

    class PackageCollectorDependencyVisitor implements DependencyVisitor {

        private ArtifactProcessor artifactProcessor;
        private ArtifactHandler artifactHandler;
        private Deque<String> curTrail = null;
        private Deque<Artifact> artifactStack = new ArrayDeque<Artifact>();
        private Deque<ParsingContext> parsingContextStack = new ArrayDeque<ParsingContext>();
        private Deque<String> loopCheckTrail = new ArrayDeque<String>();
        private int depth = 0;

        public PackageCollectorDependencyVisitor(ArtifactProcessor artifactProcessor, ArtifactHandler artifactHandler, List<String> dependencyTrail, ParsingContext rootParsingContext) {
            this.artifactProcessor = artifactProcessor;
            this.artifactHandler = artifactHandler;
            this.curTrail = new ArrayDeque<String>(dependencyTrail);
            this.parsingContextStack.push(rootParsingContext);
        }

        @Override
        public boolean visitEnter(DependencyNode node) {
            depth++;
            Artifact mavenArtifact = new org.apache.maven.artifact.DefaultArtifact(node.getDependency().getArtifact().getGroupId(),
                    node.getDependency().getArtifact().getArtifactId(),
                    node.getDependency().getArtifact().getVersion(),
                    node.getDependency().getScope(),
                    node.getDependency().getArtifact().getExtension(),
                    StringUtils.isBlank(node.getDependency().getArtifact().getClassifier()) ? null : node.getDependency().getArtifact().getClassifier(),
                    artifactHandler);
            boolean visitChildren = true;
            String trailSuffix = "";
            if (node.getDependency().isOptional()) {
                trailSuffix = "[optional]";
            }
            curTrail.push(node.toString() + trailSuffix);
            String trail = getTrailPadding(curTrail);
            if (!loopCheckTrail.contains(node.toString())) {
                loopCheckTrail.push(node.toString());
            } else {
                log.warn("Already visited dependency " + node.toString() + "!!!");
                visitChildren = false;
            }
            // log.debug(trail + "Starting visit of artifact " + node.getDependency().getArtifact() + "...");
            if (node.getDependency().getArtifact().getFile() == null) {
                log.warn(trail + "No local file for artifact " + node.getDependency().getArtifact());
                log.info(trail + "Resolving artifact " + node.getDependency().getArtifact() + "...");
                ArtifactRequest request = new ArtifactRequest();
                request.setArtifact(node.getDependency().getArtifact());
                request.setRepositories(remoteRepos);
                try {
                    ArtifactResult artifactResult = repoSystem.resolveArtifact(moreDependenciesSession, request);
                    node.getDependency().getArtifact().setFile(artifactResult.getArtifact().getFile());
                } catch (ArtifactResolutionException e) {
                    log.warn(trail + "Error resolving artifact " + node.getDependency().getArtifact() + ": " + e.getMessage());
                    visitChildren = false;
                }
            }
            mavenArtifact.setFile(node.getDependency().getArtifact().getFile());
            mavenArtifact.setOptional(node.getDependency().isOptional());
            artifactStack.push(mavenArtifact);
            ParsingContext parsingContext = null;
            ParsingContext parentParsingContext = null;
            if (parsingContextStack.size() > 0) {
                parentParsingContext = parsingContextStack.peek();
            }
            if (mavenArtifact.getFile() != null) {
                boolean external = artifactProcessor.isExternal(mavenArtifact);
                try {
                    parsingContext = artifactProcessor.enterArtifact(mavenArtifact, node.getDependency().isOptional(), external, parentParsingContext, trail, depth);
                } catch (MojoExecutionException e) {
                    e.printStackTrace();
                    visitChildren = false;
                }
                parsingContextStack.push(parsingContext);
            }
            return visitChildren;
        }

        @Override
        public boolean visitLeave(DependencyNode node) {
            depth--;
            boolean visitSiblings = true;
            String trail = getTrailPadding(curTrail);
            // log.debug(trail + "Ending visit of artifact " + node.getDependency().getArtifact() + "...");
            Artifact mavenArtifact = null;
            if (node.getDependency().getArtifact().getArtifactId().equals(artifactStack.peek().getArtifactId())) {
                mavenArtifact = artifactStack.pop();
            } else {
                log.warn(trail + "Expected artifact " + node.getDependency().getArtifact().getArtifactId() + " on artifact stack but got " + artifactStack.peek().getArtifactId());
            }
            if (mavenArtifact != null && mavenArtifact.getFile() != null) {
                try {
                    boolean external = false;
                    if (Artifact.SCOPE_PROVIDED.equals(node.getDependency().getScope())) {
                        external = true;
                    }
                    artifactProcessor.exitArtifact(mavenArtifact, node.getDependency().isOptional(), external, trail, parsingContextStack.peek(), depth);
                } catch (MojoExecutionException e) {
                    e.printStackTrace();
                }
                parsingContextStack.pop();
            }
            curTrail.pop();
            loopCheckTrail.pop();
            return visitSiblings;
        }
    }

    private Log log;

    private List<RemoteRepository> remoteRepos;

    // private RepositorySystemSession repoSession;

    private RepositorySystem repoSystem;

    private Map<String, DependencyNode> resolvedDependencyNodes = new HashMap<String, DependencyNode>();

    DefaultRepositorySystemSession moreDependenciesSession;

    public Maven30AetherHelper(RepositorySystem repoSystem, RepositorySystemSession repoSession,
            List<RemoteRepository> remoteRepos, Log log) {
        this.repoSystem = repoSystem;
        // this.repoSession = repoSession;
        // we build our own custom session to re-introduce the collection of "provided" dependencies that are excluded by the default Maven session.
        this.moreDependenciesSession = new DefaultRepositorySystemSession(repoSession);
        AndDependencySelector andDependencySelector = new AndDependencySelector(new ScopeDependencySelector("test"), new OptionalDependencySelector(), new ExclusionDependencySelector());
        this.moreDependenciesSession.setDependencySelector(andDependencySelector);
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

                node = repoSystem.collectDependencies(moreDependenciesSession, collectRequest).getRoot();

                DependencyRequest dependencyRequest = new DependencyRequest(node, null);

                repoSystem.resolveDependencies(moreDependenciesSession, dependencyRequest);

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
                                trail, null));
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
            dependencyNode = repoSystem.collectDependencies(moreDependenciesSession, collectRequest).getRoot();
            DependencyRequest dependencyRequest = new DependencyRequest(dependencyNode, null);

            DependencyResult dependencyResult = repoSystem.resolveDependencies(moreDependenciesSession, dependencyRequest);

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
            artifact = repoSystem.resolveArtifact(moreDependenciesSession, request).getArtifact();
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        return artifact != null ? artifact.getFile() : null;
    }

    @Override
    public void processArtifactAndDependencies(Artifact artifact, boolean optional, ArtifactProcessor artifactProcessor, ArtifactHandler artifactHandler, ParsingContext rootParsingContext) {
        DependencyNode dependencyNode = getDependencyNode(getCoords(artifact));
        if (dependencyNode != null) {
            List<String> trail = new LinkedList<String>(artifact.getDependencyTrail());
            dependencyNode.setScope(artifact.getScope()); // copy the scope from the artifact for the root node.
            dependencyNode.accept(new PackageCollectorDependencyVisitor(artifactProcessor, artifactHandler, trail, rootParsingContext));
        }
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
