/**
 * 
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2011 Jahia Limited. All rights reserved.
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
import org.jahia.configuration.deployers.ServerDeploymentFactory;

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
     * @parameter expression="${jahia.deploy.targetServerType}"
     */
    protected String targetServerType;

    /**
     * Server version
     * @parameter expression="${jahia.deploy.targetServerVersion}" default-value=""
     */
    protected String targetServerVersion;

    /**
     * The main directory for the target server install in which we will deploy the app-specific configuration. 
     * @parameter expression="${jahia.deploy.targetServerDirectory}"
     */
    protected String targetServerDirectory;

    /**
     * The Web application directory name (under /webapps) where the Jahia will be deployed. 
     * @parameter default-value="${jahia.deploy.war.dirName}"
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
    
    private Integer projectStructureVersion;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        ServerDeploymentFactory.setTargetServerDirectory(targetServerDirectory);
        org.jahia.utils.maven.plugin.deployers.ServerDeploymentFactory.setTargetServerDirectory(targetServerDirectory);

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

    @SuppressWarnings("unchecked")
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
        return webAppDirName != null ? webAppDirName : "jahia";
    }

    /**
     * Get the folder on the application server where the jahia webapp is unpacked
     */
    protected File getWebappDeploymentDir() throws Exception {
        return new File(targetServerDirectory, ServerDeploymentFactory.getInstance()
                .getImplementation(targetServerType + targetServerVersion).getDeploymentDirPath(getWebappDeploymentDirName(), "war"));
    }

    /**
     * Get the folder on the application server where the given artifact is unpacked ( war, rar or sar )
     * @param artifact
     */
    protected File getWarSarRarDeploymentDir(Artifact artifact) throws Exception {
        File dir;
        if (artifact.getType().equals("rar") || artifact.getType().equals("sar") || artifact.getType().equals("jboss-sar") || artifact.getArtifactId().equals("config")) {
            dir = new File(targetServerDirectory, ServerDeploymentFactory.getInstance()
                    .getImplementation(targetServerType + targetServerVersion).getDeploymentDirPath(artifact.getArtifactId(), artifact.getType()));
        } else {
            dir = getWebappDeploymentDir();
        }
        return dir;
    }
    
    public int getProjectStructureVersion() {
    	if (projectStructureVersion != null) {
    		return projectStructureVersion;
    	}
        int version = 0;
        if (project.getProperties().containsKey("jahia.project.structure.version")) {
            version = Integer.parseInt((String)project.getProperties().get("jahia.project.structure.version"));
        }
        getLog().info("Jahia project structure version is " + version);
        projectStructureVersion = version;
        
        return projectStructureVersion;
    }
    
}
