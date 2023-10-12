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
package org.jahia.utils.maven.plugin.resources;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.utils.properties.Properties;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Mojo that adds the value for a property in the properties file.
 *
 * @goal update-properties-file
 * @phase process-resources
 * @author Sergiy Shyrkov
 */
public class UpdatePropertiesFileMojo extends AbstractMojo {

    /**
     * Should the value be rather appended (in case of <code>true</code>) to the existing one or overwrite it completely (in case of
     * <code>false</code>).
     *
     * @parameter
     */
    protected boolean append;

    /**
     * The target file to write properties file into. If not specified the source file will be modified "in-place".
     *
     * @parameter
     */
    protected File dest;

    /**
     * If set to <code>true</code> the multivalue entries will be formatted into multiline.
     *
     * @parameter
     */
    protected boolean formatMultiValues;

    /**
     * The key for the entry in the properties file.
     *
     * @parameter
     */
    protected String key;

    /**
     * If set to <code>true</code> the multiline value formatting will use pretty print (space indentation).
     *
     * @parameter
     */
    protected boolean prettyPrint;

    /**
     * The properties file as a source of modifications.
     *
     * @parameter
     */
    protected File src;

    /**
     * Should we perform value interpolation in the properties?
     *
     * @parameter
     */
    protected boolean substitute;

    /**
     * The value to be added into property file. In case the value is not provided the entry with the specified key will be removed from the
     * properties file.
     *
     * @parameter
     */
    protected String value;

    /**
     * Value separator.
     *
     * @parameter
     */
    protected String valueSeparator = ",";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (src == null || !src.isFile()) {
            throw new MojoFailureException("Unable to find the source file " + src);
        }
        if (StringUtils.isEmpty(key)) {
            throw new MojoFailureException("Key should not be null or empty");
        }

        try {
            Properties p = new Properties(substitute);
            p.load(src);
            String existingValue = p.getProperty(key);
            if (value == null) {
                if (existingValue != null) {
                    getLog().info("Removing entry with the key " + key);
                    p.remove(key);
                } else {
                    getLog().info("No entry with the key " + key + " exist. No modification will be done.");
                }
            } else {
                if (existingValue == null) {
                    getLog().info("Adding entry with the key " + key + " and value " + value);
                    setValue(p, value);
                } else {
                    if (append) {
                        getLog().info("Appending value " + value + " for entry with the key " + key);
                        setValue(p, existingValue + valueSeparator + value);
                    } else {
                        getLog().info("Modifying value for entry with the key " + key + ". Setting it to " + value);
                        setValue(p, value);
                    }
                }
            }
            File target = dest != null ? dest : src;
            getLog().info("Storing updated properties into file: " + target);
            p.save(target);
        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private List<String> formatValue(String toFormat) {
        List<String> newValues = new LinkedList<String>();
        newValues.add("");
        String[] values = StringUtils.split(toFormat, valueSeparator + ' ');
        for (int i = 0; i < values.length; i++) {
            StringBuilder line = new StringBuilder(values[i].length() + 6);
            if (prettyPrint) {
                line.append("    ");
            }
            line.append(values[i]);
            if (i < values.length - 1) {
                line.append(valueSeparator);
                if (prettyPrint) {
                    line.append(" ");
                }
            }
            newValues.add(line.toString());
        }
        return newValues;
    }

    private void setValue(Properties p, String targetValue) {
        if (formatMultiValues) {
            p.put(key, p.getComments(key), formatValue(targetValue));
        } else {
            p.put(key, p.getComments(key), targetValue);
        }
    }
}
