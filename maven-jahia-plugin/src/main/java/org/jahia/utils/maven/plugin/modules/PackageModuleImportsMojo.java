/**
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2012 Jahia Solutions Group SA. All rights reserved.
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
 * between you and Jahia Solutions Group SA. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */

package org.jahia.utils.maven.plugin.modules;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

/**
 * Used to automatically package module's import resources (as a ZIP file) into the target module WAR file.<br>
 * 
 * @goal package-imports
 * @phase prepare-package
 * 
 * @author Sergiy Shyrkov
 */
public class PackageModuleImportsMojo extends AbstractMojo {

    /**
     * The resulting archive name
     * 
     * @parameter default-value="import.zip"
     */
    protected String archiveName;

    /**
     * The directory to output file to
     * 
     * @parameter default-value="${project.build.directory}/${project.build.finalName}/META-INF"
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
     * If set to true the goal will be executed only if the current project is a Jahia module project with 'war' packaging. Otherwise the
     * goal will be skipped.
     * 
     * @parameter default-value="true"
     */
    protected boolean requiresModuleProject;

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
        if (requiresModuleProject
                && (!"war".equals(project.getPackaging()) || (!project
                        .getGroupId().equals("org.jahia.modules") && !project
                        .getGroupId().endsWith(".jahia.modules")))) {
            getLog().info(
                    "Current project should have 'war' packaging type"
                            + " and be a Jahia module project to be able to execute this goal."
                            + " Skipping package-imports task.");
            return;
        }

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
            archiver.addDirectory(src, excludes != null ? excludes.split(",")
                    : null, includes != null ? includes.split(",") : null);

            archiver.createArchive();
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
