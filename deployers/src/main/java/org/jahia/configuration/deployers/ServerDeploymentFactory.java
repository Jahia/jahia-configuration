/**
 * 
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2011 Jahia Limited. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;

import org.jahia.configuration.deployers.jboss.JBossServerDeploymentImpl;

/**
 * Factory that sets up all the different server deployer implementations, and links them to supported versions of
 * the various application servers.
 * @author Serge Huber
 */
public class ServerDeploymentFactory {

    private static ServerDeploymentFactory instance;

    private Map<String, ServerDeploymentInterface> implementations = new HashMap<String, ServerDeploymentInterface>();

    private static String targetServerDirectory;

    public ServerDeploymentFactory(String targetServerDirectory) {
        TomcatServerDeploymentImpl tomcatDeployer = new TomcatServerDeploymentImpl("Apache Tomcat 7.x", targetServerDirectory);
        addImplementation("tomcat", tomcatDeployer);
        addImplementation("tomcat7", tomcatDeployer);
        
        JBossServerDeploymentImpl jbossDeployer = new JBossServerDeploymentImpl("JBoss AS 7.x / EAP 6.x", targetServerDirectory);
        addImplementation("jboss", jbossDeployer);
        addImplementation("jbosseap", jbossDeployer);
        addImplementation("jbosseap6", jbossDeployer);
        addImplementation("jbosseap6.x", jbossDeployer);
        
        addImplementation("was", new WebsphereServerDeploymentImpl("IBM WebSphere Application Server 8.5.x", targetServerDirectory));
//        addImplementation("was7", getImplementation("was"));
//        
//        addImplementation("weblogic", new WeblogicServerDeploymentImpl(targetServerDirectory));
//        addImplementation("weblogic10", getImplementation("weblogic"));
    }

    public static ServerDeploymentFactory getInstance() {
        if (instance != null) {
            return instance;
        }
        if (targetServerDirectory == null) {
            throw new RuntimeException("Factory not initialized properly, you must set the targetServerDirectory variable before calling getInstance !");
        }
        instance = new ServerDeploymentFactory(targetServerDirectory);
        return instance;
    }

    public static void setTargetServerDirectory(String targetServerDirectory) {
        ServerDeploymentFactory.targetServerDirectory = targetServerDirectory;
    }

    public ServerDeploymentFactory() {}

    public void addImplementation(String implementationKey, ServerDeploymentInterface implementation) {
        implementations.put(implementationKey, implementation);
    }

    public void removeImplementation(String implementationKey) {
        implementations.remove(implementationKey);
    }

	protected ServerDeploymentInterface getImplementation(String serverType) {
		return getImplementation(serverType, null);
	}
	
	public ServerDeploymentInterface getImplementation(String serverType,
			String serverVersion) {
		return serverVersion != null && serverVersion.length() > 0 ? implementations
				.get(serverType + serverVersion) : implementations
				.get(serverType);
	}

}
