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

package org.jahia.utils.maven.plugin.reporting;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.FileUtils;

import com.sun.tlddoc.TLDDocGenerator;

/**
 * Generates the tag library documentation.
 * 
 * @author Sergiy Shyrkov
 * 
 * @goal taglibrarydoc
 * @phase site
 * 
 * @since 1.23
 */
public class TagLibraryDocMojo extends AbstractMavenReport implements MavenReport {

    /**
     * The description of the report to be displayed in the Maven Generated
     * Reports page.
     * 
     * @parameter expression="${taglibrarydoc.description}" alias="taglibrarydoc.description"
     */
    private String description;

    /**
     * The name of the destination directory.
     * 
     * @parameter expression="${taglibrarydoc.destDir}" alias="taglibrarydoc.destDir" default-value="tlddocs"
     */
    private String destDir;

    /**
     * The document title for the generated report.
     * 
     * @parameter expression="${taglibrarydoc.docTitle}" alias="taglibrarydoc.docTitle" default-value=
     *            "${project.name} v${project.version} Tag Reference"
     */
    private String docTitle;

    /**
     * Which resource should we exclude?
     * 
     * @parameter expression="${taglibrarydoc.excludes}" alias="taglibrarydoc.excludes"
     */
    private String excludes;

    /**
     * Which resource should we include? By default we only include TLD files
     * without scanning subdirectories.
     * 
     * @parameter expression="${taglibrarydoc.includes}" alias="taglibrarydoc.includes" default-value="*.tld"
     */
    private String includes;

    /**
     * The name of the report to be displayed in the Maven Generated Reports
     * page.
     * 
     * @parameter expression="${taglibrarydoc.name}" alias="taglibrarydoc.name"
     */
    private String name;

    /**
     * Directory where reports will go.
     * 
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     * @readonly
     */
    private String outputDirectory;

    /**
     * The name of the destination directory.
     * 
     * @parameter expression="${taglibrarydoc.outputZipFile}" alias="taglibrarydoc.outputZipFile" default-value="${project.artifactId}-${project.version}-tlddoc.zip"
     * @since 2.53
     */
    private String outputZipFile;

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    private MavenProject project;

    /**
     * Should we be quiet?
     * 
     * @parameter expression="${taglibrarydoc.quiet}" alias="taglibrarydoc.quiet" default-value="false"
     */
    private boolean quiet;

    /**
     * Site renderer component.
     * 
     * @component
     */
    private Renderer siteRenderer;

    /**
     * Source directory to lookup files.
     * 
     * @parameter expression="${taglibrarydoc.srcDir}" alias="taglibrarydoc.srcDir"
     *            default-value="${basedir}/src/main/resources/META-INF"
     */
    private File srcDir;

    /**
     * The window title for the generated report.
     * 
     * @parameter expression="${taglibrarydoc.windowTitle}" alias="taglibrarydoc.windowTitle" default-value=
     *            "${project.name} v${project.version} Tag Reference"
     */
    private String windowTitle;

    @Override
    public boolean canGenerateReport() {
        return srcDir.isDirectory() && !getSourceFiles().isEmpty();
    }

    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        getLog().info("Generating taglib documentation for resources in " + srcDir);

        List<File> files = getSourceFiles();

        if (files.isEmpty()) {
            return;
        }
        
        TLDDocGenerator generator = new TLDDocGenerator();
        generator.setDocTitle(docTitle);
        generator.setWindowTitle(windowTitle);
        File output = new File(getOutputDirectory());
        output.mkdirs();
        generator.setOutputDirectory(output);
        generator.setQuiet(quiet);

        for (Iterator<File> iterator = files.iterator(); iterator.hasNext();) {
            File file = iterator.next();
            String ext = FilenameUtils.getExtension(file.getName());
            if ("tld".equals(ext)) {
                getLog().debug("Adding TLD " + file);
                generator.addTLD(file);
            } else {
                getLog().debug("Adding tag directory " + file.getParentFile());
                generator.addTagDir(file.getParentFile());
            }
        }
        
        try {
            generator.generate();
        } catch (Exception e) {
            getLog().error(e.getMessage(), e);
        }
        
        if (outputZipFile != null && outputZipFile.length() > 0) {
            ZipArchiver zip = new ZipArchiver();
            zip.setDestFile(new File(outputDirectory, outputZipFile));
            DefaultFileSet fs = new DefaultFileSet();
            fs.setDirectory(output);
            try {
                zip.addFileSet(fs);
                zip.createArchive();
            } catch (Exception e) {
                getLog().error(e.getMessage(), e);
            }
        }

        getLog().info("Finished generating taglib documentation");
    }

    private ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle("taglibrarydoc", locale, this.getClass().getClassLoader());
    }

    public String getDescription(Locale locale) {
        return StringUtils.isNotEmpty(description) ? description : getBundle(locale).getString(
                "report.taglibrarydoc.description");
    }

    public String getName(Locale locale) {
        return StringUtils.isNotEmpty(name) ? name : getBundle(locale).getString("report.taglibrarydoc.name");
    }

    @Override
    protected String getOutputDirectory() {
        return new File(outputDirectory, destDir).getPath();
    }

    public String getOutputName() {
        return destDir + "/index";
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    @Override
    protected Renderer getSiteRenderer() {
        return siteRenderer;
    }

    @SuppressWarnings("unchecked")
    private List<File> getSourceFiles() {
        List<File> files = Collections.emptyList();

        try {
            files = FileUtils.getFiles(srcDir, includes, excludes);
            for (Iterator<File> iterator = files.iterator(); iterator.hasNext();) {
                File file = iterator.next();
                if (!file.isFile()) {
                    iterator.remove();
                } else {
                    String ext = FilenameUtils.getExtension(file.getName());
                    if (ext == null || !"tld".equals(ext) && !"tag".equals(ext) && !"tagx".equals(ext)) {
                        iterator.remove();
                    }
                }
            }
        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
        }

        if (files.isEmpty()) {
            getLog().info(
                    "No *.tld, *.tag or *.tagx files found for processing in the folder " + srcDir
                            + " using includes '" + includes + "' and excludes '" + excludes + "'");
        }
        return files;
    }

    @Override
    public boolean isExternalReport() {
        return true;
    }
}
