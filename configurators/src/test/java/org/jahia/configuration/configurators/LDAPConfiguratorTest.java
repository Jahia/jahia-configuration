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

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Test unit for LDAP configurator
 * User: loom
 * Date: 27.04.11
 * Time: 17:22
 */
public class LDAPConfiguratorTest extends AbstractConfiguratorTestCase {
    @Override
    public void testUpdateConfiguration() throws Exception {
        File targetFolder = new File(FileUtils.getTempDirectory(), "ldap-config-test-" + System.currentTimeMillis());
        FileInputStream inStream = null;
        try {
            LDAPConfigurator ldapConfigurator = new LDAPConfigurator(oracleDBProperties, websphereOracleConfigBean);
            ldapConfigurator.updateConfiguration(null, targetFolder.getPath());
    
            Properties ldapProperties = new Properties();
            inStream = new FileInputStream(new File(targetFolder, "org.jahia.services.usermanager.ldap-config.cfg"));
            ldapProperties.load(inStream);
            assertEquals("ldap://10.8.37.17:389/", ldapProperties.getProperty("user.url"));
            assertEquals("dc=jahia", ldapProperties.getProperty("group.search.name"));
            assertEquals("givenName", ldapProperties.getProperty("user.j:firstName.attribute.map"));
        } finally {
            IOUtils.closeQuietly(inStream);
            FileUtils.deleteDirectory(targetFolder);
        }
    }
}
