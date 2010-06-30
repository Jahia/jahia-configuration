/**
 *
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2009 Jahia Limited. All rights reserved.
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

import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.xpath.XPath;
import org.jdom.input.SAXBuilder;

import java.io.FileWriter;
import java.io.FileReader;
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
   
    public void updateConfiguration(String sourceFileName, String destFileName) throws Exception {
        try {
            boolean directConnectionToDB = false;
            if ("was".equals(jahiaConfigInterface.getTargetServerType())) {
                directConnectionToDB = true;
            }

            SAXBuilder saxBuilder = new SAXBuilder();
            saxBuilder.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            FileReader fileReader = new FileReader(sourceFileName);
            org.jdom.Document jdomDocument = saxBuilder.build(fileReader);
            Element webAppElement = jdomDocument.getRootElement();
            String namespaceURI = webAppElement.getNamespaceURI();
            Namespace namespace = webAppElement.getNamespace();

            // we must first check if the cluster nodes are present so that they will be configured by the next queries.
            XPath clusterXPath = XPath.newInstance("//Cluster");
            Element clusterElement = (Element) clusterXPath.selectSingleNode(jdomDocument);
            Element journalElement;
            if (clusterElement != null) {
                journalElement = clusterElement.getChild("Journal");
                clusterElement.setAttribute("id", jahiaConfigInterface.getCluster_node_serverId());
                journalElement.setAttribute("class", getValue(dbProperties, "jahia.jackrabbit.journal"));
//            } else {
//                clusterElement.getParent().removeContent(clusterElement);
//            }
            }
            setupDatabaseConnection(jdomDocument, "/Repository/FileSystem", dbProperties, directConnectionToDB, namespace);
            setupDatabaseConnection(jdomDocument, "/Repository/Workspace/FileSystem", dbProperties, directConnectionToDB, namespace);
            setupDatabaseConnection(jdomDocument, "/Repository/Workspace/PersistenceManager", dbProperties, directConnectionToDB, namespace);
            setupDatabaseConnection(jdomDocument, "/Repository/Versioning/FileSystem", dbProperties, directConnectionToDB, namespace);
            setupDatabaseConnection(jdomDocument, "/Repository/Versioning/PersistenceManager", dbProperties, directConnectionToDB, namespace);
            setupDatabaseConnection(jdomDocument, "/Repository/Cluster/Journal", dbProperties, directConnectionToDB, namespace);

            String storeFilesInDB = getValue(dbProperties, "storeFilesInDB");
            String externalBLOBsValue = "true";
            if (Boolean.valueOf(storeFilesInDB)) {
                externalBLOBsValue = "false";
            }
            XPath externalBlobsXPath = XPath.newInstance("//param[@name=\"externalBLOBs\"]");
            List<Element> externalBlobsList = (List<Element>) externalBlobsXPath.selectNodes(jdomDocument);
            for (Element paramElement : externalBlobsList) {
                paramElement.setAttribute("value", externalBLOBsValue);
            }

            setElementAttribute(webAppElement, "//FileSystem", "class", getValue(dbProperties, "jahia.jackrabbit.filesystem"));
            setElementAttribute(webAppElement, "//PersistenceManager", "class", getValue(dbProperties, "jahia.jackrabbit.persistence"));

            Format customFormat = Format.getPrettyFormat();
            customFormat.setLineSeparator(System.getProperty("line.separator"));
            XMLOutputter xmlOutputter = new XMLOutputter(customFormat);
            xmlOutputter.output(jdomDocument, new FileWriter(destFileName));

        } catch (JDOMException jdome) {
            throw new Exception("Error while updating configuration file " + sourceFileName, jdome);
        }
    }

    private void setupDatabaseConnection(Document jdomDocument, String xPathExpression, Map values, boolean directConnectionToDB, Namespace namespace) throws JDOMException {
        XPath xPath = XPath.newInstance(xPathExpression);
        Element element = (Element) (xPath.selectSingleNode(jdomDocument));
        if (element != null) {
            Element userElement = getElement(element, "param[@name=\"user\"]");
            Element passwordElement = getElement(element, "param[@name=\"password\"]");
            if (directConnectionToDB) {
                setElementAttribute(element, "param[@name=\"driver\"]", "value", getValue(values, "jahia.database.driver"));
                setElementAttribute(element, "param[@name=\"schema\"]", "value", getValue(values, "jahia.jackrabbit.schema"));
                setElementAttribute(element, "param[@name=\"url\"]", "value", getValue(values, "jahia.database.url"));
                if (userElement == null) {
                    userElement = new Element("param", namespace).setAttribute("name", "user").setAttribute("value", getValue(values, "jahia.database.user"));
                    element.addContent(userElement);
                } else {
                    setElementAttribute(element, "param[@name=\"user\"]", "value", getValue(values, "jahia.database.user"));
                }
                if (passwordElement == null) {
                    passwordElement = new Element("param", namespace).setAttribute("name", "password").setAttribute("value", getValue(values, "jahia.database.pass"));
                    element.addContent(passwordElement);
                } else {
                    setElementAttribute(element, "param[@name=\"password\"]", "value", getValue(values, "jahia.database.pass"));
                }
            } else {
                // no direct connection to database, let's check if we have the user and password elements present,
                // remove them, and then perform database configuration the usual way.
                if (userElement != null) {
                    userElement.getParent().removeContent(userElement);
                }
                if (passwordElement != null) {
                    passwordElement.getParent().removeContent(passwordElement);
                }
                setElementAttribute(element, "param[@name=\"driver\"]", "value", "javax.naming.InitialContext");
                setElementAttribute(element, "param[@name=\"schema\"]", "value", getValue(values, "jahia.jackrabbit.schema"));
                setElementAttribute(element, "param[@name=\"url\"]", "value", "java:comp/env/jdbc/jahia");
            }
        }
    }

}
