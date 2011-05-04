package org.jahia.configuration.configurators;

import java.util.List;
import java.util.Map;

/**
 * Configuration interface used by the configurators.
 *
 * @author loom
 *         Date: May 11, 2010
 *         Time: 1:18:20 PM
 */
public interface JahiaConfigInterface {
    
    String getRelease();

    String getDb_script();

    String getJahiaEtcDiskPath();

    String getJahiaVarDiskPath();

    String getJahiaSharedModulesDiskPath();

    String getJahiaModulesHttpPath();

    String getJahiaEnginesHttpPath();

    String getJahiaJavaScriptHttpPath();

    String getJahiaWebAppsDeployerBaseURL();

    String getCluster_activated();
    
    String getClusterStartIpAddress();

    String getCluster_node_serverId();

    String getProcessingServer();

    String getJahiaImportsDiskPath();

    List<String> getClusterNodes();

    String getClusterTCPEHCacheHibernatePort();

    String getClusterTCPEHCacheJahiaPort();

    List<String> getClusterTCPEHCacheHibernateRemotePorts();

    List<String> getClusterTCPEHCacheJahiaRemotePorts();

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

    String getTargetConfigurationDirectory();

    void setTargetConfigurationDirectory(String targetConfigurationDirectory);

    String getJahiaRootUsername();
    
    String getJahiaRootPassword();

    String getJahiaRootFirstname();
    
    String getJahiaRootLastname();
    
    String getJahiaRootEmail();
    
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

    /**
     * Returns <code>true</code> if Jackrabbit should store binary data in the DB, otherwise this data is store in a file system
     * (corresponding Jackrabbit property: externalBlobs).
     * 
     * @return <code>true</code> if Jackrabbit should store binary data in the DB, otherwise this data is store in a file system
     *         (corresponding Jackrabbit property: externalBlobs).
     */
    String getStoreFilesInDB();

    /**
     * Returns the name of the user that is used to protect Jahia tools area (/tools, etc.).
     * 
     * @return the name of the user that is used to protect Jahia tools area (/tools, etc.)
     */
    String getJahiaToolManagerUsername();

    /**
     * Returns the password of the user that is used to protect Jahia tools area (/tools, etc.).
     * 
     * @return the password of the user that is used to protect Jahia tools area (/tools, etc.)
     */
    String getJahiaToolManagerPassword();

    /**
     * Returns true if LDAP configuration is activated.
     * @return
     */
    String getLdapActivated();
    /**
     * The LDAP group manager provider options, including LDAP directory URL, bind DN and password, search attributes, field mapping etc.
     * 
     * @return LDAP group manager provider options, including LDAP directory URL, bind DN and password, search attributes, field mapping etc
     */
    Map<String, String> getGroupLdapProviderProperties();

    /**
     * The LDAP user manager provider options, including LDAP directory URL, bind DN and password, search attributes, field mapping etc.
     * 
     * @return LDAP user manager provider options, including LDAP directory URL, bind DN and password, search attributes, field mapping etc
     */
    Map<String, String> getUserLdapProviderProperties();

}
