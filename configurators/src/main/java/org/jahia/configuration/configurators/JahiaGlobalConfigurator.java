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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.PropertyUtils;
import org.codehaus.plexus.util.StringUtils;
import org.jahia.commons.encryption.EncryptionUtils;
import org.jahia.configuration.deployers.ServerDeploymentFactory;
import org.jahia.configuration.deployers.ServerDeploymentInterface;
import org.jahia.configuration.logging.AbstractLogger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Global Jahia configuration utility.
 *
 * @author loom
 * Date: May 11, 2010
 * Time: 11:10:23 AM
 */
public class JahiaGlobalConfigurator {

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

    public static File resolveDataDir(String dataDirPath, String targetWebappDirPath) {
        return resolveDataDir(dataDirPath, targetWebappDirPath, true);
    }

    public static File resolveDataDir(String dataDirPath, String targetWebappDirPath, boolean doCreate) {
        File dataDir = null;
        if (dataDirPath.indexOf('$') != -1) {
            Map<String, String> sysProps = new HashMap<>();
            String webappPath = targetWebappDirPath;
            sysProps.put("jahiaWebAppRoot", webappPath);
            if (dataDirPath.contains("$context")) {
                sysProps.put("context", webappPath);
            }
            for (Map.Entry<Object, Object> el : System.getProperties().entrySet()) {
                sysProps.put(String.valueOf(el.getKey()), String.valueOf(el.getValue()));
            }
            dataDirPath = StringUtils.interpolate(dataDirPath, sysProps);
        }
        try {
            dataDir = new File(dataDirPath).getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (doCreate && !dataDir.exists() && !dataDir.mkdirs()) {
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

    AbstractLogger logger;
    private ServerDeploymentInterface deployer;
    private File dataDir;

    public JahiaGlobalConfigurator(AbstractLogger logger, JahiaConfigInterface jahiaConfig) {
        this.jahiaConfig = jahiaConfig;
        this.logger = logger;
    }

    public AbstractLogger getLogger() {
        return logger;
    }

    public void execute() throws Exception {
        if (jahiaConfig.isExternalizedConfigActivated() &&
                !StringUtils.isBlank(jahiaConfig.getExternalizedConfigTargetPath())) {
            File tempDirectory = FileUtils.getTempDirectory();
            jahiaConfigDir = new File(tempDirectory, "jahia-config");
            File jahiaConfigConfigDir = new File(jahiaConfigDir, "jahia");
            jahiaConfigConfigDir.mkdirs();
            externalizedConfigTempPath = jahiaConfigConfigDir.getPath();
        }

        db = new DatabaseConnection(logger);

        getLogger().info("Configuring for server " + jahiaConfig.getTargetServerType() + (StringUtils.isNotEmpty(jahiaConfig.getTargetServerVersion()) ? (" version " + jahiaConfig.getTargetServerVersion()) : "") + " with database type " + jahiaConfig.getDatabaseType());

        try {
            setProperties();
        } finally {
            VFSConfigFile.closeAllOpened();
        }
    }

    private void updateConfigurationFiles(String sourceWebAppPath, String webappPath, Properties dbProps, JahiaConfigInterface jahiaConfigInterface) throws Exception {
        getLogger().info("Configuring file using source " + sourceWebAppPath + " to target " + webappPath);

        FileSystemManager fsManager = VFS.getManager();

        new JackrabbitConfigurator(dbProps, jahiaConfigInterface, getLogger()).updateConfiguration(new VFSConfigFile(fsManager, sourceWebAppPath + "/WEB-INF/etc/repository/jackrabbit/repository.xml"), webappPath + "/WEB-INF/etc/repository/jackrabbit/repository.xml");
        new TomcatContextXmlConfigurator(dbProps, jahiaConfigInterface).updateConfiguration(new VFSConfigFile(fsManager, sourceWebAppPath + "/META-INF/context.xml"), webappPath + "/META-INF/context.xml");

        String rootUserTemplate = sourceWebAppPath + "/WEB-INF/etc/repository/template-root-user.xml";
        FileObject rootUserTemplateFile = fsManager.resolveFile(rootUserTemplate);
        if (rootUserTemplateFile.exists()) {
            if (Boolean.parseBoolean(jahiaConfigInterface.getProcessingServer())) {
                new RootUserConfigurator(dbProps, jahiaConfigInterface, encryptPassword(jahiaConfigInterface.getJahiaRootPassword())).updateConfiguration(new VFSConfigFile(fsManager, rootUserTemplate), webappPath + "/WEB-INF/etc/repository/root-user.xml");
            }
        } else {
            new RootUserConfigurator(dbProps, jahiaConfigInterface, encryptPassword(jahiaConfigInterface.getJahiaRootPassword())).updateConfiguration(new VFSConfigFile(fsManager, sourceWebAppPath + "/WEB-INF/etc/repository/root.xml"), webappPath + "/WEB-INF/etc/repository/root.xml");
        }

        String mailServerTemplate = sourceWebAppPath + "/WEB-INF/etc/repository/template-root-mail-server.xml";
        if (fsManager.resolveFile(mailServerTemplate).exists() && Boolean.parseBoolean(jahiaConfigInterface.getProcessingServer())) {
            new MailServerConfigurator(dbProps, jahiaConfigInterface).updateConfiguration(new VFSConfigFile(fsManager, mailServerTemplate), webappPath + "/WEB-INF/etc/repository/root-mail-server.xml");
        }
        if ("jboss".equalsIgnoreCase(jahiaConfigInterface.getTargetServerType())) {
            updateForJBoss(dbProps, jahiaConfigInterface, fsManager);
        }

        String targetConfigPath = webappPath + "/WEB-INF/etc/config";
        String jahiaPropertiesFileName = "jahia.properties";
        String jahiaNodePropertiesFileName = "jahia.node.properties";
        if (externalizedConfigTempPath != null) {
            targetConfigPath = externalizedConfigTempPath;
            if (!StringUtils.isBlank(jahiaConfigInterface.getExternalizedConfigClassifier())) {
                jahiaPropertiesFileName = "jahia." + jahiaConfigInterface.getExternalizedConfigClassifier() + ".properties";
                jahiaNodePropertiesFileName = "jahia.node." + jahiaConfigInterface.getExternalizedConfigClassifier() + ".properties";
            }
        }

        ConfigFile jahiaPropertiesConfigFile = readJahiaProperties(sourceWebAppPath, fsManager);

        new JahiaPropertiesConfigurator(dbProps, jahiaConfigInterface).updateConfiguration(jahiaPropertiesConfigFile, targetConfigPath + "/" + jahiaPropertiesFileName);

        try {
            ConfigFile jahiaNodePropertiesConfigFile = readJahiaNodeProperties(sourceWebAppPath, fsManager);
            if (jahiaNodePropertiesConfigFile != null) {
                new JahiaNodePropertiesConfigurator(logger, jahiaConfigInterface).updateConfiguration(jahiaNodePropertiesConfigFile, targetConfigPath + "/" + jahiaNodePropertiesFileName);
            }
        } catch (FileSystemException fse) {
            // in the case we cannot access the file, it means we should not do the advanced configuration, which is expected for community edition.
        }

        // create empty Spring config file for custom configuration
        InputStream is = getClass().getResourceAsStream("/applicationcontext-custom.xml");
        FileOutputStream os = new FileOutputStream(new File(targetConfigPath, "applicationcontext-custom.xml"));
        try {
            IOUtils.copy(is, os);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }

        String ldapTargetFile = new File(getDataDir(), "karaf/etc").getAbsolutePath();
        new LDAPConfigurator(dbProps, jahiaConfigInterface).updateConfiguration(new VFSConfigFile(fsManager, sourceWebAppPath), ldapTargetFile);

        String jeeApplicationLocation = jahiaConfigInterface.getJeeApplicationLocation();
        boolean jeeLocationSpecified = !StringUtils.isEmpty(jeeApplicationLocation);
        if (jeeLocationSpecified || getDeployer().isEarDeployment()) {
            if (!jeeLocationSpecified) {
                jeeApplicationLocation = getDeployer().getDeploymentFilePath("digitalfactory", "ear").getAbsolutePath();
            }
            String jeeApplicationModuleList = jahiaConfigInterface.getJeeApplicationModuleList();
            if (StringUtils.isEmpty(jeeApplicationModuleList)) {
                jeeApplicationModuleList = "ROOT".equals(jahiaConfigInterface.getWebAppDirName()) ? "jahia-war:web:jahia.war:"
                        : ("jahia-war:web:jahia.war:" + jahiaConfigInterface.getWebAppDirName());
            }
            new ApplicationXmlConfigurator(jahiaConfigInterface, jeeApplicationModuleList).updateConfiguration(
                    new VFSConfigFile(fsManager, jeeApplicationLocation + "/META-INF/application.xml"),
                    jeeApplicationLocation + "/META-INF/application.xml");
        }
    }

    private ConfigFile readJahiaNodeProperties(String sourceWebAppPath, FileSystemManager fsManager)
            throws FileSystemException {
        VFSConfigFile file = null;
        FileObject jahiaImplFileObject = findVFSFile(sourceWebAppPath + "/WEB-INF/lib", "jahia\\-impl\\-.*\\.jar");
        FileObject jahiaEEImplFileObject = findVFSFile(sourceWebAppPath + "/WEB-INF/lib", "jahia\\-ee\\-impl.*\\.jar");
        if (jahiaEEImplFileObject != null) {
            file = getFileInJar(fsManager, jahiaEEImplFileObject.getURL(), "org/jahia/defaults/config/properties/jahia.node.properties");
        }
        if (jahiaImplFileObject != null && file == null) {
            file = getFileInJar(fsManager, jahiaImplFileObject.getURL(), "org/jahia/defaults/config/properties/jahia.node.properties");
        }
        if (file == null) {
            file = getFileInJar(fsManager, this.getClass().getClassLoader().getResource("jahia-default-config.jar"), "org/jahia/defaults/config/properties/jahia.node.properties");
        }

        return file;
    }

    private VFSConfigFile getFileInJar(FileSystemManager fsManager, URL jarUrl, String path) {
        try {
            VFSConfigFile vfsConfigFile = new VFSConfigFile(fsManager.resolveFile("jar:" + jarUrl.toExternalForm()), path);
            vfsConfigFile.getInputStream();
            return vfsConfigFile;
        } catch (FileSystemException e) {
            return null;
        }
    }

    private ConfigFile readJahiaProperties(String sourceWebAppPath, FileSystemManager fsManager) throws IOException {
        ConfigFile cfg = null;

        // Locate the Jar file
        FileObject jahiaImplFileObject = findVFSFile(sourceWebAppPath + "/WEB-INF/lib", "jahia\\-impl\\-.*\\.jar");
        URL jahiaDefaultConfigJARURL = this.getClass().getClassLoader().getResource("jahia-default-config.jar");
        if (jahiaImplFileObject != null) {
            jahiaDefaultConfigJARURL = jahiaImplFileObject.getURL();
        }

        try (VFSConfigFile jahiaPropertiesConfigFile = new VFSConfigFile(fsManager.resolveFile("jar:" + jahiaDefaultConfigJARURL.toExternalForm()),
                                                                         "org/jahia/defaults/config/properties/jahia.properties")) {
            cfg = jahiaPropertiesConfigFile;

            FileObject jahiaEEImplFileObject = findVFSFile(sourceWebAppPath + "/WEB-INF/lib",
                                                           "jahia\\-ee\\-impl.*\\.jar");
            if (jahiaEEImplFileObject != null) {
                jahiaDefaultConfigJARURL = jahiaEEImplFileObject.getURL();
            }
            try (VFSConfigFile jahiaAdvancedPropertiesConfigFile = new VFSConfigFile(fsManager.resolveFile("jar:" + jahiaDefaultConfigJARURL.toExternalForm()),
                                                                                     "org/jahia/defaults/config/properties/jahia.advanced.properties");
                 InputStream is1 = jahiaPropertiesConfigFile.getInputStream();
                 InputStream is2 = jahiaAdvancedPropertiesConfigFile.getInputStream()
            ) {
                final String content = IOUtils.toString(is1) + "\n" + IOUtils.toString(is2);
                cfg = new ConfigFile() {
                    @Override
                    public URI getURI() throws IOException, URISyntaxException {
                        return null;
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return IOUtils.toInputStream(content);
                    }
                };
            } catch (FileSystemException fse) {
                // in the case we cannot access the file, it means we should not do the advanced configuration, which is expected for
                // Jahia "core".
            }
        }

        return cfg;
    }

    private void updateForJBoss(
            Properties dbProps, JahiaConfigInterface jahiaConfigInterface,
            FileSystemManager fsManager
    ) throws Exception, FileSystemException, IOException {
        JBossConfigurator configurator = new JBossConfigurator(dbProps, jahiaConfigInterface, getDeployer(),
                                                               getLogger());
        File datasourcePath = new File(jahiaConfigInterface.getTargetServerDirectory(),
                                       "standalone/configuration/standalone.xml");
        if (datasourcePath.exists()) {
            configurator.updateConfiguration(new VFSConfigFile(fsManager, datasourcePath.getPath()),
                                             datasourcePath.getPath());
        } else {
            // check if there is a fragment file perhaps
            File fragmentFile = new File(jahiaConfigInterface.getTargetServerDirectory(),
                                         "standalone/configuration/standalone.xml.fragment");
            if (fragmentFile.exists()) {
                configurator.updateConfiguration(new VFSConfigFile(fsManager, fragmentFile.getPath()),
                                                 fragmentFile.getPath());
            }

            // check if we need to update the CLI configuration file
            File cliFile = new File(jahiaConfigInterface.getTargetServerDirectory(), "bin/jahia-config.cli");
            if (cliFile.exists()) {
                configurator.writeCLIConfiguration(cliFile, null);
                configurator.writeCLIConfiguration(new File(jahiaConfigInterface.getTargetServerDirectory(), "bin/jahia-config-domain.cli"), "default");
            }
        }
        configurator.updateDriverModule();
    }

    public FileObject findVFSFile(String parentPath, String fileMatchingPattern) {
        Pattern matchingPattern = Pattern.compile(fileMatchingPattern);
        try {
            FileSystemManager fsManager = VFS.getManager();
            FileObject parentFileObject = fsManager.resolveFile(parentPath);
            FileObject[] children = parentFileObject.getChildren();
            for (FileObject child : children) {
                Matcher matcher = matchingPattern.matcher(child.getName().getBaseName());
                if (matcher.matches()) {
                    return child;
                }
            }
        } catch (FileSystemException e) {
            logger.debug("Couldn't find file matching pattern " + fileMatchingPattern + " at path " + parentPath);
        }
        return null;
    }

    private void setProperties() throws Exception {

        //now set the common properties to both a clustered environment and a standalone one

        webappDir = getWebappDeploymentDir();
        String sourceWebappPath = webappDir.toString();
        String databaseType = jahiaConfig.getDatabaseType();

        getLogger().info("Deployed in standalone for server in " + webappDir);

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
            getLogger().error("Error in loading database settings because of " + e);
        }

        getLogger().info("Updating configuration files...");

        //updates jackrabbit, quartz and spring files
        updateConfigurationFiles(sourceWebappPath, webappDir.getPath(), dbProps, jahiaConfig);
        getLogger().info("Copying license file...");
        String targetConfigPath = webappDir.getPath() + "/WEB-INF/etc/config";
        if (externalizedConfigTempPath != null) {
            targetConfigPath = externalizedConfigTempPath;
        }
        try {
            String existingLicense = jahiaConfig.getLicenseFile();
            copyLicense(existingLicense != null && existingLicense.length() > 0 ? existingLicense : sourceWebappPath
                    + "/WEB-INF/etc/config/licenses/license-free.xml", targetConfigPath + "/license.xml");
            if (jahiaConfig.getOverwritedb().equals("true") || jahiaConfig.getOverwritedb().equals(IF_NECESSARY)) {
                getLogger().info("driver: " + dbProps.getProperty(JAHIA_DATABASE_DRIVER));
                getLogger().info("url: " + jahiaConfig.getDatabaseUrl());
                getLogger().info("user: " + jahiaConfig.getDatabaseUsername());
                if (!databaseScript.exists()) {
                    getLogger().info("cannot find script in " + databaseScript.getPath());
                    throw new Exception("Cannot find script for database " + databaseType);
                }

                if (cleanDatabase(databaseType, dbUrl)) {
                    getLogger().info("Creating tables");
                    db.databaseOpen(dbProps.getProperty(JAHIA_DATABASE_DRIVER), dbUrl, jahiaConfig.getDatabaseUsername(), jahiaConfig.getDatabasePassword());
                    createDBTables(databaseScript);
                }

                if (isEmbeddedDerby) {
                    shutdownDerby();
                }
            }

            if (jahiaConfig.getDeleteFiles().equals("true")) {
                deleteRepositoryAndIndexes();
                if ("tomcat".equals(jahiaConfig.getTargetServerType())) {
                    deleteTomcatFiles();
                }
            }
            if (jahiaConfig.getSiteImportLocation() != null) {
                File importsFolder = new File(getDataDir(), "imports");
                getLogger().info("Copying site Export to the " + importsFolder);
                copyImports(importsFolder.getAbsolutePath());
            } else {
                getLogger().info("No site import found, no import needed.");
            }

            if ((jahiaConfigDir != null) && (externalizedConfigTempPath != null)) {
                copyExternalizedConfig();
            }

        } catch (Exception e) {
            getLogger().error("exception in setting the properties because of " + e, e);
        }
    }

    private void shutdownDerby() {
        // Shutdown embedded Derby
        getLogger().info("Shutting down embedded Derby...");
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true", jahiaConfig.getDatabaseUsername(), jahiaConfig.getDatabasePassword());
        } catch (Exception e) {
            if (!(e instanceof SQLException) || e.getMessage() == null
                    || !e.getMessage().contains("Derby system shutdown")) {
                logger.warn(e.getMessage(), e);
            } else {
                getLogger().info("...done shutting down Derby.");
            }
        }
    }

    private boolean cleanDatabase(String databaseType, String dbUrl) throws ClassNotFoundException, SQLException {
        boolean shouldCreateTables = true;
        try {
            if (databaseType.contains(DERBY) && !dbUrl.contains("create=true")) {
                if (jahiaConfig.getOverwritedb().equals(IF_NECESSARY)) {
                    // Check database existence
                    try {
                        db.databaseOpen(dbProps.getProperty(JAHIA_DATABASE_DRIVER), dbUrl, jahiaConfig.getDatabaseUsername(), jahiaConfig.getDatabasePassword());
                        return false;
                    } catch (SQLException sqlException) {
                        // Database exist
                    }
                }
                // Append create=true to recreate derby db
                dbUrl = dbUrl + ";create=true";
                db.databaseOpen(dbProps.getProperty(JAHIA_DATABASE_DRIVER), dbUrl, jahiaConfig.getDatabaseUsername(), jahiaConfig.getDatabasePassword());
            } else if (databaseType.equals(MYSQL) || databaseType.equals(MARIADB) || databaseType.equals(POSTGRESQL)) {
                URI dbSubURI = URI.create(dbUrl.substring(5)); // strip "jdbc:"
                String databaseName = dbSubURI.getPath().substring(1); // strip starting "/"
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
                    getLogger().info("Database already exist");

                    try {
                        db.databaseOpen(dbProps.getProperty(JAHIA_DATABASE_DRIVER), dbUrl, jahiaConfig.getDatabaseUsername(), jahiaConfig.getDatabasePassword());
                        db.getStatement().execute("select count(*) from JR_DEFAULT_BINVAL");
                        // Tables are already there
                        shouldCreateTables = false;
                    } catch (SQLException sqlException) {
                        // Table does not exist
                    }
                } else {
                    getLogger().info("Database is " + databaseType + " trying to drop it and create a new one");
                    cleanDatabase(databaseName);
                }
            }
        } catch (Exception t) {
            // ignore because if this fails it's ok
            getLogger().error("Error when recreating db", t);
        }
        db.databaseClose();
        return shouldCreateTables;
    }

    private boolean exists(String dbName) throws SQLException {
        ResultSet rs;
        switch (jahiaConfig.getDatabaseType()) {
            case MYSQL:
            case MARIADB:
                rs = db.getConnection().getMetaData().getCatalogs();
                break;
            case POSTGRESQL:
                rs = db.getStatement().executeQuery("SELECT datname FROM pg_catalog.pg_database");
                break;
            default:
                return false;
        }
        while (rs.next()) {
            if (dbName.equals(rs.getString(1))) {
                return true;
            }
        }

        return false;
    }

    private void cleanDatabase(String databaseName) throws SQLException {
        switch (jahiaConfig.getDatabaseType()) {
            case MYSQL:
            case MARIADB:
                db.query("drop database if exists `" + databaseName + "`");
                db.query("create database `" + databaseName + "`");
                db.query("alter database `" + databaseName + "` charset utf8");
                break;
            case POSTGRESQL:
                db.query("drop database if exists \"" + databaseName + "\"");
                db.query("create database \"" + databaseName + "\"");
            default:
        }
    }


    private void copyExternalizedConfig() throws IOException, ArchiverException {
        if (jahiaConfig.isExternalizedConfigExploded()) {
            // we copy configuration to folder without archiving it

            File target = new File(jahiaConfig.getExternalizedConfigTargetPath());
            final File targetCfgDir = new File(target, "jahia");
            final File srcDir = new File(jahiaConfigDir, "jahia");
            if (targetCfgDir.isDirectory()) {
                File jahiaPropsFile = new File(targetCfgDir, "jahia.properties");
                if (jahiaPropsFile.exists()) {
                    Properties p = PropertyUtils.loadProperties(jahiaPropsFile);
                    if (p.containsKey(DB_SCRIPT) && !jahiaConfig.getDatabaseType().equals(p.getProperty(DB_SCRIPT))
                            || !p.containsKey(DB_SCRIPT) && !jahiaConfig.getDatabaseType().equals(DERBY_EMBEDDED)) {
                        getLogger().info("Deleting existing " + jahiaPropsFile + " file as the target database type has changed");
                        jahiaPropsFile.delete();
                    }
                }
                // we won't overwrite existing files
                FileUtils.copyDirectory(srcDir, targetCfgDir, pathname -> !pathname.isFile() ||
                        !new File(targetCfgDir, pathname.getAbsolutePath().substring(srcDir.getAbsolutePath().length())).exists()
                );
            } else {
                FileUtils.copyDirectoryToDirectory(srcDir, target);
            }
        } else {
            JarArchiver archiver = new JarArchiver();

            String jarFileName = "jahia-config.jar";
            if (!StringUtils.isBlank(jahiaConfig.getExternalizedConfigFinalName())) {
                jarFileName = jahiaConfig.getExternalizedConfigFinalName();
                if (!StringUtils.isBlank(jahiaConfig.getExternalizedConfigClassifier())) {
                    jarFileName += "-" + jahiaConfig.getExternalizedConfigClassifier();
                }
                jarFileName += ".jar";
            }

            // let's generate the WAR file
            File targetFile = new File(jahiaConfig.getExternalizedConfigTargetPath(), jarFileName);
            archiver.setDestFile(targetFile);
            archiver.addDirectory(jahiaConfigDir, null, null);
            archiver.createArchive();
        }

        FileUtils.deleteDirectory(jahiaConfigDir);
    }

    private void copyImports(String importsFolder) {
        for (int i = 0; i < jahiaConfig.getSiteImportLocation().size(); i++) {
            try {
                copy(jahiaConfig.getSiteImportLocation().get(i), importsFolder);
            } catch (IOException e) {
                getLogger().error("error in copying siteImport file " + e);
            }
        }
    }


    private void cleanDirectory(File toDelete) {
        if (toDelete.exists()) {
            try {
                FileUtils.cleanDirectory(toDelete);
            } catch (IOException e) {
                getLogger().error(
                        "Error deleting content of the folder '" + toDelete
                                + "'. Cause: " + e.getMessage(), e);
            }
        }
    }

    private void deleteDirectory(File toDelete) {
        if (toDelete.exists()) {
            try {
                FileUtils.deleteDirectory(toDelete);
            } catch (IOException e) {
                getLogger().error(
                        "Error deleting content of the folder '" + toDelete
                                + "'. Cause: " + e.getMessage(), e);
            }
        }
    }

    private void deleteTomcatFiles() {

        File toDelete1 = new File(jahiaConfig.getTargetServerDirectory() + "/temp");
        cleanDirectory(toDelete1);
        File toDelete2 = new File(jahiaConfig.getTargetServerDirectory() + "/work");
        cleanDirectory(toDelete2);
        getLogger().info("Finished deleting content of Tomcat's " + toDelete1 + " and " + toDelete2 + " folders");
    }

    private void deleteRepositoryAndIndexes() {

        try {
            File[] files = new File(getDataDir(), "repository")
                    .listFiles((File dir, String name) -> name == null || !(name.startsWith("indexing_configuration") && name.endsWith(".xml")));
            if (files != null) {
                for (File file : files) {
                    FileUtils.forceDelete(file);
                }
            }
        } catch (IOException e) {
            getLogger().error(
                    "Error deleting content of the Jahia's /repository folder. Cause: "
                            + e.getMessage(), e);
        }

        deleteDirectory(new File(getDataDir(), "bundles-deployed"));
        deleteDirectory(new File(getDataDir(), "compiledRules"));
        deleteDirectory(new File(getDataDir(), "content"));
        deleteDirectory(new File(getDataDir(), "generated-resources"));
        deleteDirectory(new File(new File(getDataDir(), "karaf"), "instances"));
        cleanDirectory(new File(new File(getDataDir(), "karaf"), "data"));
        cleanDirectory(new File(new File(getDataDir(), "karaf"), "deploy"));

        getLogger().info("Finished deleting content of the data and cache related folders");
    }

    //copy method for the license for instance
    private void copyLicense(String fromFileName, String toFileName)
            throws IOException {
        File fromFile = new File(fromFileName);
        File toFile = new File(toFileName);

        if (toFile.exists() && !jahiaConfig.getDeleteFiles().equals("true")) {
            return;
        }

        if (!fromFile.exists()) {
            return;
        }
        if (!fromFile.isFile()) {
            throw new IOException("FileCopy: can't copy directory: " + fromFileName);
        }
        if (!fromFile.canRead()) {
            throw new IOException("FileCopy: source file is unreadable: " + fromFileName);
        }

        if (toFile.isDirectory()) {
            toFile = new File(toFile, fromFile.getName());
        }

        try (FileInputStream from = new FileInputStream(fromFile); FileOutputStream to = new FileOutputStream(toFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytesRead); // write
            }
        }
    }


    private void copy(String fromFileName, String toFileName)
            throws IOException {
        File fromFile = new File(fromFileName);
        File toFile = new File(toFileName);

        if (!fromFile.exists()) {
            throw new IOException("FileCopy: no such source file: " + fromFileName);
        }
        if (!fromFile.isFile()) {
            throw new IOException("FileCopy: can't copy directory: " + fromFileName);
        }
        if (!fromFile.canRead()) {
            throw new IOException("FileCopy: source file is unreadable: " + fromFileName);
        }
        toFile.mkdir();

        if (toFile.isDirectory()) {
            toFile = new File(toFile, fromFile.getName());
        }

        try (FileInputStream from = new FileInputStream(fromFile); FileOutputStream to = new FileOutputStream(toFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytesRead); // write
            }
        }
    }

    private void createDBTables(File dbScript) throws Exception {
        List<String> sqlStatements;

        // get script runtime...
        sqlStatements = DatabaseScripts.getSchemaSQL(dbScript);
        org.jahia.commons.DatabaseScripts.executeStatements(sqlStatements, db.getConnection());
    }


    public static String encryptPassword(String password) {
        return EncryptionUtils.pbkdf2Digest(password, true);
    }

    protected String getWebappDeploymentDirName() {
        return jahiaConfig.getWebAppDirName() != null ? jahiaConfig.getWebAppDirName() : "jahia";
    }

    /**
     * Get the folder on the application server where the jahia webapp is unpacked
     */
    protected File getWebappDeploymentDir() {
        if (StringUtils.isNotEmpty(jahiaConfig.getTargetConfigurationDirectory())) {
            return new File(jahiaConfig.getTargetConfigurationDirectory());
        }
        String jeeApplicationLocation = jahiaConfig.getJeeApplicationLocation();
        if (!StringUtils.isEmpty(jeeApplicationLocation)) {
            return new File(jeeApplicationLocation, "jahia.war");
        } else {
            return getDeployer().getDeploymentDirPath(
                    StringUtils.defaultString(getDeployer()
                                                      .getWebappDeploymentDirNameOverride(),
                                              getWebappDeploymentDirName()), "war");
        }
    }

    private ServerDeploymentInterface getDeployer() {
        if (deployer == null) {
            deployer = ServerDeploymentFactory.getImplementation(
                    jahiaConfig.getTargetServerType(),
                    jahiaConfig.getTargetServerVersion(),
                    new File(jahiaConfig.getTargetServerDirectory()), null,
                    null);
        }

        return deployer;
    }

    public static JahiaConfigInterface getConfiguration(File configFile, AbstractLogger logger) throws IOException, IllegalAccessException,
                                                                                                       InvocationTargetException {
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
        if (logger != null) {
            if (props != null) {
                props.put("databasePassword", "***");
                props.put("jahiaRootPassword", "***");
                props.put("jahiaToolManagerPassword", "***");
                props.put("mailServer", "***");
            }
            logger.info("Loaded configuration from file " + configFile + ":\n" + props);
        }
        return config;
    }

    private File getDataDir() {
        if (dataDir == null) {
            dataDir = resolveDataDir(jahiaConfig.getJahiaVarDiskPath(),
                                     getWebappDeploymentDir().getAbsolutePath());
            getLogger().info("Data directory resolved to folder: " + dataDir);
        }

        return dataDir;
    }

}
