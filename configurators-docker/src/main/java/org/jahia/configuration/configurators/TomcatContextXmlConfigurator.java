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

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Configurator for Tomcat's context.xml descriptor
 * Date: 16 juil. 2008
 * Time: 15:42:41
 */
public class TomcatContextXmlConfigurator extends AbstractXMLConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(TomcatContextXmlConfigurator.class);

    public TomcatContextXmlConfigurator(Map dbProperties, JahiaConfigInterface jahiaConfigInterface) {
        super(dbProperties, jahiaConfigInterface);
    }

    public void updateConfiguration(InputStream inputStream, String destFileName) throws Exception {
        logger.info("Updating Tomcat context.xml ...");
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            InputStreamReader fileReader = new InputStreamReader(inputStream);
            org.jdom2.Document jdomDocument = saxBuilder.build(fileReader);
            Element root = jdomDocument.getRootElement();

            setElementAttribute(root, "/Context/Resource", "password", getValue(dbProperties, "jahia.database.pass"));

            Element resource = (Element)XPath.newInstance("/Context/Resource").selectSingleNode(root);
            if (resource.getAttributeValue("username") != null) {
                resource.setAttribute("username", getValue(dbProperties, "jahia.database.user"));
            }
            if (resource.getAttributeValue("user") != null) {
                resource.setAttribute("user", getValue(dbProperties, "jahia.database.user"));
            }
            if (resource.getAttributeValue("url") != null) {
                resource.setAttribute("url", getValue(dbProperties, "jahia.database.url"));
            }
            if (resource.getAttributeValue("jdbcUrl") != null) {
                resource.setAttribute("jdbcUrl", getValue(dbProperties, "jahia.database.url"));
            }
            if (resource.getAttributeValue("driverClassName") != null) {
                resource.setAttribute("driverClassName", getValue(dbProperties, "jahia.database.driver"));
            }
            if (resource.getAttributeValue("driverClass") != null) {
                resource.setAttribute("driverClass", getValue(dbProperties, "jahia.database.driver"));
            }
            if (resource.getAttributeValue("validationQuery") != null) {
                resource.setAttribute("validationQuery", getValue(dbProperties, "jahia.database.validationQuery"));
            }

            write(jdomDocument, new File(destFileName));
            logger.info("Successfully updated Tomcat context.xml in {}", destFileName);

        } catch (JDOMException jdome) {
            logger.error("Error while updating configuration file " + destFileName, jdome);
            throw new Exception("Error while updating configuration file " + destFileName, jdome);
        }
    }
}

