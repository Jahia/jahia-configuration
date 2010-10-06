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

package org.jahia.configuration.configurators;

import java.util.List;
import java.util.Map;
import java.io.IOException;

/**
 * Property configuration for the jahia.properties file.
 * User: islam
 * Date: 16 juil. 2008
 * Time: 10:09:49
 */
public class JahiaPropertiesConfigurator extends AbstractConfigurator {

    private PropertiesManager properties;

    public JahiaPropertiesConfigurator(Map dbProperties, JahiaConfigInterface jahiaConfigInterface) {
        super(dbProperties, jahiaConfigInterface);
    }

    public void updateConfiguration(String sourceJahiaPath, String targetJahiaPath) throws IOException {
        properties = new PropertiesManager(sourceJahiaPath, targetJahiaPath);
        // context  path
        properties.setProperty("jahia.contextPath", jahiaConfigInterface.getContextPath());
        properties.setProperty("release", jahiaConfigInterface.getRelease());
        properties.setProperty("server", jahiaConfigInterface.getTargetServerType());
        properties.setProperty("serverVersion", jahiaConfigInterface.getTargetServerVersion());
        properties.setProperty("serverHome", jahiaConfigInterface.getTargetServerDirectory() != null ? jahiaConfigInterface.getTargetServerDirectory().replace("\\\\", "/").replace("\\", "/") : null);
        properties.setProperty("jahiaEtcDiskPath", jahiaConfigInterface.getJahiaEtcDiskPath());
        properties.setProperty("jahiaVarDiskPath", jahiaConfigInterface.getJahiaVarDiskPath());
        properties.setProperty("jahiaSharedTemplatesDiskPath", jahiaConfigInterface.getJahiaSharedModulesDiskPath());
        properties.setProperty("jahiaTemplatesHttpPath", jahiaConfigInterface.getJahiaModulesHttpPath());
        properties.setProperty("jahiaEnginesHttpPath", jahiaConfigInterface.getJahiaEnginesHttpPath());
        properties.setProperty("jahiaJavaScriptHttpPath", jahiaConfigInterface.getJahiaJavaScriptHttpPath());
        properties.setProperty("jahiaWebAppsDeployerBaseURL", jahiaConfigInterface.getJahiaWebAppsDeployerBaseURL());
        properties.setProperty("jahiaImportsDiskPath", jahiaConfigInterface.getJahiaImportsDiskPath());
        properties.setProperty("cluster.activated", jahiaConfigInterface.getCluster_activated());
        properties.setProperty("cluster.node.serverId", jahiaConfigInterface.getCluster_node_serverId());
        properties.setProperty("db_script", jahiaConfigInterface.getDb_script());
        properties.setProperty("developmentMode", jahiaConfigInterface.getDevelopmentMode());

        if (jahiaConfigInterface.getCluster_activated().equals("true")) {
            addClusterNodes(jahiaConfigInterface.getClusterNodes());

        }
        properties.setProperty("cluster.node.serverId", jahiaConfigInterface.getCluster_node_serverId());
        properties.setProperty("mail_paranoia", jahiaConfigInterface.getMailParanoia());
        if (jahiaConfigInterface.getMailServer() != null && jahiaConfigInterface.getMailServer().length() > 0) {
            properties.setProperty("mail_service_activated", "true");
            properties.setProperty("mail_server", jahiaConfigInterface.getMailServer());
            properties.setProperty("mail_administrator", jahiaConfigInterface.getMailAdministrator());            
            properties.setProperty("mail_from", jahiaConfigInterface.getMailFrom());            
        }
        properties.setProperty("hibernate.dialect", getDBProperty("jahia.database.hibernate.dialect"));
        properties.setProperty("nested.transaction.allowed", getDBProperty("jahia.nested_transaction_allowed"));
        
        properties.storeProperties(sourceJahiaPath, targetJahiaPath);
        
        configureScheduler();
    }

	private void addClusterNodes(List<String> clusterNodes) {

        final String propvalue = jahiaConfigInterface.getClusterStartIpAddress();
        properties.setProperty("cluster.tcp.start.ip_address", propvalue);
        String clusterTtcpServiceNodesIp_address = propvalue+"[7840],";
        String clustertcpidgeneratornodesip_address = propvalue+"[7850],";
        String clustertcpesicontentidsnodesip_address = propvalue+"[7860],";
        String clustertcphibernatenodesip_address = propvalue+"[7870],";
        for (int i = 0; i < clusterNodes.size(); i++) {

            if (i == clusterNodes.size() - 1) {
                clusterTtcpServiceNodesIp_address = clusterTtcpServiceNodesIp_address + clusterNodes.get(i) + "[7840]";
                clustertcpidgeneratornodesip_address = clustertcpidgeneratornodesip_address + clusterNodes.get(i) + "[7850]";
                clustertcpesicontentidsnodesip_address = clustertcpesicontentidsnodesip_address + clusterNodes.get(i) + "[7860]";
                clustertcphibernatenodesip_address = clustertcphibernatenodesip_address + clusterNodes.get(i) + "[7870]";
            } else {

                clusterTtcpServiceNodesIp_address = clusterTtcpServiceNodesIp_address + clusterNodes.get(i) + "[7840],";
                clustertcpidgeneratornodesip_address = clustertcpidgeneratornodesip_address + clusterNodes.get(i) + "[7850],";
                clustertcpesicontentidsnodesip_address = clustertcpesicontentidsnodesip_address + clusterNodes.get(i) + "[7860],";
                clustertcphibernatenodesip_address = clustertcphibernatenodesip_address + clusterNodes.get(i) + "[7870],";
            }


        }
        properties.setProperty("cluster.tcp.service.nodes.ip_address", clusterTtcpServiceNodesIp_address);
        if(properties.getProperty("ehcache.hibernate.file") != null){
            properties.setProperty("cluster.tcp.ehcache.hibernate.nodes.ip_address", clustertcpesicontentidsnodesip_address);
            properties.setProperty("cluster.tcp.ehcache.jahia.nodes.ip_address", clustertcphibernatenodesip_address);
            properties.setProperty("ehcache.hibernate.file","ehcache-hibernate_cluster.xml");
            properties.setProperty("ehcache.jahia.file","ehcache-jahia_cluster.xml");
        }
        properties.setProperty("cluster.tcp.num_initial_members",clusterNodes.size()+1>=3?"3":"2");
        properties.setProperty("processingServer", jahiaConfigInterface.getProcessingServer());
    }


	private void configureScheduler() {
		String delegate = (String) dbProperties
				.get("jahia.quartz.jdbcDelegate");
		if (delegate == null || delegate.length() == 0) {
			delegate = "org.quartz.impl.jdbcjobstore.StdJDBCDelegate";
		}

		properties.setProperty("org.quartz.driverDelegateClass", delegate);
	}
}
