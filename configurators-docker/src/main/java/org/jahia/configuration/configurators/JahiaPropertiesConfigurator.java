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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Property configuration for the jahia.properties file.
 * User: islam
 * Date: 16 juil. 2008
 * Time: 10:09:49
 */
public class JahiaPropertiesConfigurator extends AbstractConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(JahiaPropertiesConfigurator.class);

    private PropertiesManager properties;

    public JahiaPropertiesConfigurator(Map dbProperties, JahiaConfigInterface jahiaConfigInterface) {
        super(dbProperties, jahiaConfigInterface);
    }

    public void updateConfiguration(InputStream inputStream, String targetJahiaPath) throws IOException {
        logger.info("Updating jahia.properties ...");
        properties = new PropertiesManager(inputStream);
        properties.setUnmodifiedCommentingActivated(true);

        File targetJahiaFile = new File(targetJahiaPath);
        Properties existingProperties = new Properties();
        if (targetJahiaFile.exists()) {
            try (FileInputStream fis = new FileInputStream(targetJahiaFile)) {
                existingProperties.load(fis);
            }
            for (Object key : existingProperties.keySet()) {
                String propertyName = String.valueOf(key);
                properties.setProperty(propertyName, existingProperties.getProperty(propertyName));
            }
        }

        properties.setProperty("jahiaVarDiskPath", jahiaConfigInterface.getJahiaVarDiskPath());
        properties.setProperty("jahiaModulesDiskPath", jahiaConfigInterface.getJahiaModulesDiskPath());
        properties.setProperty("jahiaWebAppsDeployerBaseURL", jahiaConfigInterface.getJahiaWebAppsDeployerBaseURL());
        properties.setProperty("jahiaImportsDiskPath", jahiaConfigInterface.getJahiaImportsDiskPath());
        properties.setProperty("db_script", jahiaConfigInterface.getDatabaseType() + ".script");
        properties.setProperty("operatingMode", jahiaConfigInterface.getOperatingMode());

        properties.setProperty("hibernate.dialect", getDBProperty("jahia.database.hibernate.dialect"));

        if (jahiaConfigInterface.getJahiaProperties() != null) {
            for (Map.Entry<String, String> entry : jahiaConfigInterface.getJahiaProperties().entrySet()) {
                properties.setProperty(entry.getKey(), entry.getValue());
            }
        }

        configureScheduler();

        String fileDataStorePath = getValue(dbProperties, "fileDataStorePath");
        if (StringUtils.isNotEmpty(fileDataStorePath)) {
            properties.setProperty("jahia.jackrabbit.datastore.path", fileDataStorePath);
        }

        properties.storeProperties(inputStream, targetJahiaPath);
        logger.info("Successfully updated jahia.properties in {}", targetJahiaPath);
    }

    private void configureScheduler() {
        String delegate = (String) dbProperties.get("jahia.quartz.jdbcDelegate");
        if (StringUtils.isEmpty(delegate)) {
            delegate = "org.quartz.impl.jdbcjobstore.StdJDBCDelegate";
        }

        properties.setProperty("org.quartz.driverDelegateClass", delegate);
    }
}
