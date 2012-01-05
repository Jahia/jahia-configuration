package org.jahia.configuration.cluster.tool.operations;

import com.jcraft.jsch.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jahia.configuration.cluster.tool.ClusterConfigBean;
import org.jahia.configuration.cluster.tool.ClusterUserInfo;
import org.jahia.configuration.logging.AbstractLogger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Cluster start Tomcat, waiting for each node to complete startup before starting the next.
 */
public class ClusterSequentialTomcatStart extends AbstractClusterOperation {

    public ClusterSequentialTomcatStart(AbstractLogger logger, ClusterConfigBean clusterConfigBean) {
        super(logger, clusterConfigBean);
    }

    @Override
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

            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(clusterConfigBean.getStartupCommandLine());

            //channel.setInputStream(System.in);
            channel.setInputStream(null);

            //channel.setOutputStream(System.out);

            //FileOutputStream fos=new FileOutputStream("/tmp/stderr");
            //((ChannelExec)channel).setErrStream(fos);
            ((ChannelExec) channel).setErrStream(System.err);

            InputStream in = channel.getInputStream();

            info(i, "Executing remote command: " + clusterConfigBean.getStartupCommandLine());

            channel.connect();

            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int j = in.read(tmp, 0, 1024);
                    if (j < 0) break;
                    System.out.print(new String(tmp, 0, j));
                }
                if (channel.isClosed()) {
                    info(i, "exit-status: " + channel.getExitStatus());
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }

            if (channel.getExitStatus() > 0) {
                error(i, "Exit status was non-zero, aborting startup of cluster nodes ! ");
                break;
            }

            channel.disconnect();
            session.disconnect();

            info(i, "Waiting for Tomcat to startup up...");

            // now we must wait for Tomcat server to become available before continuing to the next node.
            HttpClient httpClient = new DefaultHttpClient();
            boolean available = false;
            try {
                String waitForStartupURL = clusterConfigBean.getWaitForStartupURL();
                waitForStartupURL = waitForStartupURL.replaceAll("\\$\\{hostname\\}", clusterConfigBean.getExternalHostNames().get(i));
                int maxCount = 0;
                while ((!available) && (maxCount < 300)) {
                    HttpGet httpGet = new HttpGet(waitForStartupURL);
                    info(i, "Try #" + maxCount + " to reach server at " + waitForStartupURL + "...");
                    try {
                        HttpResponse response = httpClient.execute(httpGet);
                        if (response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 401) {
                            available = true;
                            break;
                        }
                        info(i, "Server returned " + response.getStatusLine().getStatusCode() + " error code, still waiting...");
                        response.getEntity().getContent().close();
                    } catch (Throwable t) {
                        info(i, "Error reaching server: " + t.getMessage() + " still waiting...");
                    }
                    Thread.sleep(1000);
                    maxCount++;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } finally {
                httpClient.getConnectionManager().shutdown();
            }
            if (!available) {
                error(i, "Maximum tries of 240 was reached, and server is still not reachable. Aborting cluster startup");
                i = clusterConfigBean.getNumberOfNodes() + 1;
                break;
            }
        }
    }
}
