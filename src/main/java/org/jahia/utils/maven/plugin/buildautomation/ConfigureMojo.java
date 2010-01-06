/**
 *
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2009 Jahia Limited. All rights reserved.
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

package org.jahia.utils.maven.plugin.buildautomation;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jahia.utils.maven.plugin.AbstractManagementMojo;
import org.jahia.utils.maven.plugin.configurators.*;
import org.jahia.utils.maven.plugin.deployers.ServerDeploymentFactory;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Properties;
import java.util.ArrayList;

/**
 * Implementation of the Jahia's configuration Mojo. 
 * User: islam
 * Date: Jul 28, 2008
 * Time: 3:40:32 PM
 *
 * @goal configure
 * @requiresDependencyResolution runtime
 * @requiresProject false
 */
public class ConfigureMojo extends AbstractManagementMojo

{
    /**
     * @parameter expression="${jahia.configure.active}" default-value="false"
     * activates the configure goal
     */
    protected boolean active;

    /**
     * @parameter expression="${jahia.configure.externalConfigPath}"
     * if included, points to a directory that will be merged with the deployed Jahia to allow for custom deployment of configuration and/or extensions
     */
    protected String externalConfigPath;

    // Now for the build automation parameters

    /**
     * @parameter expression="${jahia.configure.deploymentMode}"
     * can be clustered or standalone
     */
    protected String deploymentMode;


    /**
     * @parameter expression="${jahia.configure.configureBeforePackaging}" default-value="false"
     * activates the configuration before the packaging of the WAR file, to allow to build a WAR file already
     * configured.
     */
    protected boolean configureBeforePackaging;


    /**
     * @parameter expression="${basedir}/src/main/webapp"
     * The source directory for the webapp resource when the configureBeforePackaging setting is activated.
     */
    protected String sourceWebAppDir;


    //The following are jahia.properties values that are not present in the skeleton but that
    //do not require to be changed so we set them here but they can easyly be changed

    /**
     * properties file path
     *
     * @parameter default-value="$context/WEB-INF/var/content/filemanager/"
     */
    protected String jahiaFileRepositoryDiskPath;

    /**
     * properties file path
     *
     * @parameter default-value="Testing_release"
     */
    protected String release;
    /**
     * properties file path
     *
     * @parameter default-value="${jahia.configure.localIp}"
     */
    protected String localIp;
    /**
     * properties file path
     *
     * @parameter expression="${jahia.configure.localPort}" default-value="8080"
     */
    protected String localPort;
    /**
     * properties file path
     *
     * @parameter default-value="$context/WEB-INF/etc/"
     */
    protected String jahiaEtcDiskPath;
    /**
     * properties file path
     *
     * @parameter default-value="$context/WEB-INF/var/"
     */
    protected String jahiaVarDiskPath;
    /**
     * properties file path
     *
     * @parameter default-value="$context/WEB-INF/var/new_templates/"
     */
    protected String jahiaNewTemplatesDiskPath;
    /**
     * properties file path
     *
     * @parameter default-value="$context/WEB-INF/var/new_webapps/"
     */
    protected String jahiaNewWebAppsDiskPath;
    /**
     * properties file path
     *
     * @parameter default-value="$context/WEB-INF/var/shared_templates/"
     */
    protected String jahiaSharedTemplatesDiskPath;
    /**
     * properties file path
     *
     * @parameter default-value="$webContext/templates/"
     */
    protected String jahiaTemplatesHttpPath;
    /**
     * properties file path
     *
     * @parameter default-value="$webContext/engines/"
     */
    protected String jahiaEnginesHttpPath;
    /**
     * properties file path
     *
     * @parameter default-value="$webContext/javascript/jahia.js"
     */
    protected String jahiaJavaScriptHttpPath;
    /**
     * properties file path
     *
     * @parameter default-value="http\\://localhost\\:8080/manager"
     */
    protected String jahiaWebAppsDeployerBaseURL;
    /**
     * properties file path
     *
     * @parameter default-value="java\\:comp/env/jdbc/jahia"
     */
    protected String datasource_name;
    /**
     * properties file path
     *
     * @parameter default-value="false"
     */
    protected String outputCacheActivated;
    /**
     * properties file path
     *
     * @parameter default-value="-1"
     */
    protected String outputCacheDefaultExpirationDelay;
    /**
     * properties file path
     *
     * @parameter default-value="false"
     */
    protected String outputCacheExpirationOnly;
    /**
     * properties file path
     *
     * @parameter default-value="true"
     */
    protected String outputContainerCacheActivated;
    /**
     * properties file path
     *
     * @parameter default-value="14400"
     */
    protected String containerCacheDefaultExpirationDelay;
    /**
     * properties file path
     *
     * @parameter default-value="false"
     */
    protected String containerCacheLiveModeOnly;
    /**
     * properties file path
     *
     * @parameter default-value="false"
     */
    protected String esiCacheActivated;
    /**
     * properties file path
     *
     * @parameter expression="${jahia.configure.webAppsDeployerService}" default-value="org.jahia.services.webapps_deployer.JahiaTomcatWebAppsDeployerBaseService"
     */
    protected String Jahia_WebApps_Deployer_Service;
    /**
     * properties file path
     *
     * @parameter default-value="mySite"
     */
    protected String defautSite;
    /**
     * properties file path
     *
     * @parameter default-value="false"
     */
    protected String cluster_activated;
    /**
     * properties file path
     *
     * @parameter expression="${jahia.configure.cluster_node_serverId}" default-value="Jahia1"
     */
    protected String cluster_node_serverId;
    /**
     * properties jahiaRootPassword
     *
     * @parameter expression="${jahia.configure.jahiaRootPassword}" default-value="root1234"
     */
    protected String jahiaRootPassword;
    /**
     * properties file path
     *
     * @parameter expression="${jahia.deploy.processingServer}"
     */
    protected String processingServer;
    /**
     * properties file path
     *
     * @parameter default-value="DBJahiaText"
     */
    protected String bigtext_service;
    /**
     * properties file path
     *
     * @parameter default-value="$context/WEB-INF/var/templates/"
     */
    protected String jahiaFilesTemplatesDiskPath;
    /**
     * properties file path
     *
     * @parameter default-value="$context/WEB-INF/var/imports/"
     */
    protected String jahiaImportsDiskPath;

    /**
     * properties file path
     *
     * @parameter default-value="$context/WEB-INF/var/content/bigtext/"
     */
    protected String jahiaFilesBigTextDiskPath;

    /**
     * List of nodes in the cluster.
     *
     * @parameter
     */
    protected List<String> clusterNodes;

    /**
     * Database type used
     *
     * @parameter expression="${jahia.configure.databaseType}"
     */
    protected String databaseType;

    /**
     * URL to connect to the database
     *
     * @parameter expression="${jahia.configure.databaseUrl}"
     */
    protected String databaseUrl;

    /**
     * List of nodes in the cluster.
     *
     * @parameter expression="${jahia.configure.databaseUsername}"
     */
    protected String databaseUsername;

    /**
     * List of nodes in the cluster.
     *
     * @parameter expression="${jahia.configure.databasePassword}"
     */
    protected String databasePassword;

    /**
     * List of nodes in the cluster.
     *
     * @parameter
     */
    protected List<String> siteImportLocation;

    /**
     * List of nodes in the cluster.
     *
     * @parameter expression="${jahia.configure.overwritedb}"  default-value="true"
     * <p/>
     * This property is here for instance when we are in a clustered mode we don not want the database scripts to be
     * executed for every node
     */
    protected String overwritedb;

    /**
     * properties developmentMode
     *
     * @parameter expression="${jahia.configure.developmentMode}" default-value="false"
     */
    protected String developmentMode;

    /**
     * properties storeFilesInDB
     *
     * @parameter expression="${jahia.configure.storeFilesInDB}" default-value="true"
     */
    protected String storeFilesInDB;

    /**
     * The directory that will be used to store the configured Jahia in. Defaults to the
     * ${jahia.deploy.targetServerDirectory} value.  
     *
     * @parameter expression="${jahia.deploy.targetServerDirectory}"
     */
    protected String targetConfigurationDirectory;

    JahiaPropertiesBean jahiaPropertiesBean;
    DatabaseConnection db;
    File webappDir;
    Properties dbProps;
    File databaseScript;
    List<AbstractConfigurator> configurators = new ArrayList<AbstractConfigurator>();

    public void doExecute() throws MojoExecutionException, MojoFailureException {
        if (active) {
            if (targetConfigurationDirectory == null) {
                targetConfigurationDirectory = targetServerDirectory;
            } else if (!targetConfigurationDirectory.equals(targetServerDirectory)) {
                // Configuration directory and target server directory are not equal, we will use the configuration
                // directory for the configurators.
                ServerDeploymentFactory.setTargetServerDirectory(targetConfigurationDirectory);
            }
            try {
                db = new DatabaseConnection();

                getLog().info ("Configuring for server " + targetServerType + " version " + targetServerVersion + " with database type " + databaseType);

                setProperties();

                if (databaseType.equals("hypersonic")) {
                    try {
                        db.query("SHUTDOWN");
                    } catch (Exception e) {
                        e.printStackTrace();
                        //
                    }
                }


                // copying existing config
                copyExternalConfig();
            } catch (Exception e) {
                throw new MojoExecutionException("Error while configuring Jahia", e);
            }
        }
    }

    private void deployOnCluster() throws MojoExecutionException, MojoFailureException {
        getLog().info(" Deploying in cluster for server in " + webappDir);
        jahiaPropertiesBean.setClusterNodes(clusterNodes);
        jahiaPropertiesBean.setProcessingServer(processingServer);
    }

    private void cleanDatabase() {
        //if it is a mysql, try to drop the database and create a new one  your user must have full rights on this database
        int begindatabasename = databaseUrl.indexOf("/", 13);
        int enddatabaseName = databaseUrl.indexOf("?", begindatabasename);
        String databaseName = "`" + databaseUrl.substring(begindatabasename + 1, enddatabaseName) + "`";  //because of the last '/' we added +1
        try {

            db.query("drop  database if exists " + databaseName);
            db.query("create database " + databaseName);
            db.query("alter database " + databaseName + " charset utf8");
        } catch (Throwable t) {
            // ignore because if this fails it's ok
            getLog().info("error in " + databaseName + " because of" + t);
        }
    }

    private void updateConfigurationFiles(String sourceWebAppPath, String webappPath, Properties dbProps, JahiaPropertiesBean jahiaPropertiesBean) throws Exception {
        getLog().info("Configuring file using source " + sourceWebAppPath + " to target " + webappPath);
        getLog().info("Store files in database is :" + storeFilesInDB);
        new SpringHibernateConfigurator(dbProps, jahiaPropertiesBean).updateConfiguration(sourceWebAppPath + "/WEB-INF/etc/spring/applicationcontext-hibernate.xml", webappPath + "/WEB-INF/etc/spring/applicationcontext-hibernate.xml");
        new QuartzConfigurator(dbProps, jahiaPropertiesBean).updateConfiguration(sourceWebAppPath + "/WEB-INF/etc/config/quartz.properties", webappPath + "/WEB-INF/etc/config/quartz.properties");
        new JackrabbitConfigurator(dbProps, jahiaPropertiesBean).updateConfiguration(sourceWebAppPath + "/WEB-INF/etc/repository/jackrabbit/repository.xml", webappPath + "/WEB-INF/etc/repository/jackrabbit/repository.xml");
        new TomcatContextXmlConfigurator(dbProps, jahiaPropertiesBean).updateConfiguration(sourceWebAppPath + "/META-INF/context.xml", webappPath + "/META-INF/context.xml");
        new RootUserConfigurator(dbProps, jahiaPropertiesBean, encryptPassword(jahiaRootPassword)).updateConfiguration(sourceWebAppPath + "/WEB-INF/etc/repository/root.xml", webappPath + "/WEB-INF/etc/repository/root.xml");
        new WebXmlConfigurator(dbProps, jahiaPropertiesBean).updateConfiguration(sourceWebAppPath + "/WEB-INF/web.xml", webappPath + "/WEB-INF/web.xml");
        new SpringServicesConfigurator(dbProps, jahiaPropertiesBean).updateConfiguration(sourceWebAppPath + "/WEB-INF/etc/spring/applicationcontext-services.xml", webappPath + "/WEB-INF/etc/spring/applicationcontext-services.xml");
        if ("jboss".equalsIgnoreCase(targetServerType)) {
            String datasourcePath = new File(targetServerDirectory, ServerDeploymentFactory.getInstance().getImplementation(targetServerType + targetServerVersion).getDeploymentFilePath("jahia-jboss-config.sar/jahia-ds", "xml")).getPath();
            new JBossDatasourceConfigurator(dbProps, jahiaPropertiesBean).updateConfiguration(datasourcePath, datasourcePath);
        }

        new IndexationPolicyConfigurator(dbProps, jahiaPropertiesBean).updateConfiguration(sourceWebAppPath + "/WEB-INF/etc/spring/applicationcontext-indexationpolicy.xml", webappPath + "/WEB-INF/etc/spring/applicationcontext-indexationpolicy.xml");
        new JahiaPropertiesConfigurator(dbProps, jahiaPropertiesBean).updateConfiguration (sourceWebAppPath + "/WEB-INF/etc/config/jahia.skeleton", webappPath + "/WEB-INF/etc/config/jahia.properties");

    }

    private void setProperties() throws Exception {

        //create the bean that will contain all the necessary information for the building of the Jahia.properties file
        jahiaPropertiesBean = new JahiaPropertiesBean();
        jahiaPropertiesBean.setBigtext_service(bigtext_service);
        jahiaPropertiesBean.setCluster_activated(cluster_activated);
        jahiaPropertiesBean.setCluster_node_serverId(cluster_node_serverId);
        jahiaPropertiesBean.setClusterNodes(null);
        jahiaPropertiesBean.setContainerCacheDefaultExpirationDelay(containerCacheDefaultExpirationDelay);
        jahiaPropertiesBean.setContainerCacheLiveModeOnly(containerCacheLiveModeOnly);
        jahiaPropertiesBean.setDatasource_name(datasource_name);
        jahiaPropertiesBean.setDefautSite(defautSite);
        jahiaPropertiesBean.setEsiCacheActivated(esiCacheActivated);
        jahiaPropertiesBean.setJahia_WebApps_Deployer_Service(getJahia_WebApps_Deployer_Service());
        jahiaPropertiesBean.setJahiaEnginesHttpPath(jahiaEnginesHttpPath);
        jahiaPropertiesBean.setJahiaEtcDiskPath(jahiaEtcDiskPath);
        jahiaPropertiesBean.setJahiaFileRepositoryDiskPath(jahiaFileRepositoryDiskPath);
        jahiaPropertiesBean.setJahiaFilesBigTextDiskPath(jahiaFilesBigTextDiskPath);
        jahiaPropertiesBean.setJahiaFilesTemplatesDiskPath(jahiaFilesTemplatesDiskPath);
        jahiaPropertiesBean.setJahiaImportsDiskPath(jahiaImportsDiskPath);
        jahiaPropertiesBean.setJahiaJavaScriptHttpPath(jahiaJavaScriptHttpPath);
        jahiaPropertiesBean.setJahiaNewTemplatesDiskPath(jahiaNewTemplatesDiskPath);
        jahiaPropertiesBean.setJahiaNewWebAppsDiskPath(jahiaNewWebAppsDiskPath);
        jahiaPropertiesBean.setJahiaSharedTemplatesDiskPath(jahiaSharedTemplatesDiskPath);
        jahiaPropertiesBean.setJahiaTemplatesHttpPath(jahiaTemplatesHttpPath);
        jahiaPropertiesBean.setJahiaVarDiskPath(jahiaVarDiskPath);
        jahiaPropertiesBean.setJahiaWebAppsDeployerBaseURL(jahiaWebAppsDeployerBaseURL);
        jahiaPropertiesBean.setLocalIp(localIp);
        jahiaPropertiesBean.setOutputCacheActivated(outputCacheActivated);
        jahiaPropertiesBean.setOutputCacheDefaultExpirationDelay(outputCacheDefaultExpirationDelay);
        jahiaPropertiesBean.setOutputCacheExpirationOnly(outputCacheExpirationOnly);
        jahiaPropertiesBean.setOutputContainerCacheActivated(outputContainerCacheActivated);
        jahiaPropertiesBean.setOutputContainerCacheActivated(outputContainerCacheActivated);
        jahiaPropertiesBean.setProcessingServer(processingServer);
        jahiaPropertiesBean.setRelease(release);
        jahiaPropertiesBean.setServer(targetServerType);
        jahiaPropertiesBean.setLocalIp(localIp);
        jahiaPropertiesBean.setLocalPort(localPort);
        jahiaPropertiesBean.setDb_script(databaseType + ".script");
        jahiaPropertiesBean.setDevelopmentMode(developmentMode);

        if (cluster_activated.equals("true")) {
            deployOnCluster();
        } else {
            getLog().info("Deployed in standalone for server in " + webappDir);
        }

        //now set the common properties to both a clustered environment and a standalone one

        webappDir = getWebAppTargetConfigurationDir();
        String sourceWebappPath = webappDir.toString();
        if (configureBeforePackaging) {
            sourceWebappPath = sourceWebAppDir;
            getLog().info("Configuration before WAR packaging is active, will look for configuration files in directory " + sourceWebappPath + " and store the modified files in " + webappDir);
        }

        dbProps = new Properties();
        //database script always ends with a .script
        databaseScript = new File(sourceWebappPath + "/WEB-INF/var/db/" + databaseType + ".script");
        try {
            dbProps.load(new FileInputStream(databaseScript));
            // we override these just as the configuration wizard does
            dbProps.put("storeFilesInDB", storeFilesInDB);
            dbProps.put("jahia.database.url", databaseUrl);
            dbProps.put("jahia.database.user", databaseUsername);
            dbProps.put("jahia.database.pass", databasePassword);
        } catch (IOException e) {
            getLog().error("Error in loading database settings because of " + e);
        }

        jahiaPropertiesBean.setHibernateDialect(dbProps.getProperty("jahia.database.hibernate.dialect"));
        jahiaPropertiesBean.setNestedTransactionAllowed(dbProps.getProperty("jahia.nested_transaction_allowed"));
        
        getLog().info("Updating configuration files...");

        //updates jackrabbit, quartz and spring files
        updateConfigurationFiles(sourceWebappPath, webappDir.getPath(), dbProps, jahiaPropertiesBean);
        getLog().info("creating database tables and copying license file");
        try {
            copyLicense(sourceWebappPath + "/WEB-INF/etc/config/licenses/license-free.xml", webappDir.getPath() + "/WEB-INF/etc/config/license.xml");
            if (overwritedb.equals("true")) {
                if (!databaseScript.exists()) {
                    getLog().info("cannot find script in " + databaseScript.getPath());
                    throw new MojoExecutionException("Cannot find script for database " + databaseType);
                }
                db.databaseOpen(dbProps.getProperty("jahia.database.driver"), databaseUrl, databaseUsername, databasePassword);
                if (databaseType.equals("mysql")) {
                    getLog().info("database is mysql trying to drop it and create a new one");
                    cleanDatabase();
                    //you have to reopen the database connection as before you just dropped the database
                    db.databaseOpen(dbProps.getProperty("jahia.database.driver"), databaseUrl, databaseUsername, databasePassword);
                }
                createDBTables(databaseScript);
                insertDBCustomContent();
            }

            if (!configureBeforePackaging) {
                deleteRepositoryAndIndexes();
                if ("tomcat".equals(targetServerType)) {
                    deleteTomcatFiles();
                }
                if (siteImportLocation != null) {
                    getLog().info("copying site Export to the " + webappDir + "/WEB-INF/var/imports");
                    importSites();
                } else {
                    getLog().info("no site import found ");
                }
            }

        } catch (Exception e) {
            getLog().error("exception in setting the properties because of " + e, e);
        }
    }

    private void importSites() {
        for (int i = 0; i < siteImportLocation.size(); i++) {
            try {
                copy(siteImportLocation.get(i), webappDir + "/WEB-INF/var/imports");
            } catch (IOException e) {
                getLog().error("error in copying siteImport file " + e);
            }
        }
    }


    private void cleanDirectory(File toDelete) {
        if (toDelete.exists()) {
            try {
                FileUtils.cleanDirectory(toDelete);
            } catch (IOException e) {
                getLog().error(
                        "Error deleting content of the folder '" + toDelete
                                + "'. Cause: " + e.getMessage(), e);
            }
        }
    }
    
    private void deleteTomcatFiles() {

        cleanDirectory(new File(targetServerDirectory + "/temp"));
        cleanDirectory(new File(targetServerDirectory + "/work"));
        getLog().info("finished deleting content of Tomcat's /temp and /work folders");
    }

    private void deleteRepositoryAndIndexes() {

        try {
            File[] files = new File(webappDir + "/WEB-INF/var/repository")
                    .listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return !"indexing_configuration.xml".equals(name);
                        }
                    });
            if (files != null) {
                for (File file : files) {
                    FileUtils.forceDelete(file);
                }
            }
        } catch (IOException e) {
            getLog().error(
                    "Error deleting content of the Jahia's /repository folder. Cause: "
                            + e.getMessage(), e);
        }

        cleanDirectory(new File(webappDir + "/WEB-INF/var/search_indexes"));

        cleanDirectory(new File(webappDir + "/templates"));

        getLog().info("finished deleting content of the /var/repository and /var/search_indexes folders");
    }

    //copy method for the license for instance
    private void copyLicense(String fromFileName, String toFileName)
            throws IOException {
        File fromFile = new File(fromFileName);
        File toFile = new File(toFileName);

        if (!fromFile.exists())
            throw new IOException("FileCopy: " + "no such source file: "
                    + fromFileName);
        if (!fromFile.isFile())
            throw new IOException("FileCopy: " + "can't copy directory: "
                    + fromFileName);
        if (!fromFile.canRead())
            throw new IOException("FileCopy: " + "source file is unreadable: "
                    + fromFileName);

        if (toFile.isDirectory()) {
            toFile = new File(toFile, fromFile.getName());

        }

        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = from.read(buffer)) != -1)
                to.write(buffer, 0, bytesRead); // write
        } finally {
            if (from != null)
                try {
                    from.close();
                } catch (IOException e) {
                }
            if (to != null)
                try {
                    to.close();
                } catch (IOException e) {
                }
        }
    }


    private void copy(String fromFileName, String toFileName)
            throws IOException {
        File fromFile = new File(fromFileName);
        File toFile = new File(toFileName);

        if (!fromFile.exists())
            throw new IOException("FileCopy: " + "no such source file: "
                    + fromFileName);
        if (!fromFile.isFile())
            throw new IOException("FileCopy: " + "can't copy directory: "
                    + fromFileName);
        if (!fromFile.canRead())
            throw new IOException("FileCopy: " + "source file is unreadable: "
                    + fromFileName);
        toFile.mkdir();
        if (toFile.isDirectory()) {
            toFile = new File(toFile, fromFile.getName());

        }

        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = from.read(buffer)) != -1)
                to.write(buffer, 0, bytesRead); // write
        } finally {
            if (from != null)
                try {
                    from.close();
                } catch (IOException e) {
                }
            if (to != null)
                try {
                    to.close();
                } catch (IOException e) {
                }
        }
    }

    private void createDBTables(File dbScript) throws Exception {

        List<String> sqlStatements;

        DatabaseScripts scripts = new DatabaseScripts();

// get script runtime...
        try {
            sqlStatements = scripts.getSchemaSQL(dbScript);
        } catch (Exception e) {
            throw e;
        }
// drop each tables (if present) and (re-)create it after...
        for (String line : sqlStatements) {
            final String lowerCaseLine = line.toLowerCase();
            final int tableNamePos = lowerCaseLine.indexOf("create table");
            if (tableNamePos != -1) {
                final String tableName = line.substring("create table".length() +
                        tableNamePos,
                        line.indexOf("(")).trim();
//getLog().info("Creating table [" + tableName + "] ...");
                try {
                    db.query("DROP TABLE " + tableName);
                } catch (Throwable t) {
                    // ignore because if this fails it's ok
                    getLog().debug("Drop failed on " + tableName + " because of " + t + " but that's acceptable...");
                }
            }
            try {
                db.query(line);
            } catch (Exception e) {
                // first let's check if it is a DROP TABLE query, if it is,
                // we will just fail silently.

                String upperCaseLine = line.toUpperCase().trim();
                String errorMsg = "Error while trying to execute query: " + line + ". Cause: " + e.getMessage();
                if (upperCaseLine.startsWith("DROP ") || upperCaseLine.contains(" DROP ") || upperCaseLine.contains("\nDROP ") || upperCaseLine.contains(" DROP\n") || upperCaseLine.contains("\nDROP\n")) {
                    getLog().debug(errorMsg, e);
                } else if (upperCaseLine.startsWith("ALTER TABLE") || upperCaseLine.startsWith("CREATE INDEX")){
                    if (getLog().isDebugEnabled()) {
                        getLog().warn(errorMsg, e);
                    } else {
                        getLog().warn(errorMsg);
                    }
                } else {
                    getLog().error(errorMsg, e);
                    throw e;
                }
            }
        }


    }
// end createDBTables()


    /**
     * Insert database custom data, like root user and properties.
     */
    private void insertDBCustomContent() throws Exception {

        getLog().debug("Inserting customized settings into database...");

// get two keys...
        final String rootName = "root";

        final String password = encryptPassword(jahiaRootPassword);   //root1234
        getLog().info("Encrypted root password for jahia is " + jahiaRootPassword);
        final int siteID0 = 0;
        final String rootKey = rootName + ":" + siteID0;
        final String grpKey0 = "administrators" + ":" + siteID0;
        final String grpKey1 = "users" + ":" + siteID0;
        final String grpKey2 = "guest" + ":" + siteID0;

// query insert root user...
        db.queryPreparedStatement("INSERT INTO jahia_users(id_jahia_users, name_jahia_users, password_jahia_users, key_jahia_users) VALUES(0,?,?,?)",
                new Object[]{rootName, password, rootKey});

// query insert root first name...
        db.queryPreparedStatement("INSERT INTO jahia_user_prop(id_jahia_users, name_jahia_user_prop, value_jahia_user_prop, provider_jahia_user_prop, userkey_jahia_user_prop) VALUES(0, 'firstname', ?, 'jahia',?)",
                new Object[]{"root", rootKey});

// query insert root last name...
        db.queryPreparedStatement("INSERT INTO jahia_user_prop(id_jahia_users, name_jahia_user_prop, value_jahia_user_prop, provider_jahia_user_prop, userkey_jahia_user_prop) VALUES(0, 'lastname', ?, 'jahia',?)",
                new Object[]{"", rootKey});

// query insert root e-mail address...
        db.queryPreparedStatement("INSERT INTO jahia_user_prop(id_jahia_users, name_jahia_user_prop, value_jahia_user_prop, provider_jahia_user_prop, userkey_jahia_user_prop) VALUES(0, 'email', ?, 'jahia',?)",
                new Object[]{(String) "", rootKey});

// query insert administrators group...
        db.queryPreparedStatement("INSERT INTO jahia_grps(id_jahia_grps, name_jahia_grps, key_jahia_grps, siteid_jahia_grps) VALUES(0,?,?,null)",
                new Object[]{"administrators", grpKey0});

// query insert users group...
        db.queryPreparedStatement("INSERT INTO jahia_grps(id_jahia_grps, name_jahia_grps, key_jahia_grps, siteid_jahia_grps) VALUES(1,?,?,null)",
                new Object[]{"users", grpKey1});

// query insert guest group...
        db.queryPreparedStatement("INSERT INTO jahia_grps(id_jahia_grps, name_jahia_grps, key_jahia_grps, siteid_jahia_grps) VALUES(2,?,?,null)",
                new Object[]{"guest", grpKey2});


// query insert administrators group access...
        db.queryPreparedStatement("INSERT INTO jahia_grp_access(id_jahia_member, id_jahia_grps, membertype_grp_access) VALUES(?,?,1)",
                new Object[]{rootKey, grpKey0});

// create guest user
        db.queryPreparedStatement("INSERT INTO jahia_users(id_jahia_users, name_jahia_users, password_jahia_users, key_jahia_users) VALUES(1,?,?,?)",
                new Object[]{"guest", "*", "guest:0"});

//db.queryPreparedStatement("INSERT INTO jahia_version(install_number, build, release_number, install_date) VALUES(0, ?,?,?)",
//new Object[] { new Integer(Jahia.getBuildNumber()), Jahia.getReleaseNumber() + "." + Jahia.getPatchNumber(), new Timestamp(System.currentTimeMillis()) } );
    }
// end insertDBCustomContent()

    /**
         * Copy the external config
         *
         * @throws IOException
         * @throws MojoExecutionException
         * @return true if an external config has been found

         */
        private boolean copyExternalConfig()
                throws Exception {
            if (externalConfigPath == null) {
                getLog().info("External jahia config. not specified.");
                return false;

            }
            File externalConfigDirectory = new File(externalConfigPath);
            if (!externalConfigDirectory.exists()) {
                getLog().warn("Not copying external jahia config. Directory[" + externalConfigDirectory.getAbsolutePath()
                        + "] does not exist!");
                return false;
            }

            getLog().info("Copying external jahia config. directory [" + externalConfigDirectory.getAbsolutePath() + "] to [" + getWebappDeploymentDir().getAbsolutePath() + "]");
            String[] fileNames = getFilesToCopy(externalConfigDirectory);
            for (int i = 0; i < fileNames.length; i++) {
                copyFile(new File(externalConfigDirectory, fileNames[i]), new File(getWebappDeploymentDir(), fileNames[i]));
            }
            return true;
        }

        /**
         * Returns a list of filenames that should be copied
         * over to the destination directory.
         *
         * @param directory the parent diretory to be scanned
         * @return the array of filenames, relative to the sourceDir
         */
        private String[] getFilesToCopy(File directory) {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(directory);
            scanner.addDefaultExcludes();
            scanner.scan();
            return scanner.getIncludedFiles();
        }


        /**
         * Copy file from source to destination
         *
         * @param source
         * @param destination
         * @return
         * @throws IOException
         */
        private boolean copyFile(File source, File destination) {
            try {
                boolean doOverride = destination.exists();
                FileUtils.copyFile(source.getCanonicalFile(), destination);
                // preserve timestamp
                destination.setLastModified(source.lastModified());

                if (!doOverride) {
                    getLog().debug(" + [" + source.getPath() + "] has been copied to [" + destination.getAbsolutePath()+"]");
                } else {
                    getLog().debug(" o [" + destination.getAbsolutePath() + "] has been overrided by " + source.getPath());
                }
            } catch (Exception e) {
                getLog().error(" + Unable to copy" + source.getPath(),e);

            }
            return true;
        }



    public String encryptPassword(String password) {
        if (password == null) {
            return null;
        }

        if (password.length() == 0) {
            return null;
        }

        String result = null;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            if (md != null) {
                md.reset();
                md.update(password.getBytes());
                result = new String(Base64.encodeBase64(md.digest()));
            }
            md = null;
        } catch (NoSuchAlgorithmException ex) {

            result = null;
        }

        return result;
    }

    public String getJahia_WebApps_Deployer_Service() {
        return Jahia_WebApps_Deployer_Service;
    }

    public void setJahia_WebApps_Deployer_Service(String jahia_WebApps_Deployer_Service) {
        Jahia_WebApps_Deployer_Service = jahia_WebApps_Deployer_Service;
    }

    /**
     * We override this method so that we can have, in the same project, both a targetServerDirectory and a
     * targetConfigurationDirectory that are not the same, so that we can configure in a directory, and then deploy
     * to another directory.
     * @return if the target server directory and configuration directory are the same, the targetServerDirectory value
     * is returned, otherwise the targetConfigurationDirectory value is return.
     * @throws Exception can be raised if there is a problem initializing the deployer classes.
     */
    public File getWebAppTargetConfigurationDir () throws Exception {
        if (targetServerDirectory.equals(targetConfigurationDirectory)) {
            return getWebappDeploymentDir();
        } else {
            return new File(targetConfigurationDirectory);
        }
    }

}
