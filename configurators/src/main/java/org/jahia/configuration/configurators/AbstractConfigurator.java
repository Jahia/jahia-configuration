/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.configuration.configurators;

import java.util.Collections;
import java.util.Map;

import org.jahia.configuration.logging.AbstractLogger;

/**
 * Base configurator implementation. A configurator is responsible for configuring a sub-system of Jahia. It should
 * work with all versions of Jahia and applications servers, or declare that it isn't compatible with them.
 *
 * TODO add supported versions and supported servers structure so that configurators can declared their support.
 * 
 * User: loom
 * Date: Feb 13, 2009
 * Time: 3:29:37 PM
 */
public abstract class AbstractConfigurator {

    protected static String getValue(Map values, String key) {
        String replacement = (String) values.get(key);
        if (replacement == null) {
            return "";
        }
        replacement = replacement.trim();
        return replacement;
    }
    
    protected Map dbProperties = Collections.emptyMap();
    protected JahiaConfigInterface jahiaConfigInterface;
    private AbstractLogger logger;

    public AbstractConfigurator(JahiaConfigInterface jahiaConfigInterface) {
        this.jahiaConfigInterface = jahiaConfigInterface;
    }

    public AbstractConfigurator(Map dbProps, JahiaConfigInterface jahiaConfigInterface) {
        this(jahiaConfigInterface);
        this.dbProperties = dbProps;
    }
    
    public AbstractConfigurator(Map dbProps, JahiaConfigInterface jahiaConfigInterface, AbstractLogger logger) {
        this(jahiaConfigInterface);
        this.dbProperties = dbProps;
        this.logger = logger;
    }

    public abstract void updateConfiguration(ConfigFile sourceConfigFile, String destFileName) throws Exception;

    protected String getDBProperty(String key) {
        return getValue(dbProperties, key);
    }

    public AbstractLogger getLogger() {
        return logger;
    }

    public void setLogger(AbstractLogger logger) {
        this.logger = logger;
    }
}
