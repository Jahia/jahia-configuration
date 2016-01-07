/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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

/**
 * Created by IntelliJ IDEA.
 * User: islam
 * Date: 24 juin 2008
 * Time: 11:51:55
 * To change this template use File | Settings | File Templates.
 */


import java.io.*;
import java.util.*;
import java.util.regex.PatternSyntaxException;

import org.codehaus.plexus.util.StringUtils;

/**
 * desc:  This class provides to you a *super interface* for properties.
 * It allows you to create a new properties file, set properties, remove
 * properties, get properties, get properties from an existing properties
 * object or from a flat file, get a complete properties object, and you can
 * store the new properties object where you want on the filesystem. The store
 * method keep your base properties file design (not like the store() method
 * from java properties object)!
 * <p/>
 * Copyright:    Copyright (c) 2002
 * Company:      Jahia Ltd
 *
 * @author Alexandre Kraft
 * @version 1.0
 */
public class PropertiesManager {
    private Properties properties = new Properties();
    private Set<String> modifiedProperties = new HashSet<String>();
    private boolean loadedFromInputStream = false;
    private Set<String> removedPropertyNames = new HashSet<String>();
    private boolean unmodifiedCommentingActivated = false;
    private String additionalPropertiesMessage = "The following properties were added.";

    /**
     * Default constructor.
     *
     */
    public PropertiesManager() {
        // do nothing :o)
    } // end constructor


    /**
     * Construct a properties manager from an existing input stream that references a properties file.
     * @param sourcePropertiesInputStream
     * @throws IOException
     */
    public PropertiesManager(InputStream sourcePropertiesInputStream) throws IOException {
        this.loadProperties(sourcePropertiesInputStream);
    } // end constructor

    /**
     * Default constructor.
     *
     * @param properties The properties object used to define base properties.
     */
    public PropertiesManager(Properties properties) {
        this.properties = properties;
    } // end constructor


    /**
     * Load a complete properties file in memory by its filename.
     *
     * @param sourcePropertiesInputStream
     */
    private void loadProperties(InputStream sourcePropertiesInputStream) throws IOException {
        properties = new Properties();
        properties.load(sourcePropertiesInputStream);
        sourcePropertiesInputStream.close();
        loadedFromInputStream = true;
    } // end loadProperties


    /**
     * Get a property value by its name.
     *
     * @param propertyName The property name to get its value.
     * @return Returns a String containing the value of the property name.
     */
    public String getProperty(String propertyName) {
        return properties.getProperty(propertyName);
    } // end getProperty


    /**
     * Set a property value by its name.
     *
     * @param propertyName The property name to set.
     * @param propvalue    The property value to set.
     */
    public void setProperty(String propertyName,
                            String propvalue) {
        String oldValue = (String) properties.setProperty(propertyName, propvalue);
        if (!StringUtils.equals(oldValue, propvalue)) {
        	// set "modified" flag only if the value was effectively changed
        	modifiedProperties.add(propertyName);
        }
    } // end setProperty


    /**
     * Remove a property by its name.
     *
     * @param propertyName The property name to remove.
     * @author Alexandre Kraft
     */
    public void removeProperty(String propertyName) {
        properties.remove(propertyName);
        removedPropertyNames.add(propertyName);

    } // end removeProperty

    /**
     * Store new properties and values in the properties file.
     * If the file where you want to write doesn't exists, the file is created.
     *
     * @param sourcePropertiesInputStream The source input stream used to load the properties from, or null if it
     * doesn't exist.
     * @param destPropertiesFilePath   The filesystem path where the file is saved.
     * @author Alexandre Kraft
     * @author Khue N'Guyen
     */
    public void storeProperties(InputStream sourcePropertiesInputStream, String destPropertiesFilePath)
            throws IOException {
        if (loadedFromInputStream && sourcePropertiesInputStream == null) {
            throw new UnsupportedOperationException("If loaded from an input stream, it must be provided when storing the file");
        }
        List outputLineList = new ArrayList();
        String currentLine = null;

        File propertiesFileObject = new File(destPropertiesFilePath);
        File propertiesFileFolder = propertiesFileObject.getParentFile();

        // check if the destination folder exists and create it if needed...
        if (!propertiesFileFolder.exists()) {
            propertiesFileFolder.mkdirs();
            propertiesFileFolder = null;
        }

        // try to create a file object via the propertiesFilePath...

        if (loadedFromInputStream) {
            BufferedReader buffered = new BufferedReader(new InputStreamReader(sourcePropertiesInputStream));
            int position = 0;

            Set remainingPropertyNames = new HashSet(properties.keySet());

            // parse the file...
            while ((currentLine = buffered.readLine()) != null) {
                try {
                    currentLine = currentLine.replaceAll("\\t", "    "); //now supports Tab characters in lines
                    int equalPosition = currentLine.indexOf("=");
                    if (!currentLine.trim().equals("") && !currentLine.trim().startsWith("#") && (equalPosition >= 0)) {
                        String currentPropertyName = currentLine.substring(0, equalPosition).trim();
                        if (remainingPropertyNames.contains(currentPropertyName)) {
                            String propValue = properties.getProperty(currentPropertyName);
                            remainingPropertyNames.remove(currentPropertyName);
                            StringBuffer thisLineBuffer = new StringBuffer();
                            if (!unmodifiedCommentingActivated ||
                                    modifiedProperties.contains(currentPropertyName)) {
                                thisLineBuffer.append(currentLine.substring(0, equalPosition + 1));
                                thisLineBuffer.append(" ");
                                thisLineBuffer.append(escapeValue(propValue));
                            } else {
                                // this property was not modified, we comment it out
                                thisLineBuffer.append("#");
                                thisLineBuffer.append(currentLine.substring(0, equalPosition + 1));
                                thisLineBuffer.append(" ");
                                thisLineBuffer.append(escapeValue(propValue));
                            }
                            outputLineList.add(thisLineBuffer.toString());
                        } else if (removedPropertyNames.contains(currentPropertyName)) {
                            // the property must be removed from the file, we do not add it to the output.                            
                        }
                    } else {
                        // this is a special line only for layout, like a comment or a blank line...
                        outputLineList.add(currentLine.trim());
                    }
                } catch (IndexOutOfBoundsException ioobe) {
                } catch (PatternSyntaxException ex1) {
                    ex1.printStackTrace();
                } catch (IllegalArgumentException ex2) {
                    ex2.printStackTrace();
                }
            }

            // add not found properties at the end of the file (and the jahia.properties layout is kept)...
            if ((remainingPropertyNames.size() > 0) && (additionalPropertiesMessage != null)) {
                outputLineList.add("# " + additionalPropertiesMessage);
            }
            Iterator remainingPropNameIterator = remainingPropertyNames.iterator();
            while (remainingPropNameIterator.hasNext()) {
                String restantPropertyName = (String) remainingPropNameIterator.next();
                StringBuffer specialLineBuffer = new StringBuffer();
                specialLineBuffer.append(restantPropertyName);
                for (int i = 0; i < 55 - restantPropertyName.length(); i++) {
                    specialLineBuffer.append(" ");
                }
                specialLineBuffer.append("=   ");
                specialLineBuffer.append(escapeValue(properties.getProperty(restantPropertyName)));
                outputLineList.add(specialLineBuffer.toString());
            }

            // close the buffered filereader...
            buffered.close();

            // write the file...
            writeTheFile(destPropertiesFilePath, outputLineList);
        } else {
            FileOutputStream outputStream = new FileOutputStream(destPropertiesFilePath);
            properties.store(outputStream, "This file has been written by Jahia.");
            outputStream.close();
        }
    } // end storeProperties


    /**
     * Write the file composed by the storeProperties() method, using
     *
     * @param propertiesFilePath The filesystem path where the file is saved.
     * @param bufferList         List containing all the string lines of the new file.
     * @author Alexandre Kraft
     */
    private void writeTheFile(String propertiesFilePath,
                              List bufferList) {

        File thisFile = null;
        FileWriter fileWriter = null;
        StringBuffer outputBuffer = null;

        try {
            thisFile = new File(propertiesFilePath);
            fileWriter = new FileWriter(thisFile);
            outputBuffer = new StringBuffer();

            for (int i = 0; i < bufferList.size(); i++) {
                outputBuffer.append((String) bufferList.get(i));
                outputBuffer.append("\n");
            }

            fileWriter.write(outputBuffer.toString());
        } catch (java.io.IOException ioe) {
        } finally {
            try {
                fileWriter.close();
            } catch (java.io.IOException ioe2) {
            }
            fileWriter = null;
            thisFile = null;
        }
    } // end writeTheFile


    /**
     * Get the properties object for the instance of this class.
     *
     * @return The properties object for the instance of this class.
     * @author Alexandre Kraft
     */
    public Properties getPropertiesObject() {
        return properties;
    } // end getPropertiesObject

    public boolean isUnmodifiedCommentingActivated() {
        return unmodifiedCommentingActivated;
    }

    /**
     * If activated, non-modified properties will be commented out when saving the modifications
     * @param unmodifiedCommentingActivated
     */
    public void setUnmodifiedCommentingActivated(boolean unmodifiedCommentingActivated) {
        this.unmodifiedCommentingActivated = unmodifiedCommentingActivated;
    }

    public String getAdditionalPropertiesMessage() {
        return additionalPropertiesMessage;
    }

    /**
     * The message to use in a comment before the list of properties that were not present in the
     * original properties file and that were added dynamically. Set this message to null to avoid
     * the generation of this message.
     * @param additionalPropertiesMessage
     */
    public void setAdditionalPropertiesMessage(String additionalPropertiesMessage) {
        this.additionalPropertiesMessage = additionalPropertiesMessage;
    }

    /**
     * Converts unicodes to encoded &#92;uxxxx and escapes
     * special characters with a preceding slash
     * 
     * @see Properties
     */
    private String escapeValue(String theString) {
        if (theString == null || theString.length() == 0 || !theString.contains("\\")) {
            return theString;
        }
        return StringUtils.replace(theString, "\\", "\\\\");
    }
}
