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
import java.util.Map;
import java.util.TreeMap;

/**
 * A disk cache for ParsingContext instances, so that we don't have to calculate them on each build.
 * Also, this cache can load up pre-built instances from the class loader, to provide prepopulated cache entries
 * for JARs that contain errors or missing information (such as missing dependencies).
 */
public class ParsingContextCache {

    private static final String CACHE_MODEL_VERSION = "0.2";

    private File cacheLocation;
    private Map<String,ParsingContext> parsingContextCache = new TreeMap<String,ParsingContext>();
    private ObjectMapper objectMapper;
    ClassLoader resourceClassLoader = this.getClass().getClassLoader();

    public ParsingContextCache(File cacheLocation, ClassLoader classLoader) {
        if (cacheLocation == null) {
            throw new RuntimeException("Cache location is null, aborting!");
        }
        if (!cacheLocation.exists()) {
            if (!cacheLocation.mkdirs()) {
                throw new RuntimeException("Cache location couldn't be created at " + cacheLocation + ", aborting !");
            }
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
        this.cacheLocation = cacheLocation;
        if (classLoader != null) {
            resourceClassLoader = classLoader;
        }
    }

    public void writeCacheInfo(File cacheInfoFile) {
        try {
            getObjectMapper().writeValue(cacheInfoFile, CACHE_MODEL_VERSION);
        } catch (IOException e) {
            throw new RuntimeException("Error writing cache info file " + cacheInfoFile + ", aborting !");
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
        if (!cacheLocation.mkdirs()) {
            throw new RuntimeException("Cache location couldn't be created at " + cacheLocation + ", aborting !");
        }
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
        if (!artifactJSONFile.getParentFile().exists()) {
            if (!artifactJSONFile.getParentFile().mkdirs()) {
                throw new RuntimeException("Couldn't create parent directory " + artifactJSONFile.getParentFile() + " to store file " + artifactJSONFile + ", aborting !");
            }
        }
        try {
            parsingContext.setInCache(true);
            getObjectMapper().writeValue(artifactJSONFile, parsingContext);
            ParsingContext previousParsingContext = parsingContextCache.put(MavenAetherHelperUtils.getCoords(artifact), parsingContext);
            return previousParsingContext;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        return objectMapper;
    }

}
