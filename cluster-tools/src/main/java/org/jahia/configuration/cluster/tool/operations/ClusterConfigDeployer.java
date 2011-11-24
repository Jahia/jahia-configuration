package org.jahia.configuration.cluster.tool.operations;

import com.jcraft.jsch.*;
import org.jahia.configuration.cluster.tool.ClusterConfigBean;
import org.jahia.configuration.cluster.tool.ClusterUserInfo;
import org.jahia.configuration.logging.AbstractLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Configuration deployer to a cluster of Jahia installs.
 */
public class ClusterConfigDeployer extends AbstractClusterOperation {

    public ClusterConfigDeployer(AbstractLogger logger, ClusterConfigBean clusterConfigBean) {
        super(logger, clusterConfigBean);
    }

    public void execute() throws JSchException, SftpException, IOException {
        JSch jSch = new JSch();
        final byte[] prvkey = readPrivateKeyFromFile(); // Private key must be byte array
        final byte[] emptyPassPhrase = new byte[0]; // Empty passphrase for now, get real passphrase from MyUserInfo

        jSch.addIdentity(
            clusterConfigBean.getDeploymentUserName(),    // String userName
            prvkey,          // byte[] privateKey
            null,            // byte[] publicKey
            emptyPassPhrase  // byte[] passPhrase
        );

        for (int i=0; i < clusterConfigBean.getNumberOfNodes(); i++) {
            logger.info("Processing server " + Integer.toString(i+1) + " : " + clusterConfigBean.getNodeNamePrefix() + Integer.toString(i+1));

            Session session = jSch.getSession(clusterConfigBean.getDeploymentUserName(), clusterConfigBean.getExternalHostNames().get(i), 22);
            // session.setConfig("");
            UserInfo ui = new ClusterUserInfo(logger);
            session.setUserInfo(ui);
            session.connect();
            Channel channel = session.openChannel("sftp");
            ChannelSftp sftp = (ChannelSftp) channel;

            logger.info("Connecting to " + clusterConfigBean.getDeploymentUserName() + "@" + clusterConfigBean.getExternalHostNames().get(i) + "...");
            sftp.connect();
            sftp.cd(clusterConfigBean.getDeploymentTargetPath());
            String currentDirectory = sftp.pwd();
            logger.debug("Current directory on server:" + currentDirectory);

            String nodeId = clusterConfigBean.getNodeNamePrefix() + Integer.toString(i+1);
            File nodeDir = new File(clusterConfigBean.getNodesDirectoryName() + File.separator + nodeId);

            // now let's copy the local files to each server.
            copy(nodeDir, sftp);

            sftp.disconnect();
            session.disconnect();
        }
    }

    public void copy(File dir, ChannelSftp sftp) throws SftpException, FileNotFoundException {

        File listFile[] = dir.listFiles();
        if(listFile != null) {
            for(int i=0; i<listFile.length; i++) {
                if (listFile[i].getName().startsWith(".")) {
                    // we ignore hidden files and directories
                    continue;
                }
                if(listFile[i].isDirectory()) {
                    sftp.cd(listFile[i].getName());
                    copy(listFile[i], sftp);
                    sftp.cd("..");
                } else {
                    sftp.put(listFile[i].getPath(), listFile[i].getName(), new SftpProgressMonitor() {
                        public void init(int op, String src, String dest, long max) {
                            logger.info("Copying " + src + " to " + dest + " (" + max + " bytes)...");
                        }

                        public boolean count(long count) {
                            logger.info(count + " bytes transferred.");
                            return true;  //To change body of implemented methods use File | Settings | File Templates.
                        }

                        public void end() {
                            logger.info("Transfer completed.");
                        }
                    }, ChannelSftp.OVERWRITE);
                }
            }
        }
    }

}
