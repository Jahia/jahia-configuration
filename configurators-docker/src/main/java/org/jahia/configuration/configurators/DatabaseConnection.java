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

import java.sql.*;
import java.util.Enumeration;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * desc:  It's a class used only by JahiaInstallation and
 * JahiaAdministration (they only have rights to establish direct connections
 * to a database and not via the connection pooling). This class mainly
 * provides you to open test a database connection and to execute sql queries.
 * All methods returns an <code>int</code>, and not ResultSet or anything
 * else. The principe is to check only status of connections, queries, etc.
 *
 * Copyright:    Copyright (c) 2002
 * Company:      Jahia Ltd
 *
 * @author Alexandre Kraft
 * @version 1.0
 */
public class DatabaseConnection {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

    private Statement theStatement;
    private Connection theConnection;

    /**
     * Open a database connection, using settings defined by the user like
     * the database driver, the url, username and password.
     *
     * @param   driver    The JDBC driver for the database.
     * @param   url       The url where JDBC can found the database.
     * @param   username  The username required to access the database.
     * @param   password  The password required to access the database.
     * @return  <code>0</code> if the connection is open, <code>1</code> if the
     *          driver class is not found, or <code>2</code> if the connection
     *          cannot be opened.
     */
    public void databaseOpen (String driver,
                              String url,
                              String username,
                              String password)
        throws ClassNotFoundException, SQLException {
        logger.debug("Opening database connection with driver: {}, url: {}, username: {}",
                driver, url, username);

        // always close a possibly old connection before open a new...
        databaseClose();

        Driver driverInstance = getMatchingDriver(driver);
        if (driverInstance != null) {
            logger.debug("Using driver instance: {}", driverInstance.getClass().getName());
            Properties props = getDriverProperties(username, password);
            logger.debug("Connection properties prepared with username: {}", username);
            theConnection = driverInstance.connect(url, props);
            logger.debug("Connection established successfully with driver instance");
        } else {
            logger.debug("Driver instance is not found. Will rely on DriverManager.getConnection()");
            // try to open a database connection...
            theConnection = DriverManager.getConnection(url, username, password);
            logger.debug("Connection established successfully with DriverManager");
        }

        theStatement = theConnection.createStatement();
        logger.debug("Database connection opened successfully");
    }

    protected Properties getDriverProperties(String username, String password) {
        Properties info = new Properties();
        if (username != null) {
            info.put("user", username);
        }
        if (password != null) {
            info.put("password", password);
        }
        return info;
    }

    /**
     * Retrieves the driver for the specified class name. FIrst tries to instantiate the specified class. If fails, look through the
     * drivers, registered in DriverManager.
     * 
     * @param driverClass
     *            the class name of the driver to look for
     * @return the matching driver instance or <code>null</code> if the driver cannot be found directly for the specified class
     */
    protected Driver getMatchingDriver(String driverClass) {
        logger.debug("Looking up driver for class {}", driverClass);
        try {
            Driver driver = (Driver) Class.forName(driverClass).newInstance();
            logger.debug("Driver {} instantiated directly", driverClass);
            return driver;
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            logger.debug("Driver {} cannot be instantiated directly: {}. Will look it up through DriverManager.",
                    driverClass, e.getMessage());
            logger.debug("Exception details:", e);

            Enumeration<Driver> drivers = DriverManager.getDrivers();
            logger.debug("Searching through registered drivers");

            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                logger.trace("Checking driver: {}", driver.getClass().getName());
                if (driver.getClass().getName().equals(driverClass)) {
                    logger.debug("Found matching driver for class {} via DriverManager", driverClass);
                    return driver;
                }
            }

            logger.warn("No matching driver found for class: {}", driverClass);
        }

        return null;
    }

    /**
     * Execute a SQL query. Be careful, this method don't return an ResultSet.
     *
     * @param   sqlCode     The sql query you want to execute.
     *
     * @throws  SQLException   Propagate any exceptions
     */
    public void query(String sqlCode) throws SQLException {
        logger.debug("Executing SQL query: {}", sqlCode);
        try {
            boolean result = theStatement.execute(sqlCode);
            logger.debug("SQL query executed successfully, returns result set: {}", result);
        } catch (SQLException e) {
            logger.error("SQL query execution failed: {}", e.getMessage());
            logger.debug("Failed SQL query: {}", sqlCode);
            throw e;
        }
    }

    /**
     * Close the current database connection. If the connection statement do
     * not exists, the exception is simply catched. There is no problem about
     * this and completely transparent for the user.
     */
    public void databaseClose() {
        logger.debug("Closing database connection");
        try {
            if (theStatement != null) {
                theStatement.close();
                logger.debug("Statement closed successfully");
                theStatement = null;
            } else {
                logger.debug("No statement to close");
            }

            if (theConnection != null) {
                theConnection.close();
                logger.debug("Connection closed successfully");
                theConnection = null;
            } else {
                logger.debug("No connection to close");
            }
        } catch (SQLException sqle) {
            logger.warn("Error closing database resources: {}", sqle.getMessage());
            logger.debug("Exception details:", sqle);
        } catch (NullPointerException npe) {
            logger.warn("Null pointer exception while closing database resources: {}", npe.getMessage());
            logger.debug("Exception details:", npe);
        }
        logger.debug("Database connection closed");
    }

    /**
     * Get the connection of this instance of the class.
     *
     * @return  The connection of this instance of the class
     */
    public Connection getConnection () {
        return theConnection;
    }

    /**
     * Get the statement of this instance of the class.
     *
     * @return  The statement of this instance of the class
     */
    public Statement getStatement () {
        return theStatement;
    }

}
