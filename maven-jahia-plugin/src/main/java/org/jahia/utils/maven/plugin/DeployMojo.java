/**
 * 
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2012 Jahia Limited. All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * 
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 * 
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Limited. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */

package org.jahia.utils.maven.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.codehaus.plexus.util.SelectorUtils;
import org.jahia.configuration.deployers.ServerDeploymentFactory;
import org.jahia.configuration.deployers.ServerDeploymentInterface;
import org.jahia.configuration.modules.ModuleDeployer;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

/**
 * Jahia server deployment mojo.
 * @author Serge Huber
 * @goal deploy
 * @requiresDependencyResolution runtime
 */
public class DeployMojo extends AbstractManagementMojo {
    
    private static final Set<String> JAHIA_SYSTEM_BUNDLES = new HashSet<String>(Arrays.asList(
            "org.jahia.bundles.url.jahiawar", "org.jahia.bundles.extender.jahiamodules",
            "org.jahia.bundles.blueprint.extender.config", "org.jahia.bundles.http.bridge",
            "org.jahia.bundles.webconsole.config"));

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

    protected ServerDeploymentInterface serverDeployer;

    /**
     * @parameter expression="${jahia.deploy.deployTests}" default-value="false"
     */
    private boolean deployTests;

    public void doExecute() throws MojoExecutionException, MojoFailureException {
        try {
            if (targetServerDirectory != null) {
                deployProject();
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error when deploying",e);
        }
    }

    public void doValidate() throws MojoExecutionException, MojoFailureException {
        try {
            serverDeployer = ServerDeploymentFactory.getInstance().getImplementation(targetServerType, targetServerVersion);
        } catch (Exception e) {
            throw new MojoExecutionException("Error while validating deployers", e);
        }
        if (serverDeployer == null) {
            throw new MojoFailureException("Server " + targetServerType + " v" + targetServerVersion + " not (yet) supported by this plugin.");
        }

        if (! serverDeployer.validateInstallationDirectory(targetServerDirectory)) {
            throw new MojoFailureException("Directory " + targetServerDirectory + " is not a valid installation directory for server " + targetServerType + " v" + targetServerVersion);
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
                    || project.getGroupId().equals("org.jahia.templates")
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
        } else if (project.getPackaging().equals("sar") || project.getPackaging().equals("jboss-sar") || project.getPackaging().equals("rar")) {
            deploySarRarProject();
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
                File destDir = new File(getWebappDeploymentDir(), "WEB-INF/bundles");
                FileUtils.copyFileToDirectory(srcFile, destDir);
                getLog().info("Copied " + srcFile + " to " + destDir);
                removeCachedBundle(fileName, new File(getWebappDeploymentDir(), "WEB-INF/var/bundles-deployed"));
            } else {
                boolean isStandardModule = project.getGroupId().equals("org.jahia.module") || project.getGroupId().endsWith(".jahia.modules");
                if (isStandardModule || isJahiaModuleBundle(new File(output, project.getArtifactId() + "-" + project.getVersion() + "." + "jar"))) {
                    deployModuleProject();
                } else {
                    File srcFile = new File(output, project.getArtifactId() + "-" + project.getVersion() + "." + "jar");
                    File destDir = new File(getWebappDeploymentDir(), "WEB-INF/var/bundles");
                    FileUtils.copyFileToDirectory(srcFile, destDir);
                    getLog().info("Copied " + srcFile + " to " + destDir);
                }
            }
        }
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
     * @throws Exception
     */
    private void deployEarProject() throws Exception {
        getLog().info("Deploying application server specific files for " + targetServerType + " v" + targetServerVersion + " in directory " + targetServerDirectory);
        DependencyNode node = getRootDependencyNode();
        deployEarDependency(node);
    }

    /**
     * Copy output folder of WAR / SAR / RAR project to application server
     * @throws Exception
     */
    private void deployWarProject() throws Exception {
        File webappDir = getWarSarRarDeploymentDir(project.getArtifact());
        getLog().info(
                "Update " + project.getPackaging() + " resources for " + targetServerType
                        + (StringUtils.isNotEmpty(targetServerVersion) ? " v" + targetServerVersion : "")
                        + " in directory " + webappDir);
        if ("was".equals(targetServerType)) {
            File source = new File(output, project.getBuild().getFinalName()+".war");
            File target = new File(webappDir +"/" + project.getBuild().getFinalName()+".war");

            try {
                FileUtils.copyFile(source,target);
            }
            catch (IOException e) {
                getLog().error("Error while deploying WAR project", e);
            }
        } else {
            File origSource = new File(baseDir, "src/main/webapp");
            File source = new File(output, project.getBuild().getFinalName());
            try {
                int cnt = updateFiles(source, origSource, webappDir, serverDeployer.getWarExcludes());
                getLog().info("Copied "+cnt+" files.");
            } catch (IOException e) {
                getLog().error("Error while deploying WAR project", e);
            }
        }
    }

    /**
     * Copy output folder of WAR / SAR / RAR project to application server
     * @throws Exception
     */
    private void deploySarRarProject() throws Exception {
        File webappDir = getWarSarRarDeploymentDir(project.getArtifact());
        getLog().info("Update " + project.getPackaging() +
                " resources for " + targetServerType +
                " v" + targetServerVersion +
                " in directory " + webappDir);

        File source = new File(output, project.getBuild().getFinalName());
        try {
            int cnt = updateFiles(source, webappDir);
            getLog().info("Copied "+cnt+" files.");
        } catch (IOException e) {
            getLog().error("Error while deploying SAR or RAR project", e);
        }
    }
    
    /**
     * Copy template resources from output folder to the jsp/templates and WEB-INF/classes of jahia
     * @throws Exception
     */
    private void deployModuleProject() throws Exception {
        File source = new File(output, project.getArtifactId() + "-" + project.getVersion() + "."
                + (project.getPackaging().equals("bundle") ? "jar" : project.getPackaging()));
        File target = new File(getWebappDeploymentDir(), "WEB-INF/var/modules");
        getLog().info("Deploying module " + source + " into " + target);
        
        new ModuleDeployer(target, new MojoLogger(getLog())).deployModule(source);

        getLog().info("...done");
    }

    private void deployPrepackagedSiteProject() throws Exception {
        if (project.getAttachedArtifacts().size() > 0) {
            Artifact artifact = (Artifact) project.getAttachedArtifacts().get(project.getAttachedArtifacts().size()-1);
            try {
                artifactResolver.resolve(artifact, project.getRemoteArtifactRepositories(), localRepository);
                File libDir = new File(getWebappDeploymentDir(), "WEB-INF/var/prepackagedSites");
                libDir.mkdirs();
                File file = new File(libDir, artifact.getArtifactId()+".zip");
                getLog().info("Deploying prepackaged site "+artifact.getFile().getName() + " to "+ file);
                FileUtils.copyFile(artifact.getFile(), file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Copy JAR file to jahia webapp, tries to hotswap classes
     * @throws Exception
     */
    private void deployJarProject() {
        Artifact artifact = project.getArtifact();
        try {
            artifactResolver.resolve(artifact, project.getRemoteArtifactRepositories(), localRepository);
            File libDir = new File(new File(getWebappDeploymentDir(), "WEB-INF"), "lib");
            getLog().info("Deploying jar file "+artifact.getFile().getName() + " to "+ libDir);
            File deployedJar = new File(libDir, artifact.getFile().getName());
            if (deployedJar.exists()) {
                hotSwap(deployedJar);
            }
            FileUtils.copyFileToDirectory(artifact.getFile(), libDir);
        } catch (Exception e) {
            getLog().error("Error while deploying JAR project", e);
        }
    }

    /**
     * Look if the pom depends on a jahia-ear project and deploys it
     * @throws Exception
     */
    private void deployPomProject() {
        try {
            boolean sharedLibraries = project.getGroupId().equals("org.jahia.server")
                    && (project.getArtifactId().equals("shared-libraries") || project.getArtifactId().startsWith(
                            "jdbc-drivers"));
            DependencyNode rootNode = getRootDependencyNode();
            List<?> l = rootNode.getChildren();
            for (Iterator<?> iterator = l.iterator(); iterator.hasNext();) {
                DependencyNode dependencyNode = (DependencyNode) iterator.next();
                Artifact artifact = dependencyNode.getArtifact();
                if (artifact.getGroupId().equals("org.jahia.server")) {
                    String artifactId = artifact.getArtifactId();
                    if (artifactId.equals("jahia-ear") || artifactId.equals("jahia-ee-ear")) {
                        deployEarDependency(dependencyNode);
                    } else if (artifactId.equals("configwizard-ear")) {
                        deployEarDependency(dependencyNode);
                    } else if (artifactId.equals("configwizard-webapp") ||
                            artifactId.equals("jahia-war") ||
                            artifactId.equals("jahia-ee-war") ||
                            artifactId.equals("jahia-pack-war") ||
                            artifactId.equals("jahia-dm-package") ||
                            artifactId.equals("jahia-ee-dm-package") ||
                            artifactId.equals("jahia-wise-package") ||
                            artifactId.equals("jahia-jboss-config")) {
                        deployWarRarSarDependency(dependencyNode);
                    } else if (artifactId.equals("shared-libraries")
                            || artifactId.startsWith("jdbc-drivers")) {
                        deploySharedLibraries(dependencyNode);
                    }
                }
                if (sharedLibraries) {
                    deploySharedLibrary(artifact);
                }
            }
            if ((project.getParent() != null) && ("prepackagedSites".equals(project.getParent().getArtifactId()))
                    || project.getGroupId().equals("org.jahia.prepackagedsites")) {
                deployPrepackagedSiteProject();
            }
        } catch (Exception e) {
            getLog().error("Error while deploying POM project", e);
        }
    }


    protected DependencyNode getRootDependencyNode() throws DependencyTreeBuilderException {
        return dependencyTreeBuilder.buildDependencyTree(project, localRepository, artifactFactory,
                        artifactMetadataSource, null, artifactCollector);
    }


    /**
     * Deploy all EAR dependencies ( WARs / SARs / RARs / shared resources ) to application server
     * @throws Exception
     */
    protected void deployEarDependency(DependencyNode dependencyNode) throws ArtifactResolutionException, ArtifactNotFoundException {
        List<?> child = dependencyNode.getChildren();
        for (Iterator<?> iterator1 = child.iterator(); iterator1.hasNext();) {
            DependencyNode node = (DependencyNode) iterator1.next();
            Artifact artifact = node.getArtifact();

            artifactResolver.resolve(artifact,  project.getRemoteArtifactRepositories(), localRepository);
            try {
                if ((artifact.getGroupId().equals("org.jahia.server") &&
                        (artifact.getArtifactId().equals("jahia-war") ||
                                artifact.getArtifactId().equals("jahia-ee-war") ||
                                artifact.getArtifactId().equals("config"))) ||
                        artifact.getType().equals("rar") ||
                        artifact.getType().equals("sar") ||
                        artifact.getType().equals("jboss-sar")) {
                    deployWarRarSarDependency(node);
                } else if (Artifact.SCOPE_COMPILE.equals(artifact.getScope())) {
                    deploySharedLibrary(artifact);
                }
            } catch (Exception e) {
                getLog().error("Error while deploying EAR dependency", e);
            }

        }
    }

    @SuppressWarnings("unchecked")
    private void deploySharedLibraries(DependencyNode dependencyNode)
            throws IOException, ArtifactResolutionException,
            ArtifactNotFoundException {
        for (DependencyNode node : ((List<DependencyNode>)dependencyNode.getChildren())) {
            Artifact artifact = node.getArtifact();

            artifactResolver.resolve(artifact,
                    project.getRemoteArtifactRepositories(), localRepository);
            try {
                deploySharedLibrary(artifact);
            } catch (Exception e) {
                getLog().error("Error while deploying EAR dependency", e);
            }

        }
    }

    private void deploySharedLibrary(Artifact artifact) throws IOException {
        getLog().info("Copy shared resource " + artifact.getFile().getName());
        List<File> sharedLibs = new LinkedList<File>();
        sharedLibs.add(artifact.getFile());
     
        serverDeployer.deploySharedLibraries(targetServerDirectory, sharedLibs);
    }

    /**
     * Deploy WAR / SAR / RAR artifact to application server
     * @param dependencyNode
     */
    protected void deployWarRarSarDependency(DependencyNode dependencyNode) throws Exception {
        Artifact artifact = dependencyNode.getArtifact();
        File webappDir = getWarSarRarDeploymentDir(artifact);

        getLog().info(
                "Deploying artifact " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
                        + artifact.getVersion());
        getLog().info("Updating " + artifact.getType() +
                " resources for " + targetServerType +
                " v" + targetServerVersion +
                " in directory " + webappDir);
        
        String[] excludes = serverDeployer.getWarExcludes() != null ? StringUtils.split(serverDeployer.getWarExcludes(), ",") : null;
        
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
                    if (entry.getTime() > target.lastModified()) {

                        target.getParentFile().mkdirs();
                        FileOutputStream fileOutputStream = new FileOutputStream(
                                target);
                        IOUtils.copy(z, fileOutputStream);
                        fileOutputStream.close();
                        cnt++;
                    }
                }else{
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
        String connectorName = address.substring(0,colonIndex);
        if (connectorName.equals("socket")) connectorName = "com.sun.jdi.SocketAttach";
        else if (connectorName.equals("shmem")) connectorName = "com.sun.jdi.SharedMemoryAttach";
        String argumentsString = address.substring(colonIndex+1);

        AttachingConnector connector = (AttachingConnector) findConnector(connectorName);
        Map<String, Argument> arguments = connector.defaultArguments();

        StringTokenizer st = new StringTokenizer(argumentsString,",");
        while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            int index = pair.indexOf('=');
            String name = pair.substring(0, index);
            String value = pair.substring(index +1);
            Connector.Argument argument = (Connector.Argument)arguments.get(name);
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
            getLog().info("Connected to " + vm.name()+" "+vm.version());

            Map<String, File> files = new HashMap<String,File>();

            parse(new File(output,"classes"), dates, "", files);
            getLog().debug("Classes : "+files.keySet());
            if (!files.isEmpty()) {
                reload(vm,files);
            }
        } catch (ConnectException e) {
            getLog().warn("Cannot hotswap classes : "+e.getMessage());
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
            Connector connector = (Connector)iter.next();
            if (connector.name().equals(name)) {
                return connector;
            }
        }
        return null;
    }

    private void parse(File folder, Map<String,Long> dates, String base, Map<String,File> result) {
        File[] files = folder.listFiles();
        for (File file : files) {
            String filename = file.getName();
            if (file.isDirectory()) {
                parse(file, dates, base + filename + ".", result);
            } else if (filename.endsWith(".class")) {
                String name = base + filename.substring(0, filename.lastIndexOf("."));
                String classFileName = name.replace(".","/")+".class";

                if (dates.containsKey(classFileName)) {
                    long l = dates.get(classFileName);
                    if (file.lastModified() > l) {
                        result.put(name, file);
                        getLog().debug("Updated class : "+file);
                    }
                }
            }
        }
    }

    public void reload(VirtualMachine vm, Map<String,File> classFiles) {
        Map<ReferenceType,byte[]> map = new HashMap<ReferenceType,byte[]>();

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
            getLog().info("Reloaded "+map.size() + " classes.");
        } catch (Exception e) {
            getLog().warn("Cannot reload classes : "+ e.getMessage());
        }
    }

}
