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

package org.jahia.utils.maven.plugin.resources;

import java.io.File;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.Native2Ascii;

/**
 * Converter for resource bundles.<br>
 * Inspired by native2ascii-maven-plugin.
 * 
 * @goal native2ascii
 * @phase generate-resources
 * @author Sergiy Shyrkov
 */
public class Native2AsciiMojo extends AbstractMojo {

    /**
     * The directory to find files in (default is basedir)
     * 
     * @parameter default-value="${basedir}/src/main/resources"
     */
    protected File src;

    /**
     * The directory to output file to
     * 
     * @parameter default-value="${project.build.directory}/native2ascii"
     */
    protected File dest;

    /**
     * File extension to use in renaming output files
     * 
     * @parameter
     */
    protected String ext;

    /**
     * The native encoding the files are in (default is the default encoding for
     * the JVM)
     * 
     * @parameter
     */
    protected String encoding;

    /**
     * comma- or space-separated list of patterns of files that must be
     * included. All files are included when omitted
     * 
     * @parameter
     */
    protected String includes;

    /**
     * comma- or space-separated list of patterns of files that must be
     * excluded. No files (except default excludes) are excluded when omitted.
     * 
     * @parameter
     */
    protected String excludes;

    /**
     * Do we need to add the processed files into project's resources.
     * 
     * @parameter default-value="true"
     */
    protected boolean addToProjectResources;

    /**
     * @parameter expression="${project}"
     * @readonly
     */
    protected MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (src == null || !src.exists()) {
            getLog().info("Folder " + src + " does not exist. Skipping native2ascii task.");
            return;
        }
        Project antProject = new Project();
        antProject.setName("native2ascii");

        Native2Ascii antTask = new Native2Ascii();
        antTask.setProject(antProject);

        antTask.setSrc(src);
        antTask.setDest(dest);
        antTask.setEncoding(encoding);
        antTask.setExt(ext);
        antTask.setExcludes(excludes);
        antTask.setIncludes(includes);

        antTask.execute();

        if (addToProjectResources) {
            Resource resource = new Resource();
            resource.setDirectory(dest.getPath());
            this.project.addResource(resource);
        }
    }
}
