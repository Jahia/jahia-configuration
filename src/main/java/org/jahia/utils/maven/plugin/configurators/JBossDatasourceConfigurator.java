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

package org.jahia.utils.maven.plugin.configurators;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;

import org.jahia.utils.maven.plugin.buildautomation.JahiaPropertiesBean;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * JBoss DB datasource configurator.
 * 
 * @author Sergiy Shyrkov
 */
public class JBossDatasourceConfigurator extends AbstractXMLConfigurator {

    public JBossDatasourceConfigurator(Map dbProperties,
            JahiaPropertiesBean jahiaPropertiesBean) {
        super(dbProperties, jahiaPropertiesBean);
    }

    public void updateConfiguration(String sourceFileName, String destFileName)
            throws Exception {
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            FileReader fileReader = new FileReader(sourceFileName);
            org.jdom.Document jdomDocument = saxBuilder.build(fileReader);
            Element datasource = jdomDocument.getRootElement().getChild(
                    "no-tx-datasource");

            datasource.getChild("connection-url").setText(
                    getValue(dbProperties, "jahia.database.url"));
            datasource.getChild("driver-class").setText(
                    getValue(dbProperties, "jahia.database.driver"));
            datasource.getChild("user-name").setText(
                    getValue(dbProperties, "jahia.database.user"));
            datasource.getChild("password").setText(
                    getValue(dbProperties, "jahia.database.pass"));
            datasource.getChild("metadata").getChild("type-mapping")
                    .setText(
                            getValue(dbProperties,
                                    "jahia.jboss.datasource.typeMapping"));

            Format customFormat = Format.getPrettyFormat();
            customFormat.setLineSeparator(System.getProperty("line.separator"));
            XMLOutputter xmlOutputter = new XMLOutputter(customFormat);
            xmlOutputter.output(jdomDocument, new FileWriter(destFileName));

        } catch (JDOMException jdome) {
            throw new Exception("Error while updating configuration file "
                    + sourceFileName, jdome);
        }

    }

}
