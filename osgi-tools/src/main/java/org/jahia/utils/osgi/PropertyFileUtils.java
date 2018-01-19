/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.osgi;

import asia.redact.bracket.properties.OutputAdapter;
import asia.redact.bracket.properties.ValueModel;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

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
                                   Logger logger) throws IOException {
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
                FileUtils.touch(propertiesOutputFile);
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
