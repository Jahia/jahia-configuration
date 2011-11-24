package org.jahia.configuration.cluster.tool.operations;

import com.jcraft.jsch.*;
import org.jahia.configuration.cluster.tool.ClusterConfigBean;
import org.jahia.configuration.cluster.tool.ClusterUserInfo;
import org.jahia.configuration.logging.AbstractLogger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Kills or hard kills all running Tomcat instances on the cluster.
 */
public class ClusterKill extends AbstractClusterOperation {

    private boolean hardKill = false;

    public ClusterKill(AbstractLogger logger, ClusterConfigBean clusterConfigBean, boolean hardKill) {
        super(logger, clusterConfigBean);
        this.hardKill = hardKill;
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

            logger.info("Processing server " + Integer.toString(i+1) + " : " + clusterConfigBean.getNodeNamePrefix() + Integer.toString(i+1));

            Session session = jSch.getSession(clusterConfigBean.getDeploymentUserName(), clusterConfigBean.getExternalHostNames().get(i), 22);

            // username and password will be given via UserInfo interface.
            UserInfo ui = new ClusterUserInfo(logger);
            session.setUserInfo(ui);
            session.connect();

            String pidResult = executeCommand(session, clusterConfigBean.getGetPidCommandLine());
            if ((pidResult == null) || ("".equals(pidResult))) {
                logger.warn("No valid PID found on this cluster node, skipping kill command...");
            } else {
                pidResult = pidResult.trim();
                String killCommand = clusterConfigBean.getKillCommandLine().replaceAll("\\$\\{tomcatpid\\}", pidResult);
                if (hardKill) {
                    killCommand = clusterConfigBean.getHardKillCommandLine().replaceAll("\\$\\{tomcatpid\\}", pidResult);
                }
                String killResult = executeCommand(session, killCommand);
            }

            session.disconnect();
        }
    }

    private String executeCommand(Session session, String commandLine) throws JSchException, IOException {
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(commandLine);

        channel.setInputStream(null);

        ((ChannelExec) channel).setErrStream(System.err);

        InputStream in = channel.getInputStream();

        logger.info("Executing remote command: " + commandLine);

        channel.connect();

        byte[] tmp = new byte[1024];
        StringBuffer result = new StringBuffer();
        while (true) {
            while (in.available() > 0) {
                int j = in.read(tmp, 0, 1024);
                if (j < 0) break;
                String output = new String(tmp, 0, j);
                result.append(output);
                System.out.print(output);
            }
            if (channel.isClosed()) {
                System.out.println("exit-status: " + channel.getExitStatus());
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception ee) {
            }
        }
        channel.disconnect();
        return result.toString();
    }
}
