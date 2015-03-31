/**
 * 
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2014 Jahia Limited. All rights reserved.
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
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.jahia.configuration.modules.ModuleDeployer;

/**
 * Mojo for copying Jahia modules and pre-packaged sites into Jahia WAR file.
 * @goal copy-templates
 * @requiresDependencyResolution runtime
 */
public class CopyTemplatesMojo extends AbstractManagementMojo {

    /**
     * Tests modules deployment
     * If false, all modules within the package "org.jahia.test" will be ignored
     * @parameter default-value="false"
     */
    protected boolean deployTests;

    /**
     * Modules deployment
     * If false, all modules within the package "org.jahia.modules" will be ignored
     * Modules within the package "org.jahia.prepackagedsites" are copied to the prepackagedsites" folder of Jahia
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
                    target = new File(getDataDir(), "modules");
                    if (!target.exists()) {
                        target.mkdirs();
                    }
            } catch(Exception e) {
                throw new MojoExecutionException("Cannot deploy module", e);
            }
        } else {
                target = new File(output, "digital-factory-data/modules");
                if (!target.exists()) {
                    target.mkdirs();
                }
        }

        ModuleDeployer deployer = new ModuleDeployer(target, new MojoLogger(getLog()));
        for (Artifact dependencyFile : (Iterable<Artifact>) dependencyFiles) {
            File file = dependencyFile.getFile();
            String groupId = dependencyFile.getGroupId();
            if (deployModules && (groupId.equals("org.jahia.modules") || groupId.equals("org.jahia.templates") || groupId.endsWith(".jahia.modules")) || deployTests && groupId.equals("org.jahia.test")) {
                try {
                    deployer.deployModule(file);
                    dependenciesToRemove.add(dependencyFile);
                } catch (IOException e) {
                    getLog().error("Error when copying file " + file, e);
                }
            } else if (groupId.equals("org.jahia.prepackagedsites")) {
                try {
                    File deployDir = deployToServer ? getDataDir() : new File(output, "digital-factory-data");
                    FileUtils.copyFile(file, new File(deployDir,"prepackagedSites/"+dependencyFile.getArtifactId()+".zip"));
                    getLog().info("Copy prepackaged site " + file.getName());
                    dependenciesToRemove.add(dependencyFile);
                } catch (IOException e) {
                    getLog().error("Error when copying file " + file, e);
                } catch(Exception e) {
                    throw new MojoExecutionException("Cannot deploy prepackaged site", e);
                }
            } else if (groupId.equals("org.jahia.packages") && (dependencyFile.getType().equals("jar") || dependencyFile.getType().equals("bundle"))) {
                try {
                    getLog().info("Deploying package " + file.getName());
                    dependenciesToRemove.add(dependencyFile);
                    deployPackageFile(file, deployer);
                } catch (Exception e) {
                    getLog().error("Cannot deploy package " + dependencyFile, e);
                    throw new MojoExecutionException("Cannot deploy package " + dependencyFile, e);
                }
            }

        }
        List dependencyList = new ArrayList(dependencyFiles);
        for (Artifact dependencyFile : dependenciesToRemove) {
            dependencyList.remove(dependencyFile);
        }
        project.setDependencyArtifacts(new LinkedHashSet(dependencyList));
    }

    private void deployPackageFile(File file, ModuleDeployer deployer) throws ArchiverException, IOException {
        ZipUnArchiver unzip = new ZipUnArchiver(file);
        unzip.enableLogging(new ConsoleLogger(getLog().isDebugEnabled() ? Logger.LEVEL_DEBUG : Logger.LEVEL_INFO,
                "console"));
        File target = new File(FileUtils.getTempDirectory(), CopyTemplatesMojo.class.getSimpleName());
        FileUtils.deleteQuietly(target);
        target.mkdir();
        unzip.setDestDirectory(target);
        unzip.setFileSelectors(Collections.singletonList(new FileSelector() {
            @Override
            public boolean isSelected(FileInfo fileInfo) throws IOException {
                return fileInfo.isFile() && fileInfo.getName().endsWith(".jar");
            }
        }).toArray(new FileSelector[] {}));

        unzip.extract();

        File[] jars = target.listFiles();
        if (jars == null) {
            return;
        }

        for (File f : jars) {
            deployer.deployModule(f);
        }
    }
}
