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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Property configuration for the jahia.node.properties file. User: islam Date: 16 juil. 2008 Time: 10:09:49
 */
public class JahiaNodePropertiesConfigurator extends AbstractConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(JahiaNodePropertiesConfigurator.class);

    private PropertiesManager properties;

    public JahiaNodePropertiesConfigurator(JahiaConfigInterface cfg) {
        super(cfg);
    }

    private String getServerId(String id) {
        if (id == null || id.length() == 0 || "<auto>".equalsIgnoreCase(id)) {
            id = "dx-" + UUID.randomUUID();
            logger.info("Using generated server id: {}", id);
        } else {
            logger.info("Using configured server id: {}", id);
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

        if (properties.getProperty("cluster.hazelcast.bindPort") != null
                && StringUtils.isNotBlank(cfg.getClusterHazelcastBindPort())) {
            properties.setProperty("cluster.hazelcast.bindPort", cfg.getClusterHazelcastBindPort());
        }
    }

    @Override
    public void updateConfiguration(InputStream inputStream, String destFileName) throws Exception {
        logger.info("Updating jahia.node.properties");
        properties = new PropertiesManager(inputStream);
        properties.setUnmodifiedCommentingActivated(true);

        File targetJahiaFile = new File(destFileName);
        Properties existingProperties = new Properties();
        if (targetJahiaFile.exists()) {
            try (FileInputStream fis = new FileInputStream(targetJahiaFile)) {
                existingProperties.load(fis);
            }
            for (Object key : existingProperties.keySet()) {
                String propertyName = String.valueOf(key);
                properties.setProperty(propertyName, existingProperties.getProperty(propertyName));
            }
        }

        JahiaConfigInterface cfg = jahiaConfigInterface;

        properties.setProperty("processingServer", cfg.getProcessingServer());

        setClusterProperties();

        properties.storeProperties(inputStream, destFileName);
        logger.info("Successfully updated jahia.node.properties in {}", destFileName);
    }
}

