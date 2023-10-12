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
package org.jahia.izpack;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.PanelAction;
import com.izforge.izpack.installer.PanelActionConfiguration;
import com.izforge.izpack.util.AbstractUIHandler;

/**
 * Base panel action that processes the studio tools variables (SCM and Maven).
 * 
 * @author Sergiy Shyrkov
 */
public abstract class StudioToolsPanelAction implements PanelAction {

    public static class StudioToolsPostValidateAction extends StudioToolsPanelAction {

        public void executeAction(AutomatedInstallData adata, AbstractUIHandler handler) {
            updateVariableIfEmpty("studioToolSettings.gitPath", "git", adata);
            updateVariableIfEmpty("studioToolSettings.mvnPath", "mvn", adata);
            updateVariableIfEmpty("studioToolSettings.svnPath", "svn", adata);
        }

    }

    public static class StudioToolsPreActivateAction extends StudioToolsPanelAction {

        public void executeAction(AutomatedInstallData adata, AbstractUIHandler handler) {
            clearVariableIfDefault("studioToolSettings.gitPath", "git", adata);
            clearVariableIfDefault("studioToolSettings.mvnPath", "mvn", adata);
            clearVariableIfDefault("studioToolSettings.svnPath", "svn", adata);
        }

    }

    public void initialize(PanelActionConfiguration configuration) {
        // do nothing
    }

    protected static void updateVariableIfEmpty(String var, String value, AutomatedInstallData adata) {
        String path = adata.getVariable(var);
        if (path == null || path.length() == 0) {
            adata.setVariable(var, value);
        }
    }

    protected static void clearVariableIfDefault(String var, String value, AutomatedInstallData adata) {
        String path = adata.getVariable(var);
        if (path != null && path.equals(value)) {
            adata.setVariable(var, "");
        }
    }
}
