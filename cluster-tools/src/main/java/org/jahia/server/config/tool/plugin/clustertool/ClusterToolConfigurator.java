package org.jahia.server.config.tool.plugin.clustertool;

import org.jahia.configuration.cluster.tool.ClusterConfigBean;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.util.Dictionary;

/**
 * OSGi configurator managed service class for cluster tool configuration.
 */
public class ClusterToolConfigurator implements ManagedService {

    private ClusterConfigBean clusterConfigBean = null;

    public ClusterToolConfigurator() {
    }

    @Override
    public void updated(Dictionary dictionary) throws ConfigurationException {
        if (dictionary == null) {
           // no configuration from configuration admin
           // or old configuration has been deleted
            clusterConfigBean = null;
        } else {
           // apply configuration from config admin
            clusterConfigBean = new ClusterConfigBean(dictionary, null);
        }
    }

    public ClusterConfigBean getClusterConfigBean() {
        return clusterConfigBean;
    }
}
