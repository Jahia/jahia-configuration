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
package org.jahia.configuration.configurators;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Holder for the Jahia properties.
 * User: islam
 * Date: 16 juil. 2008
 * Time: 10:33:15
 */
public class JahiaConfigBean implements Cloneable, JahiaConfigInterface {

    private String jahiaVarDiskPath = "${jahiaWebAppRoot}/WEB-INF/var/";
    private String jahiaModulesDiskPath = "${jahia.data.dir}/modules/";
    private String jahiaWebAppsDeployerBaseURL = "http://127.0.0.1:8080/manager/html/";
    private String cluster_activated = "false";
    private String cluster_node_serverId;
    private String clusterHazelcastBindPort = "7860";
    private String processingServer = "true";
    private String jahiaImportsDiskPath = "${jahia.data.dir}/imports/";
    private String clusterTCPBindAddress = null;
    private String clusterTCPBindPort = "7870";
    private String db_script = "hypersonic.script";
    private String operatingMode = "development";
    private String targetServerDirectory = "";
    private String databaseType = "derby_embedded";
    private String databaseUrl = "jdbc:derby:directory:jahia";
    private String databaseUsername = "";
    private String databasePassword = "";

    private String jahiaRootUsername = "root";
    private String jahiaRootPassword = "root1234";
    private String jahiaRootFirstname = "";
    private String jahiaRootLastname = "";
    private String jahiaRootEmail = "";
    private String jahiaRootPreferredLang = "en";
    private String webAppDirName = "ROOT";

    /**
     * This property is here for instance when we are in a clustered mode we don not want the database scripts to be
     * executed for every node
     */
    private String overwritedb = "true";
    private String storeFilesInDB = "false";
    private String storeFilesInAWS = "false";
    private String fileDataStorePath = "";
    private String targetConfigurationDirectory = "";

    private boolean externalizedConfigActivated = true;
    private boolean externalizedConfigExploded = true;
    private String externalizedConfigTargetPath;
    private Map<String, String> jahiaProperties = new HashMap<String, String>();

    private String licenseFile;
    
    public void setFileDataStorePath(String fileDataStorePath) {
        this.fileDataStorePath = fileDataStorePath;
    }

    public JahiaConfigInterface clone() throws CloneNotSupportedException {
        return (JahiaConfigInterface) super.clone();
    }

    public String getDb_script() {
        return db_script;
    }

    public void setDb_script(String db_script) {
        this.db_script = db_script;
    }

    public String getJahiaVarDiskPath() {
        return jahiaVarDiskPath;
    }

    public void setJahiaVarDiskPath(String jahiaVarDiskPath) {
        this.jahiaVarDiskPath = jahiaVarDiskPath;
    }

    public String getJahiaModulesDiskPath() {
        return jahiaModulesDiskPath;
    }
    public void setJahiaSharedModulesDiskPath(String jahiaSharedModulesDiskPath) {
        this.jahiaModulesDiskPath = jahiaSharedModulesDiskPath;
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

    /**
     * Returns the port number for module manager cluster communication (based on Karaf Cellar + Hazelcast).
     */
    public String getClusterHazelcastBindPort() {
        return clusterHazelcastBindPort;
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


    public String getStoreFilesInDB() {
        return storeFilesInDB;
    }

    public void setStoreFilesInDB(String storeFilesInDB) {
        this.storeFilesInDB = storeFilesInDB;
    }

    @Override
    public String getStoreFilesInAWS() {
        return storeFilesInAWS;
    }

    public void setStoreFilesInAWS(String storeFilesInAWS) {
        this.storeFilesInAWS = storeFilesInAWS;
    }

    public String getTargetConfigurationDirectory() {
        return targetConfigurationDirectory;
    }

    public void setTargetConfigurationDirectory(String targetConfigurationDirectory) {
        this.targetConfigurationDirectory = targetConfigurationDirectory;
    }

    public String getJahiaRootPassword() {
        return jahiaRootPassword;
    }

    public String getWebAppDirName() {
        return webAppDirName;
    }

    public void setJahiaRootPassword(String jahiaRootPassword) {
        this.jahiaRootPassword = jahiaRootPassword;
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

    public String getFileDataStorePath() {
        return fileDataStorePath;
    }

    public Map<String, String> getJahiaProperties() {
        return jahiaProperties;
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

    @Override
    public String getJahiaRootPreferredLang() {
        return jahiaRootPreferredLang;
    }

    public void setJahiaRootPreferredLang(String jahiaRootPreferredLang) {
        this.jahiaRootPreferredLang = jahiaRootPreferredLang;
    }

    @Override
    public String getLicenseFile() {
        return licenseFile;
    }

    public void setLicenseFile(String licenseFile) {
        this.licenseFile = licenseFile;
    }

    public boolean isExternalizedConfigExploded() {
        return externalizedConfigExploded;
    }

    public void setExternalizedConfigExploded(boolean externalizedConfigExploded) {
        this.externalizedConfigExploded = externalizedConfigExploded;
    }

    public void setClusterHazelcastBindPort(String clusterHazelcastBindPort) {
        this.clusterHazelcastBindPort = clusterHazelcastBindPort;
    }
}
