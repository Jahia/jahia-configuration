package org.jahia.configuration.cluster.tool.operations;

import com.jcraft.jsch.*;
import org.jahia.configuration.cluster.tool.ClusterConfigBean;
import org.jahia.configuration.cluster.tool.ClusterUserInfo;
import org.jahia.configuration.logging.AbstractLogger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Ask all nodes to perform a thread dump
 */
public class ClusterDumpThreads extends AbstractClusterOperation {

    public ClusterDumpThreads(AbstractLogger logger, ClusterConfigBean clusterConfigBean) {
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

        for (int i = 0; i < clusterConfigBean.getNumberOfNodes(); i++) {

            info(i, "-- " + clusterConfigBean.getNodeId(i) + " ------------------------------------------------------- ");

            Session session = jSch.getSession(clusterConfigBean.getDeploymentUserName(), clusterConfigBean.getExternalHostNames().get(i), 22);

            // username and password will be given via UserInfo interface.
            UserInfo ui = new ClusterUserInfo(logger);
            session.setUserInfo(ui);
            session.connect();

            String pidResult = executeCommand(session, clusterConfigBean.getGetPidCommandLine(), i);
            if ((pidResult == null) || ("".equals(pidResult))) {
                warn(i, "No valid PID found on this cluster node, skipping thread dump command...");
            } else {
                pidResult = pidResult.trim();
                String dumpThreadsCommand = clusterConfigBean.getDumpThreadsCommandLine().replaceAll("\\$\\{tomcatpid\\}", pidResult);
                String dumpThreadsResult = executeCommand(session, dumpThreadsCommand, i);
            }

            session.disconnect();
        }
    }

    private String executeCommand(Session session, String commandLine, int nodeIndex) throws JSchException, IOException {
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(commandLine);

        channel.setInputStream(null);

        ((ChannelExec) channel).setErrStream(System.err);

        InputStream in = channel.getInputStream();

        info(nodeIndex, "Executing remote command: " + commandLine);

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
                        info(nodeIndex, curOutputLine.toString());
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
                info(nodeIndex, curOutputLine.toString());
                info(nodeIndex, "Command exit-status: " + channel.getExitStatus());
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
