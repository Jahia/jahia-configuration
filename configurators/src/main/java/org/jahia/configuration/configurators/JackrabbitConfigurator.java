/**
 *
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2012 Jahia Limited. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Limited. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */

package org.jahia.configuration.configurators;

import org.codehaus.plexus.util.StringUtils;
import org.jahia.configuration.logging.AbstractLogger;
import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.xpath.XPath;
import org.jdom.input.SAXBuilder;

import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.List;

/**
 * Configurator for Jackrabbit's repository.xml file.
 * Note that the XML configuration processing does not currently support namespaces !
 * User: islam
 * Date: 25 juin 2008
 * Time: 11:05:00
 */
public class JackrabbitConfigurator extends AbstractXMLConfigurator {

    public JackrabbitConfigurator(Map dbProperties, JahiaConfigInterface jahiaConfigInterface) {
        super(dbProperties, jahiaConfigInterface);
    }
   
    public JackrabbitConfigurator(Map dbProperties, JahiaConfigInterface jahiaConfigInterface, AbstractLogger logger) {
        super(dbProperties, jahiaConfigInterface, logger);
    }
   
    public void updateConfiguration(ConfigFile sourceConfigFile, String destFileName) throws Exception {
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            saxBuilder.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            InputStreamReader fileReader = new InputStreamReader(sourceConfigFile.getInputStream());
            org.jdom.Document jdomDocument = saxBuilder.build(fileReader);
            Element repositoryElement = jdomDocument.getRootElement();
            Namespace namespace = repositoryElement.getNamespace();

            String schema = getValue(dbProperties, "jahia.jackrabbit.schema");
            XPath databaseTypeXPath = XPath.newInstance("//Repository/DataSources/DataSource/param[@name=\"databaseType\"]");
            for (Element paramElement : (List<Element>) databaseTypeXPath.selectNodes(jdomDocument)) {
                paramElement.setAttribute("value", schema);
            }
            
            // we must first check if the cluster nodes are present so that they will be configured by the next queries.
            XPath clusterXPath = XPath.newInstance("//Cluster");
            Element clusterElement = (Element) clusterXPath.selectSingleNode(jdomDocument);
            Element journalElement;
            if (clusterElement != null) {
                journalElement = clusterElement.getChild("Journal");
                journalElement.setAttribute("class", getValue(dbProperties, "jahia.jackrabbit.journal"));
            }
            
            configureBinaryStorage(repositoryElement, namespace, dbProperties);

            setElementAttribute(repositoryElement, "/Repository/FileSystem", "class", getValue(dbProperties, "jahia.jackrabbit.filesystem"));
            setElementAttribute(repositoryElement, "//PersistenceManager", "class", getValue(dbProperties, "jahia.jackrabbit.persistence"));

            // backward compatibility for workspace level FileSystem element
            Element fs = getElement(repositoryElement, "//Workspace/FileSystem");
            if (fs != null && fs.getAttributeValue("class").equals("@FILESYSTEM_CLASS@")) {
            	fs.setAttribute("class", "org.apache.jackrabbit.core.fs.local.LocalFileSystem");
            	removeElementIfExists(repositoryElement, "//Workspace/FileSystem/param[@name=\"dataSourceName\"]");
            	removeElementIfExists(repositoryElement, "//Workspace/FileSystem/param[@name=\"schemaObjectPrefix\"]");
            	removeElementIfExists(repositoryElement, "//Workspace/FileSystem/param[@name=\"schemaCheckEnabled\"]");
            	fs.addContent(new Element("param", namespace).setAttribute("name", "path").setAttribute("value", "${wsp.home}"));
            }
            
            // backward compatibility for version level FileSystem element
            fs = (Element) XPath.newInstance("//Versioning/FileSystem").selectSingleNode(jdomDocument);
            if (fs != null && fs.getAttributeValue("class").equals("@FILESYSTEM_CLASS@")) {
            	fs.setAttribute("class", "org.apache.jackrabbit.core.fs.local.LocalFileSystem");
            	removeElementIfExists(repositoryElement, "//Versioning/FileSystem/param[@name=\"dataSourceName\"]");
            	removeElementIfExists(repositoryElement, "//Versioning/FileSystem/param[@name=\"schemaObjectPrefix\"]");
            	removeElementIfExists(repositoryElement, "//Versioning/FileSystem/param[@name=\"schemaCheckEnabled\"]");
            	fs.addContent(new Element("param", namespace).setAttribute("name", "path").setAttribute("value", "${rep.home}/version"));
            }
            
            Format customFormat = Format.getPrettyFormat();
            customFormat.setLineSeparator(System.getProperty("line.separator"));
            XMLOutputter xmlOutputter = new XMLOutputter(customFormat);
            xmlOutputter.output(jdomDocument, new FileWriter(destFileName));

        } catch (JDOMException jdome) {
            throw new Exception("Error while updating configuration file " + sourceConfigFile, jdome);
        }
    }

    @SuppressWarnings("unchecked")
    protected void configureBinaryStorage(Element repositoryElement, Namespace namespace, Map dbProperties)
            throws JDOMException {

        boolean storeFilesInDB = Boolean.valueOf(getValue(dbProperties, "storeFilesInDB"));
        boolean useDataStore = Boolean.valueOf(getValue(dbProperties, "useDataStore"));
        String fileDataStorePath = getValue(dbProperties, "fileDataStorePath");

        getLogger().info(
                "Configuring Jackrabbit binary storage. Using data store: "
                        + useDataStore
                        + ". Store files in DB: "
                        + storeFilesInDB
                        + "."
                        + (useDataStore && !storeFilesInDB ? " File data store path: "
                                + (StringUtils.isNotEmpty(fileDataStorePath) ? fileDataStorePath
                                        : "${rep.home}/datastore") + "." : ""));

        if (useDataStore) {
            // data store will be used

            // remove externalBLOBs attributes
            removeAllElements(repositoryElement, "//param[@name=\"externalBLOBs\"]");

            if (storeFilesInDB) {
                // We will use the DB-based data store

                // remove the FileDataStore if present
                removeAllElements(repositoryElement,
                        "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.FileDataStore\"]");

                // get the DbDataStore element
                Element store = getElement(repositoryElement,
                        "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.db.DbDataStore\"]");
                if (store == null) {
                    // DbDataStore element not found -> create it
                    store = new Element("DataStore", namespace);
                    store.setAttribute("class", "org.apache.jackrabbit.core.data.db.DbDataStore");
                    store.addContent(new Element("param").setAttribute("name", "dataSourceName")
                            .setAttribute("value", "jahiaDS"));
                    store.addContent(new Element("param").setAttribute("name", "schemaObjectPrefix")
                            .setAttribute("value", "JR_"));
                    store.addContent(new Element("param").setAttribute("name", "schemaCheckEnabled")
                            .setAttribute("value", "false"));
                    store.addContent(new Element("param").setAttribute("name", "copyWhenReading")
                            .setAttribute("value", "true"));
                    store.addContent(new Element("param").setAttribute("name", "minRecordLength")
                            .setAttribute("value", "1024"));
                    repositoryElement.addContent(store);
                }
            } else {
                // We will use the filesystem-based data store

                // remove the DbDataStore if present
                removeAllElements(repositoryElement,
                        "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.db.DbDataStore\"]");

                // get the FileDataStore element
                Element store = getElement(repositoryElement,
                        "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.FileDataStore\"]");
                Element pathParam = null;
                if (store == null) {
                    // FileDataStore element not found -> create it
                    store = new Element("DataStore", namespace);
                    store.setAttribute("class", "org.apache.jackrabbit.core.data.FileDataStore");
                    store.addContent(new Element("param").setAttribute("name", "minRecordLength")
                            .setAttribute("value", "1024"));
                    pathParam = new Element("param").setAttribute("name", "path").setAttribute("value", "");
                    store.addContent(pathParam);
                    repositoryElement.addContent(store);
                } else {
                    pathParam = getElement(store, "//DataStore/param[@name=\"path\"]");
                }
                if (fileDataStorePath != null && fileDataStorePath.length() > 0) {
                    pathParam.setAttribute("value", fileDataStorePath);
                }
                if (StringUtils.isEmpty(pathParam.getAttributeValue("value"))) {
                    pathParam.setAttribute("value", "${rep.home}/datastore");
                }
            }
        } else {
            // we will use persistence manager store (no data store)

            // remove the FileDataStore if present
            removeAllElements(repositoryElement,
                    "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.FileDataStore\"]");
            // remove the DbDataStore if present
            removeAllElements(repositoryElement,
                    "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.db.DbDataStore\"]");

            // set externalBLOBs to true if the files should not be stored in the DB
            for (Element paramElement : (List<Element>) XPath.selectNodes(repositoryElement,
                    "//param[@name=\"externalBLOBs\"]")) {
                paramElement.setAttribute("value", storeFilesInDB ? "false" : "true");
            }
        }
    }
}
