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

import junit.framework.TestCase;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Common abstract class for setting up all the stuff that is needed to test configurators, such as resource paths,
 * Oracle and MySQL configurations, default properties, etc...
 *
 * @author loom
 * Date: Oct 27, 2009
 * Time: 9:04:52 AM
 */
public abstract class AbstractConfiguratorTestCase extends TestCase {

    Properties oracleDBProperties = new Properties();
    Properties mysqlDBProperties = new Properties();

    JahiaConfigBean websphereOracleConfigBean;
    JahiaConfigBean tomcatMySQLConfigBean;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        oracleDBProperties = new Properties();
        oracleDBProperties.load(this.getClass().getClassLoader().getResourceAsStream("configurators/WEB-INF/var/db/oracle.script"));
        mysqlDBProperties = new Properties();
        mysqlDBProperties.load(this.getClass().getClassLoader().getResourceAsStream("configurators/WEB-INF/var/db/mysql.script"));

        URL configuratorsResourceURL = this.getClass().getClassLoader().getResource("configurators");
        File configuratorsFile = new File(configuratorsResourceURL.toURI());

        websphereOracleConfigBean = new JahiaConfigBean();
        websphereOracleConfigBean.setDatabaseType("oracle");
        websphereOracleConfigBean.setCluster_activated("true");
        websphereOracleConfigBean.setClusterTCPBindAddress("1.2.3.4");
        websphereOracleConfigBean.setCluster_node_serverId("jahiaServer1");
        websphereOracleConfigBean.setProcessingServer("true");
        websphereOracleConfigBean.setClusterTCPBindPort("7870");

        websphereOracleConfigBean.setJahiaRootUsername("superUser");
        websphereOracleConfigBean.setJahiaRootPassword("password");
        websphereOracleConfigBean.setJahiaRootFirstname("Jahia");
        websphereOracleConfigBean.setJahiaRootLastname("Root");
        websphereOracleConfigBean.setJahiaRootEmail("root@jahia.org");
        websphereOracleConfigBean.setJahiaRootPreferredLang("de");
        
        websphereOracleConfigBean.getJahiaProperties().put("jahia.dm.viewer.enabled", "true");
        websphereOracleConfigBean.getJahiaProperties().put("jahia.dm.viewer.pdf2swf", "c:\\Program Files (x86)\\SWFTools\\pdf2swf.exe");


        tomcatMySQLConfigBean = new JahiaConfigBean();
        tomcatMySQLConfigBean.setDatabaseType("mysql");
        tomcatMySQLConfigBean.setCluster_activated("false");
        tomcatMySQLConfigBean.setCluster_node_serverId("jahiaServer1");
        tomcatMySQLConfigBean.setProcessingServer("true");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        
    }

    public abstract void testUpdateConfiguration() throws Exception;

}
