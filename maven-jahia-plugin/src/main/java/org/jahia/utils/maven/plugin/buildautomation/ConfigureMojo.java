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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jahia.utils.maven.plugin.AbstractManagementMojo;
import org.jahia.utils.maven.plugin.MojoLogger;
import org.jahia.utils.maven.plugin.configurators.AbstractConfigurator;
import org.jahia.utils.maven.plugin.configurators.DatabaseConnection;
import org.jahia.utils.maven.plugin.configurators.JahiaConfigInterface;
import org.jahia.utils.maven.plugin.configurators.JahiaGlobalConfigurator;

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
public class ConfigureMojo extends AbstractManagementMojo implements JahiaConfigInterface {

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
     * @parameter expression="${basedir}/src/main/webapp"
     * The source directory for the webapp resource when the configureBeforePackaging setting is activated.
     */
    protected String sourceWebAppDir;


    //The following are jahia.properties values that are not present in the skeleton but that
    //do not require to be changed so we set them here but they can easyly be changed

    /**
     * properties file path
     *
     * @parameter default-value="Testing_release"
     */
    protected String release;
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
     * @parameter default-value="$context/WEB-INF/var/shared_modules/"
     */
    protected String jahiaSharedTemplatesDiskPath;
    /**
     * properties file path
     *
     * @parameter default-value="$webContext/modules/"
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
     * @parameter expression="${jahia.configure.jahiaRootUsername}" default-value="root"
     */
    protected String jahiaRootUsername;
    /**
     * properties jahiaRootPassword
     *
     * @parameter expression="${jahia.configure.jahiaRootPassword}" default-value="root1234"
     */
    protected String jahiaRootPassword;
    /**
     * @parameter expression="${jahia.configure.jahiaRootFirstname}" default-value=""
     */
    protected String jahiaRootFirstname;
    /**
     * @parameter expression="${jahia.configure.jahiaRootLastname}" default-value=""
     */
    protected String jahiaRootLastname;
    /**
     * @parameter expression="${jahia.configure.jahiaRootEmail}" default-value=""
     */
    protected String jahiaRootEmail;
    /**
     * properties file path
     *
     * @parameter expression="${jahia.deploy.processingServer}"
     */
    protected String processingServer;
    /**
     * properties file path
     *
     * @parameter default-value="$context/WEB-INF/var/imports/"
     */
    protected String jahiaImportsDiskPath;

    /**
     * List of nodes in the cluster.
     *
     * @parameter
     */
    protected List<String> clusterNodes;
    
    /**
     * @parameter expression="${jahia.configure.cluster.tcp.start.ip_address}" default-value="192.168.1.100"
     */
    protected String clusterStartIpAddress;

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

    /**
     * @parameter expression="${jahia.configure.mailServer}" default-value=""
     */
    protected String mailServer;
    
    /**
     * @parameter expression="${jahia.configure.mailAdministrator}" default-value=""
     */
    protected String mailAdministrator;
    
    /**
     * @parameter expression="${jahia.configure.mailFrom}" default-value=""
     */
    protected String mailFrom;
    
    /**
     * @parameter expression="${jahia.configure.mailParanoia}" default-value="Disabled"
     */
    protected String mailParanoia;
    
    /**
     * @parameter expression="${jahia.deploy.war.contextPath}" default-value=""
     */
    protected String contextPath;
    
    // JahiaConfigBean jahiaPropertiesBean;
    DatabaseConnection db;
    File webappDir;
    Properties dbProps;
    File databaseScript;
    List<AbstractConfigurator> configurators = new ArrayList<AbstractConfigurator>();

    public void doExecute() throws MojoExecutionException, MojoFailureException {
        try {
            JahiaGlobalConfigurator jahiaGlobalConfigurator = new JahiaGlobalConfigurator(new MojoLogger(getLog()),
                    this);
            jahiaGlobalConfigurator.execute();
        } catch (Exception e) {
            throw new MojoExecutionException("Error while configuring Jahia", e);
        }
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

    public String getCluster_activated() {
        return cluster_activated;
    }

    public String getCluster_node_serverId() {
        return cluster_node_serverId;
    }

    public List<String> getClusterNodes() {
        return clusterNodes;
    }

    public List<AbstractConfigurator> getConfigurators() {
        return configurators;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public File getDatabaseScript() {
        return databaseScript;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public String getDatabaseUsername() {
        return databaseUsername;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public String getDeploymentMode() {
        return deploymentMode;
    }

    public String getDevelopmentMode() {
        return developmentMode;
    }

    public String getTargetServerDirectory() {
        return targetServerDirectory;
    }

    public String getTargetServerType() {
        return targetServerType;
    }

    public String getTargetServerVersion() {
        return targetServerVersion;
    }

    public String getExternalConfigPath() {
        return externalConfigPath;
    }

    public String getWebAppDirName() {
        return webAppDirName;
    }

    public String getJahiaEnginesHttpPath() {
        return jahiaEnginesHttpPath;
    }

    public String getJahiaEtcDiskPath() {
        return jahiaEtcDiskPath;
    }

    public String getJahiaImportsDiskPath() {
        return jahiaImportsDiskPath;
    }

    public String getJahiaJavaScriptHttpPath() {
        return jahiaJavaScriptHttpPath;
    }

    public String getJahiaWebAppsDeployerBaseURL() {
        return jahiaWebAppsDeployerBaseURL;
    }

    public String getJahiaRootPassword() {
        return jahiaRootPassword;
    }

    public String getJahiaSharedTemplatesDiskPath() {
        return jahiaSharedTemplatesDiskPath;
    }

    public String getJahiaTemplatesHttpPath() {
        return jahiaTemplatesHttpPath;
    }

    public String getJahiaVarDiskPath() {
        return jahiaVarDiskPath;
    }

    public String getDb_script() {
        return getDatabaseType() + ".script";
    }

    public String getOverwritedb() {
        return overwritedb;
    }

    public String getProcessingServer() {
        return processingServer;
    }

    public String getRelease() {
        return release;
    }

    public String getServer() {
        return targetServerType;
    }

    public List<String> getSiteImportLocation() {
        return siteImportLocation;
    }

    public String getSourceWebAppDir() {
        return sourceWebAppDir;
    }

    public String getStoreFilesInDB() {
        return storeFilesInDB;
    }

    public String getTargetConfigurationDirectory() {
        return targetConfigurationDirectory;
    }

    public void setTargetConfigurationDirectory(String targetConfigurationDirectory) {
        this.targetConfigurationDirectory = targetConfigurationDirectory;
    }

    public String getJahiaRootEmail() {
        return jahiaRootEmail;
    }

    public String getJahiaRootFirstname() {
        return jahiaRootFirstname;
    }

    public String getJahiaRootLastname() {
        return jahiaRootLastname;
    }

    public String getJahiaRootUsername() {
        return jahiaRootUsername;
    }

    public String getMailAdministrator() {
        return mailAdministrator;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public String getMailParanoia() {
        return mailParanoia;
    }

    public String getMailServer() {
        return mailServer;
    }

    /**
     * @return the contextPath
     */
    public String getContextPath() {
        return contextPath;
    }

    public String getClusterStartIpAddress() {
        return clusterStartIpAddress;
    }


}
