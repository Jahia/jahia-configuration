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

package org.jahia.utils.maven.plugin.configurators;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * Holder for the Jahia properties.
 * User: islam
 * Date: 16 juil. 2008
 * Time: 10:33:15
 */
public class JahiaConfigBean implements Cloneable, JahiaConfigInterface {
    private File outputDirectory;
    private String jahiaFileRepositoryDiskPath = "$context/WEB-INF/var/content/filemanager/";
    private String release = "Testing_release";
    private String localIp = "localhost";
    private String localPort = "8080";
    private String jahiaEtcDiskPath = "$context/WEB-INF/etc/";
    private String jahiaVarDiskPath = "$context/WEB-INF/var/";
    private String jahiaNewTemplatesDiskPath = "$context/WEB-INF/var/new_templates/";
    private String jahiaNewWebAppsDiskPath = "$context/WEB-INF/var/new_webapps/";
    private String jahiaSharedTemplatesDiskPath = "$context/WEB-INF/var/shared_templates/";
    private String jahiaTemplatesHttpPath = "$webContext/templates/";
    private String jahiaEnginesHttpPath = "$webContext/engines/";
    private String jahiaJavaScriptHttpPath = "$webContext/javascript/jahia.js";
    private String jahiaWebAppsDeployerBaseURL = "http\\://localhost\\:8080/manager";
    private String datasource_name = "java\\:comp/env/jdbc/jahia";
    private String outputCacheActivated = "false";
    private String outputCacheDefaultExpirationDelay = "-1";
    private String outputCacheExpirationOnly = "false";
    private String outputContainerCacheActivated = "true";
    private String containerCacheDefaultExpirationDelay = "14400";
    private String containerCacheLiveModeOnly = "false";
    private String esiCacheActivated = "false";
    private String Jahia_WebApps_Deployer_Service = "org.jahia.services.webapps_deployer.JahiaTomcatWebAppsDeployerBaseService";
    private String defautSite = "mySite";
    private String cluster_activated = "false";
    private String cluster_node_serverId = "Jahia1";
    private String processingServer = "true";
    private String jahiaFilesTemplatesDiskPath = "$context/WEB-INF/var/templates/";
    private String jahiaImportsDiskPath = "$context/WEB-INF/var/imports/";
    private String jahiaFilesBigTextDiskPath = "$context/WEB-INF/var/content/bigtext/";
    private String bigtext_service = "DBJahiaText";
    private String jahiaxmlPath;
    private List<String> clusterNodes = new ArrayList<String>();
    private String db_script = "hypersonic.script";
    private String developmentMode = "false";
    private String targetServerType = "tomcat";
    private String targetServerVersion = "5.5";
    private String targetServerDirectory = "";
    private String databaseType = "derby";
    private String databaseUrl = "jdbc:derby:target/jahiadb;create=true";
    private String databaseUsername = "";
    private String databasePassword = "";
    private List<String> siteImportLocation;

    private String externalConfigPath = null;
    private String jahiaRootUsername = "root";
    private String jahiaRootPassword = "root1234";
    private String jahiaRootFirstname = "";
    private String jahiaRootLastname = "";
    private String jahiaRootEmail = "";
    private float jahiaVersion = 6.0f;
    private String webAppDirName = "";
    private boolean configureBeforePackaging = true;
    private String sourceWebAppDir = "";
    
    private String mailServer = "";
    private String mailFrom = "";
    private String mailAdministrator = "";
    private String mailParanoia = "Disabled";

    /**
     * This property is here for instance when we are in a clustered mode we don not want the database scripts to be
     * executed for every node
     */
    private String overwritedb = "true";
    private String storeFilesInDB = "false";
    private String targetConfigurationDirectory = "";
    
    public JahiaConfigInterface clone() throws CloneNotSupportedException {
        return (JahiaConfigInterface) super.clone();
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getJahiaFileRepositoryDiskPath() {
        return jahiaFileRepositoryDiskPath;
    }

    public void setJahiaFileRepositoryDiskPath(String jahiaFileRepositoryDiskPath) {
        this.jahiaFileRepositoryDiskPath = jahiaFileRepositoryDiskPath;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public String getLocalPort() {
        return localPort;
    }

    public void setLocalPort(String localPort) {
        this.localPort = localPort;
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

    public String getJahiaNewTemplatesDiskPath() {
        return jahiaNewTemplatesDiskPath;
    }

    public void setJahiaNewTemplatesDiskPath(String jahiaNewTemplatesDiskPath) {
        this.jahiaNewTemplatesDiskPath = jahiaNewTemplatesDiskPath;
    }

    public String getJahiaNewWebAppsDiskPath() {
        return jahiaNewWebAppsDiskPath;
    }

    public void setJahiaNewWebAppsDiskPath(String jahiaNewWebAppsDiskPath) {
        this.jahiaNewWebAppsDiskPath = jahiaNewWebAppsDiskPath;
    }

    public String getJahiaSharedTemplatesDiskPath() {
        return jahiaSharedTemplatesDiskPath;
    }

    public void setJahiaSharedTemplatesDiskPath(String jahiaSharedTemplatesDiskPath) {
        this.jahiaSharedTemplatesDiskPath = jahiaSharedTemplatesDiskPath;
    }

    public String getJahiaTemplatesHttpPath() {
        return jahiaTemplatesHttpPath;
    }

    public void setJahiaTemplatesHttpPath(String jahiaTemplatesHttpPath) {
        this.jahiaTemplatesHttpPath = jahiaTemplatesHttpPath;
    }

    public String getJahiaEnginesHttpPath() {
        return jahiaEnginesHttpPath;
    }

    public void setJahiaEnginesHttpPath(String jahiaEnginesHttpPath) {
        this.jahiaEnginesHttpPath = jahiaEnginesHttpPath;
    }

    public String getJahiaJavaScriptHttpPath() {
        return jahiaJavaScriptHttpPath;
    }

    public void setJahiaJavaScriptHttpPath(String jahiaJavaScriptHttpPath) {
        this.jahiaJavaScriptHttpPath = jahiaJavaScriptHttpPath;
    }

    public String getJahiaWebAppsDeployerBaseURL() {
        return jahiaWebAppsDeployerBaseURL;
    }

    public void setJahiaWebAppsDeployerBaseURL(String jahiaWebAppsDeployerBaseURL) {
        this.jahiaWebAppsDeployerBaseURL = jahiaWebAppsDeployerBaseURL;
    }

    public String getDatasource_name() {
        return datasource_name;
    }

    public void setDatasource_name(String datasource_name) {
        this.datasource_name = datasource_name;
    }

    public String getOutputCacheActivated() {
        return outputCacheActivated;
    }

    public void setOutputCacheActivated(String outputCacheActivated) {
        this.outputCacheActivated = outputCacheActivated;
    }

    public String getOutputCacheDefaultExpirationDelay() {
        return outputCacheDefaultExpirationDelay;
    }

    public void setOutputCacheDefaultExpirationDelay(String outputCacheDefaultExpirationDelay) {
        this.outputCacheDefaultExpirationDelay = outputCacheDefaultExpirationDelay;
    }

    public String getOutputCacheExpirationOnly() {
        return outputCacheExpirationOnly;
    }

    public void setOutputCacheExpirationOnly(String outputCacheExpirationOnly) {
        this.outputCacheExpirationOnly = outputCacheExpirationOnly;
    }

    public String getOutputContainerCacheActivated() {
        return outputContainerCacheActivated;
    }

    public void setOutputContainerCacheActivated(String outputContainerCacheActivated) {
        this.outputContainerCacheActivated = outputContainerCacheActivated;
    }

    public String getContainerCacheDefaultExpirationDelay() {
        return containerCacheDefaultExpirationDelay;
    }

    public void setContainerCacheDefaultExpirationDelay(String containerCacheDefaultExpirationDelay) {
        this.containerCacheDefaultExpirationDelay = containerCacheDefaultExpirationDelay;
    }

    public String getContainerCacheLiveModeOnly() {
        return containerCacheLiveModeOnly;
    }

    public void setContainerCacheLiveModeOnly(String containerCacheLiveModeOnly) {
        this.containerCacheLiveModeOnly = containerCacheLiveModeOnly;
    }

    public String getEsiCacheActivated() {
        return esiCacheActivated;
    }

    public void setEsiCacheActivated(String esiCacheActivated) {
        this.esiCacheActivated = esiCacheActivated;
    }

    public String getJahia_WebApps_Deployer_Service() {
        return Jahia_WebApps_Deployer_Service;
    }

    public void setJahia_WebApps_Deployer_Service(String jahia_WebApps_Deployer_Service) {
        Jahia_WebApps_Deployer_Service = jahia_WebApps_Deployer_Service;
    }

    public String getDefautSite() {
        return defautSite;
    }

    public void setDefautSite(String defautSite) {
        this.defautSite = defautSite;
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

    public String getBigtext_service() {
        return bigtext_service;
    }

    public void setBigtext_service(String bigtext_service) {
        this.bigtext_service = bigtext_service;
    }

    public String getJahiaFilesTemplatesDiskPath() {
        return jahiaFilesTemplatesDiskPath;
    }

    public void setJahiaFilesTemplatesDiskPath(String jahiaFilesTemplatesDiskPath) {
        this.jahiaFilesTemplatesDiskPath = jahiaFilesTemplatesDiskPath;
    }

    public String getJahiaImportsDiskPath() {
        return jahiaImportsDiskPath;
    }

    public void setJahiaImportsDiskPath(String jahiaImportsDiskPath) {
        this.jahiaImportsDiskPath = jahiaImportsDiskPath;
    }

    public String getJahiaFilesBigTextDiskPath() {
        return jahiaFilesBigTextDiskPath;
    }

    public void setJahiaFilesBigTextDiskPath(String jahiaFilesBigTextDiskPath) {
        this.jahiaFilesBigTextDiskPath = jahiaFilesBigTextDiskPath;
    }

    public String getJahiaxmlPath() {
        return jahiaxmlPath;
    }

    public void setJahiaxmlPath(String jahiaxmlPath) {
        this.jahiaxmlPath = jahiaxmlPath;
    }

    public List<String> getClusterNodes() {
        return clusterNodes;
    }

    public void setClusterNodes(List<String> clusterNodes) {
        this.clusterNodes = clusterNodes;
    }

    public void setDevelopmentMode(String developmentMode) {
        this.developmentMode = developmentMode;
    }

    public String getDevelopmentMode() {
        return developmentMode;
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

    public boolean isConfigureBeforePackaging() {
        return configureBeforePackaging;
    }

    public String getExternalConfigPath() {
        return externalConfigPath;
    }

    public String getJahiaRootPassword() {
        return jahiaRootPassword;
    }

    public float getJahiaVersion() {
        return jahiaVersion;
    }

    public String getWebAppDirName() {
        return webAppDirName;
    }

    public String getSourceWebAppDir() {
        return sourceWebAppDir;
    }

    public void setConfigureBeforePackaging(boolean configureBeforePackaging) {
        this.configureBeforePackaging = configureBeforePackaging;
    }

    public void setExternalConfigPath(String externalConfigPath) {
        this.externalConfigPath = externalConfigPath;
    }

    public void setJahiaRootPassword(String jahiaRootPassword) {
        this.jahiaRootPassword = jahiaRootPassword;
    }

    public void setJahiaVersion(float jahiaVersion) {
        this.jahiaVersion = jahiaVersion;
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
}
