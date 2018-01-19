/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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

import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.net.URL;

/**
 * Test unit for the root user configurator
 *
 * @author loom
 *         Date: Nov 3, 2009
 *         Time: 4:15:35 PM
 */
public class RootUserConfiguratorTest extends AbstractXMLConfiguratorTestCase {
    public void testUpdateConfiguration() throws Exception {
        FileSystemManager fsManager = VFS.getManager();
        URL rootXmlUrl = this.getClass().getClassLoader().getResource("configurators/WEB-INF/etc/repository/root-test.xml");
        File rootXmlFile = new File(rootXmlUrl.getFile());
        String rootXmlFileParentPath = rootXmlFile.getParentFile().getPath() + File.separator;

        RootUserConfigurator websphereOracleConfigurator = new RootUserConfigurator(oracleDBProperties, websphereOracleConfigBean, "password");
        websphereOracleConfigurator.updateConfiguration(new VFSConfigFile(fsManager, rootXmlUrl.toExternalForm()), rootXmlFileParentPath + "root-modified.xml");

        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Document jdomDocument = saxBuilder.build(rootXmlFileParentPath + "root-modified.xml");
        String prefix = "";

        Element el = (Element) getNode(jdomDocument, "/content/users/*", prefix);
        assertEquals(websphereOracleConfigBean.getJahiaRootUsername(), el.getName());
        assertEquals("password", ((Attribute) getNode(jdomDocument, "/content/users/"
                + websphereOracleConfigBean.getJahiaRootUsername() + "/@j:password", prefix)).getValue());
        assertEquals(websphereOracleConfigBean.getJahiaRootFirstname(), ((Attribute) getNode(jdomDocument, "/content/users/"
                + websphereOracleConfigBean.getJahiaRootUsername() + "/@j:firstName", prefix)).getValue());
        assertEquals(websphereOracleConfigBean.getJahiaRootLastname(), ((Attribute) getNode(jdomDocument, "/content/users/"
                + websphereOracleConfigBean.getJahiaRootUsername() + "/@j:lastName", prefix)).getValue());
        assertEquals(websphereOracleConfigBean.getJahiaRootEmail(), ((Attribute) getNode(jdomDocument, "/content/users/"
                + websphereOracleConfigBean.getJahiaRootUsername() + "/@j:email", prefix)).getValue());
        assertEquals("de", ((Attribute) getNode(jdomDocument, "/content/users/"
                + websphereOracleConfigBean.getJahiaRootUsername() + "/@preferredLanguage", prefix)).getValue());

        RootUserConfigurator tomcatMySQLConfigurator = new RootUserConfigurator(mysqlDBProperties, tomcatMySQLConfigBean, "1234root");
        tomcatMySQLConfigurator.updateConfiguration(new VFSConfigFile(fsManager, rootXmlFileParentPath + "root-test.xml"), rootXmlFileParentPath + "root-modified2.xml");

        jdomDocument = saxBuilder.build(rootXmlFileParentPath + "root-modified2.xml");

        assertEquals("root", ((Element) getNode(jdomDocument, "/content/users/*", prefix)).getName());
        assertEquals("1234root", ((Attribute) getNode(jdomDocument, "/content/users/root/@j:password", prefix)).getValue());

    }
}
