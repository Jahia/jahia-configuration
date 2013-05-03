package org.jahia.utils.osgi;

import asia.redact.bracket.properties.OutputAdapter;
import asia.redact.bracket.properties.ValueModel;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A utility class to handle property file operations.
 */
public class PropertyFileUtils {

    public static void updatePropertyFile(File propertiesInputFile,
                                   File propertiesOutputFile,
                                   String propertyFilePropertyName,
                                   String[] propertyValues,
                                   Log logger) throws IOException {
        if ((propertiesOutputFile != null) && (propertyValues.length > 0)) {
            asia.redact.bracket.properties.Properties frameworkProperties = null;
            if (propertiesInputFile != null && propertiesInputFile.exists()) {
                FileReader propertiesInputFileReader = null;
                try {
                    propertiesInputFileReader = new FileReader(propertiesInputFile);
                    asia.redact.bracket.properties.Properties.Factory.mode = asia.redact.bracket.properties.Properties.Mode.Line;
                    asia.redact.bracket.properties.Properties props = asia.redact.bracket.properties.Properties.Factory.getInstance(propertiesInputFileReader);
                    // we will now reprocess the keys to trim them since there is a bug in the library http://code.google.com/p/bracket-properties/issues/detail?id=1
                    Map<String,ValueModel> propertyMap = props.getPropertyMap();
                    Map<String,ValueModel> propertyMapCopy = new LinkedHashMap<String,ValueModel>(props.getPropertyMap());
                    propertyMap.clear();
                    for (Map.Entry<String,ValueModel> propertyMapEntry : propertyMapCopy.entrySet()) {
                        String trimmedPropertyKey = propertyMapEntry.getKey().trim();
                        if (!trimmedPropertyKey.equals(propertyMapEntry.getKey())) {
                            logger.debug("Replacing invalid property key [" + propertyMapEntry.getKey() + "] with [" + trimmedPropertyKey + "]");
                        }
                        propertyMap.put(trimmedPropertyKey, propertyMapEntry.getValue());
                    }
                    frameworkProperties = props;
                } finally {
                    IOUtils.closeQuietly(propertiesInputFileReader);
                }
            } else {
                frameworkProperties = asia.redact.bracket.properties.Properties.Factory.getInstance();
            }
            frameworkProperties.put(propertyFilePropertyName, propertyValues);
            OutputAdapter out = new OutputAdapter(frameworkProperties);
            FileWriter propertyOutputFileWriter = null;
            try {
                propertyOutputFileWriter = new FileWriter(propertiesOutputFile);
                out.writeTo(propertyOutputFileWriter);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(propertyOutputFileWriter);
            }
            logger.info("Generated property file saved in " + propertiesOutputFile.getCanonicalPath());
        }
    }

}
