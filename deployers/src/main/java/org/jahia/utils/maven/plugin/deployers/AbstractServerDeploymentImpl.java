package org.jahia.utils.maven.plugin.deployers;

/**
 * Abstract class for all the server deployment implementations.
 *
 * @author loom
 *         Date: Oct 29, 2009
 *         Time: 8:08:09 AM
 */
public abstract class AbstractServerDeploymentImpl implements ServerDeploymentInterface {

    private String targetServerDirectory;

    public AbstractServerDeploymentImpl(String targetServerDirectory) {
        this.targetServerDirectory = targetServerDirectory;
    }

    public String getTargetServerDirectory() {
        return targetServerDirectory;
    }



}
