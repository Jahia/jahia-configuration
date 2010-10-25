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

package org.jahia.configuration.configurators;

import java.util.Map;
import java.io.IOException;

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

    public void updateConfiguration(String sourceJahiaPath, String targetJahiaPath) throws IOException {
        properties = new PropertiesManager(sourceJahiaPath, targetJahiaPath);
        // context  path
        properties.setProperty("jahia.contextPath", jahiaConfigInterface.getContextPath());
        properties.setProperty("release", jahiaConfigInterface.getRelease());
        properties.setProperty("server", jahiaConfigInterface.getTargetServerType());
        properties.setProperty("serverVersion", jahiaConfigInterface.getTargetServerVersion());
        properties.setProperty("serverHome", jahiaConfigInterface.getTargetServerDirectory() != null ? jahiaConfigInterface.getTargetServerDirectory().replace("\\\\", "/").replace("\\", "/") : null);
        properties.setProperty("jahiaEtcDiskPath", jahiaConfigInterface.getJahiaEtcDiskPath());
        properties.setProperty("jahiaVarDiskPath", jahiaConfigInterface.getJahiaVarDiskPath());
        properties.setProperty("jahiaSharedTemplatesDiskPath", jahiaConfigInterface.getJahiaSharedModulesDiskPath());
        properties.setProperty("jahiaTemplatesHttpPath", jahiaConfigInterface.getJahiaModulesHttpPath());
        properties.setProperty("jahiaEnginesHttpPath", jahiaConfigInterface.getJahiaEnginesHttpPath());
        properties.setProperty("jahiaJavaScriptHttpPath", jahiaConfigInterface.getJahiaJavaScriptHttpPath());
        properties.setProperty("jahiaWebAppsDeployerBaseURL", jahiaConfigInterface.getJahiaWebAppsDeployerBaseURL());
        properties.setProperty("jahiaImportsDiskPath", jahiaConfigInterface.getJahiaImportsDiskPath());
        properties.setProperty("db_script", jahiaConfigInterface.getDb_script());
        properties.setProperty("developmentMode", jahiaConfigInterface.getDevelopmentMode());

        properties.setProperty("mail_paranoia", jahiaConfigInterface.getMailParanoia());
        if (jahiaConfigInterface.getMailServer() != null && jahiaConfigInterface.getMailServer().length() > 0) {
            properties.setProperty("mail_service_activated", "true");
            properties.setProperty("mail_server", jahiaConfigInterface.getMailServer());
            properties.setProperty("mail_administrator", jahiaConfigInterface.getMailAdministrator());            
            properties.setProperty("mail_from", jahiaConfigInterface.getMailFrom());            
        }
        properties.setProperty("hibernate.dialect", getDBProperty("jahia.database.hibernate.dialect"));
        properties.setProperty("nested.transaction.allowed", getDBProperty("jahia.nested_transaction_allowed"));
        
        configureScheduler();
        
        properties.storeProperties(sourceJahiaPath, targetJahiaPath);
    }

	private void configureScheduler() {
		String delegate = (String) dbProperties
				.get("jahia.quartz.jdbcDelegate");
		if (delegate == null || delegate.length() == 0) {
			delegate = "org.quartz.impl.jdbcjobstore.StdJDBCDelegate";
		}

		properties.setProperty("org.quartz.driverDelegateClass", delegate);
	}
}
