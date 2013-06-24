/**
 *
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2012 Jahia Limited. All rights reserved.
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
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.PropertyUtils;
import org.jdom.Attribute;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.*;
import java.util.*;

/**
 * LDAP configurator that generate a module WAR configured to the ldap properties
 * User: loom
 * Date: 19.04.11
 * Time: 15:54
 */
public class LDAPConfigurator extends AbstractConfigurator {

    public LDAPConfigurator(Map dbProps, JahiaConfigInterface jahiaConfigInterface) {
        super(dbProps, jahiaConfigInterface);
    }

    @Override
    public void updateConfiguration(ConfigFile sourceConfigFile, String destFileName) throws Exception {

        Map<String, String> groupProps = jahiaConfigInterface.getGroupLdapProviderProperties();
        Map<String, String> userProps = jahiaConfigInterface.getUserLdapProviderProperties();
        if (!Boolean.valueOf(jahiaConfigInterface.getLdapActivated())
                || (groupProps == null || groupProps.isEmpty())
                && (userProps == null || userProps.isEmpty())) {
            // no configuration provided
            return;
        }

        Properties ldapProperties = new SortedProperties();
        InputStream skeletonStream = this.getClass().getClassLoader().getResourceAsStream("ldap/org.jahia.services.usermanager.ldap-config.cfg");
        ldapProperties.load(skeletonStream);
        for (String key : userProps.keySet()) {
            ldapProperties.setProperty("user." + key, userProps.get(key));
        }
        for (String key : groupProps.keySet()) {
            ldapProperties.setProperty("group." + key, groupProps.get(key));
        }
        File destFile = new File(destFileName);
        if (!destFile.exists()) {
            destFile.mkdir();
        }
        ldapProperties.store(new FileOutputStream(new File(destFile, "org.jahia.services.usermanager.ldap-config.cfg")), null);
    }

    public class SortedProperties extends Properties {

        public SortedProperties() {
        }

        public SortedProperties(Properties defaults) {
            super(defaults);
        }

        private final TreeSet<Object> keys = new TreeSet<Object>();

        public Set<Object> keySet() { return keys; }

        public Enumeration<Object> keys() {
            return Collections.<Object> enumeration(keys);
        }

        public Object put(Object key, Object value) {
            keys.add(key);
            return super.put(key, value);
        }
    }
}
