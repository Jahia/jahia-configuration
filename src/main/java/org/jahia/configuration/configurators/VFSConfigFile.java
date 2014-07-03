package org.jahia.configuration.configurators;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Implementation of the ConfigFile interface that uses Common VFS to provide the internal functionality.
 */
public class VFSConfigFile implements ConfigFile, Closeable {
    
    private static Set<Closeable> OPENED = new HashSet<Closeable>();
    
    public static synchronized void closeAllOpened() {
        for (Iterator<Closeable> iterator = OPENED.iterator(); iterator.hasNext();) {
            try {
                iterator.next().close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                iterator.remove();
            }
        }
    }

    private FileObject fileObject;

    public VFSConfigFile(FileObject fileObject) {
        this.fileObject = fileObject;
    }

    public VFSConfigFile(FileObject parentFileObject, String path) throws FileSystemException {
        this.fileObject = parentFileObject.resolveFile(path);
    }

    public VFSConfigFile(FileSystemManager fileSystemManager, String uri) throws FileSystemException {
        this.fileObject = fileSystemManager.resolveFile(uri);
    }

    @Override
    public void close() throws IOException {
        if (fileObject != null) {
            fileObject.close();
            fileObject.getFileSystem().getFileSystemManager().closeFileSystem(fileObject.getFileSystem());
        }
    }

    public InputStream getInputStream() throws FileSystemException {
        synchronized (OPENED) { 
            OPENED.add(this);
        }
        return fileObject.getContent().getInputStream();
    }

    public URI getURI() throws FileSystemException, URISyntaxException {
        return fileObject.getURL().toURI();
    }
}
