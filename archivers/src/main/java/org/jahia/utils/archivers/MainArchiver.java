package org.jahia.utils.archivers;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * MainArchiver, that handles different behaviors for different server types.
 * TODO For the moment serverType is not handled, we will implement later.
 * User: loom
 * Date: May 20, 2010
 * Time: 2:41:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class MainArchiver {

    public String serverType;
    public String targetArchiveName;
    public String sourceDirectory;

    public MainArchiver(String serverType, String targetArchiveName, String sourceDirectory) {
        this.serverType = serverType;
        this.targetArchiveName = targetArchiveName;
        this.sourceDirectory = sourceDirectory;
    }

    public void execute() throws IOException, ArchiverException {
        ZipArchiver archiver = new ZipArchiver();
        File absoluteDestFile = new File( new File(targetArchiveName). getAbsolutePath()); // little trick to get File.getParentFile() to work properly in the setDestFile call.
        archiver.setDestFile( absoluteDestFile );
        archiver.addDirectory( new File(sourceDirectory) );
        archiver.createArchive();
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage is : serverType targetArchiveFileName sourceDirectory [moveFileToArchive]. For example : was jahia.war jahia-directory true. Valid values for serverType are : tomcat, was, weblogic, jboss");
            return;
        }
        try {
            new MainArchiver(args[0], args[1], args[2]).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (args.length > 3 && Boolean.valueOf(args[3])) {
            // need to delete the original directory
            try {
                FileUtils.deleteDirectory(new File(args[2]));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
