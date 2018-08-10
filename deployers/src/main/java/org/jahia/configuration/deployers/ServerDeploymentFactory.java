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
					"Apache Tomcat 9.x", targetServerDir);
		} else if (serverType.startsWith("jboss")) {
			deployer = new JBossServerDeploymentImpl("jboss",
					"Red Hat JBoss EAP 6.x", targetServerDir);
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
