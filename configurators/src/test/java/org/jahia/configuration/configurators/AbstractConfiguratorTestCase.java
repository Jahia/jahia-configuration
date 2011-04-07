package org.jahia.configuration.configurators;

import junit.framework.TestCase;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

import org.jahia.configuration.configurators.JahiaConfigBean;

/**
 * Common abstract class for setting up all the stuff that is needed to test configurators, such as resource paths,
 * Oracle and MySQL configurations, default properties, etc...
 *
 * @author loom
 *         Date: Oct 27, 2009
 *         Time: 9:04:52 AM
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
        websphereOracleConfigBean.setTargetServerType("was");
        websphereOracleConfigBean.setTargetServerVersion("6.1.0.25");
        websphereOracleConfigBean.setTargetConfigurationDirectory(configuratorsFile.toString());
        websphereOracleConfigBean.setSourceWebAppDir(configuratorsFile.toString());
        websphereOracleConfigBean.setCluster_activated("true");
        websphereOracleConfigBean.setClusterStartIpAddress("1.2.3.4");
        websphereOracleConfigBean.setCluster_node_serverId("jahiaServer1");
        websphereOracleConfigBean.setProcessingServer("true");
        List<String> clusterNodes = new ArrayList<String>();
        clusterNodes.add("2.3.4.5");
        clusterNodes.add("3.4.5.6");
        clusterNodes.add("4.5.6.7");
        websphereOracleConfigBean.setClusterNodes(clusterNodes);
        List<String> clusterTCPServiceRemotePorts = new ArrayList<String>();
        clusterTCPServiceRemotePorts.add("7840");
        clusterTCPServiceRemotePorts.add("8840");
        clusterTCPServiceRemotePorts.add("9840");
        clusterTCPServiceRemotePorts.add("10840");
        websphereOracleConfigBean.setClusterTCPServiceRemotePorts(clusterTCPServiceRemotePorts);
        List<String> clusterTCPEHCacheHibernateRemotePorts = new ArrayList<String>();
        clusterTCPEHCacheHibernateRemotePorts.add("7860");
        clusterTCPEHCacheHibernateRemotePorts.add("8860");
        clusterTCPEHCacheHibernateRemotePorts.add("9860");
        clusterTCPEHCacheHibernateRemotePorts.add("10860");
        websphereOracleConfigBean.setClusterTCPEHCacheHibernateRemotePorts(clusterTCPEHCacheHibernateRemotePorts);
        List<String> clusterTCPEHCacheJahiaRemotePorts = new ArrayList<String>();
        clusterTCPEHCacheJahiaRemotePorts.add("7870");
        clusterTCPEHCacheJahiaRemotePorts.add("8870");
        clusterTCPEHCacheJahiaRemotePorts.add("9870");
        clusterTCPEHCacheJahiaRemotePorts.add("10870");
        websphereOracleConfigBean.setClusterTCPEHCacheJahiaRemotePorts(clusterTCPEHCacheJahiaRemotePorts);
        websphereOracleConfigBean.setClusterTCPServicePort("7840");
        websphereOracleConfigBean.setClusterTCPEHCacheHibernatePort("7860");
        websphereOracleConfigBean.setClusterTCPEHCacheJahiaPort("7870");

        websphereOracleConfigBean.setJahiaRootUsername("superUser");
        websphereOracleConfigBean.setJahiaRootPassword("password");
        websphereOracleConfigBean.setJahiaRootFirstname("Jahia");
        websphereOracleConfigBean.setJahiaRootLastname("Root");
        websphereOracleConfigBean.setJahiaRootEmail("root@jahia.org");

        tomcatMySQLConfigBean = new JahiaConfigBean();
        tomcatMySQLConfigBean.setDatabaseType("mysql");
        tomcatMySQLConfigBean.setTargetServerType("tomcat");
        tomcatMySQLConfigBean.setTargetServerVersion("6");
        tomcatMySQLConfigBean.setTargetConfigurationDirectory(configuratorsFile.toString());
        tomcatMySQLConfigBean.setSourceWebAppDir(configuratorsFile.toString());
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
