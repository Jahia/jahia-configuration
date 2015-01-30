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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.util.Debug;

/**
 * Data validator that is responsible for validating DB connection settings.
 * 
 * @author Sergiy Shyrkov
 */
public class DbConnectionValidator extends MySQLDriverValidator {
    
    /**
     * Driver delegate for MySQL driver.
     * 
     * @author Sergiy Shyrkov
     */
    public static class DriverDelegate implements Driver {
        
        private final Driver driver;
        
        public DriverDelegate(Driver driver) {
            if (driver == null) {
                throw new IllegalArgumentException("Driver cannot be null.");
            }
            this.driver = driver;
        }

        public Connection connect(String url, Properties info) throws SQLException {
            return driver.connect(url, info);
        }

        public boolean acceptsURL(String url) throws SQLException {
            return driver.acceptsURL(url);
        }

        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return driver.getPropertyInfo(url, info);
        }

        public int getMajorVersion() {
            return driver.getMajorVersion();
        }

        public int getMinorVersion() {
            return driver.getMinorVersion();
        }

        public boolean jdbcCompliant() {
            return driver.jdbcCompliant();
        }

        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return driver.getParentLogger();
        }
    }

    protected boolean doValidate(AutomatedInstallData adata) {
        if (!validateOracleDriverLicense(adata)) {
            return false;
        }
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
        }
        
        if (errorMsg != null) {
            System.out.println("\n" + errorMsg + "\n");
        }

        return passed;
    }

    protected String getMessage(AutomatedInstallData adata, String key) {
        return super.getMessage(adata, key, "An error occurred while establishing the connection to the database");
    }

    private boolean validateDbConnection(String driver, String url,
                                         String username, String password, String dbmsType, AutomatedInstallData adata) throws ClassNotFoundException,
            SQLException, InstantiationException, IllegalAccessException, MalformedURLException {
        boolean valid = true;

        if (dbmsType.equals("mysql")) {
            // need to load the driver JAR
            loadMySQLDriver(driver, adata);
        } else {
            Class.forName(driver).newInstance();
        }

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

    private void loadMySQLDriver(String driverClass, AutomatedInstallData adata) throws MalformedURLException,
            InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
        String path = getDriverPath(adata);
        URLClassLoader urlLoader = new URLClassLoader(new URL[] { new File(path).toURI().toURL() });
        Driver driverInstance = (Driver) Class.forName(driverClass, true, urlLoader).newInstance();
        DriverManager.registerDriver(new DriverDelegate(driverInstance));
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


    private boolean validateOracleDriverLicense(AutomatedInstallData adata) {
        String dbType = adata.getVariable(getVar(adata, "DbConnectionValidationPanelAction.dbTypeVariable",
                "dbSettings.dbms.type"));
        if (!"oracle".equals(dbType)) {
            return true;
        }
        String licenseAccepted = adata.getVariable(getVar(adata,
                "DbConnectionValidationPanelAction.oracleDriverLicenseVariable", "oracle.driver.license"));
        if (licenseAccepted != null && !"true".equals(licenseAccepted)) {
            errorMsg = getMessage(adata, "dbSettings.dbms.type.oracle.license.validator",
                    "You must accept the terms and conditions of the OTN License Agreement");
            System.out.println("\n" + errorMsg + "\n");
            return false;
        }

        return true;
    }

}
