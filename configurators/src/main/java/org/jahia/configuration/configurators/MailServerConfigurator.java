/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2012 Jahia Solutions Group SA. All rights reserved.
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
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */
package org.jahia.configuration.configurators;

import java.io.FileWriter;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Repository root configurator for root user.
 * 
 * @author Sergiy Shyrkov
 */
public class MailServerConfigurator extends AbstractXMLConfigurator {

    private JahiaConfigInterface cfg;

    public MailServerConfigurator(@SuppressWarnings("rawtypes") Map dbProperties, JahiaConfigInterface jahiaConfigInterface) {
        super(dbProperties, jahiaConfigInterface);
        cfg = jahiaConfigInterface;
    }

    public void updateConfiguration(ConfigFile sourceConfigFile, String destFileName) throws Exception {

        if (sourceConfigFile == null) {
            return;
        }

        SAXBuilder saxBuilder = new SAXBuilder();
        Document jdomDocument = saxBuilder.build(sourceConfigFile.getInputStream());
        Element el = getElement(jdomDocument.getRootElement(), "/content/settings/mail-server");

        if (el != null && cfg.getMailServer() != null && cfg.getMailServer().length() > 0) {
            Namespace jahiaNamespace = el.getNamespace("j");
            el.setAttribute("activated", "true", jahiaNamespace);
            el.setAttribute("uri", cfg.getMailServer(), jahiaNamespace);
            if (cfg.getMailFrom() != null) {
                el.setAttribute("from", cfg.getMailFrom(), jahiaNamespace);
            }
            if (cfg.getMailAdministrator() != null) {
                el.setAttribute("to", cfg.getMailAdministrator(), jahiaNamespace);
            }
            if (cfg.getMailParanoia() != null) {
                el.setAttribute("notificationLevel", cfg.getMailParanoia(), jahiaNamespace);
            }
        }

        Format customFormat = Format.getPrettyFormat();
        customFormat.setLineSeparator(System.getProperty("line.separator"));
        XMLOutputter xmlOutputter = new XMLOutputter(customFormat);
        FileWriter out = new FileWriter(destFileName);
        try {
            xmlOutputter.output(jdomDocument, out);
        } finally {
            out.close();
        }

    }
}
