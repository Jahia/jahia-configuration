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
package org.jahia.maven.hbm2ddl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaExport.Type;
import org.hibernate.tool.hbm2ddl.Target;

/**
 * Exports database schema creation scripts using JPA Hibernate configuration.<br>
 * 
 * @goal jpa-schema-export
 * @phase process-classes
 * @requiresProject
 * @requiresDependencyResolution compile+runtime
 * @description Performs Hibernate's schema export to DDL using JPA (persistence.xml) configuration.
 * @author Sergiy Shyrkov
 */
@SuppressWarnings("deprecation")
public class JpaSchemaExportMojo extends AbstractMojo {

    /**
     * The Hibernate dialect to use
     * 
     * @parameter
     */
    private String hibernateDialect;

    /**
     * The Hibernate naming strategy
     * 
     * @parameter
     */
    private String hibernateNamingStrategy;

    /**
     * The output file to export DDL into
     * 
     * @parameter default-value="${project.build.directory}/schema.sql"
     */
    private File outputFile;
    
    /**
     * The alternative file name of the persistence.xml resource in case it is neede to override it.
     * 
     * @parameter
     */
    private String persistenceFileName;

    /**
     * The name of the persistence unit to export.
     * 
     * @parameter
     */
    private String persistenceUnitName;

    /**
     * The maven project
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Statement type to be exported
     * 
     * @parameter
     */
    private SchemaExport.Type statementType = Type.BOTH;

    private void configureNamingStrategy(Configuration cfg) throws MojoExecutionException {
        if (hibernateNamingStrategy == null || hibernateNamingStrategy.length() == 0) {
            return;
        }

        try {
            cfg.setNamingStrategy((NamingStrategy) Thread.currentThread().getContextClassLoader()
                    .loadClass(hibernateNamingStrategy).newInstance());

            getLog().info("Using naming strategy: " + hibernateNamingStrategy);
        } catch (Exception e) {
            getLog().error("Unable to instantiate the class for the naming strategy: " + hibernateNamingStrategy, e);
            throw new MojoExecutionException("Unable to instantiate the class for the naming strategy: "
                    + hibernateNamingStrategy, e);
        }
    }

    private ClassLoader createClassLoader(ClassLoader contextClassLoader) throws MojoExecutionException {
        try {
            List<String> locations = new LinkedList<String>(project.getCompileClasspathElements());
            locations.addAll(project.getRuntimeClasspathElements());
            locations.add(project.getBuild().getOutputDirectory());
            List<URL> urls = new LinkedList<URL>();
            for (String location : locations) {
                urls.add(new File(location).toURI().toURL());
            }
            if (getLog().isDebugEnabled()) {
                getLog().debug("Using following locations for class loader:\n" + locations);
            }
            URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[] {}), getClass().getClassLoader());
            if (persistenceFileName == null) {
                return urlClassLoader;
            } else {
                URL persistenceUrl = urlClassLoader.getResource("META-INF/" + persistenceFileName);
                if (persistenceUrl != null) {
                    File outputFile = new File(project.getBuild().getOutputDirectory(), "META-INF/persistence.xml");
                    getLog().info("Copying " + persistenceFileName + " from " + persistenceUrl + " to " + outputFile);
                    InputStream is = null;
                    OutputStream os = null;
                    try {
                        outputFile.getParentFile().mkdirs();
                        is = persistenceUrl.openStream();
                        os = new BufferedOutputStream(new FileOutputStream(outputFile));
                        IOUtil.copy(is, os);
                    } finally {
                        IOUtil.close(is);
                        IOUtil.close(os);
                    }
                }
                
                return urlClassLoader;
            }
        } catch (Exception e) {
            getLog().error("Unable to create class loader", e);
            throw new MojoExecutionException("Unable to create class loader", e);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info(
                "Performing DDL schema export for persistence unit '" + persistenceUnitName + "' using dialect '"
                        + hibernateDialect + "'. Output file is: " + outputFile);

        File outputFolder = outputFile.getParentFile();
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader customClassLoader = createClassLoader(contextClassLoader);
            // override context class loader
            Thread.currentThread().setContextClassLoader(customClassLoader);

            performExport();
        } finally {
            // reset context class loader
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private Map<String, String> getHibernateProperties() {
        if (hibernateDialect != null && hibernateDialect.length() > 0) {
            Map<String, String> props = new HashMap<String, String>(2);
            props.put(AvailableSettings.DIALECT, hibernateDialect);
            props.put(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "false");
            getLog().info("Using the following Hibernate dialect: " + hibernateDialect);
            return props;

        }
        return null;
    }

    private void performExport() throws MojoExecutionException {
        final Configuration cfg = new Ejb3Configuration().configure(persistenceUnitName, getHibernateProperties())
                .getHibernateConfiguration();

        configureNamingStrategy(cfg);

        SchemaExport schemaExport = new SchemaExport(cfg);
        schemaExport.setDelimiter(";");
        schemaExport.setOutputFile(outputFile.getAbsolutePath());
        schemaExport.execute(Target.SCRIPT, statementType);
    }

}
