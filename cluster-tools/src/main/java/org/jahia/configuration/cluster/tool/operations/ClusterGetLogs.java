package org.jahia.configuration.cluster.tool.operations;

import com.jcraft.jsch.*;
import org.jahia.configuration.cluster.tool.ClusterConfigBean;
import org.jahia.configuration.cluster.tool.ClusterUserInfo;
import org.jahia.configuration.logging.AbstractLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

/**
 * Retrieve logs files from all cluster nodes, makes it easy to view them locally
 */
public class ClusterGetLogs extends AbstractClusterOperation {

    public ClusterGetLogs(AbstractLogger logger, ClusterConfigBean clusterConfigBean) {
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
            info(i, "Current directory = " + sftp.pwd());
            sftp.cd(clusterConfigBean.getRemoteLogDirectory());
            String currentDirectory = sftp.pwd();
            debug(i, "Current directory on server:" + currentDirectory);

            File nodeDir = new File(clusterConfigBean.getLogsDirectoryName(), clusterConfigBean.getNodeId(i));
            if (!nodeDir.exists()) {
                nodeDir.mkdirs();
            }

            // now let's copy the local files to each server.
            copyFrom(sftp, nodeDir, i);

            sftp.disconnect();
            session.disconnect();
        }
    }

    public void copyFrom(ChannelSftp sftp, File destinationDir, final int nodeIndex) throws SftpException, FileNotFoundException {

        Vector files = sftp.ls(".");
        Iterator fileIterator = files.iterator();
        while (fileIterator.hasNext()) {
            ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) fileIterator.next();
            if (lsEntry.getAttrs().isDir()) {
                if (lsEntry.getFilename().equals(".") || lsEntry.getFilename().equals("..")) {
                    // let's ignore these directories
                } else {
                    sftp.cd(lsEntry.getFilename());
                    copyFrom(sftp, new File(destinationDir,lsEntry.getFilename()), nodeIndex);
                    sftp.cd("..");
                }
            } else {
                sftp.get(lsEntry.getFilename(), new File(destinationDir, lsEntry.getFilename()).toString(), new SftpProgressMonitor() {

                    private long byteCount;
                    private long fileSize;

                    public void init(int op, String src, String dest, long max) {
                        this.fileSize = max;
                        info(nodeIndex, "Copying " + src + " to " + dest + " (" + max + " bytes)...");
                    }

                    public boolean count(long count) {
                        byteCount += count;
                        info(nodeIndex, byteCount + " / " + fileSize + " bytes transferred.");
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
