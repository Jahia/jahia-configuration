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
    
    String getCluster_activated();

    String getCluster_node_serverId();

    List<String> getClusterNodes();

    String getClusterStartIpAddress();

    String getClusterTCPEHCacheHibernatePort();

    List<String> getClusterTCPEHCacheHibernateHosts();

    String getClusterTCPEHCacheJahiaPort();

    List<String> getClusterTCPEHCacheJahiaHosts();

    /**
     * Returns the Web application context path Jahia is deployed to. Is empty
     * for ROOT context and starts with a slash in other cases (e.g. /jahia).
     * 
     * @return the Web application context path Jahia is deployed to. Is empty
     *         for ROOT context and starts with a slash in other cases (e.g.
     *         /jahia)
     */
    String getContextPath();

    String getDatabasePassword();
    
    String getDatabaseType();

    String getDatabaseUrl();

    String getDatabaseUsername();

    String getDb_script();

    String getExternalConfigPath();

    /**
     * The LDAP group manager provider options, including LDAP directory URL, bind DN and password, search attributes, field mapping etc.
     * 
     * @return LDAP group manager provider options, including LDAP directory URL, bind DN and password, search attributes, field mapping etc
     */
    Map<String, String> getGroupLdapProviderProperties();

    String getJahiaEtcDiskPath();

    String getJahiaImportsDiskPath();

    String getJahiaRootEmail();

    String getJahiaRootFirstname();

    String getJahiaRootLastname();

    String getJahiaRootPassword();

    String getJahiaRootUsername();

    String getJahiaSharedModulesDiskPath();

    /**
     * Returns the password of the user that is used to protect Jahia tools area (/tools, etc.).
     * 
     * @return the password of the user that is used to protect Jahia tools area (/tools, etc.)
     */
    String getJahiaToolManagerPassword();

    /**
     * Returns the name of the user that is used to protect Jahia tools area (/tools, etc.).
     * 
     * @return the name of the user that is used to protect Jahia tools area (/tools, etc.)
     */
    String getJahiaToolManagerUsername();

    String getJahiaVarDiskPath();

    String getJahiaWebAppsDeployerBaseURL();

    /**
     * Returns true if LDAP configuration is activated.
     * @return
     */
    String getLdapActivated();
    
    String getMailAdministrator();

    String getMailFrom();
    
    String getMailParanoia();
    
    String getMailServer();
    
    String getOperatingMode();

    String getOverwritedb();

    String getProcessingServer();
    
    List<String> getSiteImportLocation();
    
    String getSourceWebAppDir();
    
    /**
     * Returns <code>true</code> if Jackrabbit should store binary data in the DB, otherwise this data is store in a file system
     * (corresponding Jackrabbit property: externalBlobs).
     * 
     * @return <code>true</code> if Jackrabbit should store binary data in the DB, otherwise this data is store in a file system
     *         (corresponding Jackrabbit property: externalBlobs).
     */
    String getStoreFilesInDB();

    String getTargetConfigurationDirectory();

    String getTargetServerDirectory();

    String getTargetServerType();

    String getTargetServerVersion();

    /**
     * The LDAP user manager provider options, including LDAP directory URL, bind DN and password, search attributes, field mapping etc.
     * 
     * @return LDAP user manager provider options, including LDAP directory URL, bind DN and password, search attributes, field mapping etc
     */
    Map<String, String> getUserLdapProviderProperties();
    String getWebAppDirName();

    void setTargetConfigurationDirectory(String targetConfigurationDirectory);

    /**
     * If active, we will package the configuration as a JAR file and place it in the location specified in the
     * exrernalizedConfigTargetPath bean variable
     * @return
     */
    boolean isExternalizedConfigActivated();

    /**
     * The location at which to store Jahia's externalized configuration. Setting this value will enable the
     * externalization generation.
     * @return
     */
    String getExternalizedConfigTargetPath();

    /**
     * Allows to specify a classifier on the configuration, usually used to identify cluster node configurations, such
     * as jahiaNode1, jahiaNode2, etc...
     * @return
     */
    String getExternalizedConfigClassifier();

    /**
     * The name of the JAR file (without the extension)
     * @return
     */
    String getExternalizedConfigFinalName();
}
