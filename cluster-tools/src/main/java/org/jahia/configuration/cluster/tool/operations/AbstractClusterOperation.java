package org.jahia.configuration.cluster.tool.operations;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.apache.commons.io.FileUtils;
import org.jahia.configuration.cluster.ClusterConfigBean;
import org.jahia.configuration.logging.AbstractLogger;

import java.io.File;
import java.io.IOException;

/**
 * Abstract class shared across all cluster operations
 */
public abstract class AbstractClusterOperation {

    protected AbstractLogger logger;
    protected ClusterConfigBean clusterConfigBean;

    public AbstractClusterOperation(AbstractLogger logger, ClusterConfigBean clusterConfigBean) {
        this.logger = logger;
        this.clusterConfigBean = clusterConfigBean;
    }

    protected byte[] readPrivateKeyFromFile() throws IOException {
        return FileUtils.readFileToByteArray(new File(clusterConfigBean.getPrivateKeyFileLocation()));
    }

    public abstract void execute() throws JSchException, SftpException, IOException;
}
