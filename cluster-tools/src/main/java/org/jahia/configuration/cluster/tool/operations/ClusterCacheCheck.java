package org.jahia.configuration.cluster.tool.operations;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jahia.configuration.cluster.tool.ClusterConfigBean;
import org.jahia.configuration.logging.AbstractLogger;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * This tool connects to the various cache nodes, asks them to generate cache checksums, compares the results and
 * if there are any inconsistencies it will send flush commands to flush the invalid keys.
 */
public class ClusterCacheCheck extends AbstractClusterOperation {

    public ClusterCacheCheck(AbstractLogger logger, ClusterConfigBean clusterConfigBean) {
        super(logger, clusterConfigBean);
    }

    @Override
    public void execute() throws JSchException, SftpException, IOException {

        for (int i = 0; i < clusterConfigBean.getNumberOfNodes(); i++) {

            info(i, "-- " + clusterConfigBean.getNodeId(i) + " ------------------------------------------------------- ");
            info(i, "Step 1 : Retrieve all cache checksums...");

            HttpClient httpClient = new DefaultHttpClient();
            try {
                if (!loginUser(i, httpClient)) {
                    warn(i, "Couldn't login user on server " + clusterConfigBean.getNodeId(i));
                    continue;
                }
                JSONObject jsonObject = getCacheChecksums(i, httpClient);
                logoutUser(i, httpClient);
            } finally {
                httpClient.getConnectionManager().shutdown();
            }
        }

    }

    private boolean loginUser(int i, HttpClient httpClient) throws UnsupportedEncodingException {
        String loginURL = clusterConfigBean.getLoginURL();
        loginURL = loginURL.replaceAll("\\$\\{hostname\\}", clusterConfigBean.getExternalHostNames().get(i));
        HttpPost httpPost = new HttpPost(loginURL);
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("username", clusterConfigBean.getLoginUserName()));
        formparams.add(new BasicNameValuePair("password", clusterConfigBean.getLoginPassword()));
        formparams.add(new BasicNameValuePair("doLogin", "yes"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        httpPost.setEntity(entity);
        info(i, "Sending login form to " + loginURL + "...");
        try {
            HttpResponse response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 200) {
                return false;
            }
            info(i, "Server returned " + response.getStatusLine().getStatusCode() + " error code, still waiting...");
            response.getEntity().getContent().close();
        } catch (Throwable t) {
            info(i, "Error reaching server: " + t.getMessage() + " still waiting...");
        }
        return true;
    }

    private boolean logoutUser(int i, HttpClient httpClient) throws UnsupportedEncodingException {
        String logoutURL = clusterConfigBean.getLogoutURL();
        logoutURL = logoutURL.replaceAll("\\$\\{hostname\\}", clusterConfigBean.getExternalHostNames().get(i));
        HttpPost httpPost = new HttpPost(logoutURL);
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        httpPost.setEntity(entity);
        info(i, "Sending logout form to " + logoutURL + "...");
        try {
            HttpResponse response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 200) {
                return false;
            }
            info(i, "Server returned " + response.getStatusLine().getStatusCode() + " error code, still waiting...");
            response.getEntity().getContent().close();
        } catch (Throwable t) {
            info(i, "Error reaching server: " + t.getMessage() + " still waiting...");
        }
        return true;

    }

    private JSONObject getCacheChecksums(int i, HttpClient httpClient) {
        String cacheChecksumURL = clusterConfigBean.getCacheChecksumURL();
        cacheChecksumURL = cacheChecksumURL.replaceAll("\\$\\{hostname\\}", clusterConfigBean.getExternalHostNames().get(i));
        HttpGet httpGet = new HttpGet(cacheChecksumURL);
        info(i, "Sending login form to " + cacheChecksumURL + "...");
        try {
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                return null;
            }
            info(i, "Server returned " + response.getStatusLine().getStatusCode() + " error code, still waiting...");
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                long len = entity.getContentLength();
                if (len != -1) {
                    JSONObject jsonObject = new JSONObject(EntityUtils.toString(entity));
                    return jsonObject;
                } else {
                    // Stream content out
                }
            }
            response.getEntity().getContent().close();
        } catch (Throwable t) {
            info(i, "Error reaching server: " + t.getMessage() + " still waiting...");
        }
        return null;

    }

}
