/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2023 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.osgi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.jahia.utils.maven.plugin.support.MavenAetherHelperUtils;
import org.jahia.utils.osgi.parsers.ParsingContext;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;

/**
 * A disk cache for ParsingContext instances, so that we don't have to calculate them on each build.
 * Also, this cache can load up pre-built instances from the class loader, to provide prepopulated cache entries
 * for JARs that contain errors or missing information (such as missing dependencies).
 */
public class ParsingContextCache {

    private static final String CACHE_MODEL_VERSION = "0.2";

    /**
     * The cache location is shared by every module of the build, so the modules of a parallel build (mvn -T) set it
     * up at the same time. Purging it and recreating it is a sequence, not a single atomic step.
     */
    private static final Object CACHE_DIRECTORY_LOCK = new Object();

    private File cacheLocation;
    private Map<String,ParsingContext> parsingContextCache = new TreeMap<String,ParsingContext>();
    private ObjectMapper objectMapper;
    ClassLoader resourceClassLoader = this.getClass().getClassLoader();

    public ParsingContextCache(File cacheLocation, ClassLoader classLoader) {
        if (cacheLocation == null) {
            throw new RuntimeException("Cache location is null, aborting!");
        }
        synchronized (CACHE_DIRECTORY_LOCK) {
            if (!cacheLocation.exists()) {
                createDirectory(cacheLocation);
            } else {
                File cacheInfoFile = new File(cacheLocation, "cache-info.json");
                if (!cacheInfoFile.exists()) {
                    purgeCacheDirectory(cacheLocation);
                    writeCacheInfo(cacheInfoFile);
                } else {
                    String cacheModelVersion = null;
                    try {
                        cacheModelVersion = getObjectMapper().readValue(cacheInfoFile, String.class);
                    } catch (IOException e) {
                        // This can happen, if the cache info file is somehow corrupted, in this case we simply recreate
                        // the cache.
                    }
                    if (cacheModelVersion == null || !cacheModelVersion.equals(CACHE_MODEL_VERSION)) {
                        purgeCacheDirectory(cacheLocation);
                        writeCacheInfo(cacheInfoFile);
                    }
                }
            }
        }
        this.cacheLocation = cacheLocation;
        if (classLoader != null) {
            resourceClassLoader = classLoader;
        }
    }

    public void writeCacheInfo(File cacheInfoFile) {
        try {
            writeAtomically(cacheInfoFile, CACHE_MODEL_VERSION);
        } catch (IOException e) {
            throw new RuntimeException("Error writing cache info file " + cacheInfoFile + ", aborting !", e);
        }
    }

    public void purgeCacheDirectory(File cacheLocation) {
        // no cache info file found, let's purge the cache and then create it.
        try {
            FileUtils.deleteDirectory(cacheLocation);
        } catch (IOException e) {
            throw new RuntimeException("Error purging cache directory " + cacheLocation, e);
        }
        // recreate the empty directory
        createDirectory(cacheLocation);
    }

    public ParsingContext get(Artifact artifact) {
        ParsingContext parsingContext = parsingContextCache.get(MavenAetherHelperUtils.getCoords(artifact));
        if (parsingContext != null) {
            parsingContext.reconnectPackageInfos();
            return parsingContext;
        }
        File artifactJSONFile = new File(cacheLocation, MavenAetherHelperUtils.getDiskPath(artifact) + ".json");
        if (!artifactJSONFile.exists()) {
            // we didn't find a cache entry, let's check if there is a prepopulated one in the class loader
            // resources.
            URL artifactJSONUrl = resourceClassLoader.getResource("dependencyCache/" + MavenAetherHelperUtils.getDiskPath(artifact) + ".json");
            if (artifactJSONUrl == null) {
                return null;
            } else {
                try {
                    parsingContext = getObjectMapper().readValue(artifactJSONUrl, ParsingContext.class);
                    parsingContext.reconnectPackageInfos();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                parsingContext = getObjectMapper().readValue(artifactJSONFile, ParsingContext.class);
                parsingContext.reconnectPackageInfos();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (parsingContext != null) {
            if (artifact.getFile() != null) {
                if (artifact.getFile().lastModified() != parsingContext.getLastModified() ||
                        artifact.getFile().length() != parsingContext.getFileSize() ||
                        !artifact.getFile().getName().equals(parsingContext.getFileName()) ||
                        !artifact.getFile().getPath().equals(parsingContext.getFilePath())) {
                    // cached file info and artifact file do not match, we will not use the entry from the cache !
                    return null;
                }
            }

            parsingContextCache.put(MavenAetherHelperUtils.getCoords(artifact), parsingContext);
            return parsingContext;
        }
        return null;
    }

    public ParsingContext put(Artifact artifact, ParsingContext parsingContext) {
        File artifactJSONFile = new File(cacheLocation, MavenAetherHelperUtils.getDiskPath(artifact) + ".json");
        createDirectory(artifactJSONFile.getParentFile());
        try {
            parsingContext.setInCache(true);
            writeAtomically(artifactJSONFile, parsingContext);
            ParsingContext previousParsingContext = parsingContextCache.put(MavenAetherHelperUtils.getCoords(artifact), parsingContext);
            return previousParsingContext;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Creates the directory and every missing parent, and accepts a directory another module of the build created in
     * the meantime. File.mkdirs() answers false in that case, which is indistinguishable from a real failure.
     */
    private void createDirectory(File directory) {
        try {
            Files.createDirectories(directory.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Cache directory couldn't be created at " + directory + ", aborting !", e);
        }
    }

    /**
     * Writes the value to a temporary file of the target directory, then moves it over the target. A reader of a
     * concurrent module then sees either the previous file or the complete new one, never a truncated one.
     */
    private void writeAtomically(File target, Object value) throws IOException {
        Path temporaryFile = Files.createTempFile(target.getParentFile().toPath(), target.getName(), ".tmp");
        try {
            getObjectMapper().writeValue(temporaryFile.toFile(), value);
            try {
                Files.move(temporaryFile, target.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                // A few network and FUSE filesystems refuse an atomic move. The plain move keeps the build
                // working there, with the guarantee the direct write had before.
                Files.move(temporaryFile, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    protected ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        return objectMapper;
    }

}
