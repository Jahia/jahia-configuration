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

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

/**
 * Property configuration for the jahia.advanced.properties file.
 * User: islam
 * Date: 16 juil. 2008
 * Time: 10:09:49
 */
public class JahiaAdvancedPropertiesConfigurator extends AbstractConfigurator {

    private PropertiesManager properties;

    public JahiaAdvancedPropertiesConfigurator(JahiaConfigInterface jahiaConfigInterface) {
        super(jahiaConfigInterface);
    }

    public void updateConfiguration(String sourceJahiaPath, String targetJahiaPath) throws IOException {
        properties = new PropertiesManager(sourceJahiaPath, targetJahiaPath);
        // context  path
        properties.setProperty("cluster.activated", jahiaConfigInterface.getCluster_activated());
        properties.setProperty("cluster.node.serverId", jahiaConfigInterface.getCluster_node_serverId());

        if (jahiaConfigInterface.getCluster_activated().equals("true")) {
            addClusterNodes(jahiaConfigInterface.getClusterNodes());

        }
        properties.setProperty("cluster.node.serverId", jahiaConfigInterface.getCluster_node_serverId());
        
        properties.storeProperties(sourceJahiaPath, targetJahiaPath);
    }

	private void addClusterNodes(List<String> clusterNodes) {
        final String propvalue = jahiaConfigInterface.getClusterStartIpAddress();
        properties.setProperty("cluster.tcp.start.ip_address", propvalue);
        if (!clusterNodes.contains(propvalue)) {
            clusterNodes.add(0, propvalue);
        }

        StringBuilder clusterTCPServiceNodesIPAddresses = new StringBuilder();
        StringBuilder clusterTCPHibernateEHCacheNodesIPAddresses = new StringBuilder();
        StringBuilder clusterTCPJahiaEHCacheNodesIPAddresses = new StringBuilder();

        List<String> clusterTCPServiceRemotePorts = jahiaConfigInterface.getClusterTCPServiceRemotePorts();
        if (clusterTCPServiceRemotePorts == null) {
            clusterTCPServiceRemotePorts = new ArrayList<String>();
        }
        if (clusterTCPServiceRemotePorts.size() == 0) {
            for (int i=0; i < clusterNodes.size(); i++) {
                clusterTCPServiceRemotePorts.add("7840");
            }
        }
        List<String> clusterTCPEHCacheHibernateRemotePorts = jahiaConfigInterface.getClusterTCPEHCacheHibernateRemotePorts();
        if (clusterTCPEHCacheHibernateRemotePorts == null) {
            clusterTCPEHCacheHibernateRemotePorts = new ArrayList<String>();
        }
        if (clusterTCPEHCacheHibernateRemotePorts.size() == 0) {
            for (int i=0; i < clusterNodes.size(); i++) {
                clusterTCPEHCacheHibernateRemotePorts.add("7860");
            }
        }
        List<String> clusterTCPEHCacheJahiaRemotePorts = jahiaConfigInterface.getClusterTCPEHCacheJahiaRemotePorts();
        if (clusterTCPEHCacheJahiaRemotePorts == null) {
            clusterTCPEHCacheJahiaRemotePorts = new ArrayList<String>();
        }
        if (clusterTCPEHCacheJahiaRemotePorts.size() == 0) {
            for (int i=0; i < clusterNodes.size(); i++) {
                clusterTCPEHCacheJahiaRemotePorts.add("7870");
            }
        }

        for (int i = 0; i < clusterNodes.size(); i++) {
            clusterTCPServiceNodesIPAddresses.append(clusterNodes.get(i) + "["+clusterTCPServiceRemotePorts.get(i)+"]");
            clusterTCPHibernateEHCacheNodesIPAddresses.append(clusterNodes.get(i) + "["+clusterTCPEHCacheHibernateRemotePorts.get(i)+"]");
            clusterTCPJahiaEHCacheNodesIPAddresses.append(clusterNodes.get(i) + "["+clusterTCPEHCacheJahiaRemotePorts.get(i)+"]");
            if (i < clusterNodes.size() - 1) {
                clusterTCPServiceNodesIPAddresses.append(",");
                clusterTCPHibernateEHCacheNodesIPAddresses.append(",");
                clusterTCPJahiaEHCacheNodesIPAddresses.append(",");
            }

        }
        properties.setProperty("cluster.tcp.service.nodes.ip_address", clusterTCPServiceNodesIPAddresses.toString());
        properties.setProperty("cluster.tcp.service.port", jahiaConfigInterface.getClusterTCPServicePort());
        properties.setProperty("cluster.tcp.ehcache.hibernate.nodes.ip_address", clusterTCPHibernateEHCacheNodesIPAddresses.toString());
        properties.setProperty("cluster.tcp.ehcache.hibernate.port", jahiaConfigInterface.getClusterTCPEHCacheHibernatePort());
        properties.setProperty("cluster.tcp.ehcache.jahia.nodes.ip_address", clusterTCPJahiaEHCacheNodesIPAddresses.toString());
        properties.setProperty("cluster.tcp.ehcache.jahia.port", jahiaConfigInterface.getClusterTCPEHCacheJahiaPort());
        properties.setProperty("cluster.tcp.num_initial_members",String.valueOf(clusterNodes.size()));
        properties.setProperty("processingServer", jahiaConfigInterface.getProcessingServer());
    }

}
