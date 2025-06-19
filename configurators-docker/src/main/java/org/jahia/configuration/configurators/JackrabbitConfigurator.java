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

import org.apache.commons.lang3.StringUtils;
import org.jdom2.*;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Configurator for Jackrabbit's repository.xml file.
 * Note that the XML configuration processing does not currently support namespaces !
 * User: islam
 * Date: 25 juin 2008
 * Time: 11:05:00
 */
public class JackrabbitConfigurator extends AbstractXMLConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(JackrabbitConfigurator.class);

    public JackrabbitConfigurator(Map dbProperties, JahiaConfigInterface jahiaConfigInterface) {
        super(dbProperties, jahiaConfigInterface);
    }

    public void updateConfiguration(InputStream inputStream, String destFileName) throws Exception {
        logger.info("Updating Jackrabbit configuration in {}", destFileName);
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            saxBuilder.setFeature("http://apache.org/xml/features/disallow-doctype-decl",false);
            saxBuilder.setFeature("http://xml.org/sax/features/external-general-entities", true);
            saxBuilder.setFeature("http://xml.org/sax/features/external-parameter-entities", true);
            saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            saxBuilder.setExpandEntities(false);

            logger.info("Parsing repository configuration XML");
            InputStreamReader fileReader = new InputStreamReader(inputStream);
            org.jdom2.Document jdomDocument = saxBuilder.build(fileReader);
            Element repositoryElement = jdomDocument.getRootElement();
            Namespace namespace = repositoryElement.getNamespace();

            String schema = getValue(dbProperties, "jahia.jackrabbit.schema");
            logger.info("Setting database schema type to: {}", schema);
            XPathFactory xPathFactory = XPathFactory.instance();
            XPathExpression<Element> databaseTypeXPath = xPathFactory.compile("//Repository/DataSources/DataSource/param[@name=\"databaseType\"]", Filters.element());
            for (Element paramElement : databaseTypeXPath.evaluate(jdomDocument)) {
                logger.info("Setting databaseType param value to: {}", schema);
                paramElement.setAttribute("value", schema);
            }

            // we must first check if the cluster nodes are present so that they will be configured by the next queries.
            logger.info("Checking for clustering configuration");
            XPathExpression<Element> clusterXPath = xPathFactory.compile("//Cluster",Filters.element());
            Element clusterElement = (Element) clusterXPath.evaluateFirst(jdomDocument);
            Element journalElement;
            if (clusterElement != null) {
                logger.info("Found cluster configuration, setting journal class");
                journalElement = clusterElement.getChild("Journal");
                String journalClass = getValue(dbProperties, "jahia.jackrabbit.journal");
                logger.info("Setting Journal class to: {}", journalClass);
                journalElement.setAttribute("class", journalClass);
            } else {
                logger.info("No cluster configuration found");
            }

            logger.info("Configuring binary storage");
            configureBinaryStorage(repositoryElement, namespace, dbProperties);

            logger.info("Setting FileSystem class to: {}", getValue(dbProperties, "jahia.jackrabbit.filesystem"));
            setElementAttribute(repositoryElement, "/Repository/FileSystem", "class", getValue(dbProperties, "jahia.jackrabbit.filesystem"));

            logger.info("Setting PersistenceManager class to: {}", getValue(dbProperties, "jahia.jackrabbit.persistence"));
            setElementAttribute(repositoryElement, "//PersistenceManager", "class", getValue(dbProperties, "jahia.jackrabbit.persistence"));

            logger.info("Writing updated configuration to: {}", destFileName);
            write(jdomDocument, new File(destFileName));
            logger.info("Jackrabbit configuration successfully updated");

        } catch (JDOMException jdome) {
            logger.error("Error updating Jackrabbit configuration in {}: {}", destFileName, jdome.getMessage(), jdome);
            throw new Exception("Error while updating configuration file " + destFileName, jdome);
        }
    }

    protected void configureBinaryStorage(Element repositoryElement, Namespace namespace, Map dbProperties)
            throws JDOMException {

        boolean storeFilesInDB = Boolean.valueOf(getValue(dbProperties, "storeFilesInDB"));
        boolean storeFilesInAWS = Boolean.valueOf(getValue(dbProperties, "storeFilesInAWS"));
        String fileDataStorePath = getValue(dbProperties, "fileDataStorePath");

        logger.info(
                "Configuring Jackrabbit binary storage: storeFilesInDB={}, storeFilesInAWS={}, fileDataStorePath={}",
                storeFilesInDB,
                storeFilesInAWS,
                StringUtils.isNotEmpty(fileDataStorePath) ? fileDataStorePath : "${jahia.jackrabbit.datastore.path}");

        if (storeFilesInAWS) {
            // We will use the AWS S3 data store
            logger.info("Configuring AWS S3 data store");
            logger.info("Removing any existing FileDataStore or DbDataStore elements");
            removeAllElements(repositoryElement,
                    "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.FileDataStore\"]");
            removeAllElements(repositoryElement,
                    "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.db.DbDataStore\"]");

        } else if (storeFilesInDB) {
            // We will use the DB-based data store
            logger.info("Configuring DB-based data store");

            // remove the FileDataStore if present
            logger.info("Removing any existing FileDataStore or S3DataStore elements");
            removeAllElements(repositoryElement,
                    "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.FileDataStore\"]");
            removeAllElements(repositoryElement,
                    "//Repository/DataStore[@class=\"org.jahia.services.content.impl.jackrabbit.S3DataStore\"]");

            // get the DbDataStore element
            Element store = getElement(repositoryElement,
                    "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.db.DbDataStore\"]");
            if (store == null) {
                // DbDataStore element not found -> create it
                logger.info("Creating new DbDataStore element");
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
                logger.info("Added DbDataStore element with default configuration");
            } else {
                logger.info("DbDataStore element already exists, keeping existing configuration");
            }
        } else {
            // We will use the filesystem-based data store
            logger.info("Configuring filesystem-based data store");

            // remove the DbDataStore if present
            logger.info("Removing any existing DbDataStore or S3DataStore elements");
            removeAllElements(repositoryElement,
                    "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.db.DbDataStore\"]");
            removeAllElements(repositoryElement,
                    "//Repository/DataStore[@class=\"org.jahia.services.content.impl.jackrabbit.S3DataStore\"]");

            // get the FileDataStore element
            Element store = getElement(repositoryElement,
                    "//Repository/DataStore[@class=\"org.apache.jackrabbit.core.data.FileDataStore\"]");
            Element pathParam = null;
            if (store == null) {
                // FileDataStore element not found -> create it
                logger.info("Creating new FileDataStore element");
                store = new Element("DataStore", namespace);
                store.setAttribute("class", "org.apache.jackrabbit.core.data.FileDataStore");
                store.addContent(new Element("param").setAttribute("name", "minRecordLength")
                        .setAttribute("value", "1024"));
                pathParam = new Element("param").setAttribute("name", "path").setAttribute("value", "");
                store.addContent(pathParam);
                repositoryElement.addContent(store);
                logger.info("Added FileDataStore element with default configuration");
            } else {
                logger.info("FileDataStore element already exists, updating path parameter");
                pathParam = getElement(store, "//DataStore/param[@name=\"path\"]");
            }
            pathParam.setAttribute("value", "${jahia.jackrabbit.datastore.path}");
            logger.info("Set FileDataStore path to: ${jahia.jackrabbit.datastore.path}");
        }
        logger.info("Binary storage configuration completed");
    }
}
