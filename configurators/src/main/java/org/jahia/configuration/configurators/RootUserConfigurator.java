/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
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
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
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
            if (cfg.getJahiaRootPreferredLang() != null && cfg.getJahiaRootPreferredLang().length() > 0) {
                rootNameElement.setAttribute("preferredLanguage", cfg.getJahiaRootPreferredLang());
            }
        }

        Format customFormat = Format.getPrettyFormat();
        customFormat.setLineSeparator(System.getProperty("line.separator"));
        XMLOutputter xmlOutputter = new XMLOutputter(customFormat);
        xmlOutputter.output(jdomDocument, new FileWriter(destFileName));

    }
}
