/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.utils.maven.plugin;

import org.apache.maven.plugin.logging.Log;
import org.slf4j.Logger;
import org.slf4j.Marker;

import java.text.MessageFormat;

/**
 * A bridge class to be able to use classes that use SLF4J's logger with Mojo's logger
 */
public class SLF4JLoggerToMojoLogBridge implements Logger {
    
    Log mavenLog;
    
    public SLF4JLoggerToMojoLogBridge(Log mavenLog) {
        this.mavenLog = mavenLog;
    }
    
    @Override
    public String getName() {
        return mavenLog.toString();
    }

    @Override
    public boolean isTraceEnabled() {
        return mavenLog.isDebugEnabled();
    }

    @Override
    public void trace(String s) {
        mavenLog.debug(s);
    }

    @Override
    public void trace(String s, Object o) {
        mavenLog.debug(MessageFormat.format(s, o));
    }

    @Override
    public void trace(String s, Object o, Object o2) {
        mavenLog.debug(MessageFormat.format(s, o, o2));
    }

    @Override
    public void trace(String s, Object... objects) {
        mavenLog.debug(MessageFormat.format(s, objects));
    }

    @Override
    public void trace(String s, Throwable throwable) {
        mavenLog.debug(s, throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return isTraceEnabled();
    }

    @Override
    public void trace(Marker marker, String s) {
        trace(s);
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        trace(s, o);
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o2) {
        trace(s, o, o2);
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        trace(s, objects);
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        trace(s, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return mavenLog.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        mavenLog.debug(s);
    }

    @Override
    public void debug(String s, Object o) {
        mavenLog.debug(MessageFormat.format(s, o));
    }

    @Override
    public void debug(String s, Object o, Object o2) {
        mavenLog.debug(MessageFormat.format(s, o, o2));
    }

    @Override
    public void debug(String s, Object... objects) {
        mavenLog.debug(MessageFormat.format(s, objects));
    }

    @Override
    public void debug(String s, Throwable throwable) {
        mavenLog.debug(s, throwable);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isDebugEnabled();
    }

    @Override
    public void debug(Marker marker, String s) {
        debug(s);
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        debug(s, o);
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o2) {
        debug(s, o, o2);
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        debug(s, objects);
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        debug(s, throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return mavenLog.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        mavenLog.info(s);
    }

    @Override
    public void info(String s, Object o) {
        mavenLog.info(MessageFormat.format(s, o));
    }

    @Override
    public void info(String s, Object o, Object o2) {
        mavenLog.info(MessageFormat.format(s, o, o2));
    }

    @Override
    public void info(String s, Object... objects) {
        mavenLog.info(MessageFormat.format(s, objects));
    }

    @Override
    public void info(String s, Throwable throwable) {
        mavenLog.info(s, throwable);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return isInfoEnabled();
    }

    @Override
    public void info(Marker marker, String s) {
        info(s);
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        info(s, o);
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o2) {
        info(s, o, o2);
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        info(s, objects);
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        info(s, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return mavenLog.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        mavenLog.warn(s);
    }

    @Override
    public void warn(String s, Object o) {
        mavenLog.warn(MessageFormat.format(s, o));
    }

    @Override
    public void warn(String s, Object... objects) {
        mavenLog.warn(MessageFormat.format(s, objects));
    }

    @Override
    public void warn(String s, Object o, Object o2) {
        mavenLog.warn(MessageFormat.format(s, o, o2));
    }

    @Override
    public void warn(String s, Throwable throwable) {
        mavenLog.warn(s, throwable);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return isWarnEnabled();
    }

    @Override
    public void warn(Marker marker, String s) {
        warn(s);
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        warn(s, o);
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o2) {
        warn(s, o, o2);
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        warn(s, objects);
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        warn(s, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return mavenLog.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        mavenLog.error(s);
    }

    @Override
    public void error(String s, Object o) {
        mavenLog.error(MessageFormat.format(s, o));
    }

    @Override
    public void error(String s, Object o, Object o2) {
        mavenLog.error(MessageFormat.format(s, o, o2));
    }

    @Override
    public void error(String s, Object... objects) {
        mavenLog.error(MessageFormat.format(s, objects));
    }

    @Override
    public void error(String s, Throwable throwable) {
        mavenLog.error(s, throwable);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return isErrorEnabled();
    }

    @Override
    public void error(Marker marker, String s) {
        error(s);
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        error(s, o);
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o2) {
        error(s, o, o2);
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        error(s, objects);
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        error(s, throwable);
    }
}
