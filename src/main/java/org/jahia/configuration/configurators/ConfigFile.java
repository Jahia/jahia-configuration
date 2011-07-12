package org.jahia.configuration.configurators;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Abstract interface that represents a source configuration file, that may be located on disk or inside a JAR
 */
public interface ConfigFile {

    public URI getURI() throws IOException, URISyntaxException;

    public InputStream getInputStream() throws IOException;

}
