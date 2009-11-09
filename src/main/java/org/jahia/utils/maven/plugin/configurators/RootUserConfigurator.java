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
 * in Jahia's FLOSS exception. You should have recieved a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license"
 *
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Limited. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */
package org.jahia.utils.maven.plugin.configurators;

import org.jahia.utils.maven.plugin.buildautomation.JahiaPropertiesBean;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

/**
 * Repository root configurator for root user.
 *
 * @author : rincevent
 * @since : JAHIA 6.1
 *        Created : 9 juil. 2009
 */
public class RootUserConfigurator extends AbstractXMLConfigurator {

    private String rootPassword;

    public RootUserConfigurator(Map dbProperties, JahiaPropertiesBean jahiaPropertiesBean, String rootPassword) {
        super(dbProperties, jahiaPropertiesBean);
        this.rootPassword = rootPassword;
    }

    public void updateConfiguration(String sourceFileName, String destFileName) throws Exception {

        File rootFile = new File(sourceFileName);
        if (!rootFile.exists()) {
            return;
        }        

        SAXBuilder saxBuilder = new SAXBuilder();
        Document jdomDocument = saxBuilder.build(sourceFileName);
        Element beansElement = jdomDocument.getRootElement();
        Element rootNameElement = getElement(beansElement, "/content/users/*");

        if (rootNameElement != null) {
            rootNameElement.setName("root");
            Namespace jahiaNamespace = rootNameElement.getNamespace("j");
            rootNameElement.setAttribute("password", rootPassword, jahiaNamespace);
            rootNameElement.removeAttribute("root_user_properties");
        }

        Format customFormat = Format.getPrettyFormat();
        customFormat.setLineSeparator(System.getProperty("line.separator"));
        XMLOutputter xmlOutputter = new XMLOutputter(customFormat);
        xmlOutputter.output(jdomDocument, new FileWriter(destFileName));

    }
}
