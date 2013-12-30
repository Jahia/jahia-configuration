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

import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.Format.TextMode;
import org.jdom.output.XMLOutputter;

/**
 * JBoss DB datasource configurator.
 * 
 * @author Sergiy Shyrkov
 */
public class JBossConfigurator extends AbstractXMLConfigurator {

    private static final Namespace DS_NS = Namespace.getNamespace("urn:jboss:domain:datasources:1.1");

    private static final Map<String, String> EXCEPTION_SORTERS;

    private static final Namespace WEB_NS = Namespace.getNamespace("urn:jboss:domain:web:1.5");

    static {
        EXCEPTION_SORTERS = new HashMap<String, String>(6);

        EXCEPTION_SORTERS.put("derby", "org.jboss.jca.adapters.jdbc.extensions.novendor.NullExceptionSorter");
        EXCEPTION_SORTERS.put("derby_embedded", "org.jboss.jca.adapters.jdbc.extensions.novendor.NullExceptionSorter");
        EXCEPTION_SORTERS.put("mssql", "org.jboss.jca.adapters.jdbc.extensions.mssql.MSSQLValidConnectionChecker");
        EXCEPTION_SORTERS.put("mysql", "org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLExceptionSorter");
        EXCEPTION_SORTERS.put("oracle", " org.jboss.jca.adapters.jdbc.extensions.oracle.OracleExceptionSorter ");
        EXCEPTION_SORTERS
                .put("postgresql", "org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter");

    }

    private String dbType;

    public JBossConfigurator(Map<?, ?> dbProperties, JahiaConfigInterface jahiaConfigInterface) {
        super(dbProperties, jahiaConfigInterface);
        dbType = jahiaConfigInterface.getDatabaseType();
    }

    private void configureDatasource(Element datasources) throws JDOMException {
        Element ds = getElement(datasources, "xp:datasource[@jndi-name=\"java:/jahiaDS\"]", DS_NS.getURI());
        if (ds == null) {
            ds = new Element("datasource", DS_NS).setAttribute("jndi-name", "java:/jahiaDS")
                    .setAttribute("pool-name", "jahiaDS").setAttribute("enabled", "true")
                    .setAttribute("use-java-context", "true");
            datasources.addContent(0, ds);
        }
        getChildCreate(ds, "connection-url").setText(getValue(dbProperties, "jahia.database.url"));
        getChildCreate(ds, "driver").setText("jahia." + dbType);
        Element pool = getChildCreate(ds, "pool");
        getChildCreate(pool, "min-pool-size").setText("10");
        getChildCreate(pool, "max-pool-size").setText("200");

        Element security = getChildCreate(ds, "security");
        getChildCreate(security, "user-name").setText(getValue(dbProperties, "jahia.database.user"));
        getChildCreate(security, "password").setText(getValue(dbProperties, "jahia.database.pass"));

        Element validation = getChildCreate(ds, "validation");
        getChildCreate(validation, "valid-connection-checker").setAttribute("class-name",
                "org.jboss.jca.adapters.jdbc.extensions.novendor.JDBC4ValidConnectionChecker");
        getChildCreate(validation, "validate-on-match").setText("false");
        getChildCreate(validation, "background-validation").setText("true");
        getChildCreate(validation, "background-validation-millis").setText("600000");
        getChildCreate(validation, "exception-sorter").setAttribute("class-name", EXCEPTION_SORTERS.get(dbType));

        getChildCreate(getChildCreate(ds, "timeout"), "idle-timeout-minutes").setText("30");
    }

    private void configureDriver(Element datasources) throws JDOMException {
        Element drivers = getChildCreate(datasources, "drivers");
        Element driver = getElement(drivers, "xp:driver[@name=\"jahia." + dbType + "\"]", DS_NS.getURI());
        if (driver == null) {
            driver = new Element("driver", DS_NS).setAttribute("name", "jahia." + dbType).setAttribute("module",
                    "org.jahia.jdbc." + dbType);
            drivers.addContent(driver);
        }
    }

    private void disableDefaultWelcomeWebApp(Element profile) {
        Element web = profile.getChild("subsystem", WEB_NS);
        if (web != null) {
            Element virtualServer = web.getChild("virtual-server", WEB_NS);
            if (virtualServer != null) {
                virtualServer.setAttribute("enable-welcome-root", "false");
            }
        }
    }

    private Element getChildCreate(Element parent, String childName) {
        Element child = parent.getChild(childName, DS_NS);
        if (child == null) {
            child = new Element(childName, DS_NS);
            parent.addContent(child);
        }
        return child;
    }

    private Element getProfile(Element root, ConfigFile sourceConfigFile) {
        Element profile = root.getChild("profile", root.getNamespace());
        if (profile == null) {
            throw new IllegalArgumentException("Cannot find <profile> element in the file" + sourceConfigFile);
        }

        return profile;
    }

    private Format getOutputFormat() {
        Format customFormat = Format.getRawFormat();
        customFormat.setTextMode(TextMode.TRIM);
        customFormat.setIndent("    ");
        customFormat.setLineSeparator(System.getProperty("line.separator"));
        return customFormat;
    }

    public void updateConfiguration(ConfigFile sourceConfigFile, String destFileName) throws Exception {
        FileWriter out = null;
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            InputStreamReader fileReader = new InputStreamReader(sourceConfigFile.getInputStream());
            org.jdom.Document jdomDocument = saxBuilder.build(fileReader);

            Element root = jdomDocument.getRootElement();
            Element profile = getProfile(root, sourceConfigFile);
            Element datasources = getChildCreate(getChildCreate(profile, "subsystem"), "datasources");

            configureDriver(datasources);

            configureDatasource(datasources);

            if (jahiaConfigInterface.getWebAppDirName().equals("ROOT")) {
                disableDefaultWelcomeWebApp(profile);
            }

            out = new FileWriter(destFileName);
            new XMLOutputter(getOutputFormat()).output(jdomDocument, out);

        } catch (JDOMException jdome) {
            throw new Exception("Error while updating configuration file " + sourceConfigFile, jdome);
        } finally {
            IOUtils.closeQuietly(out);
        }

    }

}
