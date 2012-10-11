package org.jahia.server.config.tool.plugin.clustertool;

import org.jahia.server.config.tool.framework.WebFragmentActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Bundle activator for Cluster Tool plugin
 */
public class ClusterToolActivator extends WebFragmentActivator {
    private ServiceRegistration clusterToolConfiguratorRegistration;
    private ClusterToolConfigurator clusterToolConfigurator;

    @Override
    public void start(BundleContext context) throws Exception {
        final ClusterToolServlet clusterToolServlet = new ClusterToolServlet();
        servletMappings.put("/clusterTool", clusterToolServlet);
        Dictionary props = new Hashtable();
        props.put("service.pid", "org.jahia.server.config.tool.ClusterToolConfigurator");
        clusterToolConfigurator = new ClusterToolConfigurator();
        clusterToolConfiguratorRegistration = context.registerService(ManagedService.class.getName(),
                clusterToolConfigurator, props);

        ServiceReference configurationAdminReference =
                context.getServiceReference(ConfigurationAdmin.class.getName());

        if (configurationAdminReference != null) {
            ConfigurationAdmin confAdmin = (ConfigurationAdmin) context.getService(configurationAdminReference);
            clusterToolServlet.setConfigurationAdmin(confAdmin);

            Configuration configuration = confAdmin.getConfiguration("org.jahia.server.config.tool.ClusterToolConfigurator");

            props = configuration.getProperties();

            // if null, the configuration is new
            if (props == null) {
                props = new Hashtable();
            }

            configuration.update(props);

        }

        super.start(context);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (clusterToolConfiguratorRegistration != null) {
            clusterToolConfiguratorRegistration.unregister();
        }
        super.stop(context);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
