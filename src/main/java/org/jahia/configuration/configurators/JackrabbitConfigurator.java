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

import org.codehaus.plexus.util.StringUtils;
import org.jahia.configuration.logging.AbstractLogger;
import org.jdom2.*;
import org.jdom2.xpath.XPath;
import org.jdom2.input.SAXBuilder;

import java.io.File;
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
            org.jdom2.Document jdomDocument = saxBuilder.build(fileReader);
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

            write(jdomDocument, new File(destFileName));

        } catch (JDOMException jdome) {
            throw new Exception("Error while updating configuration file " + sourceConfigFile, jdome);
        }
    }

    protected void configureBinaryStorage(Element repositoryElement, Namespace namespace, Map dbProperties)
            throws JDOMException {

        boolean storeFilesInDB = Boolean.valueOf(getValue(dbProperties, "storeFilesInDB"));
        String fileDataStorePath = getValue(dbProperties, "fileDataStorePath");

        getLogger().info(
                "Configuring Jackrabbit binary storage using data store."
                        + " Store files in DB: "
                        + storeFilesInDB
                        + "."
                        + (!storeFilesInDB ? " File data store path: "
                                + (StringUtils.isNotEmpty(fileDataStorePath) ? fileDataStorePath
                                        : "${jahia.jackrabbit.datastore.path}") + "." : ""));

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
                pathParam.setAttribute("value", "${jahia.jackrabbit.datastore.path}");
            }
    }
}
