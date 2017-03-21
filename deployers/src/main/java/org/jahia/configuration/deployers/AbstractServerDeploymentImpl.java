/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
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
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Abstract class for all the server deployment implementations.
 *
 * @author loom
 *         Date: Oct 29, 2009
 *         Time: 8:08:09 AM
 */
public abstract class AbstractServerDeploymentImpl implements ServerDeploymentInterface {

    private File configDir;
    
    private File dataDir;
    
    private Properties deployersProperties;
    
    private String id;

    private String name;

	private File targetServerDirectory;

    public AbstractServerDeploymentImpl(String id, String name, File targetServerDirectory) {
    	this.id = id;
        this.name = name;
        this.targetServerDirectory = targetServerDirectory;
    }

    @Override
    public boolean deployJdbcDriver(File driverJar) throws IOException {
        return deploySharedLibraries(driverJar);
    }

    @Override
    public boolean deploySharedLibraries(File... pathToLibraries) throws IOException {
        File targetDirectory = getSharedLibraryDirectory();
        for (File currentLibraryPath : pathToLibraries) {
			FileUtils.copyFileToDirectory(currentLibraryPath, targetDirectory);
        }
        return true;
    }

    public File getConfigDir() {
		return configDir;
	}

    public File getDataDir() {
		return dataDir;
	}
    
    protected Properties getDeployersProperties() {
        if (deployersProperties == null) {
            deployersProperties = new Properties();
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("org/jahia/configuration/deployers/JahiaDeployers.properties");
            if (is != null) {
                try {
                    deployersProperties.load(is);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }

        }
        return deployersProperties;
    }
    
    public String getId() {
		return id;
	}
    
    @Override
    public String getName() {
        return name;
    }
    
    protected abstract File getSharedLibraryDirectory();
    
    @Override
    public File getTargetServerDirectory() {
        return targetServerDirectory;
    }

	public String getWarExcludes() {
        return (String) getDeployersProperties().get("warExcludes." + getId());
    }

	@Override
    public String getWebappDeploymentDirNameOverride() {
        return null;
    }

	public void init() {
		// do nothing
	}

	@Override
    public boolean isAutoDeploySupported() {
        return true;
    }

	@Override
    public boolean isEarDeployment() {
        return false;
    }

    public void setConfigDir(File configDir) {
		this.configDir = configDir;
	}

    public void setDataDir(File dataDir) {
		this.dataDir = dataDir;
	}

    @Override
    public boolean undeploySharedLibraries(File... pathToLibraries) throws IOException {
        File targetDirectory = getSharedLibraryDirectory();
        for (File currentLibraryPath : pathToLibraries) {
            File targetFile = new File(targetDirectory, currentLibraryPath.getName());
            targetFile.delete();
        }
        return true;
    }
}
