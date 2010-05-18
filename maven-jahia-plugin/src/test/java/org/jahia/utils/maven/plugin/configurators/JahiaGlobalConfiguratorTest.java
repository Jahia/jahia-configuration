package org.jahia.utils.maven.plugin.configurators;

import junit.framework.TestCase;
import org.jahia.utils.maven.plugin.SLF4JLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO Comment me
 *
 * @author loom
 *         Date: May 11, 2010
 *         Time: 2:26:38 PM
 */
public class JahiaGlobalConfiguratorTest extends TestCase {

    public void testGlobalConfiguration() throws Exception {

        JahiaConfigBean websphereDerbyConfigBean;

        Logger logger = LoggerFactory.getLogger(JahiaGlobalConfiguratorTest.class);

        URL configuratorsResourceURL = this.getClass().getClassLoader().getResource("configurators");
        File configuratorsFile = new File(configuratorsResourceURL.toURI());

        websphereDerbyConfigBean = new JahiaConfigBean();
        websphereDerbyConfigBean.setDatabaseType("derby");
        websphereDerbyConfigBean.setTargetServerType("was");
        websphereDerbyConfigBean.setTargetServerVersion("6.1.0.25");
        websphereDerbyConfigBean.setTargetConfigurationDirectory(configuratorsFile.toString());
        websphereDerbyConfigBean.setSourceWebAppDir(configuratorsFile.toString());
        websphereDerbyConfigBean.setCluster_activated("true");
        websphereDerbyConfigBean.setCluster_node_serverId("jahiaServer1");
        websphereDerbyConfigBean.setLocalIp("1.2.3.4");
        websphereDerbyConfigBean.setLocalPort("9080");
        websphereDerbyConfigBean.setProcessingServer("true");
        List<String> clusterNodes = new ArrayList<String>();
        clusterNodes.add("2.3.4.5");
        clusterNodes.add("3.4.5.6");
        clusterNodes.add("4.5.6.7");
        websphereDerbyConfigBean.setClusterNodes(clusterNodes);

        JahiaGlobalConfigurator jahiaGlobalConfigurator = new JahiaGlobalConfigurator(new SLF4JLogger(logger), websphereDerbyConfigBean);
        jahiaGlobalConfigurator.execute();
        
    }
}
