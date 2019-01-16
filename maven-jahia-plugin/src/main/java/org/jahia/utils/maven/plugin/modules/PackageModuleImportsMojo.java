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
package org.jahia.utils.maven.plugin.modules;

import java.io.File;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

/**
 * Used to automatically package module's import resources (as a ZIP file) into the target module bundle file.<br>
 * 
 * @goal package-imports
 * @phase prepare-package
 * 
 * @author Sergiy Shyrkov
 */
public class PackageModuleImportsMojo extends AbstractMojo {

    /**
     * Do we need to add the generated import.zip file into project's resources.
     * 
     * @parameter default-value="true"
     */
    protected boolean addToProjectResources;
    
    /**
     * The resulting archive name
     * 
     * @parameter default-value="import.zip"
     */
    protected String archiveName;

    /**
     * The directory to output file to
     * 
     * @parameter default-value="${project.build.directory}/packaged-imports/META-INF"
     */
    protected File dest;

    /**
     * comma- or space-separated list of patterns of files that must be excluded. No files (except default excludes) are excluded when
     * omitted.
     * 
     * @parameter
     */
    protected String excludes;

    /**
     * comma- or space-separated list of patterns of files that must be included. All files are included when omitted
     * 
     * @parameter
     */
    protected String includes;

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    /**
     * The directory to find files in (default is basedir)
     * 
     * @parameter default-value="${basedir}/src/main/import"
     */
    protected File src;

    /**
     * Should we execute the archiver in a verbose mode?
     * 
     * @parameter default-value="false"
     */
    protected boolean verbose;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (src == null || !src.exists() || !src.isDirectory()) {
            getLog().info(
                    "Folder " + src + " does not exist."
                            + " Skipping package-imports task.");
            return;
        }
        if (src.listFiles().length == 0) {
            getLog().info(
                    "Folder " + src + " does not contain any files."
                            + " Skipping package-imports task.");
            return;
        }

        ZipArchiver archiver = new ZipArchiver();
        if (verbose) {
            archiver.enableLogging(new ConsoleLogger(Logger.LEVEL_DEBUG,
                    "console"));
        }
        if (!dest.exists()) {
            dest.mkdirs();
        }
        File absoluteDestFile = new File(dest, archiveName);
        archiver.setDestFile(absoluteDestFile);
        getLog().info(
                "Packaging imports from " + src + " into archive "
                        + absoluteDestFile + " (excludes=" + excludes
                        + ", inludes=" + includes + ")");
        try {
            archiver.addDirectory(src, includes != null ? includes.split(",") : null,
                    excludes != null ? excludes.split(",") : null);

            archiver.createArchive();
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        
        if (addToProjectResources) {
            Resource resource = new Resource();
            resource.setDirectory(dest.getPath().endsWith("META-INF") ? dest.getParentFile().getPath() : dest.getPath());
            getLog().info("Attaching directory " + resource.getDirectory() + " to project resources");
            this.project.addResource(resource);
        }
        
    }
}
