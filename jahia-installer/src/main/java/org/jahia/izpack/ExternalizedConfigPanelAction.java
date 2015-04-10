/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
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
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */

package org.jahia.izpack;

import java.util.Properties;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.PanelAction;
import com.izforge.izpack.installer.PanelActionConfiguration;
import com.izforge.izpack.util.AbstractUIHandler;

/**
 * Panel action for setting normalized path to the configuration directory as variable.
 * 
 * @author Sergiy Shyrkov
 */
public class ExternalizedConfigPanelAction implements PanelAction {

    private static String normalize(String path) {
        return path.replace('\\', '/');
    }

    protected static void processRelativePaths(Properties data) {
        String installDir = normalize(data.getProperty("INSTALL_PATH")) + '/';
        String cfgDir = data.getProperty("externalizedConfigTargetPathNormalized");
        // check that the config folder is under install dir
        if (cfgDir.startsWith(installDir)) {
            data.put("isConfigDirRelative", "true");
            // rewrite path to relative for tomcat.common.loader
            data.put("tomcat.common.loader", "${catalina.home}/../" + cfgDir.substring(installDir.length()));
        }

        String dataDir = normalize(data.getProperty("data.dir"));
        // check that the data folder is under install dir
        if (dataDir.startsWith(installDir)) {
            data.put("isDataDirRelative", "true");
            // rewrite paths to relative
            String relativeDataDir = dataDir.substring(installDir.length());
            if (relativeDataDir.endsWith("/")) {
                relativeDataDir = relativeDataDir.substring(0, relativeDataDir.length() - 1);
            }
            data.put("derby.home.unix", "$CATALINA_HOME/../" + relativeDataDir + "/dbdata");
            data.put("derby.home.win", "%CATALINA_HOME%\\..\\" + relativeDataDir.replace('/', '\\') + "\\dbdata");
            data.put("jahiaVarDiskPath", "${jahiaWebAppRoot}/../../../" + relativeDataDir);
        }
    }

    public void executeAction(AutomatedInstallData adata, AbstractUIHandler handler) {
        String value = adata.getVariable("externalizedConfigTargetPath");
        adata.setVariable("externalizedConfigTargetPathNormalized", value != null ? value.replace('\\', '/') : value);
        if ("true".equals(adata.getVariable("jahia.tomcat.pack.selected"))) {
            processRelativePaths(adata.getVariables());
        }
    }

    public void initialize(PanelActionConfiguration configuration) {
        // do nothing
    }
}
