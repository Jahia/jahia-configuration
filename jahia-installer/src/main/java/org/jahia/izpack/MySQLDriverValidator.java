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
package org.jahia.izpack;

import com.izforge.izpack.installer.AutomatedInstallData;

/**
 * Validator for the specified MySQL driver.
 * 
 * @author Sergiy Shyrkov
 */
public class MySQLDriverValidator extends BaseDataValidator {

    protected String getDriverPath(AutomatedInstallData adata) {
        return adata.getVariable(getVar(adata, "DbConnectionValidationPanelAction.mysqlDriverPathVariable",
                "mysql.driver.path"));
    }

    protected String getExpectedDriverVersion(AutomatedInstallData adata) {
        return adata.getVariable(getVar(adata, "DbConnectionValidationPanelAction.mysqlDriverVersionVariable",
                "driver.mysql.version"));
    }

    protected boolean doValidate(AutomatedInstallData adata) {
        String path = getDriverPath(adata);
        String version = getExpectedDriverVersion(adata);

        path = path.replace('\\', '/');
        boolean passed = path.indexOf('/') == -1 ? path.equals("mysql-connector-java-" + version + ".jar") : path
                .endsWith("/mysql-connector-java-" + version + ".jar");

        if (!passed) {
            errorMsg = getMessage(adata, "dbType.mysql.path.validator",
                    "The selected file does not match the expected one: mysql-connector-java-" + version + ".jar");
            System.out.println("\n" + errorMsg + "\n");
        } else {
            String licenseAccepted = adata.getVariable(getVar(adata, "DbConnectionValidationPanelAction.mysqlDriverLicenseVariable",
                    "mysql.driver.license"));
            if (licenseAccepted != null && !"true".equals(licenseAccepted)) {
                errorMsg = getMessage(adata, "dbType.mysql.license.validator",
                        "You must accept the terms and conditions of the GPL License");
                System.out.println("\n" + errorMsg + "\n");
                passed = false;
            }
        }
        

        return passed;
    }

}
