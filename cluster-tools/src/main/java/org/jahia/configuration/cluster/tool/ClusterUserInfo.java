package org.jahia.configuration.cluster.tool;

import com.jcraft.jsch.UserInfo;
import org.jahia.configuration.logging.AbstractLogger;

/**
 * Cluster JSch user query class. This class is normally interactive, asking the user questions, but as we are
 * implementing automation tools it will always says yes to everything.
 *
*/
public class ClusterUserInfo implements UserInfo {

    private AbstractLogger logger;

    public ClusterUserInfo(AbstractLogger logger) {
        this.logger = logger;
    }

    public String getPassphrase() {
        logger.info("getPassphrase from UserInfo");
        return null;
    }

    public String getPassword() {
        logger.info("getPassword from UserInfo");
        return null;
    }

    public boolean promptPassword(String message) {
        logger.info("Prompt password in UserInfo with message: " + message);
        logger.info("Sending true result");
        return true;
    }

    public boolean promptPassphrase(String message) {
        logger.info("Prompt pass phrase in UserInfo with message: " + message);
        logger.info("Sending true result");
        return true;
    }

    public boolean promptYesNo(String message) {
        logger.info("Prompt Yes/No in UserInfo with message: " + message);
        logger.info("Answering YES");
        return true;
    }

    public void showMessage(String message) {
        logger.info("Show message in UserInfo: " + message);
    }
}
