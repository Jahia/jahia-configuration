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
            logger.warn("Couldn't find Jahia advanced property file template at " + jahiaAdvancedPropertiesConfigFile + ", will skip node configuration step.");
        }

        // first let's create the node directories
        File nodesDirectory = new File(clusterConfigBean.getNodesDirectoryName());
        if (!nodesDirectory.exists()) {
            nodesDirectory.mkdirs();
        }

        for (int i=0; i < clusterConfigBean.getNumberOfNodes(); i++) {
            File currentNodeDirectory = new File(nodesDirectory, clusterConfigBean.getNodeId(i));
            if (!currentNodeDirectory.exists()) {
                info(i, "Creating node directory " + currentNodeDirectory);
                boolean nodeDirectoryCreated = currentNodeDirectory.mkdirs();
                if (!nodeDirectoryCreated) {
                    error(i, "Error while creating node directory !");
                }
            }

            info(i, "Copying template files to " + currentNodeDirectory + "...");
            FileUtils.copyDirectory(templateDirectory, currentNodeDirectory);

            if (jahiaAdvancedPropertiesConfigFile.exists()) {
                jahiaConfigBean.setCluster_node_serverId(clusterConfigBean.getNodeId(i));
                if ("processing".equals(clusterConfigBean.getNodeType(i))) {
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

            // filter files
            for (String fileToFilter : clusterConfigBean.getFilesToFilter()) {
                File targetFile = new File(currentNodeDirectory, fileToFilter);
                if (!targetFile.exists()) {
                    warn(i, "No file " + targetFile + " found for filtering, ignoring...");
                    continue;
                }
                String fileEncoding = null;
                if (fileToFilter.endsWith(".properties")) {
                    // always use ISO-8859-1 for properties files.
                    fileEncoding = "ISO-8859-1";
                } else {
                    fileEncoding = getFileEncoding(targetFile);
                }
                if (fileEncoding != null) {
                    info(i, "Detected encoding " + fileEncoding + " for file " + targetFile);
                } else {
                    info(i, "No specific encoding found for file " + targetFile + " will use platform default (" + Charset.defaultCharset() + ")");
                }
                String fileContents = FileUtils.readFileToString(targetFile);
                fileContents = StringUtils.replace(fileContents, filterStartMarker + "cluster.nodeId" + filterEndMarker, clusterConfigBean.getNodeId(i));
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
