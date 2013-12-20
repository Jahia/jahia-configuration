package org.jahia.configuration.deployers;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;

/**
 * WebLogic deployer.
 * User: rincevent
 * Date: 10 févr. 2009
 * Time: 15:13:25
 */
public class WeblogicServerDeploymentImpl extends AbstractServerDeploymentImpl {

    public WeblogicServerDeploymentImpl(String name, String targetServerDirectory) {
        super(name, targetServerDirectory);
    }

    /**
     * Returns true if the specified directory indeed contains a valid installation of the application server
     *
     * @param targetServerDirectory the server directory that should be validated.
     * @return true if the directory is indeed a valid server installation directory, false otherwise.
     */
    public boolean validateInstallationDirectory(String targetServerDirectory) {
        return true;
    }

     private String getSharedLibraryDirectory() {
        return "lib";
    }

    public boolean deploySharedLibraries(String targetServerDirectory, File... pathToLibraries) throws IOException {
        File targetDirectory = new File(targetServerDirectory, getSharedLibraryDirectory());
        for (File currentLibraryPath : pathToLibraries) {
            FileUtils.copyFileToDirectory(currentLibraryPath, targetDirectory);
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
        return "";
    }

    public String getDeploymentDirPath(String name, String type) {
        return getDeploymentBaseDir() + "/" + name;
    }

    public String getDeploymentFilePath(String name, String type) {
        return getDeploymentBaseDir() + "/" + name + "." + type;
    }

    @Override
    public String getWarExcludes() {
        return (String) getDeployersProperties().get("weblogic");
    }

}
