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
package org.jahia.utils.maven.plugin;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.SelectorUtils;
import org.jahia.configuration.deployers.ServerDeploymentInterface;
import org.jahia.configuration.modules.ModuleDeployer;
import org.jahia.utils.maven.plugin.support.AetherHelper;
import org.jahia.utils.maven.plugin.support.AetherHelperFactory;

import java.io.*;
import java.net.ConnectException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Jahia server deployment mojo.
 *
 * @author Serge Huber
 * @goal deploy
 * @requiresDependencyResolution runtime
 */
public class DeployMojo extends AbstractManagementMojo {

    private static final Set<String> JAHIA_SYSTEM_BUNDLES = new HashSet<String>(Arrays.asList(
            "org.jahia.bundles.url.jahiawar", "org.jahia.bundles.extender.jahiamodules",
            "org.jahia.bundles.blueprint.extender.config", "org.jahia.bundles.http.bridge",
            "org.jahia.bundles.webconsole.config", "org.jahia.bundles.jspapiusage.repackaging",
            "org.jahia.bundles.configadmin.persistence",
            "org.jahia.bundles.clustering", "org.jahia.bundles.clustering.enabler", "org.jahia.bundles.hazelcast.discovery",
            "org.jahia.bundles.spring.bridge"));

    private static final Set<String> JAHIA_PACKAGE_PROJECTS = new HashSet<String>(
            Arrays.asList("jahia-data", "jahia-core-modules",
                          "jahia-additional-modules", "jahia-ee-data",
                          "jahia-ee-core-modules", "jahia-ee-additional-modules"));

    /**
     * The dependency tree builder to use.
     *
     * @component
     * @required
     * @readonly
     */
    private DependencyTreeBuilder dependencyTreeBuilder;

    /**
     * The artifact metadata source to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * The artifact collector to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactCollector artifactCollector;

    /**
     * @parameter expression="${jahia.debug.address}" default-value="socket:hostname=localhost,port=8000"
     */
    private String address;

    /**
     * @parameter expression="${jahia.deploy.deployTests}" default-value="false"
     */
    private boolean deployTests;

    /**
     * The main directory for the target server install in which we will deploy the app-specific configuration.
     *
     * @parameter expression="${jahia.deploy.targetContainerName}"
     */
    protected String targetContainerName;

    private AetherHelper aetherHelper;

    /**
     * @component
     * @required
     * @readonly
     */
    private PlexusContainer container;

    /**
     * The current build session instance.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    private MavenSession mavenSession;
    private DockerClient dockerClient;

    public void doExecute() throws MojoExecutionException, MojoFailureException {
        try {
            if (targetServerDirectory != null) {
                deployProject();
            } else if (targetContainerName != null) {
                getLog().info("Deploying module to docker container " + targetContainerName);
                initDockerClient();
                if (project.getPackaging().equals("bundle")) {
                    boolean isStandardModule = project.getGroupId().equals("org.jahia.module") || project.getGroupId().endsWith(".jahia.modules");
                    String filename = project.getArtifactId() + "-" + project.getVersion() + "." + "jar";
                    File source = new File(output, filename);
                    String dockerDatadir = "/var/jahia";
                    String[] env = dockerClient.inspectContainerCmd(targetContainerName).exec().getConfig().getEnv();
                    for (String s : env) {
                        if(s.startsWith("DATA_FOLDER=")) {
                            getLog().info("Found DATA_FOLDER env variable in container " + targetContainerName + ", using it for deployment: " + s);
                            dockerDatadir = s.split("=")[1];
                        }
                    }
                    if (isStandardModule || isJahiaModuleBundle(new File(output, filename))) {
                        copyFileToContainer(filename, source, dockerDatadir + "/modules");
                    } else {
                        copyFileToContainer(filename, source, dockerDatadir + "/karaf/deploy");
                    }
                } else {
                    throw new MojoFailureException("Only bundle can be deployed to a target container, either a Jahia module or an OSGI bundle");
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error when deploying", e);
        }
    }

    public void doValidate() throws MojoExecutionException, MojoFailureException {
        if (targetContainerName != null) {
            initDockerClient();
            dockerClient.pingCmd().exec();
            if (dockerClient.listContainersCmd().exec().stream().noneMatch(container1 -> Arrays.stream(container1.getNames()).anyMatch(s -> s.endsWith(targetContainerName)))) {
                throw new MojoFailureException(MessageFormat.format("Your docker container {0} is not running", targetContainerName));
            }
            return;
        }
        ServerDeploymentInterface serverDeployer = null;
        try {
            serverDeployer = getDeployer();
        } catch (Exception e) {
            throw new MojoExecutionException("Error while validating deployers", e);
        }
        if (serverDeployer == null) {
            throw new MojoFailureException("Server " + targetServerType + " v" + targetServerVersion + " not (yet) supported by this plugin.");
        }

        if (!getDeployer().validateInstallationDirectory()) {
            throw new MojoFailureException("Directory " + targetServerDirectory + " is not a valid installation directory for server " + getDeployer().getName());
        }
    }

    private void deployProject() throws Exception {
        if (skipDeploy()) {
            getLog().info("jahia.deploy.skip is set to 'true' for the current project. Skip deploying it.");
            return;
        }
        if (project.getPackaging().equals("ear")) {
            deployEarProject();
        } else if (project.getPackaging().equals("war")) {
            if (project.getGroupId().equals("org.jahia.server") || project.getGroupId().equals("org.jahia.extensions")) {
                deployWarProject();
            } else if (project.getGroupId().equals("org.jahia.modules")
                    || (deployTests && project.getGroupId().equals("org.jahia.test"))
                    || project.getGroupId().endsWith(".jahia.modules")) {
                deployModuleProject();
            } else if (project.getGroupId().equals("org.jahia.test") && !deployTests) {
                getLog().info(
                        "Deployment of test projects "
                                + "(with groupId 'org.jahia.test') is disabled by default. "
                                + "You can enable it by specifying -Djahia.deploy.deployTests"
                                + " option for the 'mvn jahia:deploy' command");
            } else {
                getLog().warn("Unrecognized type of the WAR project. Skipping deployment");
            }
        } else if (project.getPackaging().equals("jar")) {
            if (project.getGroupId().equals("org.jahia.test") && !deployTests) {
                getLog().info(
                        "Deployment of test projects "
                                + "(with groupId 'org.jahia.test') is disabled by default. "
                                + "You can enable it by specifying -Djahia.deploy.deployTests"
                                + " option for the 'mvn jahia:deploy' command");
            } else {
                deployJarProject();
            }
        } else if (project.getPackaging().equals("pom")) {
            deployPomProject();
        } else if (project.getPackaging().equals("bundle")) {
            boolean isJahiaTaglib = project.getArtifactId().equals("jahia-taglib");
            if (isJahiaTaglib) {
                deployJarProject();
            }
            if (project.getGroupId().equals("org.jahia.bundles")
                    && JAHIA_SYSTEM_BUNDLES.contains(project.getArtifactId()) || isJahiaTaglib) {
                String fileName = project.getArtifactId() + "-" + project.getVersion() + "." + "jar";
                File srcFile = new File(output, fileName);
                File destDir = null;
                File karafDir = new File(getWebappDeploymentDir(), "WEB-INF/karaf/system");
                boolean karafDeployment = karafDir.isDirectory();
                if (karafDeployment) {
                    destDir = new File(new File(karafDir, StringUtils.join(StringUtils.split(project.getGroupId(), '.'), File.separatorChar)), project.getArtifactId() + File.separatorChar + project.getVersion());
                } else {
                    destDir = new File(getWebappDeploymentDir(), "WEB-INF/bundles");
                }
                FileUtils.copyFileToDirectory(srcFile, destDir);
                getLog().info("Copied " + srcFile + " to " + destDir);
                if (karafDeployment) {
                    copyToKarafDeploy(srcFile);
                } else {
                    removeCachedBundle(fileName, new File(getDataDir(), "bundles-deployed"));
                }
            } else {
                boolean isStandardModule = project.getGroupId().equals("org.jahia.module") || project.getGroupId().endsWith(".jahia.modules");
                if (isStandardModule || isJahiaModuleBundle(new File(output, project.getArtifactId() + "-" + project.getVersion() + "." + "jar"))) {
                    deployModuleProject();
                } else {
                    File srcFile = new File(output, project.getArtifactId() + "-" + project.getVersion() + "." + "jar");
                    File destDir = null;
                    File karafDir = new File(getWebappDeploymentDir(), "WEB-INF/karaf/system");
                    boolean karafDeployment = karafDir.isDirectory();
                    if (karafDeployment) {
                        destDir = new File(getDataDir(), "karaf/deploy");
                    } else {
                        destDir = new File(getDataDir(), "bundles");
                    }
                    FileUtils.copyFileToDirectory(srcFile, destDir);
                    getLog().info("Copied " + srcFile + " to " + destDir);
                }
            }
        }
    }

    private void copyToKarafDeploy(File srcFile) throws IOException {
        File deployDir = new File(getDataDir(), "karaf/deploy");
        getLog().info(
                "Copying file " + srcFile.getCanonicalPath() + " into folder " + deployDir.getCanonicalPath());
        FileUtils.copyFileToDirectory(srcFile, deployDir, false);
        getLog().info("...done");
    }

    private void removeCachedBundle(String fileName, File deployedBundeDir) {
        if (!deployedBundeDir.isDirectory()) {
            return;
        }

        for (File info : FileUtils.listFiles(deployedBundeDir, new NameFileFilter("bundle.info"),
                                             TrueFileFilter.INSTANCE)) {
            try {
                if (FileUtils.readFileToString(info).contains(fileName)) {
                    try {
                        FileUtils.deleteDirectory(info.getParentFile());
                        getLog().info("Deleted deployed bundle in folder " + info.getParentFile());
                    } catch (IOException e) {
                        getLog().warn(
                                "Unable to deleted deployed bundle in folder " + info.getParentFile() + ". Cause: "
                                        + e.getMessage());
                    }
                    break;
                }
            } catch (IOException e) {
                getLog().warn(e.getMessage());
            }
        }
    }

    private boolean isJahiaModuleBundle(File file) {
        if (!file.exists()) {
            return false;
        }

        // check the manifest
        JarFile jar = null;
        try {
            jar = new JarFile(file, false);
            return jar.getManifest().getMainAttributes().containsKey(new Attributes.Name("Jahia-Module-Type"));
        } catch (IOException e) {
            getLog().error(e);
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException e) {
                    getLog().warn(e);
                }
            }
        }

        return false;
    }

    private boolean skipDeploy() {
        return Boolean.valueOf(project.getProperties().getProperty(
                "jahia.deploy.skip", "false"));
    }

    /**
     * Deploy all EAR dependencies ( WARs / SARs / RARs / shared resources ) to application server
     *
     * @throws Exception
     */
    private void deployEarProject() throws Exception {
        if (getDeployer().isEarDeployment()) {
            getLog().info(
                    "Deploying application server specific files for " + getDeployer().getName() + " in directory "
                            + targetServerDirectory);
            File targetEarFolder = getDeployer().getDeploymentFilePath("digitalfactory", "ear");
            getLog().info("Updating EAR resources in " + targetEarFolder);
            int updateFileCount = updateFiles(new File(baseDir, "src/main/resources"), targetEarFolder);
            getLog().info("Updated " + updateFileCount + " resources");
            if ("jboss".equals(targetServerType)) {
                // for JBoss create deployment marker file
                FileUtils.touch(new File(targetEarFolder.getParentFile(), targetEarFolder.getName() + ".dodeploy"));
            }
        }
    }

    /**
     * Copy output folder of WAR / SAR / RAR project to application server
     *
     * @throws Exception
     */
    private void deployWarProject() throws Exception {
        File webappDir = getWebappDeploymentDir();
        getLog().info(
                "Update " + project.getPackaging() + " resources for " + getDeployer().getName()
                        + " in directory " + webappDir);
        if ("was".equals(targetServerType)) {
            File source = new File(output, project.getBuild().getFinalName() + ".war");
            File target = new File(webappDir + "/" + project.getBuild().getFinalName() + ".war");

            try {
                FileUtils.copyFile(source, target);
            } catch (IOException e) {
                getLog().error("Error while deploying WAR project", e);
            }
        } else {
            try {
                ZipUnArchiver unarch = new ZipUnArchiver(new File(output, project.getBuild().getFinalName() + ".war"));
                unarch.enableLogging(getLog().isDebugEnabled() ? new ConsoleLogger(Logger.LEVEL_DEBUG, "console")
                                             : new ConsoleLogger());
                if (!webappDir.exists() && !webappDir.mkdirs()) {
                    throw new IOException("Unable to create target WAR directory " + webappDir);
                }
                unarch.setDestDirectory(webappDir);
                String warExcludes = getDeployer().getWarExcludes();
                String[] excludes = warExcludes != null ? StringUtils.split(warExcludes, " ,") : null;
                if (excludes != null && excludes.length > 0) {
                    IncludeExcludeFileSelector selector = new IncludeExcludeFileSelector();
                    selector.setExcludes(excludes);
                    unarch.setFileSelectors(new FileSelector[]{selector});
                }
                unarch.extract();
                getLog().info("...done.");
            } catch (ArchiverException e) {
                getLog().error("Error while deploying WAR project", e);
            }
        }

        if (project.getArtifactId().equals("jahia-war") || project.getArtifactId().equals("jahia-ee-war")) {
            File dataPackage = getAetherHelper().resolveArtifactFile(project.getGroupId() + ":" + project.getArtifactId() + ":zip:data-package:" + project.getArtifact().getVersion());
            if (dataPackage != null) {
                deployPackageFile(dataPackage);
            }
        }
    }

    /**
     * Copy template resources from output folder to the jsp/templates and WEB-INF/classes of jahia
     *
     * @throws Exception
     */
    private void deployModuleProject() throws Exception {
        File source = new File(output, project.getArtifactId() + "-" + project.getVersion() + "."
                + (project.getPackaging().equals("bundle") ? "jar" : project.getPackaging()));
        File target = new File(getDataDir(), "modules");
        getLog().info("Deploying module " + source + " into " + target);

        new ModuleDeployer(target, new MojoLogger(getLog())).deployModule(source);

        getLog().info("...done");
    }

    private void deployModuleAsDependency(Artifact module) throws Exception {
        File target = new File(getDataDir(), "modules");
        getLog().info(
                "Deploying module "
                        + module.getArtifactId() + " into " + target + "...");
        try {
            File file = resolveArtifactFile(module);
            new ModuleDeployer(target, new MojoLogger(getLog())).deployModule(file);
        } catch (Exception e) {
            getLog().error(e);
        }
        getLog().info("...done");
    }

    private void deployPrepackagedSiteProject() throws Exception {
        if (project.getAttachedArtifacts().size() > 0) {
            Artifact artifact = (Artifact) project.getAttachedArtifacts().get(project.getAttachedArtifacts().size() - 1);
            try {
                artifactResolver.resolve(artifact, project.getRemoteArtifactRepositories(), localRepository);
                File libDir = new File(getDataDir(), "prepackagedSites");
                libDir.mkdirs();
                File file = new File(libDir, artifact.getArtifactId() + ".zip");
                getLog().info("Deploying prepackaged site " + artifact.getFile().getName() + " to " + file);
                FileUtils.copyFile(artifact.getFile(), file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void deployPrepackagedSiteAsDependency(Artifact artifact)
            throws Exception {
        File target = new File(getDataDir(), "prepackagedSites");
        getLog().info(
                "Deploying prepackaged site " + artifact.getArtifactId() + " into "
                        + target + "...");
        try {
            File file = resolveArtifactFile(artifact);
            FileUtils.copyFile(file, new File(target, artifact.getArtifactId()
                    + ".zip"));
        } catch (Exception e) {
            getLog().error(e);
        }
        getLog().info("...done");
    }

    /**
     * Copy JAR file to jahia webapp, tries to hotswap classes
     *
     * @throws Exception
     */
    private void deployJarProject() {
        Artifact artifact = project.getArtifact();
        try {
            artifactResolver.resolve(artifact, project.getRemoteArtifactRepositories(), localRepository);
            File libDir = new File(new File(getWebappDeploymentDir(), "WEB-INF"), "lib");
            getLog().info("Deploying jar file " + artifact.getFile().getName() + " to " + libDir);
            File deployedJar = new File(libDir, artifact.getFile().getName());
            boolean needHotSwap = deployedJar.exists();
            FileUtils.copyFileToDirectory(artifact.getFile(), libDir);
            if (needHotSwap) {
                hotSwap(deployedJar);
            }
        } catch (Exception e) {
            getLog().error("Error while deploying JAR project", e);
        }
    }

    /**
     * Look if the pom depends on a jahia-ear project and deploys it
     *
     * @throws Exception
     */
    private void deployPomProject() {
        try {
            boolean isJahiaServerGroup = project.getGroupId().equals("org.jahia.server");
            if (isJahiaServerGroup && JAHIA_PACKAGE_PROJECTS.contains(project.getArtifactId())) {
                deployPackage();
            } else if ((project.getParent() != null) && ("prepackagedSites".equals(project.getParent().getArtifactId()))
                    || project.getGroupId().equals("org.jahia.prepackagedsites")) {
                deployPrepackagedSiteProject();
            } else {
                boolean jdbcDrivers = isJahiaServerGroup && project.getArtifactId().startsWith("jdbc-drivers");
                boolean sharedLibraries = isJahiaServerGroup && project.getArtifactId().equals("shared-libraries");
                DependencyNode rootNode = getRootDependencyNode();
                List<?> l = rootNode.getChildren();
                for (Iterator<?> iterator = l.iterator(); iterator.hasNext(); ) {
                    DependencyNode dependencyNode = (DependencyNode) iterator.next();
                    Artifact artifact = dependencyNode.getArtifact();
                    if (artifact.getGroupId().equals("org.jahia.server")) {
                        String artifactId = artifact.getArtifactId();
                        if ((artifactId.equals("jahia-war")
                                || artifactId.equals("jahia-ee-war")
                                || artifactId.equals("jahia-gwt") && artifact.getType().equals("war")) && !StringUtils.equals(artifact.getClassifier(), "data-package")) {
                            deployWarDependency(dependencyNode);
                        } else if (artifactId.equals("shared-libraries")
                                || artifactId.startsWith("jdbc-drivers")) {
                            deploySharedLibraries(dependencyNode);
                        } else if (JAHIA_PACKAGE_PROJECTS.contains(artifactId) || (artifactId.equals("jahia-war")
                                || artifactId.equals("jahia-ee-war")) && StringUtils.equals(artifact.getClassifier(), "data-package")) {
                            deployPackageFromDepenendency(artifact);
                        }
                    } else if (artifact.getGroupId()
                                       .equals("org.jahia.modules")
                            || (deployTests && artifact.getGroupId().equals(
                            "org.jahia.test"))
                            || artifact.getGroupId().endsWith(".jahia.modules")) {
                        deployModuleAsDependency(artifact);
                    } else if (artifact.getGroupId().equals("org.jahia.prepackagedsites")) {
                        deployPrepackagedSiteAsDependency(artifact);
                    }
                    if (sharedLibraries) {
                        deploySharedLibrary(artifact);
                    } else if (jdbcDrivers) {
                        deployJdbcDriver(artifact);
                    }
                }
            }
        } catch (Exception e) {
            getLog().error("Error while deploying POM project", e);
        }
    }


    private void deployPackage() {
        getLog().info(
                "Deploying Digital Experience Manager data package "
                        + project.getArtifactId() + "...");
        try {
            File file = null;
            getLog().info("Resolving artifact file...");

            if (project.getAttachedArtifacts().size() > 0) {
                Artifact artifact = (Artifact) project.getAttachedArtifacts()
                                                      .get(project.getAttachedArtifacts().size() - 1);
                artifactResolver.resolve(artifact,
                                         project.getRemoteArtifactRepositories(),
                                         localRepository);
                file = artifact.getFile();
            } else {
                file = getAetherHelper().resolveArtifactFile(project.getGroupId() + ":" + project.getArtifactId()
                                                                     + ":zip:package:"
                                                                     + project.getArtifact().getVersion());
            }
            getLog().info("...resolved to: " + file);

            deployPackageFile(file);
        } catch (Exception e) {
            getLog().error(e);
        }
    }

    private void deployPackageFromDepenendency(Artifact dependency) {
        getLog().info(
                "Deploying Digital Experience Manager data package "
                        + dependency.getArtifactId() + "...");
        try {
            File file = resolveArtifactFile(dependency);

            deployPackageFile(file);
        } catch (Exception e) {
            getLog().error(e);
        }
    }

    private File resolveArtifactFile(Artifact artifact) throws MojoExecutionException {
        File file = null;
        getLog().info("Resolving artifact file...");
        file = getAetherHelper().resolveArtifactFile(artifact.getGroupId()
                                                             + ":"
                                                             + artifact.getArtifactId()
                                                             + ":"
                                                             + artifact.getType()
                                                             + ":"
                                                             + (StringUtils.isNotEmpty(artifact.getClassifier()) ? (artifact
                .getClassifier() + ":") : "")
                                                             + artifact.getVersion());
        getLog().info("...resolved to: " + file);
        return file;
    }

    private void deployPackageFile(File packageFile) {
        getLog().info(
                "Deploying Digital Experience Manager data package file "
                        + packageFile + "...");
        try {
            getLog().info("Extracting content to data directory " + getDataDir() + "...");
            ZipUnArchiver unarch = new ZipUnArchiver(packageFile);
            unarch.enableLogging(getLog().isDebugEnabled() ? new ConsoleLogger(
                    Logger.LEVEL_DEBUG, "console") : new ConsoleLogger());
            unarch.setDestDirectory(getDataDir());
            unarch.extract();
            getLog().info("...done.");
        } catch (Exception e) {
            getLog().error(e);
        }
    }

    protected DependencyNode getRootDependencyNode() throws DependencyTreeBuilderException {
        return dependencyTreeBuilder.buildDependencyTree(project, localRepository, artifactFactory,
                                                         artifactMetadataSource, null, artifactCollector);
    }


    private void deploySharedLibraries(DependencyNode dependencyNode)
            throws IOException, ArtifactResolutionException,
                   ArtifactNotFoundException {
        boolean jdbcDrivers = dependencyNode.getArtifact().getArtifactId().startsWith("jdbc-drivers");
        for (DependencyNode node : ((List<DependencyNode>) dependencyNode.getChildren())) {
            Artifact artifact = node.getArtifact();

            artifactResolver.resolve(artifact,
                                     project.getRemoteArtifactRepositories(), localRepository);
            try {
                if (jdbcDrivers) {
                    deployJdbcDriver(artifact);
                } else {
                    deploySharedLibrary(artifact);
                }
            } catch (Exception e) {
                getLog().error("Error while deploying EAR dependency", e);
            }

        }
    }

    private void deploySharedLibrary(Artifact artifact) throws IOException {
        getLog().info("Copy shared resource " + artifact.getFile().getName());
        getDeployer().deploySharedLibraries(artifact.getFile());
    }

    private void deployJdbcDriver(Artifact artifact) throws IOException {
        getLog().info("Deploying JDBC driver " + artifact.getFile().getName());
        getDeployer().deployJdbcDriver(artifact.getFile());
    }

    /**
     * Deploy WAR artifact to application server
     *
     * @param dependencyNode
     */
    protected void deployWarDependency(DependencyNode dependencyNode) throws Exception {
        Artifact artifact = dependencyNode.getArtifact();
        File webappDir = getWebappDeploymentDir();

        getLog().info(
                "Deploying artifact " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
                        + artifact.getVersion() + "(file: " + artifact.getFile() + ")");
        getLog().info("Updating " + artifact.getType() +
                              " resources for " + getDeployer().getName() +
                              " in directory " + webappDir);

        String[] excludes = getDeployer().getWarExcludes() != null ? StringUtils.split(getDeployer().getWarExcludes(), ",") : null;

        // if we are dealing with WAR overlay, we want to overwrite the target
        boolean overwrite = StringUtils.isNotEmpty(artifact.getClassifier());

        try {
            ZipInputStream z = new ZipInputStream(
                    new FileInputStream(artifact.getFile()));
            ZipEntry entry;
            int cnt = 0;
            while ((entry = z.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    if (excludes != null) {
                        boolean doExclude = false;
                        for (String excludePattern : excludes) {
                            if (SelectorUtils.matchPath(excludePattern, entry.getName())) {
                                doExclude = true;
                                break;
                            }
                        }
                        if (doExclude) {
                            continue;
                        }
                    }
                    File target = new File(webappDir, entry
                            .getName());
                    if (overwrite || entry.getTime() > target.lastModified()) {

                        target.getParentFile().mkdirs();
                        FileOutputStream fileOutputStream = new FileOutputStream(
                                target);
                        IOUtils.copy(z, fileOutputStream);
                        fileOutputStream.close();
                        cnt++;
                    }
                } else {
                    //in the case of empty folders create anyway
                    (new File(webappDir, entry.getName())).mkdir();
                }
            }
            z.close();
            getLog().info("Copied " + cnt + " files.");
        } catch (IOException e) {
            getLog().error("Error while deploying dependency", e);
        }
    }

    // *************** Hotswap

    private void hotSwap(File deployedJar) {
        int colonIndex = address.indexOf(':');
        String connectorName = address.substring(0, colonIndex);
        if (connectorName.equals("socket")) connectorName = "com.sun.jdi.SocketAttach";
        else if (connectorName.equals("shmem")) connectorName = "com.sun.jdi.SharedMemoryAttach";
        String argumentsString = address.substring(colonIndex + 1);

        AttachingConnector connector = (AttachingConnector) findConnector(connectorName);
        Map<String, Argument> arguments = connector.defaultArguments();

        StringTokenizer st = new StringTokenizer(argumentsString, ",");
        while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            int index = pair.indexOf('=');
            String name = pair.substring(0, index);
            String value = pair.substring(index + 1);
            Connector.Argument argument = (Connector.Argument) arguments.get(name);
            if (argument != null) {
                argument.setValue(value);
            }
        }

        Map<String, Long> dates = new HashMap<String, Long>();
        try {
            ZipInputStream z = new ZipInputStream(new FileInputStream(deployedJar));
            ZipEntry entry;
            while ((entry = z.getNextEntry()) != null) {
                dates.put(entry.getName(), entry.getTime());
            }
            z.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        VirtualMachine vm = null;
        try {
            vm = connector.attach(arguments);
            getLog().info("Connected to " + vm.name() + " " + vm.version());

            Map<String, File> files = new HashMap<String, File>();

            parse(new File(output, "classes"), dates, "", files);
            getLog().debug("Classes : " + files.keySet());
            if (!files.isEmpty()) {
                reload(vm, files);
            }
        } catch (ConnectException e) {
            getLog().warn("Cannot hotswap classes : " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalConnectorArgumentsException e) {
            e.printStackTrace();
        } finally {
            if (vm != null) {
                vm.dispose();
            }
        }
    }

    private Connector findConnector(String name) {
        List<?> connectors = Bootstrap.virtualMachineManager().allConnectors();
        Iterator<?> iter = connectors.iterator();
        while (iter.hasNext()) {
            Connector connector = (Connector) iter.next();
            if (connector.name().equals(name)) {
                return connector;
            }
        }
        return null;
    }

    private void parse(File folder, Map<String, Long> dates, String base, Map<String, File> result) {
        File[] files = folder.listFiles();
        for (File file : files) {
            String filename = file.getName();
            if (file.isDirectory()) {
                parse(file, dates, base + filename + ".", result);
            } else if (filename.endsWith(".class")) {
                String name = base + filename.substring(0, filename.lastIndexOf("."));
                String classFileName = name.replace(".", "/") + ".class";

                if (dates.containsKey(classFileName)) {
                    long l = dates.get(classFileName);
                    if (file.lastModified() > l) {
                        result.put(name, file);
                        getLog().debug("Updated class : " + file);
                    }
                }
            }
        }
    }

    public void reload(VirtualMachine vm, Map<String, File> classFiles) {
        Map<ReferenceType, byte[]> map = new HashMap<ReferenceType, byte[]>();

        for (String className : classFiles.keySet()) {
            List<?> classes = vm.classesByName(className);
            if (classes.size() != 1) {
                continue;
            }

            ReferenceType refType = (ReferenceType) classes.get(0);

            File f = classFiles.get(className);
            byte[] bytes = new byte[(int) f.length()];
            try {
                InputStream in = new FileInputStream(f);
                in.read(bytes);
                in.close();
            } catch (Exception e) {
                getLog().error("Error reading file " + f, e);
                continue;
            }
            map.put(refType, bytes);
        }


        try {
            vm.redefineClasses(map);
            getLog().info("Reloaded " + map.size() + " classes.");
        } catch (Exception e) {
            getLog().warn("Cannot reload classes : " + e.getMessage());
        }
    }

    public AetherHelper getAetherHelper() throws MojoExecutionException {
        if (aetherHelper == null) {
            aetherHelper = AetherHelperFactory.create(container, project, mavenSession, getLog());
        }
        return aetherHelper;
    }


    private DockerClient initDockerClient() throws MojoExecutionException {
        if (dockerClient == null) {
            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .maxConnections(100)
                    .build();
            dockerClient = DockerClientImpl.getInstance(config, httpClient);
            getLog().info("Docker client connected: " + dockerClient);
        }
        if (dockerClient == null) {
            throw new MojoExecutionException("Impossible to initialize docker client");
        }
        return dockerClient;
    }

    private void copyFileToContainer(String filename, File source, String remotePath) throws IOException {
        getLog().info("Copying " + filename + " to " + remotePath);
        File tempFile = File.createTempFile(project.getArtifactId(), ".tar");
        try (TarArchiveOutputStream tarArchiveOutputStream =
                     new TarArchiveOutputStream(new FileOutputStream(tempFile))) {
            ArchiveEntry archiveEntry = tarArchiveOutputStream.createArchiveEntry(source, filename);
            tarArchiveOutputStream.putArchiveEntry(archiveEntry);
            try (InputStream i = Files.newInputStream(source.toPath())) {
                IOUtils.copy(i, tarArchiveOutputStream);
            }
            tarArchiveOutputStream.closeArchiveEntry();
            tarArchiveOutputStream.finish();
        }
        dockerClient.copyArchiveToContainerCmd(targetContainerName).withRemotePath(remotePath).withTarInputStream(new FileInputStream(tempFile)).withNoOverwriteDirNonDir(false).exec();
        Files.delete(tempFile.toPath());
    }
}
