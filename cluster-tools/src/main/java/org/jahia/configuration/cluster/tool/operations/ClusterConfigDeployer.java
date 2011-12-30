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

            info(i, "-- " + clusterConfigBean.getNodeId(i) + " ------------------------------------------------------- ");

            Session session = jSch.getSession(clusterConfigBean.getDeploymentUserName(), clusterConfigBean.getExternalHostNames().get(i), 22);
            // session.setConfig("");
            UserInfo ui = new ClusterUserInfo(logger);
            session.setUserInfo(ui);
            session.connect();
            Channel channel = session.openChannel("sftp");
            ChannelSftp sftp = (ChannelSftp) channel;

            info(i, "Connecting to " + clusterConfigBean.getDeploymentUserName() + "@" + clusterConfigBean.getExternalHostNames().get(i) + "...");
            sftp.connect();
            sftp.cd(clusterConfigBean.getDeploymentTargetPath());
            String currentDirectory = sftp.pwd();
            debug(i, "Current directory on server:" + currentDirectory);

            File nodeDir = new File(clusterConfigBean.getNodesDirectoryName() + File.separator + clusterConfigBean.getNodeId(i));

            // now let's copy the local files to each server.
            copy(nodeDir, sftp, i);

            sftp.disconnect();
            session.disconnect();
        }
    }

    public void copy(File dir, ChannelSftp sftp, final int nodeIndex) throws SftpException, FileNotFoundException {

        File listFile[] = dir.listFiles();
        if(listFile != null) {
            for(int i=0; i<listFile.length; i++) {
                if (listFile[i].getName().startsWith(".")) {
                    // we ignore hidden files and directories
                    continue;
                }
                boolean mustBeDeleted = false;
                if (listFile[i].getName().startsWith(clusterConfigBean.getDeleteFileNamePrefix())) {
                    // file or directory was marked to be deleted !
                    mustBeDeleted = true;
                }
                if (listFile[i].isDirectory()) {
                    if (mustBeDeleted) {
                        // @todo not yet implemented !
                        String dirName = listFile[i].getName().substring(clusterConfigBean.getDeleteFileNamePrefix().length());
                        warn(nodeIndex, "Directory deletion is not yet implemeted. Will not delete directory " + dirName);
                    } else {
                        sftp.cd(listFile[i].getName());
                        copy(listFile[i], sftp, nodeIndex);
                        sftp.cd("..");
                    }
                } else {
                    if (mustBeDeleted) {
                        String fileName = listFile[i].getName().substring(clusterConfigBean.getDeleteFileNamePrefix().length());
                        info(nodeIndex, "Deleting file " + fileName + "...");
                        sftp.rm(fileName);
                    } else {
                        sftp.put(listFile[i].getPath(), listFile[i].getName(), new SftpProgressMonitor() {
                            public void init(int op, String src, String dest, long max) {
                                info(nodeIndex, "Copying " + src + " to " + dest + " (" + max + " bytes)...");
                            }

                            public boolean count(long count) {
                                info(nodeIndex, count + " bytes transferred.");
                                return true;  //To change body of implemented methods use File | Settings | File Templates.
                            }

                            public void end() {
                                info(nodeIndex, "Transfer completed.");
                            }
                        }, ChannelSftp.OVERWRITE);
                    }
                }
            }
        }
    }

}