/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2013 Jahia Solutions Group SA. All rights reserved.
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
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */

package org.jahia.configuration.deployers.jboss;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jahia.configuration.deployers.AbstractServerDeploymentImpl;

/**
 * JBoss common server deployer implementation, supporting JBoss AS 7.x / JBoss EAP 6.x.
 * 
 * @author Serge Huber
 * @author Sergiy Shyrkov
 */
public class JBossServerDeploymentImpl extends AbstractServerDeploymentImpl {

    private static final String JAHIA_EAR_DIR_NAME = "jahia.ear";

    private static final String JAHIA_WAR_DIR_NAME = "jahia.war";

    public JBossServerDeploymentImpl(String name, String targetServerDirectory) {
        super(name, targetServerDirectory);
    }

    @Override
    public boolean deployJdbcDriver(String targetServerDirectory, File driverJar) throws IOException {
        return DriverDeploymentHelper.deploy(targetServerDirectory, driverJar);
    }

    @Override
    public boolean deploySharedLibraries(String targetServerDirectory, File... pathToLibraries) throws IOException {
        File targetDirectory = new File(targetServerDirectory, getSharedLibraryDirectory());
        for (File currentLibraryPath : pathToLibraries) {
            FileUtils.copyFileToDirectory(currentLibraryPath, targetDirectory);
        }
        return true;
    }

    @Override
    public String getDeploymentBaseDir() {
        return "standalone/deployments";
    }

    @Override
    public String getDeploymentDirPath(String name, String type) {
        String ext = "." + type;
        return getDeploymentFilePath(name.endsWith(ext) ? name.substring(0, name.length() - ext.length()) : name, type);
    }

    @Override
    public String getDeploymentFilePath(String name, String type) {
        StringBuilder path = new StringBuilder(64);
        path.append(getDeploymentBaseDir());
        if ("war".equals(type)) {
            path.append("/" + JAHIA_EAR_DIR_NAME);
        }
        path.append("/").append(name).append(".").append(type);
        return path.toString();
    }

    protected String getSharedLibraryDirectory() {
        return "standalone/deployments/jahia.ear/lib";
    }

    @Override
    public String getWarExcludes() {
        return (String) getDeployersProperties().get("jboss");
    }

    @Override
    public String getWebappDeploymentDirNameOverride() {
        return JAHIA_WAR_DIR_NAME;
    }

    @Override
    public boolean isAutoDeploySupported() {
        return true;
    }

    @Override
    public boolean isEarDeployment() {
        return true;
    }

    @Override
    public boolean undeploySharedLibraries(String targetServerDirectory, List<File> pathToLibraries) throws IOException {
        File targetDirectory = new File(targetServerDirectory, getSharedLibraryDirectory());
        for (File currentLibraryPath : pathToLibraries) {
            File targetFile = new File(targetDirectory, currentLibraryPath.getName());
            targetFile.delete();
        }
        return true;
    }

    @Override
    public boolean validateInstallationDirectory(String targetServerDirectory) {
        return new File(targetServerDirectory, "jboss-modules.jar").exists();
    }
}
