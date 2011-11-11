package org.jahia.configuration.cluster.tool;

import com.jcraft.jsch.*;
import org.apache.commons.io.FileUtils;
import org.jahia.configuration.cluster.ClusterConfigBean;
import org.jahia.configuration.logging.AbstractLogger;

import java.io.*;
import java.util.Vector;

/**
 * Configuration deployer to a cluster of Jahia installs.
 */
public class ConfigDeployer {

    private AbstractLogger logger;
    private ClusterConfigBean clusterConfigBean;

    public ConfigDeployer(AbstractLogger logger, ClusterConfigBean clusterConfigBean) {
        this.logger = logger;
        this.clusterConfigBean = clusterConfigBean;
    }

    private byte[] readPrivateKeyFromFile() throws IOException {
        return FileUtils.readFileToByteArray(new File(clusterConfigBean.getPrivateKeyFileLocation()));
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
            Session session = jSch.getSession(clusterConfigBean.getDeploymentUserName(), clusterConfigBean.getExternalHostNames().get(i), 22);
            // session.setConfig("");
            UserInfo ui = new MyUserInfo(logger); // MyUserInfo implements UserInfo
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
                if(listFile[i].isDirectory()) {
                    sftp.cd(listFile[i].getName());
                    copy(listFile[i], sftp);
                    sftp.cd("..");
                } else {
                    if (listFile[i].getName().startsWith(".")) {
                        // we ignore hidden files
                        continue;
                    }
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

    public class MyUserInfo implements UserInfo {

        private AbstractLogger logger;

        public MyUserInfo(AbstractLogger logger) {
            this.logger = logger;
        }

        public String getPassphrase() {
            logger.info("getPassphrase from UserInfo");
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getPassword() {
            logger.info("getPassword from UserInfo");
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean promptPassword(String message) {
            logger.info("Prompt password in UserInfo with message: " + message);
            logger.info("Sending true result");
            return true;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean promptPassphrase(String message) {
            logger.info("Prompt pass phrase in UserInfo with message: " + message);
            logger.info("Sending true result");
            return true;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean promptYesNo(String message) {
            logger.info("Prompt Yes/No in UserInfo with message: " + message);
            logger.info("Answering YES");
            return true;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void showMessage(String message) {
            logger.info("Show message in UserInfo: " + message);
        }
    }
}
