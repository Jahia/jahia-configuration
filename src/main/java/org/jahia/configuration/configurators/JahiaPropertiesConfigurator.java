/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2023 Jahia Solutions Group SA. All rights reserved.
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

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.PropertyUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Property configuration for the jahia.properties file.
 * User: islam
 * Date: 16 juil. 2008
 * Time: 10:09:49
 */
public class JahiaPropertiesConfigurator extends AbstractConfigurator {

    private PropertiesManager properties;

    public JahiaPropertiesConfigurator(Map dbProperties, JahiaConfigInterface jahiaConfigInterface) {
        super(dbProperties, jahiaConfigInterface);
    }

    public void updateConfiguration(ConfigFile sourceJahiaPath, String targetJahiaPath) throws IOException {
        properties = new PropertiesManager(sourceJahiaPath.getInputStream());
        properties.setUnmodifiedCommentingActivated(true);

        File targetJahiaFile = new File(targetJahiaPath);
        Properties existingProperties = new Properties();
        if (targetJahiaFile.exists()) {
            existingProperties.putAll(PropertyUtils.loadProperties(targetJahiaFile));
            for (Object key : existingProperties.keySet()) {
                String propertyName = String.valueOf(key);
                properties.setProperty(propertyName, existingProperties.getProperty(propertyName));
            }
        }

        // jahia tools manager
        if (properties.getProperty("jahiaToolManagerUsername") != null) {
            properties.setProperty("jahiaToolManagerUsername", jahiaConfigInterface.getJahiaToolManagerUsername());
            properties.setProperty("jahiaToolManagerPassword", JahiaGlobalConfigurator.encryptPassword(jahiaConfigInterface.getJahiaToolManagerPassword()));
        }

        properties.setProperty("jahiaVarDiskPath", jahiaConfigInterface.getJahiaVarDiskPath());
        properties.setProperty("jahiaModulesDiskPath", jahiaConfigInterface.getJahiaModulesDiskPath());
        properties.setProperty("jahiaWebAppsDeployerBaseURL", jahiaConfigInterface.getJahiaWebAppsDeployerBaseURL());
        properties.setProperty("jahiaImportsDiskPath", jahiaConfigInterface.getJahiaImportsDiskPath());
        properties.setProperty("db_script", jahiaConfigInterface.getDb_script());
        properties.setProperty("operatingMode", jahiaConfigInterface.getOperatingMode());

        properties.setProperty("hibernate.dialect", getDBProperty("jahia.database.hibernate.dialect"));

        if (jahiaConfigInterface.getJahiaProperties() != null) {
            for (Map.Entry<String, String> entry : jahiaConfigInterface.getJahiaProperties().entrySet()) {
                properties.setProperty(entry.getKey(), entry.getValue());
            }
        }

        configureScheduler();

        if (jahiaConfigInterface.getJahiaAdvancedProperties() != null) {
            for (Map.Entry<String, String> entry : jahiaConfigInterface.getJahiaAdvancedProperties().entrySet()) {
                properties.setProperty(entry.getKey(), entry.getValue());
            }
        }

        String fileDataStorePath = getValue(dbProperties, "fileDataStorePath");
        if (StringUtils.isNotEmpty(fileDataStorePath)) {
            String datastorePropertyName = "jackrabbit.datastore.path";
            InputStream is = sourceJahiaPath.getInputStream();
            try {
                if (IOUtils.toString(is).contains("jahia.jackrabbit.datastore.path")) {
                    // DF 7.1.0.0+
                    datastorePropertyName = "jahia.jackrabbit.datastore.path";
                }
            } finally {
                IOUtils.closeQuietly(is);
            }
            properties.setProperty(datastorePropertyName, fileDataStorePath);
        }

        properties.storeProperties(sourceJahiaPath.getInputStream(), targetJahiaPath);
    }

    private void configureScheduler() {
        String delegate = (String) dbProperties.get("jahia.quartz.jdbcDelegate");
        if (delegate == null || delegate.length() == 0) {
            delegate = "org.quartz.impl.jdbcjobstore.StdJDBCDelegate";
        }

        if (jahiaConfigInterface.getTargetServerType().startsWith("weblogic")) {
            delegate = "org.quartz.impl.jdbcjobstore.WebLogicDelegate";
            if (jahiaConfigInterface.getDatabaseType().equals("oracle")) {
                delegate = "org.quartz.impl.jdbcjobstore.oracle.weblogic.WebLogicOracleDelegate";
            }
        }

        if (jahiaConfigInterface.getTargetServerType().startsWith("was")
                && jahiaConfigInterface.getDatabaseType().equals("oracle")) {
            delegate = "org.quartz.impl.jdbcjobstore.StdJDBCDelegate";
        }

        properties.setProperty("org.quartz.driverDelegateClass", delegate);
    }
}
