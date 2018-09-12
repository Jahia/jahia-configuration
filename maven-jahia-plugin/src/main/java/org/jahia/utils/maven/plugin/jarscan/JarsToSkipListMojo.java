/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.jarscan;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Creates a list of JARs that do not contain any TLD files and can be skipped by the JAR scanner (Jasper compiler in the Pax Web JSP
 * project).
 * 
 * @goal list-jars-with-no-tlds
 * @phase prepare-package
 * @requiresDependencyResolution
 * @author Sergiy Shyrkov
 */
public class JarsToSkipListMojo extends AbstractMojo {

    private static boolean hasTLDFiles(File file) throws IOException {
        String name = file.getName();
        if (!file.isFile() || !name.endsWith(".jar")) {
            return false;
        }

        boolean tldPresent = false;
        JarFile jar = null;
        try {
            jar = new JarFile(file);
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String entry = entries.nextElement().getName();
                if (entry != null && entry.startsWith("META-INF/") && entry.endsWith(".tld")) {
                    tldPresent = true;
                    break;
                }
            }
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        return tldPresent;
    }

    /**
     * Should we merge entries with the entries in the existing <code>dest</code> file?.
     * 
     * @parameter default-value="false"
     */
    protected boolean append;

    /**
     * The file to output the list of JARs into.
     * 
     * @parameter default-value="${project.build.directory}/generated-resources/jar-scanner.conf"
     */
    protected File dest;

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    /**
     * The Jahia Web application directory to scan for JAR files in.
     * 
     * @parameter default-value="${project.build.directory}/jahia"
     */
    protected File src;

    /**
     * The list of JARs which are skipped by default.
     * 
     * @parameter default-value=
     *            "jboss-modules.jar,ehcache-sizeof-agent*.jar,ccpp-1.0.jar,derby-*.jar,derbyclient-*.jar,mariadb-java-client-*.jar,mysql-connector-java-*.jar,ojdbc6-*.jar,ojdbc7-*.jar,orai18n-*.jar,pluto-container-api-2.0.2.jar,pluto-container-driver-api-2.0.2.jar,pluto-taglib-2.0.2.jar,portals-bridges-common-1.0.4.jar,portlet-api_2.0_spec-1.0.jar,postgresql-*.jar,sqljdbc4-*.jar,sqljdbc41-*.jar,mssql-jdbc-*.jar"
     */
    protected String defaultJarsToSkip;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (src == null || !src.exists()) {
            getLog().info("Folder " + src + " does not exist. Skipping task.");
            return;
        }
        long start = System.currentTimeMillis();

        Set<String> toSkip = new TreeSet<String>();

        if (StringUtils.isNotEmpty(defaultJarsToSkip)) {
            @SuppressWarnings("unchecked")
            List<String> skipByDefault = Arrays.asList(StringUtils.split(defaultJarsToSkip, ", "));
            toSkip.addAll(skipByDefault);
        }

        try {
            if (append && dest.isFile()) {
                toSkip.addAll(FileUtils.readLines(dest));
            }
        } catch (IOException e) {
            getLog().error("Unable to read entries from existing file " + dest, e);
        }

        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.getType().equals("jar")
                    && (Artifact.SCOPE_COMPILE.equals(artifact.getScope()) || Artifact.SCOPE_RUNTIME.equals(artifact
                            .getScope()))) {
                File file = artifact.getFile();
                try {
                    boolean hasTLDFiles = hasTLDFiles(file);
                    if (getLog().isDebugEnabled()) {
                        if (hasTLDFiles) {
                            getLog().debug("Scanned dependency " + file + ". TLD files found.");
                        } else {
                            getLog().debug(
                                    "Scanned dependency " + file
                                            + ". No  TLD files found. Will be added to the skip list.");
                        }
                    }
                    if (!hasTLDFiles) {
                        toSkip.add(file.getName());
                    }
                } catch (IOException e) {
                    getLog().error(e);
                }
            }
        }

        try {
            FileUtils.writeLines(dest, toSkip, "\n");
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write results to the destination file " + dest, e);
        }

        getLog().info(
                "Took " + (System.currentTimeMillis() - start) + " ms for scanning TLDs in JARs. Found "
                        + toSkip.size() + " JARs without TLDs. The list is written into " + dest);
    }
}
