/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2023 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.osgi;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.jahia.utils.osgi.BundleUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarInputStream;

/**
 * A new goal to provide tools to inspect OSGi bundles, either generated by the project or specified as a
 * parameter.
 *
 * @goal osgi-inspect
 * @requiresProject false
 */
public class OsgiInspectorMojo extends AbstractMojo {

    /**
     * @parameter default-value="${jarBundles}"
     */
    protected List<String> jarBundles = new ArrayList<String>();

    /**
     * @parameter expression="${project}"
     * @readonly
     */
    protected MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (jarBundles == null || jarBundles.size() == 0) {
            jarBundles = new ArrayList<String>();
            String extension = project.getPackaging();
            if ("bundle".equals(extension)) {
                extension = "jar";
            }
            jarBundles.add(project.getBuild().getDirectory() + "/" + project.getBuild().getFinalName() + "." + extension);
        }
        for (String jarBundle : jarBundles) {
            JarInputStream jarInputStream = null;
            File jarFile = new File(jarBundle);
            if (!jarFile.exists()) {
                getLog().error(jarFile + " does not exist, skipping !");
                continue;
            }
            try {
                jarInputStream = new JarInputStream(new FileInputStream(jarBundle));
                StringWriter stringWriter = new StringWriter();
                PrintWriter stringPrintWriter = new PrintWriter(stringWriter);
                BundleUtils.dumpManifestHeaders(jarInputStream, stringPrintWriter);
                getLog().info(jarBundle + " header dump:\n" + stringWriter.getBuffer().toString());
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } finally {
                IOUtils.closeQuietly(jarInputStream);
            }
        }
    }
}
