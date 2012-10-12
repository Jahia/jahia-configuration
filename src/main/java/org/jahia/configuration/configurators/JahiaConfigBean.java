/**
 *
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2012 Jahia Limited. All rights reserved.
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

package org.jahia.configuration.configurators;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Holder for the Jahia properties.
 * User: islam
 * Date: 16 juil. 2008
 * Time: 10:33:15
 */
public class JahiaConfigBean implements Cloneable, JahiaConfigInterface {
    private File outputDirectory;
    private String jahiaEtcDiskPath = "$context/WEB-INF/etc/";
    private String jahiaVarDiskPath = "$context/WEB-INF/var/";
    private String jahiaSharedModulesDiskPath = "$context/WEB-INF/var/shared_modules/";
    private String jahiaWebAppsDeployerBaseURL = "http\\://localhost\\:8080/manager";
    private String cluster_activated = "false";
    private String cluster_node_serverId;
    private String clusterStartIpAddress = "192.168.1.100";
    private String processingServer = "true";
    private String jahiaImportsDiskPath = "$context/WEB-INF/var/imports/";
    private List<String> clusterNodes = new ArrayList<String>();
    private String clusterTCPBindAddress = null;
    private String clusterTCPBindPort = "7870";
    private String clusterTCPEHCacheHibernatePort = "7860";
    private String clusterTCPEHCacheJahiaPort = "7870";
    private List<String> clusterTCPEHCacheHibernateHosts = new ArrayList<String>();
    private List<String> clusterTCPEHCacheJahiaHosts = new ArrayList<String>();
    private String db_script = "hypersonic.script";
    private String operatingMode = "development";
    private String targetServerType = "tomcat";
    private String targetServerVersion = "";
    private String targetServerDirectory = "";
    private String databaseType = "derby_embedded";
    private String databaseUrl = "jdbc:derby:directory:jahia;create=true";
    private String databaseUsername = "";
    private String databasePassword = "";
    private List<String> siteImportLocation;

    private String externalConfigPath = null;
    private String jahiaRootUsername = "root";
    private String jahiaRootPassword = "root1234";
    private String jahiaRootFirstname = "";
    private String jahiaRootLastname = "";
    private String jahiaRootEmail = "";
    private String webAppDirName = "ROOT";
    private String sourceWebAppDir = "";
    
    private String mailServer = "";
    private String mailFrom = "";
    private String mailAdministrator = "";
    private String mailParanoia = "Disabled";
    
    private String contextPath = "";

    /**
     * This property is here for instance when we are in a clustered mode we don not want the database scripts to be
     * executed for every node
     */
    private String overwritedb = "true";
    private String storeFilesInDB = "false";
    private String fileDataStorePath = "";
    private String targetConfigurationDirectory = "";
    private String jahiaToolManagerPassword = "jahia";
    private String jahiaToolManagerUsername = "password";

    private String ldapActivated = "false";
    private Map<String, String> groupLdapProviderProperties = new HashMap<String, String>();
    private Map<String, String> userLdapProviderProperties = new HashMap<String, String>();

    private boolean externalizedConfigActivated = false;
    private String externalizedConfigTargetPath = null;
    private String externalizedConfigClassifier = null;
    private String externalizedConfigFinalName = "jahia-config";
    private Map<String, String> jahiaAdvancedProperties = new HashMap<String, String>();
    private Map<String, String> jahiaProperties = new HashMap<String, String>();

    private String jeeApplicationLocation = null;
    private String jeeApplicationModuleList = null;
    
    public void setFileDataStorePath(String fileDataStorePath) {
        this.fileDataStorePath = fileDataStorePath;
    }

    public JahiaConfigInterface clone() throws CloneNotSupportedException {
        return (JahiaConfigInterface) super.clone();
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getDb_script() {
        return db_script;
    }

    public void setDb_script(String db_script) {
        this.db_script = db_script;
    }

    public String getJahiaEtcDiskPath() {
        return jahiaEtcDiskPath;
    }

    public void setJahiaEtcDiskPath(String jahiaEtcDiskPath) {
        this.jahiaEtcDiskPath = jahiaEtcDiskPath;
    }

    public String getJahiaVarDiskPath() {
        return jahiaVarDiskPath;
    }

    public void setJahiaVarDiskPath(String jahiaVarDiskPath) {
        this.jahiaVarDiskPath = jahiaVarDiskPath;
    }

    public String getJahiaSharedModulesDiskPath() {
        return jahiaSharedModulesDiskPath;
    }
    public void setJahiaSharedModulesDiskPath(String jahiaSharedModulesDiskPath) {
        this.jahiaSharedModulesDiskPath = jahiaSharedModulesDiskPath;
    }

    public String getJahiaWebAppsDeployerBaseURL() {
        return jahiaWebAppsDeployerBaseURL;
    }

    public void setJahiaWebAppsDeployerBaseURL(String jahiaWebAppsDeployerBaseURL) {
        this.jahiaWebAppsDeployerBaseURL = jahiaWebAppsDeployerBaseURL;
    }

    public String getCluster_activated() {
        return cluster_activated;
    }

    public void setCluster_activated(String cluster_activated) {
        this.cluster_activated = cluster_activated;
    }

    public String getCluster_node_serverId() {
        return cluster_node_serverId;
    }

    public void setCluster_node_serverId(String cluster_node_serverId) {
        this.cluster_node_serverId = cluster_node_serverId;
    }

    public String getProcessingServer() {
        return processingServer;
    }

    public void setProcessingServer(String processingServer) {
        this.processingServer = processingServer;
    }

    public String getJahiaImportsDiskPath() {
        return jahiaImportsDiskPath;
    }

    public void setJahiaImportsDiskPath(String jahiaImportsDiskPath) {
        this.jahiaImportsDiskPath = jahiaImportsDiskPath;
    }

    public List<String> getClusterNodes() {
        return clusterNodes;
    }

    public void setClusterNodes(List<String> clusterNodes) {
        if (clusterNodes != null) {
            this.clusterNodes = clusterNodes;
        }
    }

    public String getClusterTCPEHCacheHibernatePort() {
        return clusterTCPEHCacheHibernatePort;
    }

    public void setClusterTCPEHCacheHibernatePort(String clusterTCPEHCacheHibernatePort) {
        this.clusterTCPEHCacheHibernatePort = clusterTCPEHCacheHibernatePort;
    }

    public String getClusterTCPEHCacheJahiaPort() {
        return clusterTCPEHCacheJahiaPort;
    }

    public void setClusterTCPEHCacheJahiaPort(String clusterTCPEHCacheJahiaPort) {
        this.clusterTCPEHCacheJahiaPort = clusterTCPEHCacheJahiaPort;
    }

    public List<String> getClusterTCPEHCacheHibernateHosts() {
        return clusterTCPEHCacheHibernateHosts;
    }

    public void setClusterTCPEHCacheHibernateHosts(List<String> clusterTCPEHCacheHibernateHosts) {
        if (clusterTCPEHCacheHibernateHosts != null) {
            this.clusterTCPEHCacheHibernateHosts = clusterTCPEHCacheHibernateHosts;
        }
    }

    public List<String> getClusterTCPEHCacheJahiaHosts() {
        return clusterTCPEHCacheJahiaHosts;
    }

    public void setClusterTCPEHCacheJahiaHosts(List<String> clusterTCPEHCacheJahiaHosts) {
        if (clusterTCPEHCacheJahiaHosts != null) {
            this.clusterTCPEHCacheJahiaHosts = clusterTCPEHCacheJahiaHosts;
        }
    }

    public void setOperatingMode(String operatingMode) {
        this.operatingMode = operatingMode;
    }

    public String getOperatingMode() {
        return operatingMode;
    }

    public String getTargetServerDirectory() {
        return targetServerDirectory;
    }

    public void setTargetServerDirectory(String targetServerDirectory) {
        this.targetServerDirectory = targetServerDirectory;
    }

    public String getTargetServerType() {
        return targetServerType;
    }

    public void setTargetServerType(String targetServerType) {
        this.targetServerType = targetServerType;
    }

    public String getTargetServerVersion() {
        return targetServerVersion;
    }

    public void setTargetServerVersion(String targetServerVersion) {
        this.targetServerVersion = targetServerVersion;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    public String getDatabaseUsername() {
        return databaseUsername;
    }

    public void setDatabaseUsername(String databaseUsername) {
        this.databaseUsername = databaseUsername;
    }

    public String getOverwritedb() {
        return overwritedb;
    }

    public void setOverwritedb(String overwritedb) {
        this.overwritedb = overwritedb;
    }

    public List<String> getSiteImportLocation() {
        return siteImportLocation;
    }

    public void setSiteImportLocation(List<String> siteImportLocation) {
        this.siteImportLocation = siteImportLocation;
    }

    public String getStoreFilesInDB() {
        return storeFilesInDB;
    }

    public void setStoreFilesInDB(String storeFilesInDB) {
        this.storeFilesInDB = storeFilesInDB;
    }

    public String getTargetConfigurationDirectory() {
        return targetConfigurationDirectory;
    }

    public void setTargetConfigurationDirectory(String targetConfigurationDirectory) {
        this.targetConfigurationDirectory = targetConfigurationDirectory;
    }

    public String getExternalConfigPath() {
        return externalConfigPath;
    }

    public String getJahiaRootPassword() {
        return jahiaRootPassword;
    }

    public String getWebAppDirName() {
        return webAppDirName;
    }

    public String getSourceWebAppDir() {
        return sourceWebAppDir;
    }

    public void setExternalConfigPath(String externalConfigPath) {
        this.externalConfigPath = externalConfigPath;
    }

    public void setJahiaRootPassword(String jahiaRootPassword) {
        this.jahiaRootPassword = jahiaRootPassword;
    }

    public void setSourceWebAppDir(String sourceWebAppDir) {
        this.sourceWebAppDir = sourceWebAppDir;
    }

    public void setWebAppDirName(String webAppDirName) {
        this.webAppDirName = webAppDirName;
    }

    /**
     * @return the jahiaRootUsername
     */
    public String getJahiaRootUsername() {
        return jahiaRootUsername;
    }

    /**
     * @param jahiaRootUsername the jahiaRootUsername to set
     */
    public void setJahiaRootUsername(String jahiaRootUsername) {
        this.jahiaRootUsername = jahiaRootUsername;
    }

    /**
     * @return the jahiaRootFirstname
     */
    public String getJahiaRootFirstname() {
        return jahiaRootFirstname;
    }

    /**
     * @param jahiaRootFirstname the jahiaRootFirstname to set
     */
    public void setJahiaRootFirstname(String jahiaRootFirstname) {
        this.jahiaRootFirstname = jahiaRootFirstname;
    }

    /**
     * @return the jahiaRootLastname
     */
    public String getJahiaRootLastname() {
        return jahiaRootLastname;
    }

    /**
     * @param jahiaRootLastname the jahiaRootLastname to set
     */
    public void setJahiaRootLastname(String jahiaRootLastname) {
        this.jahiaRootLastname = jahiaRootLastname;
    }

    /**
     * @return the jahiaRootEmail
     */
    public String getJahiaRootEmail() {
        return jahiaRootEmail;
    }

    /**
     * @param jahiaRootEmail the jahiaRootEmail to set
     */
    public void setJahiaRootEmail(String jahiaRootEmail) {
        this.jahiaRootEmail = jahiaRootEmail;
    }

    /**
     * @return the mailServer
     */
    public String getMailServer() {
        return mailServer;
    }

    /**
     * @param mailServer the mailServer to set
     */
    public void setMailServer(String mailServer) {
        this.mailServer = mailServer;
    }

    /**
     * @return the mailFrom
     */
    public String getMailFrom() {
        return mailFrom;
    }

    /**
     * @param mailFrom the mailFrom to set
     */
    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    /**
     * @return the mailAdministrator
     */
    public String getMailAdministrator() {
        return mailAdministrator;
    }

    /**
     * @param mailAdministrator the mailAdministrator to set
     */
    public void setMailAdministrator(String mailAdministrator) {
        this.mailAdministrator = mailAdministrator;
    }

    /**
     * @return the mailParanoia
     */
    public String getMailParanoia() {
        return mailParanoia;
    }

    /**
     * @param mailParanoia the mailParanoia to set
     */
    public void setMailParanoia(String mailParanoia) {
        this.mailParanoia = mailParanoia;
    }

    public String getContextPath() {
        return contextPath;
    }

    /**
     * @param contextPath the contextPath to set
     */
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getClusterStartIpAddress() {
        return clusterStartIpAddress;
    }

    /**
     * @param clusterStartIpAddress the clusterStartIpAddress to set
     */
    public void setClusterStartIpAddress(String clusterStartIpAddress) {
        this.clusterStartIpAddress = clusterStartIpAddress;
    }

    public String getJahiaToolManagerUsername() {
        return jahiaToolManagerUsername;
    }

    public String getJahiaToolManagerPassword() {
        return jahiaToolManagerPassword;
    }

    public void setJahiaToolManagerPassword(String jahiaToolManagerPassword) {
        this.jahiaToolManagerPassword = jahiaToolManagerPassword;
    }

    public void setJahiaToolManagerUsername(String jahiaToolManagerUsername) {
        this.jahiaToolManagerUsername = jahiaToolManagerUsername;
    }

    public String getLdapActivated() {
        return ldapActivated;
    }

    public void setLdapActivated(String ldapActivated) {
        this.ldapActivated = ldapActivated;
    }

    public Map<String, String> getGroupLdapProviderProperties() {
        return groupLdapProviderProperties;
    }

    public void setGroupLdapProviderProperties(
            Map<String, String> groupLdapProviderProperties) {
        this.groupLdapProviderProperties = groupLdapProviderProperties;
    }

    public Map<String, String> getUserLdapProviderProperties() {
        return userLdapProviderProperties;
    }

    public void setUserLdapProviderProperties(
            Map<String, String> userLdapProviderProperties) {
        this.userLdapProviderProperties = userLdapProviderProperties;
    }

    public String getExternalizedConfigTargetPath() {
        return externalizedConfigTargetPath;
    }

    public void setExternalizedConfigTargetPath(String externalizedConfigTargetPath) {
        this.externalizedConfigTargetPath = externalizedConfigTargetPath;
    }

    public boolean isExternalizedConfigActivated() {
        return externalizedConfigActivated;
    }

    public void setExternalizedConfigActivated(boolean externalizedConfigActivated) {
        this.externalizedConfigActivated = externalizedConfigActivated;
    }

    public String getExternalizedConfigClassifier() {
        return externalizedConfigClassifier;
    }

    public void setExternalizedConfigClassifier(String externalizedConfigClassifier) {
        this.externalizedConfigClassifier = externalizedConfigClassifier;
    }

    public String getExternalizedConfigFinalName() {
        return externalizedConfigFinalName;
    }

    public String getJeeApplicationLocation() {
        return jeeApplicationLocation;
    }

    public void setJeeApplicationLocation(String jeeApplicationLocation) {
        this.jeeApplicationLocation = jeeApplicationLocation;
    }

    public String getJeeApplicationModuleList() {
        return jeeApplicationModuleList;
    }

    public void setJeeApplicationModuleList(String jeeApplicationModuleList) {
        this.jeeApplicationModuleList = jeeApplicationModuleList;
    }

    public void setExternalizedConfigFinalName(String externalizedConfigFinalName) {
        this.externalizedConfigFinalName = externalizedConfigFinalName;
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

    public void setJahiaAdvancedProperties(Map<String, String> jahiaAdvancedProperties) {
        this.jahiaAdvancedProperties = jahiaAdvancedProperties;
    }

    public void setJahiaProperties(Map<String, String> jahiaProperties) {
        this.jahiaProperties = jahiaProperties;
    }

    public String getClusterTCPBindAddress() {
        return clusterTCPBindAddress;
    }

    public String getClusterTCPBindPort() {
        return clusterTCPBindPort;
    }

    public void setClusterTCPBindAddress(String clusterTCPBindAddress) {
        this.clusterTCPBindAddress = clusterTCPBindAddress;
    }

    public void setClusterTCPBindPort(String clusterTCPBindPort) {
        this.clusterTCPBindPort = clusterTCPBindPort;
    }

}
