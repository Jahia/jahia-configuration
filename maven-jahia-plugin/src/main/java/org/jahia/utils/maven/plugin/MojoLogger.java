package org.jahia.utils.maven.plugin;

import org.apache.maven.plugin.logging.Log;
import org.jahia.configuration.logging.AbstractLogger;

/**
 * Maven Mojo Log implementation
 *
 * @author loom
 *         Date: May 11, 2010
 *         Time: 11:29:05 AM
 */
public class MojoLogger implements AbstractLogger {

    Log mojoLog;

    public MojoLogger(Log mojoLog) {
        this.mojoLog = mojoLog;
    }

    public boolean isDebugEnabled() {
        return mojoLog.isDebugEnabled();
    }

    public void debug(String message) {
        mojoLog.debug(message);
    }

    public void debug(String message, Throwable t) {
        mojoLog.debug(message, t);
    }

    public void info(String message) {
        mojoLog.info(message);
    }

    public void warn(String message) {
        mojoLog.warn(message);
    }

    public void warn(String message, Throwable t) {
        mojoLog.warn(message, t);
    }

    public void error(String message) {
        mojoLog.error(message);
    }

    public void error(String message, Throwable t) {
        mojoLog.error(message, t);
    }
}
