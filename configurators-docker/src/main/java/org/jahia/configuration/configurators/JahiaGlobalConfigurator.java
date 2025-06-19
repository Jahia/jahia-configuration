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

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.lang3.StringUtils;
import org.jahia.commons.encryption.EncryptionUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Global Jahia configuration utility.
 *
 * @author loom
 * Date: May 11, 2010
 * Time: 11:10:23 AM
 */
public class JahiaGlobalConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(JahiaGlobalConfigurator.class);

    private static final ConvertUtilsBean CONVERTER_UTILS_BEAN = new ConvertUtilsBean();

    public static final String DB_SCRIPT = "db_script";
    public static final String DERBY = "derby";
    public static final String DERBY_EMBEDDED = "derby_embedded";
    public static final String MYSQL = "mysql";
    public static final String MARIADB = "mariadb";
    public static final String POSTGRESQL = "postgresql";
    public static final String JAHIA_DATABASE_DRIVER = "jahia.database.driver";
    public static final String IF_NECESSARY = "if-necessary";

    static {
        CONVERTER_UTILS_BEAN.register(new Converter() {
            @SuppressWarnings("rawtypes")
            public Object convert(Class type, Object value) {
                return fromString(value != null ? value.toString() : null);
            }
        }, List.class);
        CONVERTER_UTILS_BEAN.register(new Converter() {
            @SuppressWarnings("rawtypes")
            public Object convert(Class type, Object value) {
                return value != null ? fromJSON(value.toString()) : new HashMap<String, String>();
            }
        }, Map.class);
    }

    public static Map<String, String> fromJSON(String json) {
        Map<String, String> values = new HashMap<>();
        try {
            JSONObject obj = new JSONObject(json.contains("{") ? StringUtils.replace(json, "\\", "\\\\")
                                                    : "{" + StringUtils.replace(json, "\\", "\\\\") + "}");
            for (Iterator<?> iterator = obj.keys(); iterator.hasNext(); ) {
                String key = (String) iterator.next();
                values.put(key, obj.getString(key));
            }
        } catch (JSONException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        return values;
    }

    public static List<String> fromString(String value) {
        List<String> valueList = new LinkedList<>();
        if (value != null && value.length() > 0) {
            for (String singleValue : StringUtils.split(value, " ,;:")) {
                valueList.add(singleValue.trim());
            }
        }
        return valueList;
    }

    public static File resolveDataDir(String dataDirPath) {
        File dataDir;
        try {
            dataDir = new File(dataDirPath).getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // try to create the directory if it does not exist
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            throw new RuntimeException("Unable to create target directory: " + dataDir);
        }

        return dataDir;
    }

    JahiaConfigInterface jahiaConfig;
    DatabaseConnection db;
    File webappDir;
    Properties dbProps;
    File databaseScript;
    List<AbstractConfigurator> configurators = new ArrayList<AbstractConfigurator>();
    String externalizedConfigTempPath = null;
    File jahiaConfigDir;

    private File dataDir;

    public JahiaGlobalConfigurator(JahiaConfigInterface jahiaConfig) {
        this.jahiaConfig = jahiaConfig;
    }

    public void execute() throws Exception {
        logger.info("Starting Jahia configuration process");
        if (jahiaConfig.isExternalizedConfigActivated() &&
                !StringUtils.isBlank(jahiaConfig.getExternalizedConfigTargetPath())) {
            Path tempDirectory = Files.createTempDirectory("jahia-temp");
            jahiaConfigDir = new File(tempDirectory.toFile(), "jahia-config");
            File jahiaConfigConfigDir = new File(jahiaConfigDir, "jahia");
            jahiaConfigConfigDir.mkdirs();
            externalizedConfigTempPath = jahiaConfigConfigDir.getPath();
            logger.info("Created externalized config temporary path at: {}", externalizedConfigTempPath);
            logger.info("Config directory resolved to folder: {}", jahiaConfig.getExternalizedConfigTargetPath());
        }

        db = new DatabaseConnection();
        setProperties();
    }

    private void updateConfigurationFiles(String sourceWebAppPath, String webappPath, String targetConfigPath, Properties dbProps, JahiaConfigInterface jahiaConfigInterface) throws Exception {
        logger.info("Updating configuration files...");

        new JackrabbitConfigurator(dbProps, jahiaConfigInterface)
                .updateConfFromFile(sourceWebAppPath + "/WEB-INF/etc/repository/jackrabbit/repository.xml",
                        webappPath + "/WEB-INF/etc/repository/jackrabbit/repository.xml");

        new TomcatContextXmlConfigurator(dbProps, jahiaConfigInterface)
                .updateConfFromFile(sourceWebAppPath + "/META-INF/context.xml", webappPath + "/META-INF/context.xml");

        if (Boolean.parseBoolean(jahiaConfigInterface.getProcessingServer())) {
            new RootUserConfigurator(dbProps, jahiaConfigInterface, encryptPassword(jahiaConfigInterface.getJahiaRootPassword()))
                    .updateConfFromFile(sourceWebAppPath + "/WEB-INF/etc/repository/template-root-user.xml",
                            webappPath + "/WEB-INF/etc/repository/root-user.xml");
        } else {
            logger.info("Skipping root user configuration (processing server: false)");
        }

        File jahiaImplFile = findFile(sourceWebAppPath + "/WEB-INF/lib", "jahia\\-impl\\-.*\\.jar");
        if (jahiaImplFile != null && jahiaImplFile.exists()) {
            logger.info("Found Jahia implementation JAR: {}", jahiaImplFile.getName());
            new JahiaPropertiesConfigurator(dbProps, jahiaConfigInterface).updateConfFromFileInJar(jahiaImplFile,
                "org/jahia/defaults/config/properties/jahia.properties",
                targetConfigPath + "/jahia.properties");

            new JahiaNodePropertiesConfigurator(jahiaConfigInterface).updateConfFromFileInJar(jahiaImplFile,
                "org/jahia/defaults/config/properties/jahia.node.properties",
                targetConfigPath + "/jahia.node.properties");
        } else {
            logger.warn("Could not find Jahia implementation JAR in {}", sourceWebAppPath + "/WEB-INF/lib");
        }

        logger.info("Configuration files updated successfully");
    }

    public File findFile(String parentPath, String fileMatchingPattern) {
        Path parentDirectory = Paths.get(parentPath);
        Pattern matchingPattern = Pattern.compile(fileMatchingPattern);

        try (Stream<Path> children = Files.list(parentDirectory)) {
            return children
                    .filter(Files::isRegularFile)
                    .filter(child -> {
                        String fileName = child.getFileName().toString();
                        return matchingPattern.matcher(fileName).matches();
                    })
                    .map(Path::toFile)
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            logger.debug("Couldn't find file matching pattern {} at path {}", fileMatchingPattern, parentPath, e);
            return null;
        }
    }

    private void setProperties() throws Exception {

        webappDir = new File(new File(jahiaConfig.getTargetServerDirectory(), "webapps"), jahiaConfig.getWebAppDirName());
        String sourceWebappPath = webappDir.toString();
        String databaseType = jahiaConfig.getDatabaseType();

        logger.info("Webapp directory resolved to folder: {}", webappDir);

        String dbUrl = jahiaConfig.getDatabaseUrl();
        boolean isEmbeddedDerby = databaseType.equals(DERBY_EMBEDDED);
        if (isEmbeddedDerby) {
            if (jahiaConfig.getDatabaseUrl().contains("$context")) {
                dbUrl = StringUtils.replace(dbUrl, "$context",
                                            StringUtils.replace(sourceWebappPath, "\\", "/"));
            } else {
                System.setProperty("derby.system.home", StringUtils.replace(
                        new File(getDataDir(), "dbdata").getAbsolutePath(),
                        "\\", "/"));
            }
        }

        dbProps = new Properties();
        //database script always ends with a .script
        databaseScript = new File(getDataDir(), "db/" + databaseType + ".script");
        try (FileInputStream is = new FileInputStream(databaseScript)) {
            dbProps.load(is);
            // we override these just as the configuration wizard does
            dbProps.put("storeFilesInDB", jahiaConfig.getStoreFilesInDB());
            dbProps.put("storeFilesInAWS", jahiaConfig.getStoreFilesInAWS());
            dbProps.put("fileDataStorePath", jahiaConfig.getFileDataStorePath() != null ? jahiaConfig.getFileDataStorePath() : "");
            dbProps.put("jahia.database.url", dbUrl);
            dbProps.put("jahia.database.user", jahiaConfig.getDatabaseUsername());
            dbProps.put("jahia.database.pass", jahiaConfig.getDatabasePassword());
        } catch (IOException e) {
            logger.error("Error in loading database settings because of", e);
        }

        String targetConfigPath = externalizedConfigTempPath != null ? externalizedConfigTempPath : webappDir.getPath() + "/WEB-INF/etc/config";
        updateConfigurationFiles(sourceWebappPath, webappDir.getPath(), targetConfigPath, dbProps, jahiaConfig);

        try {
            String existingLicense = jahiaConfig.getLicenseFile();
            copyLicense(existingLicense != null && existingLicense.length() > 0 ? existingLicense : sourceWebappPath
                    + "/WEB-INF/etc/config/licenses/license-free.xml", targetConfigPath + "/license.xml");

            if (jahiaConfig.getOverwritedb().equals("true") || jahiaConfig.getOverwritedb().equals(IF_NECESSARY)) {
                if (!databaseScript.exists()) {
                    logger.info("cannot find script in {}", databaseScript.getPath());
                    throw new Exception("Cannot find script for database " + databaseType);
                }

                if (cleanDatabase(databaseType, dbUrl)) {
                    db.databaseOpen(dbProps.getProperty(JAHIA_DATABASE_DRIVER), dbUrl, jahiaConfig.getDatabaseUsername(), jahiaConfig.getDatabasePassword());
                    createDBTables(databaseScript);
                }

                if (isEmbeddedDerby) {
                    shutdownDerby();
                }
            }

            if ((jahiaConfigDir != null) && (externalizedConfigTempPath != null)) {
                copyExternalizedConfig();
            }

        } catch (Exception e) {
            logger.error("exception in setting the properties because of {}", e, e);
        }
    }

    private void shutdownDerby() {
        // Shutdown embedded Derby
        logger.info("Shutting down embedded Derby...");
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true", jahiaConfig.getDatabaseUsername(), jahiaConfig.getDatabasePassword());
        } catch (Exception e) {
            if (!(e instanceof SQLException) || e.getMessage() == null
                    || !e.getMessage().contains("Derby system shutdown")) {
                logger.warn(e.getMessage(), e);
            } else {
                logger.info("...done shutting down Derby.");
            }
        }
    }

    private boolean cleanDatabase(String databaseType, String dbUrl) throws ClassNotFoundException, SQLException {
        boolean shouldCreateTables = true;
        logger.info("Check and clean database for type: {}", databaseType);
        try {
            logger.info("driver: {}", dbProps.getProperty(JAHIA_DATABASE_DRIVER));
            logger.info("url: {}", jahiaConfig.getDatabaseUrl());
            logger.info("user: {}", jahiaConfig.getDatabaseUsername());

            if (databaseType.contains(DERBY) && !dbUrl.contains("create=true")) {
                if (jahiaConfig.getOverwritedb().equals(IF_NECESSARY)) {
                    // Check database existence
                    logger.info("Checking if Derby database exists (overwrite=if-necessary)");
                    try {
                        db.databaseOpen(dbProps.getProperty(JAHIA_DATABASE_DRIVER), dbUrl, jahiaConfig.getDatabaseUsername(), jahiaConfig.getDatabasePassword());
                        logger.info("Derby database exists, skipping table creation");
                        return false;
                    } catch (SQLException sqlException) {
                        logger.debug("Derby database doesn't exist, will create tables", sqlException);
                        // Database doesn't exist
                    }
                }
                // Append create=true to recreate derby db
                dbUrl = dbUrl + ";create=true";
                db.databaseOpen(dbProps.getProperty(JAHIA_DATABASE_DRIVER), dbUrl, jahiaConfig.getDatabaseUsername(), jahiaConfig.getDatabasePassword());
            } else if (databaseType.equals(MYSQL) || databaseType.equals(MARIADB) || databaseType.equals(POSTGRESQL)) {
                URI dbSubURI = URI.create(dbUrl.substring(5)); // strip "jdbc:"
                String databaseName = dbSubURI.getPath().substring(1); // strip starting "/"
                logger.info("Database name: {}", databaseName);

                String emptyUrl = null;
                String defaultPath = "/";
                if (POSTGRESQL.equals(databaseType)) {
                    defaultPath = "/postgres";
                }
                if (dbSubURI.getPort() != -1) {
                    emptyUrl = new URI(dbSubURI.getScheme(), null, dbSubURI.getHost(), dbSubURI.getPort(), defaultPath, null, dbSubURI.getFragment()).toString();
                } else {
                    emptyUrl = new URI(dbSubURI.getScheme(), dbSubURI.getHost(), defaultPath, dbSubURI.getFragment()).toString();
                }

                db.databaseOpen(dbProps.getProperty(JAHIA_DATABASE_DRIVER), "jdbc:" + emptyUrl, jahiaConfig.getDatabaseUsername(), jahiaConfig.getDatabasePassword());

                if (jahiaConfig.getOverwritedb().equals(IF_NECESSARY) && exists(databaseName)) {
                    logger.info("Database '{}' already exists", databaseName);

                    try {
                        logger.info("Checking if tables exist in database '{}'", databaseName);
                        db.databaseOpen(dbProps.getProperty(JAHIA_DATABASE_DRIVER), dbUrl, jahiaConfig.getDatabaseUsername(), jahiaConfig.getDatabasePassword());
                        db.getStatement().execute("select count(*) from JR_DEFAULT_BINVAL");
                        // Tables are already there
                        logger.info("Tables already exist in database, skipping creation");
                        shouldCreateTables = false;
                    } catch (SQLException sqlException) {
                        logger.debug("Tables don't exist in database, will create them", sqlException);
                        // Table does not exist
                    }
                } else {
                    logger.info("Database '{}' needs to be recreated", databaseName);
                    cleanDatabase(databaseName);
                }
            }
        } catch (Exception t) {
            logger.error("Error when cleaning database: {}", t.getMessage(), t);
        }
        db.databaseClose();
        return shouldCreateTables;
    }

    private boolean exists(String dbName) throws SQLException {
        ResultSet rs;
        String dbType = jahiaConfig.getDatabaseType();
        logger.info("Checking if database '{}' exists for type {}", dbName, dbType);

        switch (dbType) {
            case MYSQL:
            case MARIADB:
                logger.debug("Listing MySQL/MariaDB catalogs");
                rs = db.getConnection().getMetaData().getCatalogs();
                break;
            case POSTGRESQL:
                logger.debug("Querying PostgreSQL databases");
                rs = db.getStatement().executeQuery("SELECT datname FROM pg_catalog.pg_database");
                break;
            default:
                logger.debug("Unsupported database type for exists check: {}", dbType);
                return false;
        }

        while (rs.next()) {
            if (dbName.equals(rs.getString(1))) {
                logger.info("Database '{}' found", dbName);
                return true;
            }
        }

        logger.info("Database '{}' not found", dbName);
        return false;
    }

    private void cleanDatabase(String databaseName) throws SQLException {
        String dbType = jahiaConfig.getDatabaseType();
        logger.info("Cleaning database '{}' for type {}", databaseName, dbType);

        switch (dbType) {
            case MYSQL:
            case MARIADB:
                logger.info("Dropping MySQL/MariaDB database if exists: {}", databaseName);
                db.query("drop database if exists `" + databaseName + "`");

                logger.info("Creating MySQL/MariaDB database: {}", databaseName);
                db.query("create database `" + databaseName + "`");

                logger.info("Setting MySQL/MariaDB database charset to utf8");
                db.query("alter database `" + databaseName + "` charset utf8");
                break;

            case POSTGRESQL:
                logger.info("Dropping PostgreSQL database if exists: {}", databaseName);
                db.query("drop database if exists \"" + databaseName + "\"");

                logger.info("Creating PostgreSQL database: {}", databaseName);
                db.query("create database \"" + databaseName + "\"");
                break;

            default:
                logger.warn("Unsupported database type for cleaning: {}", dbType);
        }
        logger.info("Database '{}' cleaned successfully", databaseName);
    }

    private void copyExternalizedConfig() throws IOException {
        logger.info("Starting to copy externalized configuration");
        if (jahiaConfig.isExternalizedConfigExploded()) {

            File target = new File(jahiaConfig.getExternalizedConfigTargetPath());
            final File targetCfgDir = new File(target, "jahia");
            final File srcDir = new File(jahiaConfigDir, "jahia");
            logger.info("Copying configuration from {} to {}", srcDir, targetCfgDir);

            if (targetCfgDir.isDirectory()) {
                logger.info("Target configuration directory already exists");
                File jahiaPropsFile = new File(targetCfgDir, "jahia.properties");
                if (jahiaPropsFile.exists()) {
                    logger.info("Found existing jahia.properties file at {}", jahiaPropsFile);
                    Properties p = new Properties();
                    try (FileInputStream fis = new FileInputStream(jahiaPropsFile)) {
                        p.load(fis);
                    }
                    // TODO: this seem's buggy, the comparison is not correct mariadb and mariad.script are always different
                    // TODO: in case jahia.properties already exists it will always be removed (not touching this for now)
                    if (p.containsKey(DB_SCRIPT) && !jahiaConfig.getDatabaseType().equals(p.getProperty(DB_SCRIPT))
                            || !p.containsKey(DB_SCRIPT) && !jahiaConfig.getDatabaseType().equals(DERBY_EMBEDDED)) {
                        logger.info("Deleting existing {} file as the target database type has changed", jahiaPropsFile);
                        Files.deleteIfExists(jahiaPropsFile.toPath());
                    }
                }

                logger.debug("Walking file tree to copy configuration files (skipping existing files)");
                // we won't overwrite existing files
                Files.walkFileTree(srcDir.toPath(), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        Path targetDir = targetCfgDir.toPath().resolve(srcDir.toPath().relativize(dir));
                        if (!Files.exists(targetDir)) {
                            logger.debug("Creating directory: {}", targetDir);
                            Files.createDirectories(targetDir);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path targetFile = targetCfgDir.toPath().resolve(srcDir.toPath().relativize(file));
                        if (!Files.exists(targetFile)) {
                            logger.debug("Copying file {} to {}", file.getFileName(), targetFile);
                            Files.copy(file, targetFile, StandardCopyOption.COPY_ATTRIBUTES);
                        } else {
                            logger.debug("Skipping existing file: {}", targetFile);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                // Create target directory first
                logger.info("Target directory doesn't exist, creating: {}", target);
                Files.createDirectories(target.toPath());

                logger.debug("Walking file tree to copy all configuration files");
                // Copy the source directory into the target directory
                Files.walkFileTree(srcDir.toPath(), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        Path targetDir = target.toPath().resolve(srcDir.getName()).resolve(srcDir.toPath().relativize(dir));
                        logger.debug("Creating directory: {}", targetDir);
                        Files.createDirectories(targetDir);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path targetFile = target.toPath().resolve(srcDir.getName()).resolve(srcDir.toPath().relativize(file));
                        logger.debug("Copying file {} to {}", file.getFileName(), targetFile);
                        Files.copy(file, targetFile, StandardCopyOption.COPY_ATTRIBUTES);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }

        // Delete the temporary directory
        logger.debug("Cleaning up temporary directory: {}", jahiaConfigDir);
        Files.walkFileTree(jahiaConfigDir.toPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                logger.trace("Deleting temporary file: {}", file);
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                logger.trace("Deleting temporary directory: {}", dir);
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });

        logger.info("Externalized configuration copy completed");
    }

    //copy method for the license for instance
    private void copyLicense(String fromFileName, String toFileName)
            throws IOException {
        logger.info("Copying license from {} to {}", fromFileName, toFileName);
        File fromFile = new File(fromFileName);
        File toFile = new File(toFileName);

        if (toFile.exists()) {
            logger.info("Target license file already exists, skipping copy");
            return;
        }

        if (!fromFile.exists()) {
            logger.warn("Source license file does not exist: {}", fromFileName);
            return;
        }
        if (!fromFile.isFile()) {
            logger.error("Source license is not a file: {}", fromFileName);
            throw new IOException("FileCopy: can't copy directory: " + fromFileName);
        }
        if (!fromFile.canRead()) {
            logger.error("Source license file is not readable: {}", fromFileName);
            throw new IOException("FileCopy: source file is unreadable: " + fromFileName);
        }

        if (toFile.isDirectory()) {
            toFile = new File(toFile, fromFile.getName());
            logger.info("Target is a directory, using file name: {}", toFile);
        }

        logger.debug("Copying license file with REPLACE_EXISTING option");
        Files.copy(fromFile.toPath(), toFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        logger.info("License file copied successfully");
    }

    private void createDBTables(File dbScript) throws Exception {
        logger.info("Creating database tables from script: {}", dbScript.getName());
        List<String> sqlStatements;

        // get script runtime...
        sqlStatements = DatabaseScripts.getSchemaSQL(dbScript);
        logger.info("Loaded {} SQL statements from database script", sqlStatements.size());

        logger.info("Executing SQL statements to create tables");
        org.jahia.commons.DatabaseScripts.executeStatements(sqlStatements, db.getConnection());
        logger.info("Database tables created successfully");
    }


    public static String encryptPassword(String password) {
        return EncryptionUtils.pbkdf2Digest(password, true);
    }

    public static JahiaConfigInterface getConfiguration(File configFile) throws IOException, IllegalAccessException,
                                                                                                   InvocationTargetException {
        logger.info("Loading configuration from file: {}", configFile);
        JahiaConfigBean config = new JahiaConfigBean();
        Properties props = null;
        if (configFile != null) {
            try (FileInputStream is = new FileInputStream(configFile)) {
                props = new Properties();
                props.load(is);
            }
        }

        if (props != null && !props.isEmpty()) {
            Map<String, String> map = (Map) props;
            new BeanUtilsBean(CONVERTER_UTILS_BEAN, new PropertyUtilsBean()).populate(config, map);
        }

        if (props != null) {
            props.put("databasePassword", "***");
            props.put("jahiaRootPassword", "***");
        }
        logger.info("Loaded configuration: {}", props);
        return config;
    }

    private File getDataDir() {
        if (dataDir == null) {
            dataDir = resolveDataDir(jahiaConfig.getJahiaVarDiskPath());
            logger.info("Data directory resolved to folder: {}", dataDir);
        }

        return dataDir;
    }
}
