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

}