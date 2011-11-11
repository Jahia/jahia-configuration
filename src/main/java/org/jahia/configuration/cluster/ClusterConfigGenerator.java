package org.jahia.configuration.cluster;

import org.apache.commons.vfs.FileSystemException;
import org.codehaus.plexus.util.FileUtils;
import org.jahia.configuration.configurators.ConfigFile;
import org.jahia.configuration.configurators.JahiaAdvancedPropertiesConfigurator;
import org.jahia.configuration.configurators.JahiaConfigBean;
import org.jahia.configuration.logging.AbstractLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This class generates cluster configuration for a pre-defined list of nodes in separate directories. It can also
 * copy files to various cluster staging directories, ready to be uploaded to each node.
 */

public class ClusterConfigGenerator {

    public class FileConfigFile implements ConfigFile {

        File configFile;

        public FileConfigFile(File configFile) {
            this.configFile = configFile;
        }

        public URI getURI() throws IOException, URISyntaxException {
            return configFile.toURI();
        }

        public InputStream getInputStream() throws IOException {
            return new FileInputStream(configFile);  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    protected AbstractLogger logger;
    private ClusterConfigBean clusterConfigBean;

    public ClusterConfigGenerator(AbstractLogger logger, File parentDirectory) throws Exception {
        this.logger = logger;
        clusterConfigBean = new ClusterConfigBean(logger, parentDirectory);
    }

    public void generateClusterConfiguration() throws Exception {

        JahiaConfigBean jahiaConfigBean = new JahiaConfigBean();
        jahiaConfigBean.setCluster_activated("true");

        File templateDirectory = new File(clusterConfigBean.getTemplateDirectoryName());
        if (!templateDirectory.exists()) {
            throw new Exception("Template directory "+templateDirectory+"does not exist, aborting configuration generation !");
        }

        File jahiaAdvancedPropertiesConfigFile = new File(templateDirectory, clusterConfigBean.getJahiaAdvancedPropertyRelativeFileLocation());
        if (!jahiaAdvancedPropertiesConfigFile.exists()) {
            throw new Exception("Couldn't find Jahia advanced property file template at " + jahiaAdvancedPropertiesConfigFile);
        }

        // first let's create the node directories
        File nodesDirectory = new File(clusterConfigBean.getNodesDirectoryName());
        if (!nodesDirectory.exists()) {
            nodesDirectory.mkdirs();
        }

        for (int i=0; i < clusterConfigBean.getNumberOfNodes(); i++) {
            String currentNodeId = clusterConfigBean.getNodeNamePrefix() + Integer.toString(i+1);
            File currentNodeDirectory = new File(nodesDirectory, currentNodeId);
            if (!currentNodeDirectory.exists()) {
                logger.info("Creating node directory " + currentNodeDirectory);
                boolean nodeDirectoryCreated = currentNodeDirectory.mkdirs();
                if (!nodeDirectoryCreated) {
                    logger.error("Error while creating node directory !");
                }
            }

            logger.info("Copying template files to " + currentNodeDirectory + "...");
            FileUtils.copyDirectoryStructure(templateDirectory, currentNodeDirectory);

            jahiaConfigBean.setCluster_node_serverId(currentNodeId);
            if (i == 0) {
                jahiaConfigBean.setProcessingServer("true");
            } else {
                jahiaConfigBean.setProcessingServer("false");
            }
            jahiaConfigBean.setClusterStartIpAddress(clusterConfigBean.getInternalIPs().get(i));
            jahiaConfigBean.setClusterNodes(clusterConfigBean.getInternalIPs());
            jahiaConfigBean.setClusterTCPEHCacheHibernateHosts(clusterConfigBean.getInternalIPs());
            jahiaConfigBean.setClusterTCPEHCacheJahiaHosts(clusterConfigBean.getInternalIPs());
            JahiaAdvancedPropertiesConfigurator jahiaAdvancedPropertiesConfigurator = new JahiaAdvancedPropertiesConfigurator(logger, jahiaConfigBean);

            try {
                jahiaAdvancedPropertiesConfigurator.updateConfiguration(new FileConfigFile(jahiaAdvancedPropertiesConfigFile), new File(currentNodeDirectory, clusterConfigBean.getJahiaAdvancedPropertyRelativeFileLocation()).getPath());
            } catch (FileSystemException fse) {
                // in the case we cannot access the file, it means we should not do the advanced configuration, which is expected for Jahia "core".
            }
        }

    }

    public ClusterConfigBean getClusterConfigBean() {
        return clusterConfigBean;
    }
}
