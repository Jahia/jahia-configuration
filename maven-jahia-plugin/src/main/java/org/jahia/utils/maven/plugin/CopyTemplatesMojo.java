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
import java.io.IOException;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jahia.configuration.modules.ModuleDeployer;

/**
 * Mojo for copying Jahia modules and pre-packaged sites into Jahia WAR file.
 * User: toto
 * Date: Jul 23, 2008
 * Time: 10:31:17 AM
 * @goal copy-templates
 * @requiresDependencyResolution runtime
 */
public class CopyTemplatesMojo extends AbstractManagementMojo {

    /**
     * @parameter default-value="false"
     */
    protected boolean deployTests;

    /**
     * @parameter default-value="true"
     */
    protected boolean deployModules;
    
    /**
     * @parameter default-value="false"
     */
    protected boolean deployToServer;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        Set dependencyFiles = project.getDependencyArtifacts();
        // @todo as we are working around what seems to be a Maven or JVM bug, maybe we should rebuild the list with
        // just the elements we need instead of the hack that we have used at the end of this method.
        Set<Artifact> dependenciesToRemove = new HashSet<Artifact>();
        File target;
        if(deployToServer) {
            try {
                target = new File(getWebappDeploymentDir(), "WEB-INF/var/shared_modules");
            } catch(Exception e) {
                throw new MojoExecutionException("Cannot deploy module", e);
            }
        } else {
            target = new File(output, "jahia/WEB-INF/var/shared_modules");
        }

        ModuleDeployer deployer = new ModuleDeployer(target, new MojoLogger(getLog()));
        for (Artifact dependencyFile : (Iterable<Artifact>) dependencyFiles) {
            File file = dependencyFile.getFile();
            if (deployModules && (dependencyFile.getGroupId().equals("org.jahia.modules") || dependencyFile.getGroupId().equals("org.jahia.templates") || dependencyFile.getGroupId().endsWith(".jahia.modules")) || deployTests && dependencyFile.getGroupId().equals("org.jahia.test")) {
                try {
                    deployer.deployModule(file);
                    dependenciesToRemove.add(dependencyFile);
                } catch (IOException e) {
                    getLog().error("Error when copying file " + file, e);
                }
            }
            if (dependencyFile.getGroupId().equals("org.jahia.prepackagedsites")) {
                try {
                    FileUtils.copyFile(file, new File(output,"jahia/WEB-INF/var/prepackagedSites/"+dependencyFile.getArtifactId()+".zip"));
                    getLog().info("Copy prepackaged site " + file.getName());
                    dependenciesToRemove.add(dependencyFile);
                } catch (IOException e) {
                    getLog().error("Error when copying file " + file, e);
                }
            }

        }
        List dependencyList = new ArrayList(dependencyFiles);
        for (Artifact dependencyFile : dependenciesToRemove) {
            dependencyList.remove(dependencyFile);
        }
        project.setDependencyArtifacts(new LinkedHashSet(dependencyList));
    }

}
