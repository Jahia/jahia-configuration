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

    String getClusterTCPBindAddress();
    
    String getClusterTCPBindPort();
    
    String getDatabasePassword();
    
    String getDatabaseType();

    String getDatabaseUrl();

    String getDatabaseUsername();

    String getDb_script();
    
    /**
     * Additional properties, that will be used in jahia.properties. This object covers properties, which have no direct setter in
     * {@link JahiaConfigInterface} object.
     * 
     * @return a map with additional Jahia properties
     */
    Map<String, String> getJahiaProperties();

    String getJahiaImportsDiskPath();

    String getJahiaRootEmail();

    String getJahiaRootFirstname();

    String getJahiaRootLastname();

    String getJahiaRootPassword();

    String getJahiaRootPreferredLang();
    
    String getJahiaRootUsername();

    String getJahiaModulesDiskPath();

    String getJahiaVarDiskPath();

    String getJahiaWebAppsDeployerBaseURL();

    
    String getOperatingMode();

    String getOverwritedb();

    String getProcessingServer();

    
    /**
     * Returns <code>true</code> if Jackrabbit should store binary data in the DB, otherwise this data is store in a file system
     * (corresponding Jackrabbit property: externalBlobs).
     * 
     * @return <code>true</code> if Jackrabbit should store binary data in the DB, otherwise this data is store in a file system
     *         (corresponding Jackrabbit property: externalBlobs).
     */
    String getStoreFilesInDB();

    String getStoreFilesInAWS();

    /**
     * Returns a filesystem path to the folder, where the FileDataStore will put the binary data.
     * 
     * @return a filesystem path to the folder, where the FileDataStore will put the binary data
     */
    String getFileDataStorePath();

    String getTargetServerDirectory();

    String getWebAppDirName();

    /**
     * If active, we will package the configuration as a JAR file and place it in the location specified in the
     * exrernalizedConfigTargetPath bean variable
     * @return
     */
    boolean isExternalizedConfigActivated();

    /**
     * If active, the externalized configuration is deployed exploded.
     * @return <code>true</code> if the externalized configuration is deployed into a folder (nor an archive)
     */
    boolean isExternalizedConfigExploded();
    
    /**
     * The location at which to store Jahia's externalized configuration. Setting this value will enable the
     * externalization generation.
     * @return
     */
    String getExternalizedConfigTargetPath();
    
    /**
     * Provides a path to an existing license file to be used. If not provided a trial license will be used.
     * 
     * @return a path to an existing license file to be used. If not provided a trial license will be used
     */
    String getLicenseFile();

    /**
     * Return the Hazelcast bind port
     * @return the Hazelcast bind port
     */
    String getClusterHazelcastBindPort();

}
