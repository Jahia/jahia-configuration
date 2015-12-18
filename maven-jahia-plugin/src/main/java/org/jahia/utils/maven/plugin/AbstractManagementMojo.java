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
package org.jahia.utils.maven.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.commons.io.FileUtils;
import org.jahia.configuration.configurators.JahiaGlobalConfigurator;
import org.jahia.configuration.deployers.ServerDeploymentFactory;
import org.jahia.configuration.deployers.ServerDeploymentInterface;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Abstract class that is shared between some of plugin's goals.
 * User: Serge Huber
 * Date: 26 d�c. 2007
 * Time: 11:55:57
 */
public abstract class AbstractManagementMojo extends AbstractMojo {

    /**
     * Server type
     * @parameter expression="${jahia.deploy.targetServerType}" default-value="tomcat"
     */
    protected String targetServerType = "tomcat";

    /**
     * Server version
     * @parameter expression="${jahia.deploy.targetServerVersion}" default-value=""
     */
    protected String targetServerVersion = "";

    /**
     * The main directory for the target server install in which we will deploy the app-specific configuration. 
     * @parameter expression="${jahia.deploy.targetServerDirectory}"
     */
    protected String targetServerDirectory;

    /**
     * The Web application directory name (under /webapps) where the Jahia will be deployed. 
     * @parameter expression="${jahia.deploy.war.dirName}" default-value="ROOT"
     */
    public String webAppDirName;

    /**
     * @parameter default-value="${basedir}"
     */
    protected File baseDir;

    /**
     * @parameter default-value="${project.build.directory}"
     */
    protected File output;

    /**
     * @parameter expression="${jahia.deploy.dataDir}"
     */
    private String jahiaVarDiskPath;
    
    private File dataDir;
    
    /**
     * @component
     * @required
     * @readonly
     */
    protected ArtifactFactory artifactFactory;

    /**
     * @component
     * @required
     * @readonly
     */
    protected ArtifactResolver artifactResolver;

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    private ServerDeploymentInterface deployer;

    private File webappDeploymentDir;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        doValidate();
        doExecute();
    }

    public void doValidate() throws MojoExecutionException, MojoFailureException {
    	// do nothing
    }

    public abstract void doExecute() throws MojoExecutionException, MojoFailureException;

    protected List<File> getDependencies(String[] artifacts) throws ArtifactNotFoundException, ArtifactResolutionException {
        List<File> list = new ArrayList<File>();
        for (int i=0; i < artifacts.length; i++) {
            String curFQN = artifacts[i];
            StringTokenizer tokens = new StringTokenizer(curFQN, ":");
            String groupId = tokens.nextToken();
            String artifactId = tokens.nextToken();
            String versionId = tokens.nextToken();
            String typeId = "jar";
            if (tokens.hasMoreTokens()) {
                typeId = tokens.nextToken();
            }
            Artifact artifact = artifactFactory.createArtifactWithClassifier(
                    groupId, artifactId, versionId, typeId, null
            );
            artifactResolver.resolve(artifact, project.getRemoteArtifactRepositories(), localRepository);
            if(artifact.getFile() == null) {
                getLog().warn("Unable to find file for artifact: "+artifact.getArtifactId());
            }

            list.add(artifact.getFile());
        }
        return list;
    }

    protected int updateFiles(File sourceFolder, File destFolder) throws IOException {
        return updateFiles(sourceFolder, destFolder, null);
    }

    protected int updateFiles(File sourceFolder, File destFolder, String excluded) throws IOException {
        return updateFiles(sourceFolder, sourceFolder, destFolder, excluded);
    }

    protected int updateFiles(File sourceFolder, File originalSourceFolder, File destFolder, String excluded) throws IOException {
        long timer = System.currentTimeMillis();
        List<String> filesToUpdate = org.codehaus.plexus.util.FileUtils.getFileNames(sourceFolder, "**", excluded, false);
        int cnt = 0;
        for (String sourceFile : filesToUpdate) {
            File destFile = new File(destFolder, sourceFile);
            File origFile = new File(sourceFolder, sourceFile);
            File origSourceFile = new File(originalSourceFolder, sourceFile);
            long date = origFile.lastModified();
            if (origSourceFile.exists()) {
                date = origSourceFile.lastModified();
            }
            if (!destFile.exists() || destFile.lastModified() < date) {
                getLog().debug("Copy " + origFile + " to " + destFile);
                FileUtils.copyFile(origFile, destFile);
                cnt++;
            }
        }
        getLog().debug("Copy took " + (System.currentTimeMillis() - timer));
        timer = System.currentTimeMillis();
        // copy dir layout to force creation of empty folders
        org.codehaus.plexus.util.FileUtils.copyDirectoryLayout(sourceFolder, destFolder, null, null);
        getLog().debug("Dir layout took " + (System.currentTimeMillis() - timer));
        return cnt;
    }

    protected String getWebappDeploymentDirName() {
        String dirName = getDeployer().getWebappDeploymentDirNameOverride();
        return dirName != null ? dirName : (webAppDirName != null ? webAppDirName : "jahia");
    }

    /**
     * Get the folder on the application server where the jahia webapp is unpacked
     */
    protected File getWebappDeploymentDir() {
        if (webappDeploymentDir == null) {
            webappDeploymentDir = getDeployer().getDeploymentDirPath(getWebappDeploymentDirName(), "war");
        }
        return webappDeploymentDir;
    }

    protected ServerDeploymentInterface getDeployer() {
        if (deployer == null) {
            deployer = ServerDeploymentFactory.getImplementation(targetServerType, targetServerVersion, new File(
                    targetServerDirectory), null, null);
        }
        return deployer;
    }

    protected File getDataDir() {
        if (dataDir == null) {
            dataDir = JahiaGlobalConfigurator.resolveDataDir(getJahiaVarDiskPath(), getWebappDeploymentDir()
                    .getAbsolutePath());
            getLog().info("Data directory path resolved to: " + dataDir);
        }

        return dataDir;
    }

    public String getJahiaVarDiskPath() {
        if (jahiaVarDiskPath == null) {
            jahiaVarDiskPath = "${jahiaWebAppRoot}/WEB-INF/var/";
            if (targetServerType != null) {
                if (targetServerType.startsWith("jboss")) {
                    jahiaVarDiskPath = "${jahiaWebAppRoot}/../../../data/digital-factory-data/";
                } else if (targetServerType.startsWith("tomcat")) {
                    jahiaVarDiskPath = "${jahiaWebAppRoot}/../../digital-factory-data/";
                    File dir = JahiaGlobalConfigurator.resolveDataDir(jahiaVarDiskPath, getWebappDeploymentDir()
                            .getAbsolutePath(), false);
                    if (!dir.isDirectory()) {
                        // try one level up, if we deal with DF installed using installer
                        jahiaVarDiskPath = "${jahiaWebAppRoot}/../../../digital-factory-data/";
                        dir = JahiaGlobalConfigurator.resolveDataDir(jahiaVarDiskPath, getWebappDeploymentDir()
                                .getAbsolutePath(), false);
                        if (!dir.isDirectory()) {
                            // will create it under tomcat
                            jahiaVarDiskPath = "${jahiaWebAppRoot}/../../digital-factory-data/";
                        }
                    }
                }
            }
            getLog().info("Data directory path is set to \"" + jahiaVarDiskPath + "\".");
        }
        return jahiaVarDiskPath;
    }
}
