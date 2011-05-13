/**
 *
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2011 Jahia Limited. All rights reserved.
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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.plexus.util.StringUtils;
import org.jahia.configuration.logging.AbstractLogger;

/**
 * Property configuration for the jahia.advanced.properties file.
 * User: islam
 * Date: 16 juil. 2008
 * Time: 10:09:49
 */
public class JahiaAdvancedPropertiesConfigurator extends AbstractConfigurator {

    private PropertiesManager properties;
    
    protected AbstractLogger logger;

    public JahiaAdvancedPropertiesConfigurator(AbstractLogger logger, JahiaConfigInterface cfg) {
        super(cfg);
        this.logger = logger;
    }

    public void updateConfiguration(String sourceJahiaPath, String targetJahiaPath) throws IOException {
        properties = new PropertiesManager(sourceJahiaPath, targetJahiaPath);
        
        JahiaConfigInterface cfg = jahiaConfigInterface;

        properties.setProperty("processingServer", cfg.getProcessingServer());
        
        properties.setProperty("cluster.activated", cfg.getCluster_activated());
        properties.setProperty("cluster.node.serverId", cfg.getCluster_node_serverId());
        properties.setProperty("cluster.tcp.start.ip_address", cfg.getClusterStartIpAddress());

        properties.setProperty("cluster.tcp.ehcache.hibernate.port", cfg.getClusterTCPEHCacheHibernatePort());
        properties.setProperty("cluster.tcp.ehcache.jahia.port", cfg.getClusterTCPEHCacheJahiaPort());
        
        List<String> hibernateHosts = getInitialHosts(cfg.getClusterTCPEHCacheHibernateHosts(), cfg.getClusterNodes(), cfg.getClusterTCPEHCacheHibernatePort());
        List<String> jahiaHosts = getInitialHosts(cfg.getClusterTCPEHCacheJahiaHosts(), cfg.getClusterNodes(), cfg.getClusterTCPEHCacheJahiaPort());
        if (hibernateHosts.size() != jahiaHosts.size()) {
            logger.error("ERROR: number of initial hosts for Hibernate and Jahia"
                    + " caches do not match. There is an issue in the provided configuration!");
        }
        properties.setProperty("cluster.tcp.ehcache.hibernate.nodes.ip_address", StringUtils.join(hibernateHosts.iterator(), ","));
        properties.setProperty("cluster.tcp.ehcache.jahia.nodes.ip_address", StringUtils.join(jahiaHosts.iterator(), ","));

        properties.setProperty("cluster.tcp.num_initial_members",String.valueOf(hibernateHosts.size()));
        
        properties.storeProperties(sourceJahiaPath, targetJahiaPath);
    }

    private static List<String> getInitialHosts(List<String> hosts,
            List<String> nodeIps, String port) {
        List<String> finalHosts = new LinkedList<String>();

        if (!hosts.isEmpty()) {
            for (String host : hosts) {
                if (!host.contains("[")) {
                    // add a default port
                    host = host + "[" + port + "]";
                }
                finalHosts.add(host);
            }
        } else if (!nodeIps.isEmpty()) {
            for (String host : nodeIps) {
                finalHosts.add(host + "[" + port + "]");
            }
        }
        return finalHosts;
    }

}
