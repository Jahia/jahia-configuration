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
package org.jahia.configuration.deployers;

import java.io.File;

/**
 * Websphere server deployer implementation.
 * TODO This is currently not used, we use a Maven sub-project with Ant commands to do the deployment, but we should
 * really implement this instead, it would be much cleaner and easier to re-use. 
 * User: loom
 * Date: Feb 12, 2009
 * Time: 4:33:51 PM
 */
public class WebsphereServerDeploymentImpl extends AbstractServerDeploymentImpl {

    public WebsphereServerDeploymentImpl(String id, String name, File targetServerDirectory) {
        super(id, name, targetServerDirectory);
    }

    public boolean validateInstallationDirectory() {
        return true;
    }

    @Override
    protected File getSharedLibraryDirectory() {
        return new File("/AppServer/lib/ext");
    }

    @Override
    public File getDeploymentBaseDir() {
        return getTargetServerDirectory();
    }

    @Override
    public File getDeploymentDirPath(String name, String type) {
        return new File(getDeploymentBaseDir(), name);
    }

    @Override
    public File getDeploymentFilePath(String name, String type) {
        return new File(getDeploymentBaseDir(), name);
    }

    @Override
    public boolean isEarDeployment() {
        return true;
    }

    @Override
    public String getWebappDeploymentDirNameOverride() {
        return "jahia.war";
    }

    @Override
    public boolean isAutoDeploySupported() {
        return false;
    }
}