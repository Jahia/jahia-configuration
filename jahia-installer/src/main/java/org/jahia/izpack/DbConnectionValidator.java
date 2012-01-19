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

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

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
        String dbmsSuffix = dbmsType;
        if (dbmsSuffix.length() > 0) {
            dbmsSuffix = "." + dbmsSuffix;
        }
        String driver = adata.getVariable(getVar(adata,
                "DbConnectionValidationPanelAction.driverVariable",
                "dbSettings.connection.driver")
                + dbmsSuffix);
        String url = adata.getVariable(getVar(adata,
                "DbConnectionValidationPanelAction.urlVariable",
                "dbSettings.connection.url")
                + dbmsSuffix);
        String username = adata.getVariable(getVar(adata,
                "DbConnectionValidationPanelAction.usernameVariable",
                "dbSettings.connection.username"));
        String password = adata.getVariable(getVar(adata,
                "DbConnectionValidationPanelAction.passwordVariable",
                "dbSettings.connection.password"));

        // perform connection settings validation
        try {
            passed = validateDbConnection(driver, url, username, password, dbmsType, adata);
        } catch (Exception e) {
            passed = false;
            Debug.trace("Validation did not pass, error: " + e.getMessage());
            String key = "dbSettings.connection.error";
            errorMsg = getMessage(adata, key);
            errorMsg = errorMsg + "\n" + e.getClass().getName() + ": "
                    + e.getMessage();
            System.out.println("\n" + errorMsg + "\n");
        }

        return passed;
    }

    private String getMessage(AutomatedInstallData adata, String key) {
        String errorMsg = adata.langpack.getString(key);
        errorMsg = errorMsg == null || errorMsg.length() == 0
                || key.equals(errorMsg) ? "An error occurred while establishing the connection to the database"
                : errorMsg;
        return errorMsg;
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
                                         String username, String password, String dbmsType, AutomatedInstallData adata) throws ClassNotFoundException,
            SQLException, InstantiationException, IllegalAccessException {
        boolean valid = true;

        Class.forName(driver).newInstance();
        Connection theConnection = DriverManager.getConnection(url, username,
                password);
        Statement theStatement = theConnection.createStatement();

        errorMsg = checkDatabase(dbmsType, theStatement, adata);
        valid = (errorMsg == null);
        
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

    private String checkDatabase(String databaseType, Statement statement,AutomatedInstallData adata) throws SQLException{
        if (databaseType.equals("mysql")) {
            Map<String, String> properties = new HashMap<String, String>();
            ResultSet rs = statement.executeQuery("SHOW VARIABLES");
            while (rs.next()) {
                properties.put(rs.getString(1), rs.getString(2));
            }
            rs.close();

            if (properties.containsKey("max_allowed_packet")) {
                if (Long.parseLong(properties.get("max_allowed_packet")) < (1024 * 1024 * 100)) {
                    return getMessage(adata,"dbSettings.connection.error.maxallowedpackets");
                }
            }

            if (properties.containsKey("version") && properties.containsKey("version_compile_os") &&
                    properties.get("version_compile_os").toLowerCase().contains("darwin")) {
                String[] v = properties.get("version").split("[^0-9]");
                if (v[0].equals("5") && v[1].equals("5") && Long.parseLong(v[2]) >= 9 && Long.parseLong(v[2]) <= 12) {
                    if (!"1".equals(properties.get("lower_case_table_names"))) {
                        return getMessage(adata,"dbSettings.connection.error.lowercasetablenames");
                    }
                }
            }
        }
        return null;
    }



}
