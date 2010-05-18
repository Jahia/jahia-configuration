package org.jahia.utils.maven.plugin;

import org.slf4j.Logger;

/**
 * Logger implementation for SLF4J
 *
 * @author loom
 *         Date: May 11, 2010
 *         Time: 11:34:03 AM
 */
public class SLF4JLogger implements AbstractLogger {

    Logger logger;

    public SLF4JLogger(Logger logger) {
        this.logger = logger;
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public void debug(String message) {
        logger.debug(message);
    }

    public void debug(String message, Throwable t) {
        logger.debug(message, t);
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void warn(String message, Throwable t) {
        logger.warn(message, t);
    }

    public void error(String message) {
        logger.error(message);
    }

    public void error(String message, Throwable t) {
        logger.error(message, t);
    }
}
