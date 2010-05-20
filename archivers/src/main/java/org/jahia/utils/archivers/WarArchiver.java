package org.jahia.utils.archivers;

import java.io.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Very basic WAR archiver, used to recompress a previously extracted WAR.
 *
 * @author loom
 *         Date: May 18, 2010
 *         Time: 3:06:48 PM
 */
public class WarArchiver {

    public static int BUFFER_SIZE = 10240;

    public void createJarArchive(File archiveFile, File[] tobeJared) throws IOException {
        byte buffer[] = new byte[BUFFER_SIZE];
        // Open archive file
        FileOutputStream stream = new FileOutputStream(archiveFile);
        JarOutputStream out = new JarOutputStream(stream);

        for (int i = 0; i < tobeJared.length; i++) {
            if (tobeJared[i] == null || !tobeJared[i].exists()
                    || tobeJared[i].isDirectory())
                continue; // Just in case...

            // Add archive entry
            JarEntry jarAdd = new JarEntry(tobeJared[i].getName());
            jarAdd.setTime(tobeJared[i].lastModified());
            out.putNextEntry(jarAdd);

            // Write file to archive
            FileInputStream in = new FileInputStream(tobeJared[i]);
            while (true) {
                int nRead = in.read(buffer, 0, buffer.length);
                if (nRead <= 0)
                    break;
                out.write(buffer, 0, nRead);
            }
            in.close();
        }

        out.close();
        stream.close();
    }

    public void createJarArchive(File archiveFile, File directoryToJar) throws IOException {
        byte buffer[] = new byte[BUFFER_SIZE];

        if (!directoryToJar.isDirectory()) {
            return;
        }

        // relative == "stuff/xyz.dat"

        // Open archive file
        FileOutputStream stream = new FileOutputStream(archiveFile);
        JarOutputStream out = new JarOutputStream(new BufferedOutputStream(stream));

        File[] directoryContents = directoryToJar.listFiles();
        for (File curDirectoryChild : directoryContents) {

            // Add archive entry
            String relativePath = directoryToJar.toURI().relativize(new File(curDirectoryChild.getPath()).toURI()).getPath();
            JarEntry jarAdd = new JarEntry(relativePath);
            jarAdd.setTime(curDirectoryChild.lastModified());
            out.putNextEntry(jarAdd);

            if (!curDirectoryChild.isDirectory()) {
                // Write file to archive
                InputStream in = new BufferedInputStream(new FileInputStream(curDirectoryChild));
                while (true) {
                    int nRead = in.read(buffer, 0, buffer.length);
                    if (nRead <= 0)
                        break;
                    out.write(buffer, 0, nRead);
                }
                in.close();
            }
        }

        out.close();
        stream.close();
    }

}

