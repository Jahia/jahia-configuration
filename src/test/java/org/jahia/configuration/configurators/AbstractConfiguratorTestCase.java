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
        websphereOracleConfigBean.setTargetServerType("was");
        websphereOracleConfigBean.setTargetServerVersion("6.1.0.25");
        websphereOracleConfigBean.setTargetConfigurationDirectory(configuratorsFile.toString());
        websphereOracleConfigBean.setSourceWebAppDir(configuratorsFile.toString());
        websphereOracleConfigBean.setCluster_activated("true");
        websphereOracleConfigBean.setClusterStartIpAddress("1.2.3.4");
        websphereOracleConfigBean.setCluster_node_serverId("jahiaServer1");
        websphereOracleConfigBean.setProcessingServer("true");
        websphereOracleConfigBean
                .setClusterTCPEHCacheHibernateHosts(JahiaGlobalConfigurator
                        .fromString("1.2.3.4[7860] 2.3.4.5[8860] 3.4.5.6[9860] 4.5.6.7[10860]"));
        websphereOracleConfigBean
                .setClusterTCPEHCacheJahiaHosts(JahiaGlobalConfigurator
                        .fromString("1.2.3.4[7870] 2.3.4.5[8870] 3.4.5.6[9870] 4.5.6.7[10870]"));
        websphereOracleConfigBean.setClusterTCPEHCacheHibernatePort("7860");
        websphereOracleConfigBean.setClusterTCPEHCacheJahiaPort("7870");

        websphereOracleConfigBean.setJahiaRootUsername("superUser");
        websphereOracleConfigBean.setJahiaRootPassword("password");
        websphereOracleConfigBean.setJahiaRootFirstname("Jahia");
        websphereOracleConfigBean.setJahiaRootLastname("Root");
        websphereOracleConfigBean.setJahiaRootEmail("root@jahia.org");
        
        websphereOracleConfigBean.getJahiaProperties().put("jahia.dm.viewer.enabled", "true");
        websphereOracleConfigBean.getJahiaProperties().put("jahia.dm.viewer.pdf2swf", "c:\\Program Files (x86)\\SWFTools\\pdf2swf.exe");
        
        websphereOracleConfigBean.getJahiaAdvancedProperties().put("auth.spnego.bypassForUrls", "/(administration|cms/login|cms/logout|css)((\\?|/).*)?");
        
        websphereOracleConfigBean.setLdapActivated("true");
        Map<String, String> ldap = new HashMap<String, String>();
        ldap.put("url", "ldap://10.8.37.17:389/");
        ldap.put("public.bind.dn", "public.bind.dn");
        ldap.put("public.bind.password", "ldapadmin");
        ldap.put("uid.search.name", "o=jahia");
        websphereOracleConfigBean.setUserLdapProviderProperties(ldap);
        ldap = new HashMap<String, String>();
        ldap.put("url", "ldap://10.8.37.17:389/");
        ldap.put("public.bind.dn", "public.bind.dn");
        ldap.put("public.bind.password", "ldapadmin");
        ldap.put("search.name", "dc=jahia");
        websphereOracleConfigBean.setGroupLdapProviderProperties(ldap);
        websphereOracleConfigBean.setJeeApplicationLocation(configuratorsFile.toString());
        websphereOracleConfigBean.setJeeApplicationModuleList("jahia-war:web:ROOT.war:,portlet-testsuite:web:websphere-testsuite.war:testsuite");

        tomcatMySQLConfigBean = new JahiaConfigBean();
        tomcatMySQLConfigBean.setDatabaseType("mysql");
        tomcatMySQLConfigBean.setTargetServerType("tomcat");
        tomcatMySQLConfigBean.setTargetConfigurationDirectory(configuratorsFile.toString());
        tomcatMySQLConfigBean.setSourceWebAppDir(configuratorsFile.toString());
        tomcatMySQLConfigBean.setCluster_activated("false");
        tomcatMySQLConfigBean.setCluster_node_serverId("jahiaServer1");
        tomcatMySQLConfigBean.setProcessingServer("true");
        tomcatMySQLConfigBean.setJeeApplicationModuleList(null);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        
    }

    public abstract void testUpdateConfiguration() throws Exception;

}
