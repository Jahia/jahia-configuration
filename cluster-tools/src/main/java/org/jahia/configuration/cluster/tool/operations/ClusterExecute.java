package org.jahia.configuration.cluster.tool.operations;

import com.jcraft.jsch.*;
import org.jahia.configuration.cluster.ClusterConfigBean;
import org.jahia.configuration.cluster.tool.ClusterUserInfo;
import org.jahia.configuration.logging.AbstractLogger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Execute a command on all cluster nodes
 */
public class ClusterExecute extends AbstractClusterOperation {

    private String commandLine;

    public ClusterExecute(AbstractLogger logger, ClusterConfigBean clusterConfigBean, String commandLine) {
        super(logger, clusterConfigBean);
        this.commandLine = commandLine;
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

        for (int i = 0; i < clusterConfigBean.getNumberOfNodes(); i++) {

            String nodeId = clusterConfigBean.getNodeNamePrefix() + Integer.toString(i+1);

            logger.info("-- " + nodeId + " ------------------------------------------------------- ");

            Session session = jSch.getSession(clusterConfigBean.getDeploymentUserName(), clusterConfigBean.getExternalHostNames().get(i), 22);

            UserInfo ui = new ClusterUserInfo(logger);
            session.setUserInfo(ui);
            session.connect();

            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(commandLine);

            channel.setInputStream(null);

            ((ChannelExec) channel).setErrStream(System.err);

            InputStream in = channel.getInputStream();

            logger.info("Executing remote command: " + commandLine);

            channel.connect();

            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int j = in.read(tmp, 0, 1024);
                    if (j < 0) break;
                    System.out.print(new String(tmp, 0, j));
                }
                if (channel.isClosed()) {
                    System.out.println("Command exit-status: " + channel.getExitStatus());
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
            channel.disconnect();
            session.disconnect();
        }
    }

}
