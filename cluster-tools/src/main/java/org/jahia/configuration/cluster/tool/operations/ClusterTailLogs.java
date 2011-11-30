package org.jahia.configuration.cluster.tool.operations;

import com.jcraft.jsch.*;
import org.jahia.configuration.cluster.tool.ClusterConfigBean;
import org.jahia.configuration.cluster.tool.ClusterUserInfo;
import org.jahia.configuration.logging.AbstractLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Spawns multiple threads to tail the logs for each server, mixing the output with prefixes.
 */
public class ClusterTailLogs extends AbstractClusterOperation {

    public class ClusterTailLogThread implements Runnable {

        private JSch jSch;
        private AbstractLogger logger;
        private ClusterConfigBean clusterConfigBean;
        private String threadName;
        private int instanceNumber;

        public ClusterTailLogThread(JSch jSch, AbstractLogger logger, ClusterConfigBean clusterConfigBean, String threadName, int instanceNumber) {
            this.jSch = jSch;
            this.logger = logger;
            this.clusterConfigBean = clusterConfigBean;
            this.threadName = threadName;
            this.instanceNumber = instanceNumber;
        }

        public void run() {
            try {
            Session session = jSch.getSession(clusterConfigBean.getDeploymentUserName(), clusterConfigBean.getExternalHostNames().get(instanceNumber), 22);

            // username and password will be given via UserInfo interface.
            UserInfo ui = new ClusterUserInfo(logger);
            session.setUserInfo(ui);
                session.connect();

            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(clusterConfigBean.getTailLogsCommandLine());

            channel.setInputStream(null);

            ((ChannelExec) channel).setErrStream(System.err);

            InputStream in = channel.getInputStream();

            logger.info("[" + threadName + "] Executing remote command: " + clusterConfigBean.getTailLogsCommandLine());

            channel.connect();

            byte[] tmp = new byte[1024];
            StringBuffer curOutputLine = new StringBuffer();
            while (true) {
                while (in.available() > 0) {
                    int j = in.read(tmp, 0, 1024);
                    if (j < 0) break;
                    String output = new String(tmp, 0, j);
                    int newLinePos = output.indexOf('\n');
                    if (newLinePos < 0) {
                        curOutputLine.append(output);
                    } else {
                        while (newLinePos > -1) {
                            String beforeNewLine = output.substring(0, newLinePos);
                            String afterNewLine = output.substring(newLinePos+1);
                            curOutputLine.append(beforeNewLine);
                            logger.info("[" + threadName + "] " + curOutputLine);
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
                    logger.info("[" + threadName + "] " + curOutputLine);
                    logger.info("[" + threadName + "] exit-status: " + channel.getExitStatus());
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
            channel.disconnect();
            session.disconnect();
            } catch (JSchException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        }
    }

    private List<Thread> tailThreads = new ArrayList<Thread>();

    public ClusterTailLogs(AbstractLogger logger, ClusterConfigBean clusterConfigBean) {
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

            logger.info("Start tail for server " + Integer.toString(i+1) + " : " + clusterConfigBean.getNodeId(i));

            ClusterTailLogThread clusterTailLogThread = new ClusterTailLogThread(jSch, logger, clusterConfigBean, clusterConfigBean.getNodeId(i), i );

            Thread serverThread = new Thread(clusterTailLogThread, clusterConfigBean.getNodeId(i));
            tailThreads.add(serverThread);
            serverThread.start();

        }

        for (Thread curThread : tailThreads) {
            try {
                curThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }
}
