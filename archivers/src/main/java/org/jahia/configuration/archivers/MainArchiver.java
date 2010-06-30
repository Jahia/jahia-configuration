package org.jahia.configuration.archivers;

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
 */
public class MainArchiver {

    private String targetArchiveName;
    private String sourceDirectory;
    private String excludes;

    public MainArchiver(String targetArchiveName, String sourceDirectory, String excludes) {
        this.targetArchiveName = targetArchiveName;
        this.sourceDirectory = sourceDirectory;
        this.excludes = excludes;
    }

    public void execute() throws IOException, ArchiverException {
        ZipArchiver archiver = new ZipArchiver();
        File absoluteDestFile = new File( new File(targetArchiveName). getAbsolutePath()); // little trick to get File.getParentFile() to work properly in the setDestFile call.
        archiver.setDestFile( absoluteDestFile );
        archiver.addDirectory(new File(sourceDirectory), null, excludes != null ? excludes.split(",") : null);
        archiver.createArchive();
    }

    public static void main(String[] args) {
        if (args.length < 2 || args[0].equals("-m") && args.length < 3) {
            System.out
                    .println("Usage is:\n\t[-m] targetArchiveFileName sourceDirectory [excludes]. For example: -m jahia.war"
                            + " jahia-directory **/WEB-INF/lib/jstl-*.jar,**/WEB-INF/lib/standard-*.jar");
            System.out
                    .println("\tOption -m performs \"move\" operation for the source folder"
                            + " into the archive, i.e. the source folder is actually deleted after making the specified archive.");
            return;
        }
        String target = null;
        String source = null;
        String excludes = null;
        boolean performMove = args[0].equals("-m");
        if (performMove) {
            target = args[1];
            source = args[2];
            excludes = args.length > 3 ? args[3] : null;
        } else {
            target = args[0];
            source = args[1];
            excludes = args.length > 2 ? args[2] : null;
        }
        
        try {
            new MainArchiver(target, source, excludes).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (performMove) {
            // need to delete the original directory
            try {
                FileUtils.deleteDirectory(new File(source));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
