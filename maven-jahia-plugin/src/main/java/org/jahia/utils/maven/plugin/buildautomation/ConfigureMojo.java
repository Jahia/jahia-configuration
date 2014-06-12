/**
 *
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2014 Jahia Limited. All rights reserved.
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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jahia.configuration.configurators.JahiaConfigInterface;
import org.jahia.utils.maven.plugin.AbstractManagementMojo;
import org.jahia.utils.maven.plugin.MojoLogger;
import org.jahia.configuration.configurators.JahiaGlobalConfigurator;

/**
 * Configure the deployed Jahia instance
 * Basically set everything that can be found in the jahia.properties and jahia.advance.properties files.
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
     * @parameter default-value="$context/WEB-INF/var/modules/"
     */
    protected String jahiaModulesDiskPath;
    /**
     * properties file path
     *
     * @parameter default-value="http\\://localhost\\:8080/manager"
     */
    protected String jahiaWebAppsDeployerBaseURL;
    /**
     * properties file path
     *
     * @parameter expression="${jahia.configure.webAppsDeployerService}" default-value="org.jahia.services.webapps_deployer.JahiaTomcatWebAppsDeployerBaseService"
     */
    protected String Jahia_WebApps_Deployer_Service;
    /**
     * properties file path
     *
     * @parameter expression="${jahia.configure.cluster_activated}" default-value="false"
     */
    protected String cluster_activated;
    /**
     * properties file path
     *
     * @parameter expression="${jahia.configure.cluster_node_serverId}"
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
     * @parameter expression="${jahia.configure.jahiaRootPreferredLang}" default-value="en"
     */
    protected String jahiaRootPreferredLang;
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
     * @parameter expression="${jahia.configure.processingServer}" default-value="true"
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
     * @parameter expression="${jahia.configure.clusterNodes}" default-value="192.168.1.100 192.168.1.200"
     */
    protected String clusterNodes;
    
    /**
     * @parameter expression="${jahia.configure.cluster.tcp.start.ip_address}" default-value="192.168.1.100"
     */
    protected String clusterStartIpAddress;

    /**
     * TCP binding port for the EHCache Hibernate channel for this node
     *
     * @parameter expression="${jahia.configure.clusterTCPEHCacheHibernatePort}" default-value="7860"
     */
    protected String clusterTCPEHCacheHibernatePort;

    /**
     * The TCP bind address to start server socket on
     *
     * @parameter expression="${jahia.configure.clusterTCPBindAddress}"
     */
    protected String clusterTCPBindAddress;
    
    /**
     * The TCP port to bind to
     *
     * @parameter expression="${jahia.configure.clusterTCPBindPort}"
     */
    protected String clusterTCPBindPort;
    
    /**
     * TCP binding port for the EHCache Jahia channel for this node
     *
     * @parameter expression="${jahia.configure.clusterTCPEHCacheJahiaPort}" default-value="7870"
     */
    protected String clusterTCPEHCacheJahiaPort;

    /**
     * List of EHCache Hibernate channel hosts to connect to (number of values must be equal to cluster nodes count)
     *
     * @parameter expression="${jahia.configure.clusterTCPEHCacheHibernateHosts}" default-value=""
     */
    protected String clusterTCPEHCacheHibernateHosts;

    /**
     * List of EHCache Jahia channel hosts to connect to (number of values must be equal to cluster nodes count)
     *
     * @parameter expression="${jahia.configure.clusterTCPEHCacheJahiaHosts}" default-value=""
     */
    protected String clusterTCPEHCacheJahiaHosts;

    /**
     * Database type used
     *
     * @parameter expression="${jahia.configure.databaseType}" default-value="derby_embedded"
     */
    protected String databaseType;

    /**
     * URL to connect to the database
     *
     * @parameter expression="${jahia.configure.databaseUrl}" default-value="jdbc:derby:directory:jahia"
     */
    protected String databaseUrl;

    /**
     * List of nodes in the cluster.
     *
     * @parameter expression="${jahia.configure.databaseUsername}" default-value=""
     */
    protected String databaseUsername;

    /**
     * List of nodes in the cluster.
     *
     * @parameter expression="${jahia.configure.databasePassword}" default-value=""
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
     * properties storeFilesInDB
     *
     * @parameter expression="${jahia.configure.storeFilesInDB}" default-value="false"
     */
    protected String storeFilesInDB;

    /**
     * The directory that will be used to store the configured Jahia in if the target server directory has to be overidden.
     * 
     * @parameter expression="${jahia.deploy.targetConfigurationDirectory}"
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
     * @parameter expression="${jahia.configure.toolManagerUsername}" default-value="jahia"
     */
    protected String jahiaToolManagerUsername;

    /**
     * @parameter expression="${jahia.configure.toolManagerPassword}" default-value="password"
     */
    protected String jahiaToolManagerPassword;

    /**
     * @parameter expression="${jahia.configure.ldapActivated}" default-value="false"
     */
    protected String ldapActivated;

    /**
     * @parameter expression="${jahia.configure.groupLdapProviderProperties}"
     */
    protected Map<String, String> groupLdapProviderProperties;

    /**
     * @parameter expression="${jahia.configure.userLdapProviderProperties}"
     */
    protected Map<String, String> userLdapProviderProperties;

    /**
     * @parameter expression="${jahia.configure.operatingMode}" default-value="development"
     */
    protected String operatingMode;


    /**
     * Activates configuration externalization. Make sure to specify a value for the externalizedConfigTargetPath that
     * will indicate where the configuration JAR should be generated.
     *
     * @parameter expression="${jahia.configure.externalizedActivated}" default-value="false"
     */
    protected boolean externalizedConfigActivated;
    
    /**
     * If active, the externalized configuration is deployed exploded.
     * 
     * @parameter expression="${jahia.configure.externalizedExploded}" default-value="false"
     */
    protected boolean externalizedConfigExploded;

    /**
     * ACtivates the feature for externalizing the location of WEB-INF/var folder.
     * 
     * @parameter expression="${jahia.configure.externalizedDataActivated}"
     */
    protected boolean externalizedDataActivated;
    
    /**
     * The location where Jahia will move the WEB-INF/var folder to. If this path is set, the corresponding entries in jahia.properties
     * (jahiaVarDiskPath, jahiaModulesDiskPath, jahiaImportsDiskPath, modulesSourcesDiskPath, jahia.jackrabbit.home etc.) will be adjusted
     * to point to that location.
     * 
     * @parameter expression="${jahia.configure.externalizedDataTargetPath}"
     */
    protected String externalizedDataTargetPath;

    /**
     * The location at which the externalized configuration JAR will be generated.
     * @parameter expression="${jahia.configure.externalizedTargetPath}"
     */
    protected String externalizedConfigTargetPath;

    /**
     * Allows to specify a classifier on the configuration, usually used to identify cluster node configurations, such
     * as jahiaNode1, jahiaNode2, etc...
     * @parameter expression="${jahia.configure.externalizedClassifier}" default-value=""
     */
    protected String externalizedConfigClassifier;

    /**
     * The name of the JAR file (without the extension)
     *
     * @parameter expression="${jahia.configure.externalizedFinalName}" default-value="jahia-config"
     */
    protected String externalizedConfigFinalName;

    /**
     * The location of a JEE application to be configured for deployment in EAR format. If this is
     * null or empty this means we are not using EAR deployment for Jahia but WAR deployment.
     *
     * @parameter expression="${jahia.configure.jeeApplicationLocation}" default-value=""
     */
    protected String jeeApplicationLocation;

    /**
     * The list of modules to be setup in the JEE application.xml deployment descriptor.
     * List is comma separated, and each module has the following format:
     * type:id:arg1:arg2:...
     *
     * The arguments are different for each module type. Usually it is just a relative URI to the location of a JAR
     * or a SAR/RAR but in the case of a web module it is a bit different.
     *
     * For a WAR, the format is:
     *
     * web:webid:weburi:contextroot
     *
     * which will then become in the xml:
     *
     * <module id="webid">
     *     <web>
     *         <web-uri>weburi</web-uri>
     *         <context-root>contextroot</context-root>
     *     </web>
     * </module>
     *
     * The ID is an identifier used to name the module so that we can rewrite the XML more easily, and keep existing
     * structure should they exist already.
     *
     * @parameter expression="${jahia.configure.jeeApplicationModuleList}" default-value=""
     */
    protected String jeeApplicationModuleList;

    /**
     * A filesystem path to the folder, where Jackrabbit FileDataStore puts the binary content.
     *
     * @parameter expression="${jahia.configure.fileDataStorePath}" default-value=""
     */
    protected String fileDataStorePath;

    /**
     * @parameter expression="${jahia.configure.jahiaAdvancedProperties}"
     */
    protected Map<String, String> jahiaAdvancedProperties;

    /**
     * @parameter expression="${jahia.configure.jahiaProperties}"
     */
    protected Map<String, String> jahiaProperties;

    /**
     * Path to an existing license file to be used by Jahia instead of a default trial one.
     *
     * @parameter expression="${jahia.configure.licenseFile}"
     */
    private String licenseFile;

    public void doExecute() throws MojoExecutionException, MojoFailureException {
        try {
            new JahiaGlobalConfigurator(new MojoLogger(getLog()), this).execute();
        } catch (Exception e) {
            throw new MojoExecutionException("Error while configuring Jahia", e);
        }
    }

    public void setJahia_WebApps_Deployer_Service(String jahia_WebApps_Deployer_Service) {
        Jahia_WebApps_Deployer_Service = jahia_WebApps_Deployer_Service;
    }

    public String getCluster_activated() {
        return cluster_activated;
    }

    public String getCluster_node_serverId() {
        return cluster_node_serverId;
    }

    public List<String> getClusterNodes() {
        return JahiaGlobalConfigurator.fromString(clusterNodes);
    }

    public String getClusterTCPEHCacheHibernatePort() {
        return clusterTCPEHCacheHibernatePort;
    }

    public String getClusterTCPEHCacheJahiaPort() {
        return clusterTCPEHCacheJahiaPort;
    }

    public String getClusterTCPBindAddress() {
        return clusterTCPBindAddress;
    }

    public String getClusterTCPBindPort() {
        return clusterTCPBindPort;
    }

    public List<String> getClusterTCPEHCacheHibernateHosts() {
        return JahiaGlobalConfigurator.fromString(clusterTCPEHCacheHibernateHosts);
    }

    public List<String> getClusterTCPEHCacheJahiaHosts() {
        return JahiaGlobalConfigurator.fromString(clusterTCPEHCacheJahiaHosts);
    }

    public String getDatabasePassword() {
        return StringUtils.defaultString(databasePassword);
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public String getDatabaseUsername() {
        return StringUtils.defaultString(databaseUsername);
    }

    public String getDatabaseUrl() {
        return databaseUrl;
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

    public String getJahiaEtcDiskPath() {
        return jahiaEtcDiskPath;
    }

    public String getJahiaImportsDiskPath() {
        return jahiaImportsDiskPath;
    }

    public String getJahiaWebAppsDeployerBaseURL() {
        return jahiaWebAppsDeployerBaseURL;
    }

    public String getJahiaRootPassword() {
        return jahiaRootPassword;
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

    public List<String> getSiteImportLocation() {
        return siteImportLocation;
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

    public String getClusterStartIpAddress() {
        return clusterStartIpAddress;
    }

    public String getJahiaModulesDiskPath() {
        return jahiaModulesDiskPath;
    }

    public String getJahiaToolManagerUsername() {
        return jahiaToolManagerUsername;
    }

    public String getJahiaToolManagerPassword() {
        return jahiaToolManagerPassword;
    }

    public String getLdapActivated() {
        return ldapActivated;
    }

    public Map<String, String> getGroupLdapProviderProperties() {
        return groupLdapProviderProperties;
    }

    public Map<String, String> getUserLdapProviderProperties() {
        return userLdapProviderProperties;
    }

    public void setGroupLdapProviderProperties(String groupLdapProviderProperties) {
        if (groupLdapProviderProperties != null) {
            this.groupLdapProviderProperties = JahiaGlobalConfigurator.fromJSON(groupLdapProviderProperties);
        }
    }

    public void setUserLdapProviderProperties(String userLdapProviderProperties) {
        if (userLdapProviderProperties != null) {
            this.userLdapProviderProperties = JahiaGlobalConfigurator.fromJSON(userLdapProviderProperties);
        }
    }
    
    public String getOperatingMode() {
        return operatingMode;
    }

    public boolean isExternalizedConfigActivated() {
        return externalizedConfigActivated;
    }

    public String getExternalizedConfigTargetPath() {
        return externalizedConfigTargetPath;
    }

    public String getExternalizedConfigClassifier() {
        return externalizedConfigClassifier;
    }

    public String getExternalizedConfigFinalName() {
        return externalizedConfigFinalName;
    }

    public String getJeeApplicationLocation() {
        return jeeApplicationLocation;
    }

    public String getJeeApplicationModuleList() {
        return jeeApplicationModuleList;
    }

    public String getFileDataStorePath() {
        return fileDataStorePath;
    }

    public Map<String, String> getJahiaAdvancedProperties() {
        return jahiaAdvancedProperties;
    }

    public Map<String, String> getJahiaProperties() {
        return jahiaProperties;
    }

    public void setJahiaAdvancedProperties(String jahiaAdvancedProperties) {
        if (jahiaAdvancedProperties != null) {
            this.jahiaAdvancedProperties = JahiaGlobalConfigurator.fromJSON(jahiaAdvancedProperties);
        }
    }

    public void setJahiaProperties(String jahiaProperties) {
        if (jahiaProperties != null) {
            this.jahiaProperties = JahiaGlobalConfigurator.fromJSON(jahiaProperties);
        }
    }

    @Override
    public String getJahiaRootPreferredLang() {
        return jahiaRootPreferredLang;
    }

    @Override
    public String getLicenseFile() {
        return licenseFile;
    }

    @Override
    public boolean isExternalizedConfigExploded() {
        return externalizedConfigExploded;
    }

    @Override
    public String getExternalizedDataTargetPath() {
        return externalizedDataTargetPath;
    }

    @Override
    public boolean isExternalizedDataActivated() {
        return externalizedDataActivated;
    }

}
