package org.jahia.server.config.tool.plugin.clustertool;

import org.jahia.server.config.tool.framework.WebFragmentActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Bundle activator for Cluster Tool plugin
 */
public class ClusterToolActivator extends WebFragmentActivator {
    private ServiceTracker repositoryAdmin;

    @Override
    public void start(BundleContext context) throws Exception {
        final ClusterToolServlet clusterToolServlet = new ClusterToolServlet();
        servletMappings.put("/clusterTool", clusterToolServlet);
        super.start(context);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
