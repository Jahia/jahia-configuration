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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;

/**
 * Mojo for copying Jahia modules and pre-packaged sites into Jahia WAR file.
 * User: toto
 * Date: Jul 23, 2008
 * Time: 10:31:17 AM
 * @goal copy-templates
 * @requiresDependencyResolution runtime
 */
public class CopyTemplatesMojo extends AbstractManagementMojo {


    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        Set dependencyFiles = project.getDependencyArtifacts();
        Set<Artifact> dependenciesToRemove = new HashSet<Artifact>();
        for (Artifact dependencyFile : (Iterable<Artifact>) dependencyFiles) {
            if (dependencyFile.getGroupId().equals("org.jahia.modules") || dependencyFile.getGroupId().equals("org.jahia.templates") || dependencyFile.getGroupId().endsWith(".jahia.modules")) {
                try {
                    FileUtils.copyFileToDirectory(dependencyFile.getFile(), new File(output,"jahia/WEB-INF/var/shared_" + (getProjectStructureVersion() == 2 ? "modules" : "templates")));
                    getLog().info("Copy modules JAR "+dependencyFile.getFile().getName() + " to shared modules folder");
                    copyJars(dependencyFile.getFile(), new File(output,"jahia"));
                    copyDbScripts(dependencyFile.getFile(), new File(output,"jahia"));
                    dependenciesToRemove.add(dependencyFile);
                } catch (IOException e) {
                    getLog().error("Error when copying file " + dependencyFile.getFile(), e);
                }
            }
            if (dependencyFile.getGroupId().equals("org.jahia.prepackagedsites")) {
                try {
                    FileUtils.copyFile(dependencyFile.getFile(), new File(output,"jahia/WEB-INF/var/prepackagedSites/"+dependencyFile.getArtifactId()+".zip"));
                    getLog().info("Copy prepackaged site "+dependencyFile.getFile().getName());
                    dependenciesToRemove.add(dependencyFile);
                } catch (IOException e) {
                    getLog().error("Error when copying file " + dependencyFile.getFile(), e);
                }
            }

        }
        List dependencyList = new ArrayList(dependencyFiles);
        for (Artifact dependencyFile : dependenciesToRemove) {
            dependencyList.remove(dependencyFile);
        }
        project.setDependencyArtifacts(new LinkedHashSet(dependencyList));
    }

    private void copyJars(File warFile, File targetDir) {
        try {
            JarFile war = new JarFile(warFile);
            int deployed = 0;
            if (war.getJarEntry("WEB-INF/lib") != null) {
                Enumeration<JarEntry> entries = war.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith("WEB-INF/lib/") && entry.getName().endsWith(".jar")) {
                        deployed++;
                        InputStream source = war.getInputStream(entry);
                        File libsDir = new File(targetDir, "WEB-INF/lib"); 
                        if (!libsDir.exists()) {
                            libsDir.mkdirs();
                        }
                        File targetFile = new File(targetDir, entry.getName());
                        FileOutputStream target = new FileOutputStream(targetFile);
                        IOUtils.copy(source, target);
                        IOUtils.closeQuietly(source);
                        target.flush();
                        IOUtils.closeQuietly(target);
                        if (entry.getTime() > 0) {
                            targetFile.setLastModified(entry.getTime());
                        }
                    }
                }
            }
            if (deployed > 0) {
                getLog().info("Copied " + deployed + " JARs from " + warFile.getName() + " to jahia/WEB-INF/lib");
            }
        } catch (IOException e) {
            getLog().error("Error copying JAR files for module " + warFile, e);
        }
    }

    private void copyDbScripts(File warFile, File targetDir) {
        try {
            JarFile war = new JarFile(warFile);
            if (war.getJarEntry("META-INF/db") != null) {
            	war.close();
            	ZipUnArchiver unarch = new ZipUnArchiver(warFile);
            	File tmp = new File(targetDir, String.valueOf(System.currentTimeMillis()));
            	tmp.mkdirs();
            	unarch.extract("META-INF/db", tmp);
            	FileUtils.copyDirectory(new File(tmp, "META-INF/db"), new File(targetDir, "WEB-INF/var/db/sql/schema"));
            	FileUtils.deleteDirectory(tmp);
                getLog().info("Copied database scripts from " + warFile.getName() + " to jahia/WEB-INF/var/db/sql/schema");
            }
        } catch (Exception e) {
            getLog().error("Error copying database scripts for module " + warFile, e);
		}
    }
}
