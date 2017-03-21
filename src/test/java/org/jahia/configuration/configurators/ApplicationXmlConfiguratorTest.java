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

import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.net.URL;

/**
 * Test unit for application.xml configurator
 */
public class ApplicationXmlConfiguratorTest extends AbstractXMLConfiguratorTestCase {
    @Override
    public void testUpdateConfiguration() throws Exception {
        FileSystemManager fsManager = VFS.getManager();
        URL applicationXmlUrl = this.getClass().getClassLoader().getResource("configurators/META-INF/application.xml");
        File applicationXmlFile = new File(applicationXmlUrl.getFile());
        String applicationXmlParentPath = applicationXmlFile.getParentFile().getPath() + File.separator;

        ApplicationXmlConfigurator applicationXmlConfiguratorWebsphere = new ApplicationXmlConfigurator(websphereOracleConfigBean, null);
        applicationXmlConfiguratorWebsphere.updateConfiguration(new VFSConfigFile(fsManager, applicationXmlUrl.toExternalForm()), applicationXmlParentPath + "application-modified.xml");

        // The following tests are NOT exhaustive
        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Document jdomDocument = saxBuilder.build(applicationXmlParentPath + "application-modified.xml");
        String prefix = "";

        assertAllTextEquals(jdomDocument, "//application/module[@id=\"jahia-war\"]/web/web-uri/text()", prefix, "ROOT.war");
        assertAllTextEquals(jdomDocument, "//application/module[@id=\"jahia-war\"]/web/context-root/text()", prefix, "/");
        assertAllTextEquals(jdomDocument, "//application/module[@id=\"portlet-testsuite\"]/web/web-uri/text()", prefix, "websphere-testsuite.war");
        assertAllTextEquals(jdomDocument, "//application/module[@id=\"portlet-testsuite\"]/web/context-root/text()", prefix, "testsuite");

    }
}
