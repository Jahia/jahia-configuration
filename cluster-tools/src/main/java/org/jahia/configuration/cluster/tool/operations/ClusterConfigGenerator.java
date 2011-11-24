package org.jahia.configuration.cluster.tool.operations;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs.FileSystemException;
import org.jahia.configuration.cluster.tool.ClusterConfigBean;
import org.jahia.configuration.configurators.ConfigFile;
import org.jahia.configuration.configurators.JahiaAdvancedPropertiesConfigurator;
import org.jahia.configuration.configurators.JahiaConfigBean;
import org.jahia.configuration.logging.AbstractLogger;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

/**
 * This class generates cluster configuration for a pre-defined list of nodes in separate directories. It can also
 * copy files to various cluster staging directories, ready to be uploaded to each node.
 */

public class ClusterConfigGenerator extends AbstractClusterOperation {

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

    private String filterStartMarker = "#{";
    private String filterEndMarker = "}";

    public ClusterConfigGenerator(AbstractLogger logger, ClusterConfigBean clusterConfigBean) throws Exception {
        super(logger, clusterConfigBean);
    }

    @Override
    public void execute() throws JSchException, SftpException, IOException {
        JahiaConfigBean jahiaConfigBean = new JahiaConfigBean();
        jahiaConfigBean.setCluster_activated("true");

        File templateDirectory = new File(clusterConfigBean.getTemplateDirectoryName());
        if (!templateDirectory.exists()) {
            throw new IOException("Template directory "+templateDirectory+"does not exist, aborting configuration generation !");
        }

        File jahiaAdvancedPropertiesConfigFile = new File(templateDirectory, clusterConfigBean.getJahiaAdvancedPropertyRelativeFileLocation());
        if (!jahiaAdvancedPropertiesConfigFile.exists()) {
            throw new IOException("Couldn't find Jahia advanced property file template at " + jahiaAdvancedPropertiesConfigFile);
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
            FileUtils.copyDirectory(templateDirectory, currentNodeDirectory);

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

            // filter files
            for (String fileToFilter : clusterConfigBean.getFilesToFilter()) {
                File targetFile = new File(currentNodeDirectory, fileToFilter);
                if (!targetFile.exists()) {
                    logger.warn("No file " + targetFile + " found for filtering, ignoring...");
                    continue;
                }
                String fileEncoding = getFileEncoding(targetFile);
                if (fileEncoding != null) {
                    logger.info("Detected encoding " + fileEncoding + " for file " + targetFile);
                } else {
                    logger.info("No specific encoding found for file " + targetFile + " will use platform default (" + Charset.defaultCharset() + ")");
                }
                String fileContents = FileUtils.readFileToString(targetFile);
                fileContents = StringUtils.replace(fileContents, filterStartMarker + "cluster.nodeId" + filterEndMarker, currentNodeId);
                FileUtils.writeStringToFile(targetFile, fileContents);
            }

        }
    }

    public String getFileEncoding(File file) throws IOException {
        byte[] buf = new byte[4096];
        java.io.FileInputStream fis = new java.io.FileInputStream(file);

        UniversalDetector detector = new UniversalDetector(null);

        int nread;
        while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
            detector.handleData(buf, 0, nread);
        }
        detector.dataEnd();

        String encoding = detector.getDetectedCharset();

        detector.reset();

        return encoding;

    }

}
