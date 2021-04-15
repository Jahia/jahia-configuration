/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.configuration.configurators;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;

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
