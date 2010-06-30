package org.jahia.configuration.logging;

/**
 * Abstract logging interface to support both Maven's log interface and SLF4J.
 *
 * @author loom
 *         Date: May 11, 2010
 *         Time: 11:18:36 AM
 */
public interface AbstractLogger {

    public boolean isDebugEnabled();
    public void debug(String message);
    public void debug(String message, Throwable t);
    public void info(String message);
    public void warn(String message);
    public void warn(String message, Throwable t);
    public void error(String message);
    public void error(String message, Throwable t);

}
