/**
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2011 Jahia Solutions Group SA. All rights reserved.
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
 * between you and Jahia Solutions Group SA. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */

package org.jahia.izpack;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.DataValidator;
import com.izforge.izpack.util.Debug;

/**
 * Data validator that is responsible for validating DB connection settings.
 * 
 * @author Sergiy Shyrkov
 */
public class DbConnectionValidator implements DataValidator {

    private static String getVar(AutomatedInstallData adata, String name,
            String defValue) {
        String var = adata.getVariable(name);
        return var != null ? var : defValue;
    }
    private String errorMsg;

    private String warningMsg = "";

    private boolean doValidate(AutomatedInstallData adata) {
        if (!Boolean.valueOf(adata.getVariable(getVar(adata,
                "DbConnectionValidationPanelAction.validateVariable",
                "dbSettings.dbms.createTables")))) {
            // no need to validate the connection
            return true;
        }

        boolean passed = false;

        String dbmsType = adata.getVariable(getVar(adata,
                "DbConnectionValidationPanelAction.dbmsTypeVariable",
                "dbSettings.dbms.type"));
        if (dbmsType.length() > 0) {
            dbmsType = "." + dbmsType;
        }
        String driver = adata.getVariable(getVar(adata,
                "DbConnectionValidationPanelAction.driverVariable",
                "dbSettings.connection.driver")
                + dbmsType);
        String url = adata.getVariable(getVar(adata,
                "DbConnectionValidationPanelAction.urlVariable",
                "dbSettings.connection.url")
                + dbmsType);
        String username = adata.getVariable(getVar(adata,
                "DbConnectionValidationPanelAction.usernameVariable",
                "dbSettings.connection.username"));
        String password = adata.getVariable(getVar(adata,
                "DbConnectionValidationPanelAction.passwordVariable",
                "dbSettings.connection.password"));

        // perform connection settings validation
        try {
            passed = validateDbConnection(driver, url, username, password);
        } catch (Exception e) {
            passed = false;
            Debug.trace("Validation did not pass, error: " + e.getMessage());
            String key = "dbSettings.connection.error";
            errorMsg = adata.langpack.getString(key);
            errorMsg = errorMsg == null || errorMsg.length() == 0
                    || key.equals(errorMsg) ? "An error occurred while establishing the connection to the database"
                    : errorMsg;
            errorMsg = errorMsg + "\n" + e.getClass().getName() + ": "
                    + e.getMessage();
            System.out.println("\n" + errorMsg + "\n");
        }

        return passed;
    }

    public boolean getDefaultAnswer() {
        return false;
    }

    public String getErrorMessageId() {
        return errorMsg;
    }

    public String getWarningMessageId() {
        return warningMsg;
    }

    public Status validateData(AutomatedInstallData adata) {
        return doValidate(adata) ? Status.OK : Status.ERROR;
    }

    private boolean validateDbConnection(String driver, String url,
            String username, String password) throws ClassNotFoundException,
            SQLException, InstantiationException, IllegalAccessException {
        boolean valid = true;

        Class.forName(driver).newInstance();
        Connection theConnection = DriverManager.getConnection(url, username,
                password);
        Statement theStatement = theConnection.createStatement();
        try {
            theStatement.close();
        } catch (Exception e) {
            // ignore
        }
        try {
            theConnection.close();
        } catch (Exception e) {
            // ignore
        }

        return valid;
    }

}
