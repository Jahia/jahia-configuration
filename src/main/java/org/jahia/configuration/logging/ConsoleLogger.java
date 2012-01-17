/**
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2012 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Solutions Group SA. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */

package org.jahia.configuration.logging;

/**
 * An implementation of the {@link AbstractLogger} that writes all messages to
 * the system out.
 * 
 * @author Sergiy Shyrkov
 */
public class ConsoleLogger implements AbstractLogger {

    public static final byte LEVEL_DEBUG = 0;
    public static final byte LEVEL_INFO = 1;
    public static final byte LEVEL_WARN = 2;
    public static final byte LEVEL_ERROR = 3;

    private byte level = LEVEL_DEBUG;

    /**
     * Initializes an instance of the logger with debug level.
     */
    public ConsoleLogger() {
        this(LEVEL_DEBUG);
    }

    /**
     * Initializes an instance of the logger with the specified level.
     * 
     * @param level the logger severity level to use
     */
    public ConsoleLogger(byte level) {
        super();
        this.level = level;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jahia.utils.maven.plugin.AbstractLogger#debug(java.lang.String)
     */
    public void debug(String message) {
        out(LEVEL_DEBUG, message, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jahia.utils.maven.plugin.AbstractLogger#debug(java.lang.String,
     * java.lang.Throwable)
     */
    public void debug(String message, Throwable t) {
        out(LEVEL_DEBUG, message, t);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jahia.utils.maven.plugin.AbstractLogger#error(java.lang.String)
     */
    public void error(String message) {
        out(LEVEL_ERROR, message, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jahia.utils.maven.plugin.AbstractLogger#error(java.lang.String,
     * java.lang.Throwable)
     */
    public void error(String message, Throwable t) {
        out(LEVEL_ERROR, message, t);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jahia.utils.maven.plugin.AbstractLogger#info(java.lang.String)
     */
    public void info(String message) {
        out(LEVEL_INFO, message, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jahia.utils.maven.plugin.AbstractLogger#isDebugEnabled()
     */
    public boolean isDebugEnabled() {
        return level <= LEVEL_DEBUG;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jahia.utils.maven.plugin.AbstractLogger#warn(java.lang.String)
     */
    public void warn(String message) {
        out(LEVEL_WARN, message, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jahia.utils.maven.plugin.AbstractLogger#warn(java.lang.String,
     * java.lang.Throwable)
     */
    public void warn(String message, Throwable t) {
        out(LEVEL_WARN, message, t);
    }

    private void out(byte level, String message, Throwable t) {
        if (level < this.level) {
            return;
        }
        System.out.println(message);
        if (t != null) {
            t.printStackTrace(System.out);
        }
    }
}
