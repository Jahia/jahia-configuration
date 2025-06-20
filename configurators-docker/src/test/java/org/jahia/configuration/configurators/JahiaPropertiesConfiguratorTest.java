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

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * Unit test for jahia.properties configurator
 *
 * @author loom
 *         Date: Oct 27, 2009
 *         Time: 9:02:29 AM
 */
public class JahiaPropertiesConfiguratorTest extends AbstractConfiguratorTestCase {

    public void testUpdateConfiguration() throws Exception {


        File sourceProperties = new File(this.getClass().getClassLoader().getResource("jahia.properties").getFile());
        String modifiedPropertiesPath = sourceProperties.getParent() + File.separator + "jahia-modified.properties";
        File sourceNodeProperties = new File(this.getClass().getClassLoader().getResource("jahia.node.properties").getFile());
        String modifiedNodePropertiesPath = sourceProperties.getParent() + File.separator + "jahia.node-modified.properties";

        try (FileInputStream inStream = new FileInputStream(sourceProperties)) {
            new JahiaPropertiesConfigurator(oracleDBProperties, websphereOracleConfigBean).updateConfiguration(inStream, modifiedPropertiesPath);
        }

        try (FileInputStream inStream = new FileInputStream(sourceNodeProperties)) {
            new JahiaNodePropertiesConfigurator(websphereOracleConfigBean).updateConfiguration(inStream, modifiedNodePropertiesPath);
        }

        Properties websphereOracleProperties = new Properties();
        try (FileInputStream inStream = new FileInputStream(modifiedPropertiesPath)) {
            websphereOracleProperties.load(inStream);
        }

        // test for the additional properties
        assertEquals("true", websphereOracleProperties.getProperty("jahia.dm.viewer.enabled"));
        assertEquals("c:\\Program Files (x86)\\SWFTools\\pdf2swf.exe", websphereOracleProperties.getProperty("jahia.dm.viewer.pdf2swf"));
    }
}
