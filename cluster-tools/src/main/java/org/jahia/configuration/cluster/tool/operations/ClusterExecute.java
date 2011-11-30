package org.jahia.configuration.cluster.tool.operations;

import com.jcraft.jsch.*;
import org.jahia.configuration.cluster.tool.ClusterConfigBean;
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

            info(i, "-- " + clusterConfigBean.getNodeId(i) + " ------------------------------------------------------- ");

            Session session = jSch.getSession(clusterConfigBean.getDeploymentUserName(), clusterConfigBean.getExternalHostNames().get(i), 22);

            UserInfo ui = new ClusterUserInfo(logger);
            session.setUserInfo(ui);
            session.connect();

            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(commandLine);

            channel.setInputStream(null);

            ((ChannelExec) channel).setErrStream(System.err);

            InputStream in = channel.getInputStream();

            info(i, "Executing remote command: " + commandLine);

            channel.connect();

            byte[] tmp = new byte[1024];
            StringBuffer result = new StringBuffer();
            StringBuffer curOutputLine = new StringBuffer();
            while (true) {
                while (in.available() > 0) {
                    int j = in.read(tmp, 0, 1024);
                    if (j < 0) break;
                    String output = new String(tmp, 0, j);
                    result.append(output);
                    int newLinePos = output.indexOf('\n');
                    if (newLinePos < 0) {
                        curOutputLine.append(output);
                    } else {
                        while (newLinePos > -1) {
                            String beforeNewLine = output.substring(0, newLinePos);
                            String afterNewLine = output.substring(newLinePos+1);
                            curOutputLine.append(beforeNewLine);
                            info(i, curOutputLine.toString());
                            output = afterNewLine;
                            curOutputLine = new StringBuffer();
                            newLinePos = output.indexOf('\n');
                            if (newLinePos < 0) {
                                curOutputLine.append(output);
                            }
                        }
                    }
                }
                if (channel.isClosed()) {
                    info(i, curOutputLine.toString());
                    info(i, "Command exit-status: " + channel.getExitStatus());
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
