package org.jahia.utils.maven.plugin.configurators;

import java.util.List;

/**
 * Configuration interface used by the configurators.
 *
 * @author loom
 *         Date: May 11, 2010
 *         Time: 1:18:20 PM
 */
public interface JahiaConfigInterface {
    
    String getJahiaFileRepositoryDiskPath();

    String getRelease();

    String getLocalIp();

    String getLocalPort();

    String getDb_script();

    String getJahiaEtcDiskPath();

    String getJahiaVarDiskPath();

    String getJahiaNewTemplatesDiskPath();

    String getJahiaNewWebAppsDiskPath();

    String getJahiaSharedTemplatesDiskPath();

    String getJahiaTemplatesHttpPath();

    String getJahiaEnginesHttpPath();

    String getJahiaJavaScriptHttpPath();

    String getJahiaWebAppsDeployerBaseURL();

    String getDatasource_name();

    String getOutputCacheActivated();

    String getOutputCacheDefaultExpirationDelay();

    String getOutputCacheExpirationOnly();

    String getOutputContainerCacheActivated();

    String getContainerCacheDefaultExpirationDelay();

    String getContainerCacheLiveModeOnly();

    String getEsiCacheActivated();

    String getJahia_WebApps_Deployer_Service();

    String getDefautSite();

    String getCluster_activated();

    String getCluster_node_serverId();

    String getProcessingServer();

    String getBigtext_service();

    String getJahiaFilesTemplatesDiskPath();

    String getJahiaImportsDiskPath();

    String getJahiaFilesBigTextDiskPath();

    List<String> getClusterNodes();

    String getDevelopmentMode();

    String getTargetServerDirectory();

    String getTargetServerType();

    String getTargetServerVersion();

    String getDatabaseType();

    String getDatabasePassword();

    String getDatabaseUrl();

    String getDatabaseUsername();

    String getOverwritedb();

    List<String> getSiteImportLocation();

    String getStoreFilesInDB();

    String getTargetConfigurationDirectory();

    void setTargetConfigurationDirectory(String targetConfigurationDirectory);

    String getJahiaRootUsername();
    
    String getJahiaRootPassword();

    String getJahiaRootFirstname();
    
    String getJahiaRootLastname();
    
    String getJahiaRootEmail();
    
    float getJahiaVersion();

    boolean isConfigureBeforePackaging();

    String getSourceWebAppDir();

    String getExternalConfigPath();

    String getWebAppDirName();
    
    String getMailServer();
    
    String getMailFrom();
    
    String getMailAdministrator();
    
    String getMailParanoia();

    /**
     * Returns the Web application context path Jahia is deployed to. Is empty
     * for ROOT context and starts with a slash in other cases (e.g. /jahia).
     * 
     * @return the Web application context path Jahia is deployed to. Is empty
     *         for ROOT context and starts with a slash in other cases (e.g.
     *         /jahia)
     */
    String getContextPath();
}
