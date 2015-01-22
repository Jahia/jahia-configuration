/**
 * 
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2014 Jahia Limited. All rights reserved.
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

package org.jahia.configuration.deployers;

import java.io.File;

import org.jahia.configuration.deployers.jboss.JBossServerDeploymentImpl;

/**
 * Factory for obtaining server deployment implementation, based on the server
 * type.
 * 
 * @author Serge Huber
 */
public final class ServerDeploymentFactory {

	public static ServerDeploymentInterface getImplementation(
			String serverType, File targetServerDir, File configDir,
			File dataDir) {
		if (serverType == null || serverType.length() == 0) {
			throw new IllegalArgumentException("Server type is not provided");
		}
		serverType = serverType.trim().toLowerCase();

		AbstractServerDeploymentImpl deployer = null;

		if (serverType.startsWith("tomcat")) {
			deployer = new TomcatServerDeploymentImpl("tomcat",
					"Apache Tomcat 7.x/8.x", targetServerDir);
		} else if (serverType.startsWith("jboss")) {
			deployer = new JBossServerDeploymentImpl("jboss",
					"Red Hat JBoss AS 7.x / EAP 6.x", targetServerDir);
		} else if (serverType.startsWith("was")
				|| serverType.startsWith("websphere")) {
			deployer = new WebsphereServerDeploymentImpl("websphere",
					"IBM WebSphere Application Server 8.5.5.x", targetServerDir);
		} else {
			throw new IllegalArgumentException("Unsupported server type: "
					+ serverType);
		}

		deployer.setConfigDir(configDir);
		deployer.setDataDir(dataDir);
		
		deployer.init();

		return deployer;
	}
	
	public static ServerDeploymentInterface getImplementation(
			String serverType, String serverVersion, File targetServerDir, File configDir,
			File dataDir) {
		return getImplementation(serverVersion != null ? serverType + serverVersion : serverType, targetServerDir, configDir, dataDir);
	}

}
