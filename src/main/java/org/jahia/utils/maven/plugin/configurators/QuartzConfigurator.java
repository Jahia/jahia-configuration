/**
 *
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2009 Jahia Limited. All rights reserved.
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
 * between you and Jahia Limited. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */

package org.jahia.utils.maven.plugin.configurators;


import org.jahia.utils.maven.plugin.buildautomation.JahiaPropertiesBean;
import org.jahia.utils.maven.plugin.buildautomation.PropertiesManager;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Configurator for Quartz' configuration file. Supports both transactional and non-transactional deployments.
 * User: islam
 * Date: 25 juin 2008
 * Time: 11:04:01
 */
public class QuartzConfigurator extends AbstractConfigurator {

    public QuartzConfigurator(Map dbProperties, JahiaPropertiesBean jahiaPropertiesBean) {
        super(dbProperties, jahiaPropertiesBean);
    }

    public void updateConfiguration(String sourceFileName, String destFileName) throws IOException {

        boolean useJTA = false;
        if ("was".equals(jahiaPropertiesBean.getServer())) {
            useJTA = true;
        }

        File sourceConfigFile = new File(sourceFileName);
        if (sourceConfigFile.exists()) {
            PropertiesManager propertiesManager = new PropertiesManager(sourceFileName, destFileName);

            String transactionIsolationLevel = getValue(dbProperties, "jahia.quartz.selectWithLockSQL");
            String jdbcDelegate = getValue(dbProperties, "jahia.quartz.jdbcDelegate");
            String serializable = getValue(dbProperties, "jahia.quartz.serializable");

            propertiesManager.setProperty("org.quartz.jobStore.driverDelegateClass", jdbcDelegate);
            propertiesManager.setProperty("org.quartz.jobStore.selectWithLockSQL", transactionIsolationLevel);
            propertiesManager.setProperty("org.quartz.jobStore.txIsolationLevelSerializable", serializable);

            if (useJTA) {
                propertiesManager.setProperty("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreCMT");
                propertiesManager.setProperty("org.quartz.jobStore.nonManagedTXDataSource", "jahiaNonTxDS");
                propertiesManager.setProperty("org.quartz.jobStore.dontSetAutoCommitFalse", "true");
                propertiesManager.setProperty("org.quartz.dataSource.jahiaNonTxDS.jndiURL", "java:comp/env/jdbc/jahiaNonTx");
            } else {
                propertiesManager.setProperty("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
                propertiesManager.removeProperty("org.quartz.jobStore.nonManagedTXDataSource");
                propertiesManager.removeProperty("org.quartz.jobStore.dontSetAutoCommitFalse");
                propertiesManager.removeProperty("org.quartz.dataSource.jahiaNonTxDS.jndiURL");
            }

            propertiesManager.storeProperties(sourceFileName, destFileName);

        }
    }

}
