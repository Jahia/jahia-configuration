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

import org.jahia.utils.maven.plugin.buildautomation.JahiaPropertiesBean;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

/**
 * Hibernate configurator to setup the proper database. No longer needed in Jahia 6.5 and above.
 * User: islam
 * Date: 25 juin 2008
 * Time: 11:03:29
 */
public class SpringHibernateConfigurator extends AbstractXMLConfigurator {

    public SpringHibernateConfigurator(Map dbProperties, JahiaPropertiesBean jahiaPropertiesBean) {
        super(dbProperties, jahiaPropertiesBean);
    }

    public void updateConfiguration(String sourceFileName, String destFileName) throws Exception {

        File sourceConfigFile = new File(sourceFileName);
        if (!sourceConfigFile.exists()) {
            return;
        }

        SAXBuilder saxBuilder = new SAXBuilder();
        Document jdomDocument = saxBuilder.build(sourceFileName);
        Element beansElement = jdomDocument.getRootElement();
        Element hibernatePropertiesElement = getElement(beansElement, "/xp:beans/xp:bean[@id=\"sessionFactory\"]/xp:property[@name=\"hibernateProperties\"]/xp:props/xp:prop[@key=\"hibernate.dialect\"]");
        // we need to test if the file is already using a Spring marker, in which case we don't set a value...
        if ((hibernatePropertiesElement != null) && (hibernatePropertiesElement.getText().indexOf("${hibernate.dialect}") < 0)) {
            hibernatePropertiesElement.setText(getValue(dbProperties, "jahia.database.hibernate.dialect"));
        }

        Element nestedTransactionAllowedElement = getElement(beansElement, "/xp:beans/xp:bean[@id=\"transactionManager\"]/xp:property[@name=\"nestedTransactionAllowed\"]");
        // we need to test if the file is already using a Spring marker, in which case we don't set a value...
        if ((nestedTransactionAllowedElement != null) && (nestedTransactionAllowedElement.getAttributeValue("value").indexOf("${nested.transaction.allowed}") < 0)) {
            String transactionIsolationLevel = getValue(dbProperties, "jahia.nested_transaction_allowed");
            if ("".equals(transactionIsolationLevel)) {
                transactionIsolationLevel = "false";
            }            
            nestedTransactionAllowedElement.setAttribute("value", transactionIsolationLevel);
        }

        Format customFormat = Format.getPrettyFormat();
        customFormat.setLineSeparator(System.getProperty("line.separator"));
        XMLOutputter xmlOutputter = new XMLOutputter(customFormat);
        xmlOutputter.output(jdomDocument, new FileWriter(destFileName));

    }
}
