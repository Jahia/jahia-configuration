/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
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
import java.io.IOException;

/**
 * Server deployer interface. 
 * @author Serge Huber
 */
public interface ServerDeploymentInterface {

    /**
     * Performs the deployment of a JDBC driver JAR file.
     * 
     * @param driverJar
     *            the driver JAR file to be deployed
     * @return <code>true</code> in case of successful deployment; <code>false</code> otherwise
     * @throws IOException
     *             in case of an I/O error
     */
    boolean deployJdbcDriver(File driverJar) throws IOException;

    boolean deploySharedLibraries(File... pathToLibraries) throws IOException;

    File getDeploymentBaseDir();

    File getDeploymentDirPath(String name, String type);
    
    File getDeploymentFilePath(String name, String type);

    String getName();

    File getTargetServerDirectory();

    /**
     * Returns the excludes pattern for the Jahia WAR artifact, comma separated.
     * Can return null to indicate that nothing should be excluded.
     * 
     * @return the excludes pattern for the Jahia WAR artifact, comma separated;
     *         can return null to indicate that nothing should be excluded
     */
    String getWarExcludes();

    String getWebappDeploymentDirNameOverride();
    
    /**
     * Returns <code>true</code> if the server supports auto deployment of
     * packaged WAR files, required for new portlet deployment.
     * 
     * @return <code>true</code> if the server supports auto deployment of
     *         packaged WAR files, required for new portlet deployment.
     */
    boolean isAutoDeploySupported();
    
    /**
     * Returns <code>true</code> if the application server uses EAR deployment instead of WAR.
     * 
     * @return <code>true</code> if the application server uses EAR deployment; otherwise returns <code>false</code>
     */
    boolean isEarDeployment();
    
    boolean undeploySharedLibraries(File... pathToLibraries) throws IOException;
    
    /**
     * Returns <code>true</code> if the server target directory indeed contains a valid installation of the application server
     * @return <code>true</code> if the server target directory indeed contains a valid installation of the application server
     */
    boolean validateInstallationDirectory();
}
