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

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Context server deployment mojo.
 * 
 * @author Sergiy Shyrkov
 * @goal cs-deploy
 * @requiresDependencyResolution runtime
 */
public class ContextServerDeployMojo extends AbstractMojo {

    /**
     * @component
     * @required
     * @readonly
     */
    protected ArtifactResolver artifactResolver;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    /**
     * The root directory for context server.
     * 
     * @parameter expression="${contextserver.targetServerDirectory}"
     */
    protected File targetServerDirectory;

    @SuppressWarnings("deprecation")
    private void doDeploy(File deployDir) {
        Artifact artifact = project.getArtifact();
        try {
            artifactResolver.resolve(artifact, project.getRemoteArtifactRepositories(), localRepository);
            getLog().info("Deploying jar file " + artifact.getFile().getName() + " to " + deployDir);
            FileUtils.copyFileToDirectory(artifact.getFile(), deployDir);
        } catch (Exception e) {
            getLog().error("Error while deploying JAR project", e);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (targetServerDirectory == null) {
            getLog().warn("No context server target directory set for deployment. Skipping.");
            return;
        }

        File deployDir = new File(targetServerDirectory, "deploy");
        if (!deployDir.isDirectory()) {
            getLog().warn("No context server deploy directory found under " + deployDir + " + . Skipping.");
            return;
        }

        if (!project.getPackaging().equals("bundle")) {
            getLog().warn("Project does not have the \"bundle\" packaging. Skipping.");
            return;
        }

        doDeploy(deployDir);
    }

}
