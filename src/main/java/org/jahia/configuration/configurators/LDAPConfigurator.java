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

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.Map;

/**
 * LDAP configurator that generate a module WAR configured to the ldap properties
 * User: loom
 * Date: 19.04.11
 * Time: 15:54
 */
public class LDAPConfigurator extends AbstractXMLConfigurator {

    public LDAPConfigurator(Map dbProps, JahiaConfigInterface jahiaConfigInterface) {
        super(dbProps, jahiaConfigInterface);
    }

    @Override
    public void updateConfiguration(String sourceFileName, String destFileName) throws Exception {

        Map<String, String> groupProps = jahiaConfigInterface.getGroupLdapProviderProperties();
        Map<String, String> userProps = jahiaConfigInterface.getUserLdapProviderProperties();
        if (!Boolean.valueOf(jahiaConfigInterface.getLdapActivated())
                || (groupProps == null || groupProps.isEmpty())
                && (userProps == null || userProps.isEmpty())) {
            // no configuration provided
            return;
        }

        File tempDirectory = FileUtils.getTempDirectory();
        File ldapConfigDirectory = new File(tempDirectory, "ldap-config");
        ldapConfigDirectory.mkdir();
        File metainfDir = new File(ldapConfigDirectory, "META-INF");
        metainfDir.mkdir();
        File springDir = new File(metainfDir, "spring");
        springDir.mkdir();

        SAXBuilder saxBuilder = new SAXBuilder();
        InputStream skeletonContextStream = this.getClass().getClassLoader().getResourceAsStream("ldap/META-INF/spring/applicationcontext-ldap-config.xml");
        Document jdomDocument = saxBuilder.build(skeletonContextStream);
        Element rootElement = jdomDocument.getRootElement();
        Namespace ns = rootElement.getNamespace();

        // configure ldap file
        configure("JahiaUserManagerLDAPProvider", userProps, rootElement);
        configure("JahiaGroupManagerLDAPProvider", groupProps, rootElement);

        Format customFormat = Format.getPrettyFormat();
        customFormat.setLineSeparator(System.getProperty("line.separator"));
        XMLOutputter xmlOutputter = new XMLOutputter(customFormat);
        File destFile = new File(springDir, "applicationcontext-ldap-config.xml");
        FileWriter fw = new FileWriter(destFile);
        xmlOutputter.output(jdomDocument, fw);
        fw.close();

        // copy MANIFEST file.
        InputStream manifestStream = this.getClass().getClassLoader().getResourceAsStream("ldap/META-INF/MANIFEST.MF");
        File targetManifestFile = new File(metainfDir, "MANIFEST.MF");
        FileUtils.copyInputStreamToFile(manifestStream, targetManifestFile);

        boolean verbose = true;
        JarArchiver archiver = new JarArchiver();
        if (verbose) {
            archiver.enableLogging(new ConsoleLogger(Logger.LEVEL_DEBUG, "console"));
        }

        // let's generate the WAR file
        File targetFile = new File(tempDirectory, "ldap-config.war");
        archiver.setManifest(targetManifestFile);
        archiver.setDestFile(targetFile);
        String excludes = null;

        archiver.addDirectory(ldapConfigDirectory, null,
                excludes != null ? excludes.split(",") : null);
        archiver.createArchive();

        // copy the WAR file to the deployment directory
        FileUtils.copyFileToDirectory(targetFile, new File(destFileName));

        FileUtils.deleteDirectory(ldapConfigDirectory);
        targetFile.delete();

    }

    private void configure(String provider, Map<String, String> props,
            Element root) throws JDOMException {
        Namespace ns = root.getNamespace();
        if (props != null && !props.isEmpty()) {
            // setup user LDAP
            Element map = getElement(root, "/xp:beans/xp:bean[@parent=\""
                    + provider
                    + "\"]/xp:property[@name=\"ldapProperties\"]/xp:map");
            if (map != null) {
                for (Map.Entry<String, String> prop : props.entrySet()) {
                    map.addContent(new Element("entry", ns).setAttribute("key",
                            prop.getKey()).setAttribute("value",
                            prop.getValue()));
                }
            }
        } else {
            removeElementIfExists(root, "/xp:beans/xp:bean[@parent=\""
                    + provider + "\"]");
        }
    }
}
