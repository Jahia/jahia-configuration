/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.codehaus.plexus.util.PropertyUtils;
import org.codehaus.plexus.util.StringUtils;
import org.jahia.configuration.logging.AbstractLogger;

/**
 * Property configuration for the jahia.node.properties file. User: islam Date: 16 juil. 2008 Time: 10:09:49
 */
public class JahiaNodePropertiesConfigurator extends AbstractConfigurator {

    protected AbstractLogger logger;

    private PropertiesManager properties;

    public JahiaNodePropertiesConfigurator(AbstractLogger logger, JahiaConfigInterface cfg) {
        super(cfg);
        this.logger = logger;
    }

    private String getServerId(String id) {
        if (id == null || id.length() == 0 || "<auto>".equalsIgnoreCase(id)) {
            id = "df-" + UUID.randomUUID();
        }
        return id;
    }

    private void setClusterProperties() {
        JahiaConfigInterface cfg = jahiaConfigInterface;

        properties.setProperty("cluster.activated", cfg.getCluster_activated());
        properties.setProperty("cluster.node.serverId", getServerId(cfg.getCluster_node_serverId()));

        if (properties.getProperty("cluster.tcp.bindAddress") != null
                && StringUtils.isNotBlank(cfg.getClusterTCPBindAddress())) {
            properties.setProperty("cluster.tcp.bindAddress", cfg.getClusterTCPBindAddress());
        }
        if (properties.getProperty("cluster.tcp.bindPort") != null
                && StringUtils.isNotBlank(cfg.getClusterTCPBindPort())) {
            properties.setProperty("cluster.tcp.bindPort", cfg.getClusterTCPBindPort());
        }

        if (properties.getProperty("cluster.node.hazelcast.port") != null
                && StringUtils.isNotBlank(cfg.getCluster_node_hazelcast_port())) {
            properties.setProperty("cluster.node.hazelcast.port", cfg.getCluster_node_hazelcast_port());
        }
    }

    public void updateConfiguration(ConfigFile sourceJahiaPath, String targetJahiaPath) throws IOException {
        properties = new PropertiesManager(sourceJahiaPath.getInputStream());
        properties.setUnmodifiedCommentingActivated(true);

        File targetJahiaFile = new File(targetJahiaPath);
        Properties existingProperties = new Properties();
        if (targetJahiaFile.exists()) {
            existingProperties.putAll(PropertyUtils.loadProperties(targetJahiaFile));
            for (Object key : existingProperties.keySet()) {
                String propertyName = String.valueOf(key);
                properties.setProperty(propertyName, existingProperties.getProperty(propertyName));
            }
        }

        JahiaConfigInterface cfg = jahiaConfigInterface;

        properties.setProperty("processingServer", cfg.getProcessingServer());

        setClusterProperties();

        properties.storeProperties(sourceJahiaPath.getInputStream(), targetJahiaPath);
    }

}
