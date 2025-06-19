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
package org.jahia.configuration.configurators;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base configurator implementation. A configurator is responsible for configuring a sub-system of Jahia. It should
 * work with all versions of Jahia and applications servers, or declare that it isn't compatible with them.
 * 
 * User: loom
 * Date: Feb 13, 2009
 * Time: 3:29:37 PM
 */
public abstract class AbstractConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(AbstractConfigurator.class);

    protected static String getValue(Map values, String key) {
        String replacement = (String) values.get(key);
        if (replacement == null) {
            logger.debug("No value found for key: {}", key);
            return "";
        }
        replacement = replacement.trim();
        return replacement;
    }
    
    protected Map dbProperties = Collections.emptyMap();
    protected JahiaConfigInterface jahiaConfigInterface;

    public AbstractConfigurator(JahiaConfigInterface jahiaConfigInterface) {
        this.jahiaConfigInterface = jahiaConfigInterface;
        logger.debug("Created AbstractConfigurator with jahiaConfigInterface: {}", jahiaConfigInterface);
    }

    public AbstractConfigurator(Map dbProps, JahiaConfigInterface jahiaConfigInterface) {
        this(jahiaConfigInterface);
        this.dbProperties = dbProps;
        logger.debug("Set database properties with {} entries", dbProps != null ? dbProps.size() : 0);
    }

    public void updateConfFromFile(String sourceConfigFile, String destFileName) throws Exception {
        logger.debug("Updating configuration from file: {} to destination: {}", sourceConfigFile, destFileName);
        File sourceFile = new File(sourceConfigFile);
        if (sourceFile.exists()) {
            logger.debug("Source file exists, reading from: {}", sourceFile.getAbsolutePath());
            try (InputStream inputStream = new FileInputStream(sourceFile)) {
                updateConfiguration(inputStream, destFileName);
            }
        } else {
            logger.warn("Source file does not exist: {}", sourceFile.getAbsolutePath());
        }
    }

    public void updateConfFromFileInJar(File jar, String sourceConfigFile, String destFileName) throws Exception {
        logger.debug("Updating configuration from file in jar: {}, source: {}, destination: {}",
                jar != null ? jar.getName() : "null", sourceConfigFile, destFileName);
        try (InputStream jahiaPropertiesConfigFileStream = getFileInputStreamFromJar(jar, sourceConfigFile)) {
            if (jahiaPropertiesConfigFileStream != null) {
                logger.debug("Successfully found source file in jar, updating configuration");
                updateConfiguration(jahiaPropertiesConfigFileStream, destFileName);
            } else {
                logger.warn("Could not find source file '{}' in jar: {}", sourceConfigFile, jar);
            }
        }
    }

    public abstract void updateConfiguration(InputStream inputStream, String destFileName) throws Exception;

    private InputStream getFileInputStreamFromJar(File file, String resourcePath) throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            logger.warn("Invalid jar file: {}", file);
            return null;
        }

        logger.debug("Opening jar file: {}", file.getAbsolutePath());
        JarFile jarFile = new JarFile(file);
        JarEntry entry = jarFile.getJarEntry(resourcePath);

        if (entry == null) {
            logger.warn("Entry not found in jar: {}", resourcePath);
            jarFile.close();
            return null;
        }

        logger.debug("Found entry in jar: {}, size: {}", resourcePath, entry.getSize());
        // Note: Don't close jarFile here - it would close the input stream too
        // The caller must handle the JarFile's lifecycle
        InputStream inputStream = jarFile.getInputStream(entry);

        return new InputStream() {
            @Override
            public int read() throws IOException {
                return inputStream.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return inputStream.read(b, off, len);
            }

            @Override
            public void close() throws IOException {
                try {
                    inputStream.close();
                } finally {
                    jarFile.close();
                    logger.debug("Closed jar file: {}", file.getAbsolutePath());
                }
            }
        };
    }

    protected String getDBProperty(String key) {
        String value = getValue(dbProperties, key);
        logger.debug("Retrieved DB property: {} = {}", key, value.isEmpty() ? "[empty]" : (key.toLowerCase().contains("password") ? "****" : value));
        return value;
    }
}
