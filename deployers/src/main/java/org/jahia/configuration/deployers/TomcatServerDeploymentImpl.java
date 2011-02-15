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

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Tomcat 6.0 server deployer implementation.
 * User: Serge Huber
 * Date: 26 d�c. 2007
 * Time: 14:18:34
 */
public class TomcatServerDeploymentImpl extends AbstractServerDeploymentImpl {

    public TomcatServerDeploymentImpl(String targetServerDirectory) {
        super(targetServerDirectory);
    }

    protected String getEndorsedLibraryDirectory() {
        return "endorsed";
    }

    protected String getSharedLibraryDirectory() {
        return "lib";
    }

    public boolean validateInstallationDirectory(String targetServerDirectory) {
        File serverConfig = new File(targetServerDirectory, "conf/server.xml");
        File catalinaProps = new File(targetServerDirectory, "conf/catalina.properties");
        return serverConfig.exists() && catalinaProps.exists();
    }

    public boolean deploySharedLibraries(String targetServerDirectory,
                                         List<File> pathToLibraries) throws IOException {
        Iterator<File> libraryPathIterator = pathToLibraries.iterator();
        File targetDirectory = new File(targetServerDirectory, getSharedLibraryDirectory());
        File targetEndorsedDirectory = new File(targetServerDirectory, getEndorsedLibraryDirectory());
        while (libraryPathIterator.hasNext()) {
            File currentLibraryPath = libraryPathIterator.next();
            if (currentLibraryPath.getName().contains("jaxb-api")) {
                FileUtils.copyFileToDirectory(currentLibraryPath, targetEndorsedDirectory);   
            } else {
                FileUtils.copyFileToDirectory(currentLibraryPath, targetDirectory);
            }
        }
        return true;
    }

    public boolean undeploySharedLibraries(String targetServerDirectory,
                                           List<File> pathToLibraries) throws IOException {
        Iterator<File> libraryPathIterator = pathToLibraries.iterator();
        File targetDirectory = new File(targetServerDirectory, getSharedLibraryDirectory());
        File targetEndorsedDirectory = new File(targetServerDirectory, getEndorsedLibraryDirectory());
        while (libraryPathIterator.hasNext()) {
            File currentLibraryPath = libraryPathIterator.next();
            if (currentLibraryPath.getName().contains("jaxb-api")) {
                File targetFile = new File(targetEndorsedDirectory, currentLibraryPath.getName());
                targetFile.delete();
            } else {
                File targetFile = new File(targetDirectory, currentLibraryPath.getName());
                targetFile.delete();
            }
        }
        return true;
    }
    
    public String getDeploymentBaseDir() {
        return "webapps";
    }

    public String getDeploymentDirPath(String name, String type) {
        return getDeploymentBaseDir() + "/" + name;
    }

    public String getDeploymentFilePath(String name, String type) {
        return getDeploymentBaseDir() + "/" + name + "." + type;
    }

    @Override
    public boolean isAutoDeploySupported() {
        return true;
    }

    @Override
    public String getWarExcludes() {
        return (String) getDeployersProperties().get("tomcat");
    }

}
