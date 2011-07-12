/**
 *
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2011 Jahia Limited. All rights reserved.
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
package org.jahia.configuration.configurators;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

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
    private JahiaConfigInterface cfg;

    public RootUserConfigurator(Map dbProperties, JahiaConfigInterface jahiaConfigInterface, String rootPassword) {
        super(dbProperties, jahiaConfigInterface);
        this.rootPassword = rootPassword;
        cfg = jahiaConfigInterface;
    }

    public void updateConfiguration(ConfigFile sourceConfigFile, String destFileName) throws Exception {

        if (sourceConfigFile == null) {
            return;
        }        

        SAXBuilder saxBuilder = new SAXBuilder();
        Document jdomDocument = saxBuilder.build(sourceConfigFile.getInputStream());
        Element beansElement = jdomDocument.getRootElement();
        Element rootNameElement = getElement(beansElement, "/content/users/ROOT_NAME_PLACEHOLDER");
        rootNameElement = rootNameElement != null ? rootNameElement : getElement(beansElement, "/content/users/root");

        if (rootNameElement != null) {
            rootNameElement.setName(cfg.getJahiaRootUsername());
            Namespace jahiaNamespace = rootNameElement.getNamespace("j");
            Element grantElement = rootNameElement.getChild("files")!=null?
                    rootNameElement.getChild("files").getChild("private")!=null?
                    rootNameElement.getChild("files").getChild("private").getChild("acl",jahiaNamespace)!=null?
                    rootNameElement.getChild("files").getChild("private").getChild("acl",jahiaNamespace).getChild("GRANT_u_root")
                    :null:null:null ;
            if (grantElement != null) {
                grantElement.setName("GRANT_u_" + cfg.getJahiaRootUsername());
                grantElement.setAttribute("principal","u:" + cfg.getJahiaRootUsername(),jahiaNamespace);
            }
            rootNameElement.setAttribute("password", rootPassword, jahiaNamespace);
            if (cfg.getJahiaRootFirstname() != null && cfg.getJahiaRootFirstname().length() > 0) {
                rootNameElement.setAttribute("firstName", cfg.getJahiaRootFirstname(), jahiaNamespace);
            }
            if (cfg.getJahiaRootLastname() != null && cfg.getJahiaRootLastname().length() > 0) {
                rootNameElement.setAttribute("lastName", cfg.getJahiaRootLastname(), jahiaNamespace);
            }
            if (cfg.getJahiaRootEmail() != null && cfg.getJahiaRootEmail().length() > 0) {
                rootNameElement.setAttribute("email", cfg.getJahiaRootEmail(), jahiaNamespace);
            }
        }

        Format customFormat = Format.getPrettyFormat();
        customFormat.setLineSeparator(System.getProperty("line.separator"));
        XMLOutputter xmlOutputter = new XMLOutputter(customFormat);
        xmlOutputter.output(jdomDocument, new FileWriter(destFileName));

    }
}
