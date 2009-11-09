package org.jahia.utils.maven.plugin.configurators;

import junit.framework.TestCase;

import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

import org.jahia.utils.maven.plugin.buildautomation.JahiaPropertiesBean;

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

    JahiaPropertiesBean websphereOraclePropertiesBean;
    JahiaPropertiesBean tomcatMySQLPropertiesBean;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        oracleDBProperties = new Properties();
        oracleDBProperties.load(this.getClass().getClassLoader().getResourceAsStream("configurators/WEB-INF/var/db/oracle.script"));
        mysqlDBProperties = new Properties();
        mysqlDBProperties.load(this.getClass().getClassLoader().getResourceAsStream("configurators/WEB-INF/var/db/mysql.script"));

        websphereOraclePropertiesBean = new JahiaPropertiesBean();
        websphereOraclePropertiesBean.setServer("was");
        websphereOraclePropertiesBean.setCluster_activated("true");
        websphereOraclePropertiesBean.setCluster_node_serverId("jahiaServer1");
        websphereOraclePropertiesBean.setLocalIp("1.2.3.4");
        websphereOraclePropertiesBean.setLocalPort("9080");
        websphereOraclePropertiesBean.setProcessingServer("true");
        websphereOraclePropertiesBean.setHibernateDialect(oracleDBProperties.getProperty("jahia.database.hibernate.dialect"));
        websphereOraclePropertiesBean.setNestedTransactionAllowed(oracleDBProperties.getProperty("jahia.nested_transaction_allowed"));
        List<String> clusterNodes = new ArrayList<String>();
        clusterNodes.add("2.3.4.5");
        clusterNodes.add("3.4.5.6");
        clusterNodes.add("4.5.6.7");
        websphereOraclePropertiesBean.setClusterNodes(clusterNodes);

        tomcatMySQLPropertiesBean = new JahiaPropertiesBean();
        tomcatMySQLPropertiesBean.setServer("tomcat");
        tomcatMySQLPropertiesBean.setCluster_activated("false");
        tomcatMySQLPropertiesBean.setCluster_node_serverId("jahiaServer1");
        tomcatMySQLPropertiesBean.setLocalIp("localhost");
        tomcatMySQLPropertiesBean.setLocalPort("8080");
        tomcatMySQLPropertiesBean.setProcessingServer("true");
        tomcatMySQLPropertiesBean.setHibernateDialect(mysqlDBProperties.getProperty("jahia.database.hibernate.dialect"));
        tomcatMySQLPropertiesBean.setNestedTransactionAllowed(mysqlDBProperties.getProperty("jahia.nested_transaction_allowed"));        
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        
    }

    public abstract void testUpdateConfiguration() throws Exception;

}
