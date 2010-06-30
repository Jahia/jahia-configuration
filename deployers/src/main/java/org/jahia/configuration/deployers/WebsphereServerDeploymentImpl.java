package org.jahia.configuration.deployers;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;

/**
 * Websphere server deployer implementation.
 * TODO This is currently not used, we use a Maven sub-project with Ant commands to do the deployment, but we should
 * really implement this instead, it would be much cleaner and easier to re-use. 
 * User: loom
 * Date: Feb 12, 2009
 * Time: 4:33:51 PM
 */
public class WebsphereServerDeploymentImpl extends AbstractServerDeploymentImpl {

    public WebsphereServerDeploymentImpl(String targetServerDirectory) {
        super(targetServerDirectory);
    }

    public boolean validateInstallationDirectory(String targetServerDirectory) {
        //File serverLibDir = new File(targetServerDirectory, "/AppServer/lib/ext");
        //return serverLibDir.isDirectory();
        return true;
    }

    private String getSharedLibraryDirectory() {
        return "/AppServer/lib/ext";
    }

    private String getSharedJavaLibraryDirectory() {
        return "/AppServer/java/jre/lib/ext";
    }


    public boolean deploySharedLibraries(String targetServerDirectory, List<File> pathToLibraries) throws IOException {
        Iterator<File> libraryPathIterator = pathToLibraries.iterator();
        File targetDirectory = new File(targetServerDirectory, getSharedLibraryDirectory());
        File targetJavaDirectory = new File(targetServerDirectory, getSharedJavaLibraryDirectory());

        while (libraryPathIterator.hasNext()) {
            File currentLibraryPath = libraryPathIterator.next();
            if (currentLibraryPath.getName().startsWith("portlet-api-")) {
                FileUtils.copyFileToDirectory(currentLibraryPath, targetJavaDirectory);
            } else {
                FileUtils.copyFileToDirectory(currentLibraryPath, targetDirectory);
            }
        }
        return true;
    }

    public boolean undeploySharedLibraries(String targetServerDirectory, List<File> pathToLibraries) throws IOException {
        Iterator<File> libraryPathIterator = pathToLibraries.iterator();
        File targetDirectory = new File(targetServerDirectory, getSharedLibraryDirectory());
        while (libraryPathIterator.hasNext()) {
            File currentLibraryPath = libraryPathIterator.next();
            File targetFile = new File(targetDirectory, currentLibraryPath.getName());
            targetFile.delete();
        }
        return true;
    }

    public String getDeploymentBaseDir() {
        return "";  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDeploymentDirPath(String name, String type) {
        return getDeploymentBaseDir() + "/" + name;
    }

    public String getDeploymentFilePath(String name, String type) {
        return getDeploymentBaseDir() + "/" + name;
    }

}