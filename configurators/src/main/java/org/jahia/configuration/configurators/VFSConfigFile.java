package org.jahia.configuration.configurators;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Implementation of the ConfigFile interface that uses Common VFS to provide the internal functionality.
 */
public class VFSConfigFile implements ConfigFile {

    private FileObject fileObject;

    public VFSConfigFile(FileObject fileObject) {
        this.fileObject = fileObject;
    }

    public VFSConfigFile(FileSystemManager fileSystemManager, String uri) throws FileSystemException {
        this.fileObject = fileSystemManager.resolveFile(uri);
    }

    public VFSConfigFile(FileObject parentFileObject, String path) throws FileSystemException {
        this.fileObject = parentFileObject.resolveFile(path);
    }

    public URI getURI() throws FileSystemException, URISyntaxException {
        return fileObject.getURL().toURI();
    }

    public InputStream getInputStream() throws FileSystemException {
        return fileObject.getContent().getInputStream();
    }
}
