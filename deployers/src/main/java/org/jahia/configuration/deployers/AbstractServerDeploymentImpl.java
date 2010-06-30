package org.jahia.configuration.deployers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

/**
 * Abstract class for all the server deployment implementations.
 *
 * @author loom
 *         Date: Oct 29, 2009
 *         Time: 8:08:09 AM
 */
public abstract class AbstractServerDeploymentImpl implements ServerDeploymentInterface {

    private String targetServerDirectory;
    
    private Properties deployersProperties;

    public AbstractServerDeploymentImpl(String targetServerDirectory) {
        this.targetServerDirectory = targetServerDirectory;
    }

    public String getTargetServerDirectory() {
        return targetServerDirectory;
    }

    public String getWarExcludes() {
        return null;
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
    
    public boolean isAutoDeploySupported() {
        return false;
    }
}
