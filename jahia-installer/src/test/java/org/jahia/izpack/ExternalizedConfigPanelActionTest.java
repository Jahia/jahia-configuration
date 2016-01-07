/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.izpack;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

/**
 * Test for the {@link ExternalizedConfigPanelActionTest}.
 * 
 * @author Sergiy Shyrkov
 */
public class ExternalizedConfigPanelActionTest {

    private Properties data;

    @Test
    public void testAbsolute() {
        data = new Properties();

        data.put("INSTALL_PATH", "C:\\df");
        data.put("data.dir", "C:\\df71\\df-data");
        data.put("derby.home.unix", "C:\\df71\\df-data/dbdata");
        data.put("derby.home.win", "C:\\df71\\df-data\\dbdata");
        data.put("jahiaVarDiskPath", "C:\\df71\\df-data");
        data.put("externalizedConfigTargetPath", "C:\\df71\\digital-factory-cfg");
        data.put("externalizedConfigTargetPathNormalized", "C:/df71/digital-factory-cfg");
        data.put("tomcat.common.loader", "C:/df71/digital-factory-cfg");

        ExternalizedConfigPanelAction.processRelativePaths(data);

        assertEquals("tomcat.common.loader should be absolute", data.get("tomcat.common.loader"),
                "C:/df71/digital-factory-cfg");
        assertEquals("derby.home.unix should be absolute", data.get("derby.home.unix"), "C:\\df71\\df-data/dbdata");
        assertEquals("derby.home.win should be absolute", data.get("derby.home.win"), "C:\\df71\\df-data\\dbdata");
        assertEquals("jahiaVarDiskPath should be absolute", data.get("jahiaVarDiskPath"), "C:\\df71\\df-data");
    }

    @Test
    public void testRelative() {
        data = new Properties();

        data.put("INSTALL_PATH", "C:\\df71");
        data.put("data.dir", "C:\\df71\\df-data");
        data.put("derby.home.unix", "C:\\df71\\df-data/dbdata");
        data.put("derby.home.win", "C:\\df71\\df-data\\dbdata");
        data.put("jahiaVarDiskPath", "C:\\df71\\df-data");
        data.put("externalizedConfigTargetPath", "C:\\df71\\digital-factory-cfg");
        data.put("externalizedConfigTargetPathNormalized", "C:/df71/digital-factory-cfg");
        data.put("tomcat.common.loader", "C:/df71/digital-factory-cfg");

        ExternalizedConfigPanelAction.processRelativePaths(data);

        assertEquals("tomcat.common.loader should be relative", data.get("tomcat.common.loader"),
                "${catalina.home}/../digital-factory-cfg");
        assertEquals("derby.home.unix should be relative", data.get("derby.home.unix"),
                "$CATALINA_HOME/../df-data/dbdata");
        assertEquals("derby.home.win should be relative", data.get("derby.home.win"),
                "%CATALINA_HOME%\\..\\df-data\\dbdata");
        assertEquals("jahiaVarDiskPath should be relative", data.get("jahiaVarDiskPath"),
                "${jahiaWebAppRoot}/../../../df-data");
    }

    @Test
    public void testRelativeWithSubfolders() {
        data = new Properties();

        data.put("INSTALL_PATH", "C:\\df71");
        data.put("data.dir", "C:\\df71\\df\\data");
        data.put("derby.home.unix", "C:\\df71\\df\\data/dbdata");
        data.put("derby.home.win", "C:\\df71\\df\\data\\dbdata");
        data.put("jahiaVarDiskPath", "C:\\df71\\df\\data");
        data.put("externalizedConfigTargetPath", "C:\\df71\\df\\cfg");
        data.put("externalizedConfigTargetPathNormalized", "C:/df71/df/cfg");
        data.put("tomcat.common.loader", "C:/df71/df/cfg");

        ExternalizedConfigPanelAction.processRelativePaths(data);

        assertEquals("tomcat.common.loader should be relative", data.get("tomcat.common.loader"),
                "${catalina.home}/../df/cfg");
        assertEquals("derby.home.unix should be relative", data.get("derby.home.unix"),
                "$CATALINA_HOME/../df/data/dbdata");
        assertEquals("derby.home.win should be relative", data.get("derby.home.win"),
                "%CATALINA_HOME%\\..\\df\\data\\dbdata");
        assertEquals("jahiaVarDiskPath should be relative", data.get("jahiaVarDiskPath"),
                "${jahiaWebAppRoot}/../../../df/data");
    }

}
