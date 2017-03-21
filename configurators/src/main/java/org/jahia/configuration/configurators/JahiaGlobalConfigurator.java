/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
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
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.logging.Logger;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Global Jahia configuration utility.
 *
 * @author loom
 *         Date: May 11, 2010
 *         Time: 11:10:23 AM
 */
public class JahiaGlobalConfigurator {

    private static final ConvertUtilsBean CONVERTER_UTILS_BEAN = new ConvertUtilsBean();
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
        Map<String, String> values = new HashMap<String, String>();
        try {
            JSONObject obj = new JSONObject(json.contains("{") ? StringUtils.replace(json, "\\", "\\\\")
                    : "{" + StringUtils.replace(json, "\\", "\\\\") + "}");
            for (Iterator<?> iterator = obj.keys(); iterator.hasNext();) {
                String key = (String) iterator.next();
                values.put(key, obj.getString(key));
            }
        } catch (JSONException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        return values;
    }

    public static List<String> fromString(String value) {
        List<String> valueList = new LinkedList<String>();
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
            Map<String, String> sysProps = new HashMap<String, String>();
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
        if (doCreate && !dataDir.exists()) {
            if (!dataDir.mkdirs()) {
                throw new RuntimeException("Unable to create target directory: " + dataDir);
            }
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

        db = new DatabaseConnection();

        getLogger().info ("Configuring for server " + jahiaConfig.getTargetServerType() + (StringUtils.isNotEmpty(jahiaConfig.getTargetServerVersion()) ? (" version " + jahiaConfig.getTargetServerVersion()) : "") + " with database type " + jahiaConfig.getDatabaseType());

        try {
            setProperties();
        } finally {
            VFSConfigFile.closeAllOpened();
        }
    }

    private void deployOnCluster() {
        //jahiaPropertiesBean.setClusterNodes(clusterNodes);
        //jahiaPropertiesBean.setProcessingServer(processingServer);
    }

    private void cleanDatabase() {
        //if it is a mysql, try to drop the database and create a new one  your user must have full rights on this database
        int begindatabasename = jahiaConfig.getDatabaseUrl().indexOf("/", 13);
        int enddatabaseName = jahiaConfig.getDatabaseUrl().indexOf("?", begindatabasename);
        String databaseName = "`" + jahiaConfig.getDatabaseUrl().substring(begindatabasename + 1, enddatabaseName) + "`";  //because of the last '/' we added +1
        try {

            db.query("drop  database if exists " + databaseName);
            db.query("create database " + databaseName);
            db.query("alter database " + databaseName + " charset utf8");
        } catch (Throwable t) {
            // ignore because if this fails it's ok
            getLogger().info("error in " + databaseName + " because of" + t);
        }
    }

    private void updateConfigurationFiles(String sourceWebAppPath, String webappPath, Properties dbProps, JahiaConfigInterface jahiaConfigInterface) throws Exception {
        getLogger().info("Configuring file using source " + sourceWebAppPath + " to target " + webappPath);

        FileSystemManager fsManager = VFS.getManager();

        new JackrabbitConfigurator(dbProps, jahiaConfigInterface, getLogger()).updateConfiguration(new VFSConfigFile(fsManager,sourceWebAppPath + "/WEB-INF/etc/repository/jackrabbit/repository.xml"), webappPath + "/WEB-INF/etc/repository/jackrabbit/repository.xml");
        new TomcatContextXmlConfigurator(dbProps, jahiaConfigInterface).updateConfiguration(new VFSConfigFile(fsManager,sourceWebAppPath + "/META-INF/context.xml"), webappPath + "/META-INF/context.xml");
        
        String rootUserTemplate = sourceWebAppPath + "/WEB-INF/etc/repository/template-root-user.xml";
        FileObject rootUserTemplateFile = fsManager.resolveFile(rootUserTemplate);
        if (rootUserTemplateFile.exists()) {
            if (Boolean.valueOf(jahiaConfigInterface.getProcessingServer())) {
                new RootUserConfigurator(dbProps, jahiaConfigInterface, encryptPassword(jahiaConfigInterface.getJahiaRootPassword())).updateConfiguration(new VFSConfigFile(fsManager, rootUserTemplate), webappPath + "/WEB-INF/etc/repository/root-user.xml");
            }
        } else {
            new RootUserConfigurator(dbProps, jahiaConfigInterface, encryptPassword(jahiaConfigInterface.getJahiaRootPassword())).updateConfiguration(new VFSConfigFile(fsManager,sourceWebAppPath + "/WEB-INF/etc/repository/root.xml"), webappPath + "/WEB-INF/etc/repository/root.xml");
        }
        
        String mailServerTemplate = sourceWebAppPath + "/WEB-INF/etc/repository/template-root-mail-server.xml";
        if (fsManager.resolveFile(mailServerTemplate).exists() && Boolean.valueOf(jahiaConfigInterface.getProcessingServer())) {
            new MailServerConfigurator(dbProps, jahiaConfigInterface).updateConfiguration(new VFSConfigFile(fsManager,mailServerTemplate), webappPath + "/WEB-INF/etc/repository/root-mail-server.xml");
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
        
        new JahiaPropertiesConfigurator(dbProps, jahiaConfigInterface).updateConfiguration (jahiaPropertiesConfigFile, targetConfigPath + "/" + jahiaPropertiesFileName);

        try {
            ConfigFile jahiaNodePropertiesConfigFile = readJahiaNodeProperties(sourceWebAppPath, fsManager);
            if (jahiaNodePropertiesConfigFile != null) {
                new JahiaNodePropertiesConfigurator(logger, jahiaConfigInterface).updateConfiguration (jahiaNodePropertiesConfigFile, targetConfigPath + "/" + jahiaNodePropertiesFileName);
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

        String ldapTargetFile = new File(getDataDir(), "modules").getAbsolutePath();
        new LDAPConfigurator(dbProps, jahiaConfigInterface).updateConfiguration(new VFSConfigFile(fsManager,sourceWebAppPath), ldapTargetFile);

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
        URL jarUrl = null;
        FileObject jahiaEEImplFileObject = findVFSFile(sourceWebAppPath + "/WEB-INF/lib", "jahia\\-ee\\-impl.*\\.jar");
        if (jahiaEEImplFileObject != null) {
            jarUrl = jahiaEEImplFileObject.getURL();
        } else {
            jarUrl = this.getClass().getClassLoader().getResource("jahia-default-config.jar");
        }
        if (jarUrl != null) {
            return new VFSConfigFile(fsManager.resolveFile("jar:" + jarUrl.toExternalForm()),
                    "org/jahia/defaults/config/properties/jahia.node.properties");
        }
        return null;
    }

    private ConfigFile readJahiaProperties(String sourceWebAppPath, FileSystemManager fsManager) throws IOException {
        ConfigFile cfg = null;

        // Locate the Jar file
        FileObject jahiaImplFileObject = findVFSFile(sourceWebAppPath + "/WEB-INF/lib", "jahia\\-impl\\-.*\\.jar");
        URL jahiaDefaultConfigJARURL = this.getClass().getClassLoader().getResource("jahia-default-config.jar");
        if (jahiaImplFileObject != null) {
            jahiaDefaultConfigJARURL = jahiaImplFileObject.getURL();
        }
        VFSConfigFile jahiaPropertiesConfigFile = null;
        VFSConfigFile jahiaAdvancedPropertiesConfigFile = null;

        try {
            jahiaPropertiesConfigFile = new VFSConfigFile(fsManager.resolveFile("jar:"
                    + jahiaDefaultConfigJARURL.toExternalForm()),
                    "org/jahia/defaults/config/properties/jahia.properties");
            cfg = jahiaPropertiesConfigFile;

            FileObject jahiaEEImplFileObject = findVFSFile(sourceWebAppPath + "/WEB-INF/lib",
                    "jahia\\-ee\\-impl.*\\.jar");
            if (jahiaEEImplFileObject != null) {
                jahiaDefaultConfigJARURL = jahiaEEImplFileObject.getURL();
            }
            try {
                jahiaAdvancedPropertiesConfigFile = new VFSConfigFile(fsManager.resolveFile("jar:"
                        + jahiaDefaultConfigJARURL.toExternalForm()),
                        "org/jahia/defaults/config/properties/jahia.advanced.properties");
                if (jahiaAdvancedPropertiesConfigFile != null) {
                    InputStream is1 = jahiaPropertiesConfigFile.getInputStream();
                    InputStream is2 = jahiaAdvancedPropertiesConfigFile.getInputStream();
                    try {
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
                    } finally {
                        IOUtils.closeQuietly(is1);
                        IOUtils.closeQuietly(is2);
                    }
                }
            } catch (FileSystemException fse) {
                // in the case we cannot access the file, it means we should not do the advanced configuration, which is expected for
                // Jahia "core".
            }
        } finally {
            if (jahiaPropertiesConfigFile != null) {
                jahiaPropertiesConfigFile.close();
            }
            if (jahiaAdvancedPropertiesConfigFile != null) {
                jahiaAdvancedPropertiesConfigFile.close();
            }
        }

        return cfg;
    }

    private void updateForJBoss(Properties dbProps, JahiaConfigInterface jahiaConfigInterface,
            FileSystemManager fsManager) throws Exception, FileSystemException, IOException {
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

        if (jahiaConfig.getCluster_activated().equals("true")) {
            getLogger().info(" Deploying in cluster for server in " + webappDir);
            deployOnCluster();
        } else {
            getLogger().info("Deployed in standalone for server in " + webappDir);
        }
        
        String dbUrl = jahiaConfig.getDatabaseUrl();
        boolean isEmbeddedDerby = jahiaConfig.getDatabaseType().equals("derby_embedded");
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
        databaseScript = new File(getDataDir(), "db/" + jahiaConfig.getDatabaseType() + ".script");
        FileInputStream is = null;
        try {
            is = new FileInputStream(databaseScript);
            dbProps.load(is);
            // we override these just as the configuration wizard does
            dbProps.put("storeFilesInDB", jahiaConfig.getStoreFilesInDB());
            dbProps.put("fileDataStorePath", jahiaConfig.getFileDataStorePath() != null ? jahiaConfig.getFileDataStorePath() : "");
            dbProps.put("jahia.database.url", dbUrl);
            dbProps.put("jahia.database.user", jahiaConfig.getDatabaseUsername());
            dbProps.put("jahia.database.pass", jahiaConfig.getDatabasePassword());
        } catch (IOException e) {
            getLogger().error("Error in loading database settings because of " + e);
        } finally {
            IOUtils.closeQuietly(is);
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
            if (jahiaConfig.getOverwritedb().equals("true")) {
                getLogger().info("Creating database tables for " + jahiaConfig.getDatabaseType() + "...");
                getLogger().info("driver: " + dbProps.getProperty("jahia.database.driver"));
                getLogger().info("url: " + jahiaConfig.getDatabaseUrl());
                getLogger().info("user: " + jahiaConfig.getDatabaseUsername());
                if (!databaseScript.exists()) {
                    getLogger().info("cannot find script in " + databaseScript.getPath());
                    throw new Exception("Cannot find script for database " + jahiaConfig.getDatabaseType());
                }
                if (jahiaConfig.getDatabaseType().contains("derby") && !dbUrl.contains("create=true")) {
                    dbUrl = dbUrl + ";create=true";
                }
                db.databaseOpen(dbProps.getProperty("jahia.database.driver"), dbUrl, jahiaConfig.getDatabaseUsername(), jahiaConfig.getDatabasePassword());
                if (jahiaConfig.getDatabaseType().equals("mysql")) {
                    getLogger().info("database is mysql trying to drop it and create a new one");
                    cleanDatabase();
                    //you have to reopen the database connection as before you just dropped the database
                    db.databaseOpen(dbProps.getProperty("jahia.database.driver"), jahiaConfig.getDatabaseUrl(), jahiaConfig.getDatabaseUsername(), jahiaConfig.getDatabasePassword());
                }
                createDBTables(databaseScript);
                
                if (isEmbeddedDerby) {
                    // shutdown embedded Derby
                    getLogger().info("Shutting down embedded Derby...");
                    try {
                        DriverManager.getConnection("jdbc:derby:;shutdown=true",
                                jahiaConfig.getDatabaseUsername(), jahiaConfig.getDatabasePassword());
                    } catch (Exception e) {
                        if (!(e instanceof SQLException) || e.getMessage() == null
                                || !e.getMessage().contains("Derby system shutdown")) {
                            logger.warn(e.getMessage(), e);
                        } else {
                            getLogger().info("...done shutting down Derby.");
                        }
                    }
                }
            }

            deleteRepositoryAndIndexes();
            if ("tomcat".equals(jahiaConfig.getTargetServerType())) {
                deleteTomcatFiles();
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
                    if (p.containsKey("db_script") && !jahiaConfig.getDatabaseType().equals(p.getProperty("db_script"))
                            || !p.containsKey("db_script") && !jahiaConfig.getDatabaseType().equals("derby_embedded")) {
                        getLogger()
                                .info("Deleting existing " + jahiaPropsFile
                                        + " file as the target database type has changed");
                        jahiaPropsFile.delete();
                    }
                }
                // we won't overwrite existing files
                FileUtils.copyDirectory(srcDir, targetCfgDir, new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        if (!pathname.isFile()) {
                            return true;
                        }
                        return !new File(targetCfgDir, pathname.getAbsolutePath().substring(
                                srcDir.getAbsolutePath().length())).exists();
                    }
                });
            } else {
                FileUtils.copyDirectoryToDirectory(srcDir, target);
            }
        } else {
            boolean verbose = true;
            JarArchiver archiver = new JarArchiver();
            if (verbose) {
                archiver.enableLogging(new org.codehaus.plexus.logging.console.ConsoleLogger(Logger.LEVEL_DEBUG,
                        "console"));
            }
   
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
            // archiver.setManifest(targetManifestFile);
            archiver.setDestFile(targetFile);
            String excludes = null;
   
            archiver.addDirectory(jahiaConfigDir, null,
                    excludes != null ? excludes.split(",") : null);
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
                    .listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name == null || !(name.startsWith("indexing_configuration") && name.endsWith(".xml"));
                        }
                    });
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

        if (!fromFile.exists())
            return;
//            throw new IOException("FileCopy: " + "no such source file: "
//                    + fromFileName);
        if (!fromFile.isFile())
            throw new IOException("FileCopy: " + "can't copy directory: "
                    + fromFileName);
        if (!fromFile.canRead())
            throw new IOException("FileCopy: " + "source file is unreadable: "
                    + fromFileName);

        if (toFile.isDirectory()) {
            toFile = new File(toFile, fromFile.getName());

        }

        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = from.read(buffer)) != -1)
                to.write(buffer, 0, bytesRead); // write
        } finally {
            if (from != null)
                try {
                    from.close();
                } catch (IOException e) {
                }
            if (to != null)
                try {
                    to.close();
                } catch (IOException e) {
                }
        }
    }


    private void copy(String fromFileName, String toFileName)
            throws IOException {
        File fromFile = new File(fromFileName);
        File toFile = new File(toFileName);

        if (!fromFile.exists())
            throw new IOException("FileCopy: " + "no such source file: "
                    + fromFileName);
        if (!fromFile.isFile())
            throw new IOException("FileCopy: " + "can't copy directory: "
                    + fromFileName);
        if (!fromFile.canRead())
            throw new IOException("FileCopy: " + "source file is unreadable: "
                    + fromFileName);
        toFile.mkdir();
        if (toFile.isDirectory()) {
            toFile = new File(toFile, fromFile.getName());

        }

        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = from.read(buffer)) != -1)
                to.write(buffer, 0, bytesRead); // write
        } finally {
            if (from != null)
                try {
                    from.close();
                } catch (IOException e) {
                }
            if (to != null)
                try {
                    to.close();
                } catch (IOException e) {
                }
        }
    }

    private void createDBTables(File dbScript) throws Exception {

        List<String> sqlStatements;

// get script runtime...
        try {
            sqlStatements = DatabaseScripts.getSchemaSQL(dbScript);
        } catch (Exception e) {
            throw e;
        }
        
        org.jahia.commons.DatabaseScripts.executeStatements(sqlStatements, db.getConnection());
        
//// drop each tables (if present) and (re-)create it after...
//        for (String line : sqlStatements) {
//            final String lowerCaseLine = line.toLowerCase();
//            final int tableNamePos = lowerCaseLine.indexOf("create table");
//            if (tableNamePos != -1) {
//                final String tableName = line.substring("create table".length() +
//                        tableNamePos,
//                        line.indexOf("(")).trim();
////getLog().info("Creating table [" + tableName + "] ...");
//                try {
//                    db.query("DROP TABLE " + tableName);
//                } catch (Throwable t) {
//                    // ignore because if this fails it's ok
//                    getLogger().debug("Drop failed on " + tableName + " because of " + t + " but that's acceptable...");
//                }
//            }
//            try {
//                db.query(line);
//            } catch (Exception e) {
//                // first let's check if it is a DROP TABLE query, if it is,
//                // we will just fail silently.
//
//                String upperCaseLine = line.toUpperCase().trim();
//                String errorMsg = "Error while trying to execute query: " + line + ". Cause: " + e.getMessage();
//                if (upperCaseLine.startsWith("DROP ") || upperCaseLine.contains(" DROP ") || upperCaseLine.contains("\nDROP ") || upperCaseLine.contains(" DROP\n") || upperCaseLine.contains("\nDROP\n")) {
//                    getLogger().debug(errorMsg, e);
//                } else if (upperCaseLine.startsWith("ALTER TABLE") || upperCaseLine.startsWith("CREATE INDEX")){
//                    if (getLogger().isDebugEnabled()) {
//                        getLogger().warn(errorMsg, e);
//                    } else {
//                        getLogger().warn(errorMsg);
//                    }
//                } else {
//                    getLogger().error(errorMsg, e);
//                    throw e;
//                }
//            }
//        }
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
            FileInputStream is = null;
            try {
                is = new FileInputStream(configFile);
                props = new Properties();
                props.load(is);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
        if (props != null && !props.isEmpty()) {
            new BeanUtilsBean(CONVERTER_UTILS_BEAN, new PropertyUtilsBean()).populate(config, props);
        }
        if (logger != null) {
            props.put("databasePassword", "***");
            props.put("jahiaRootPassword", "***");
            props.put("jahiaToolManagerPassword", "***");
            props.put("mailServer", "***");
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
