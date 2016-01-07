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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.Artifact;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

/**
 * Copy Jahia war
 * @goal copy-jahiawar
 * @requiresDependencyResolution runtime
 */
public class CopyJahiaWarMojo extends AbstractMojo {


    /**
     * @parameter default-value="${project.build.directory}"
     */
    protected File output;

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    @SuppressWarnings("unchecked")
    public void execute() throws MojoExecutionException, MojoFailureException {
        for (Artifact dependencyFile : (Iterable<Artifact>) project.getDependencyArtifacts()) {
            if ("org.jahia.server".equals(dependencyFile.getGroupId())
                    && "jahia-war".equals(dependencyFile.getArtifactId())) {
                try {
                    File webappDir = new File(output,"config/WEB-INF/jahia");
                    ZipInputStream z = new ZipInputStream(
                            new FileInputStream(dependencyFile.getFile()));
                    ZipEntry entry;
                    int cnt = 0;
                    while ((entry = z.getNextEntry()) != null) {
                        if (!entry.isDirectory()) {
                            File target = new File(webappDir, entry.getName());

                            if (entry.getTime() > target.lastModified()) {

                                target.getParentFile().mkdirs();
                                FileOutputStream fileOutputStream = new FileOutputStream(
                                        target);
                                IOUtils.copy(z, fileOutputStream);
                                fileOutputStream.close();
                                cnt++;
                            }
                        }
                    }
                    z.close();
                    getLog().info("Copied " + cnt + " files.");
                } catch (IOException e) {
                    getLog().error("Error when copying file");
                }
            }
        }
    }
}