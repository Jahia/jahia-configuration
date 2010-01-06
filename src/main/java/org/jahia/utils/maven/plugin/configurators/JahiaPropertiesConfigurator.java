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

import org.jahia.utils.maven.plugin.buildautomation.PropertiesManager;
import org.jahia.utils.maven.plugin.buildautomation.JahiaPropertiesBean;

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

    public JahiaPropertiesConfigurator(Map dbProperties, JahiaPropertiesBean jahiaPropertiesBean) {
        super(dbProperties, jahiaPropertiesBean);
    }

    public void updateConfiguration(String sourceJahiaPath, String targetJahiaPath) throws IOException {
        properties = new PropertiesManager(sourceJahiaPath, targetJahiaPath);
        properties.setProperty("release", jahiaPropertiesBean.getRelease());
        properties.setProperty("server", jahiaPropertiesBean.getServer());
//        properties.setProperty("serverHomeDiskPath", targetServerDirectory);
        properties.setProperty("jahiaEtcDiskPath", jahiaPropertiesBean.getJahiaEtcDiskPath());
        properties.setProperty("jahiaVarDiskPath", jahiaPropertiesBean.getJahiaVarDiskPath());
        properties.setProperty("jahiaNewTemplatesDiskPath", jahiaPropertiesBean.getJahiaNewTemplatesDiskPath());
        properties.setProperty("jahiaSharedTemplatesDiskPath", jahiaPropertiesBean.getJahiaSharedTemplatesDiskPath());
        properties.setProperty("jahiaNewWebAppsDiskPath", jahiaPropertiesBean.getJahiaNewWebAppsDiskPath());
        properties.setProperty("jahiaFileRepositoryDiskPath", jahiaPropertiesBean.getJahiaFileRepositoryDiskPath());
        properties.setProperty("jahiaFilesBigTextDiskPath", jahiaPropertiesBean.getJahiaFilesBigTextDiskPath());
        properties.setProperty("jahiaFilesTemplatesDiskPath", jahiaPropertiesBean.getJahiaFilesTemplatesDiskPath());
        properties.setProperty("jahiaTemplatesHttpPath", jahiaPropertiesBean.getJahiaTemplatesHttpPath());
        properties.setProperty("jahiaEnginesHttpPath", jahiaPropertiesBean.getJahiaEnginesHttpPath());
        properties.setProperty("jahiaJavaScriptHttpPath", jahiaPropertiesBean.getJahiaJavaScriptHttpPath());
        properties.setProperty("jahiaWebAppsDeployerBaseURL", jahiaPropertiesBean.getJahiaWebAppsDeployerBaseURL());
        properties.setProperty("datasource_name", jahiaPropertiesBean.getDatasource_name());
        properties.setProperty("outputCacheActivated", jahiaPropertiesBean.getOutputCacheActivated());
        properties.setProperty("outputCacheDefaultExpirationDelay", jahiaPropertiesBean.getOutputCacheDefaultExpirationDelay());
        properties.setProperty("outputCacheExpirationOnly", jahiaPropertiesBean.getOutputCacheExpirationOnly());
        properties.setProperty("outputContainerCacheActivated", jahiaPropertiesBean.getOutputContainerCacheActivated());
        properties.setProperty("containerCacheDefaultExpirationDelay", jahiaPropertiesBean.getContainerCacheDefaultExpirationDelay());
        properties.setProperty("outputContainerCacheActivated", jahiaPropertiesBean.getOutputContainerCacheActivated());
        properties.setProperty("jahiaImportsDiskPath", jahiaPropertiesBean.getJahiaImportsDiskPath());
        properties.setProperty("containerCacheDefaultExpirationDelay", jahiaPropertiesBean.getContainerCacheDefaultExpirationDelay());
        properties.setProperty("containerCacheLiveModeOnly", jahiaPropertiesBean.getContainerCacheLiveModeOnly());
        properties.setProperty("esiCacheActivated", jahiaPropertiesBean.getEsiCacheActivated());
        properties.setProperty("datasource.name", jahiaPropertiesBean.getDatasource_name());
        properties.setProperty("JahiaWebAppsDeployerService", jahiaPropertiesBean.getJahia_WebApps_Deployer_Service());
        properties.setProperty("defautSite", jahiaPropertiesBean.getDefautSite());
        properties.setProperty("cluster.activated", jahiaPropertiesBean.getCluster_activated());
        properties.setProperty("localIp", jahiaPropertiesBean.getLocalIp());
        properties.setProperty("localPort", jahiaPropertiesBean.getLocalPort());
        if (!"8080".equals(jahiaPropertiesBean.getLocalPort())) {
            properties.setProperty("localAccessUri", "http://" + jahiaPropertiesBean.getLocalIp() + ":" + jahiaPropertiesBean.getLocalPort());
        } else {
            if ("localhost".equals(jahiaPropertiesBean.getLocalIp())) {
                properties.removeProperty("localAccessUri");
            }
        }
        properties.setProperty("cluster.node.serverId", jahiaPropertiesBean.getCluster_node_serverId());
        properties.setProperty("db_script", jahiaPropertiesBean.getDb_script());
        properties.setProperty("developmentMode", jahiaPropertiesBean.getDevelopmentMode());

        if (jahiaPropertiesBean.getCluster_activated().equals("true")) {
            addClusterNodes(jahiaPropertiesBean.getClusterNodes());

        }
        properties.setProperty("bigtext.service", jahiaPropertiesBean.getBigtext_service());
        properties.setProperty("cluster.node.serverId", jahiaPropertiesBean.getCluster_node_serverId());
        properties.setProperty("mail_paranoia", "Disabled");
        
        properties.setProperty("hibernate.dialect", jahiaPropertiesBean.getHibernateDialect());
        properties.setProperty("nested.transaction.allowed", jahiaPropertiesBean.getNestedTransactionAllowed());

        properties.storeProperties(sourceJahiaPath, targetJahiaPath);
    }

    private void addClusterNodes(List<String> clusterNodes) {

        final String propvalue = jahiaPropertiesBean.getLocalIp();
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
        if(properties.getProperty("cluster.tcp.esicontentids.nodes.ip_address") != null){
            properties.setProperty("cluster.tcp.idgenerator.nodes.ip_address", clustertcpidgeneratornodesip_address);
            properties.setProperty("cluster.tcp.esicontentids.nodes.ip_address", clustertcpesicontentidsnodesip_address);
            properties.setProperty("cluster.tcp.hibernate.nodes.ip_address", clustertcphibernatenodesip_address);
        } else if(properties.getProperty("ehcache.hibernate.file") != null){
            properties.setProperty("cluster.tcp.ehcache.hibernate.nodes.ip_address", clustertcpesicontentidsnodesip_address);
            properties.setProperty("cluster.tcp.ehcache.jahia.nodes.ip_address", clustertcphibernatenodesip_address);
            properties.setProperty("ehcache.hibernate.file","ehcache-hibernate_cluster.xml");
            properties.setProperty("ehcache.jahia.file","ehcache-jahia_cluster.xml");
        }
        properties.setProperty("cluster.tcp.num_initial_members",clusterNodes.size()+1>=3?"3":"2");
        properties.setProperty("processingServer", jahiaPropertiesBean.getProcessingServer());
    }

}