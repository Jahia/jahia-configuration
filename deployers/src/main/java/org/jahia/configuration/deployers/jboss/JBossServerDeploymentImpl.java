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
package org.jahia.configuration.deployers.jboss;

import java.io.File;
import java.io.IOException;

import org.jahia.configuration.deployers.AbstractServerDeploymentImpl;

/**
 * JBoss common server deployer implementation, supporting JBoss AS 7.x / JBoss EAP 6.x.
 * 
 * @author Serge Huber
 * @author Sergiy Shyrkov
 */
public class JBossServerDeploymentImpl extends AbstractServerDeploymentImpl {

    private static final String JAHIA_EAR_DIR_NAME = "digitalfactory.ear";

    private static final String JAHIA_WAR_DIR_NAME = "jahia.war";

    public JBossServerDeploymentImpl(String id, String name, File targetServerDirectory) {
        super(id, name, targetServerDirectory);
    }

    @Override
    public boolean deployJdbcDriver(File driverJar) throws IOException {
        return DriverDeploymentHelper.deploy(getTargetServerDirectory(), driverJar);
    }

    @Override
    public File getDeploymentBaseDir() {
        return new File(getTargetServerDirectory(), "standalone/deployments");
    }

    @Override
    public File getDeploymentDirPath(String name, String type) {
        String ext = "." + type;
        return getDeploymentFilePath(name.endsWith(ext) ? name.substring(0, name.length() - ext.length()) : name, type);
    }

    @Override
	public File getDeploymentFilePath(String name, String type) {
		return new File(
				getDeploymentBaseDir(),
				"war".equals(type) ? (JAHIA_EAR_DIR_NAME + "/" + name + "." + type)
						: (name + "." + type));
	}

    @Override
    protected File getSharedLibraryDirectory() {
        return new File(getTargetServerDirectory(), "standalone/deployments/" + JAHIA_EAR_DIR_NAME +"/lib");
    }

    @Override
    public String getWebappDeploymentDirNameOverride() {
        return JAHIA_WAR_DIR_NAME;
    }

    @Override
    public boolean isEarDeployment() {
        return true;
    }

    @Override
    public boolean validateInstallationDirectory() {
        return new File(getTargetServerDirectory(), "jboss-modules.jar").exists();
    }
}
