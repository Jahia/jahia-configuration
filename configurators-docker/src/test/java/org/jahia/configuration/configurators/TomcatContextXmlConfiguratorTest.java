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

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * Test unit for Tomcat's context deployment configurator.
 *
 * @author loom
 *         Date: Nov 3, 2009
 *         Time: 2:59:53 PM
 */
public class TomcatContextXmlConfiguratorTest extends AbstractXMLConfiguratorTestCase {

    public void testUpdateConfiguration() throws Exception {
        URL contextXmlUrl = this.getClass().getClassLoader().getResource("context.xml");
        File contextXmlFile = new File(contextXmlUrl.getFile());
        String contextXmlParentPath = contextXmlFile.getParentFile().getPath() + File.separator;

        TomcatContextXmlConfigurator tomcatOracleConfigurator = new TomcatContextXmlConfigurator(oracleDBProperties, tomcatMySQLConfigBean);
        try (InputStream inputStream = contextXmlUrl.openStream()){
            tomcatOracleConfigurator.updateConfiguration(inputStream, contextXmlParentPath + "context-modified.xml");
        }

        // The following tests are NOT exhaustive
        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Document jdomDocument = saxBuilder.build(contextXmlParentPath + "context-modified.xml");
        String prefix = "";

        assertAllTextEquals(jdomDocument, "//Resource/@username", prefix, oracleDBProperties.getProperty("jahia.database.user"));
        assertAllTextEquals(jdomDocument, "//Resource/@password", prefix, oracleDBProperties.getProperty("jahia.database.pass"));
        assertAllTextEquals(jdomDocument, "//Resource/@url", prefix, oracleDBProperties.getProperty("jahia.database.url"));
        assertAllTextEquals(jdomDocument, "//Resource/@driverClassName", prefix, oracleDBProperties.getProperty("jahia.database.driver"));

        TomcatContextXmlConfigurator tomcatMySQLContextXmlConfigurator = new TomcatContextXmlConfigurator(mysqlDBProperties, tomcatMySQLConfigBean);
        tomcatMySQLContextXmlConfigurator.updateConfFromFile(contextXmlParentPath + "context-modified.xml", contextXmlParentPath + "context-modified2.xml");

        // The following tests are NOT exhaustive
        saxBuilder = new SAXBuilder();
        jdomDocument = saxBuilder.build(contextXmlParentPath + "context-modified2.xml");
        assertAllTextEquals(jdomDocument, "//Resource/@username", prefix, mysqlDBProperties.getProperty("jahia.database.user"));
        assertAllTextEquals(jdomDocument, "//Resource/@password", prefix, mysqlDBProperties.getProperty("jahia.database.pass"));
        assertAllTextEquals(jdomDocument, "//Resource/@url", prefix, mysqlDBProperties.getProperty("jahia.database.url"));
        assertAllTextEquals(jdomDocument, "//Resource/@driverClassName", prefix, mysqlDBProperties.getProperty("jahia.database.driver"));
    }
}
