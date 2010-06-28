package org.jahia.utils.maven.plugin.configurators;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;
import org.jahia.utils.maven.plugin.AbstractLogger;
import org.jahia.utils.maven.plugin.ConsoleLogger;
import org.jahia.utils.maven.plugin.deployers.ServerDeploymentFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Global Jahia configuration utility.
 *
 * @author loom
 *         Date: May 11, 2010
 *         Time: 11:10:23 AM
 */
public class JahiaGlobalConfigurator {

    JahiaConfigInterface jahiaConfig;
    DatabaseConnection db;
    File webappDir;
    Properties dbProps;
    File databaseScript;
    List<AbstractConfigurator> configurators = new ArrayList<AbstractConfigurator>();

    AbstractLogger logger;

    public JahiaGlobalConfigurator(AbstractLogger logger, JahiaConfigInterface jahiaConfig) {
        this.jahiaConfig = jahiaConfig;
        this.logger = logger;
    }

    public AbstractLogger getLogger() {
        return logger;
    }

    public void execute() throws Exception {
        ServerDeploymentFactory.setTargetServerDirectory(jahiaConfig.getTargetServerDirectory());
        if (jahiaConfig.getTargetConfigurationDirectory() == null) {
            jahiaConfig.setTargetConfigurationDirectory(jahiaConfig.getTargetServerDirectory());
        } else if (!jahiaConfig.getTargetConfigurationDirectory().equals(jahiaConfig.getTargetServerDirectory())) {
            // Configuration directory and target server directory are not equal, we will use the configuration
            // directory for the configurators.
            ServerDeploymentFactory.setTargetServerDirectory(jahiaConfig.getTargetConfigurationDirectory());
        }
        db = new DatabaseConnection();

        getLogger().info ("Configuring for server " + jahiaConfig.getTargetServerType() + " version " + jahiaConfig.getTargetServerVersion() + " with database type " + jahiaConfig.getDatabaseType());

        setProperties();

        if (jahiaConfig.getDatabaseType().equals("hypersonic")) {
            try {
                db.query("SHUTDOWN");
            } catch (Exception e) {
                e.printStackTrace();
                //
            }
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
        getLogger().info("Store files in database is :" + jahiaConfigInterface.getStoreFilesInDB());
        new SpringHibernateConfigurator(dbProps, jahiaConfigInterface).updateConfiguration(sourceWebAppPath + "/WEB-INF/etc/spring/applicationcontext-hibernate.xml", webappPath + "/WEB-INF/etc/spring/applicationcontext-hibernate.xml");
        new QuartzConfigurator(dbProps, jahiaConfigInterface).updateConfiguration(sourceWebAppPath + "/WEB-INF/etc/config/quartz.properties", webappPath + "/WEB-INF/etc/config/quartz.properties");
        new JackrabbitConfigurator(dbProps, jahiaConfigInterface).updateConfiguration(sourceWebAppPath + "/WEB-INF/etc/repository/jackrabbit/repository.xml", webappPath + "/WEB-INF/etc/repository/jackrabbit/repository.xml");
        new TomcatContextXmlConfigurator(dbProps, jahiaConfigInterface).updateConfiguration(sourceWebAppPath + "/META-INF/context.xml", webappPath + "/META-INF/context.xml");
        new RootUserConfigurator(dbProps, jahiaConfigInterface, encryptPassword(jahiaConfigInterface.getJahiaRootPassword())).updateConfiguration(sourceWebAppPath + "/WEB-INF/etc/repository/root.xml", webappPath + "/WEB-INF/etc/repository/root.xml");
        new WebXmlConfigurator(dbProps, jahiaConfigInterface).updateConfiguration(sourceWebAppPath + "/WEB-INF/web.xml", webappPath + "/WEB-INF/web.xml");
        new SpringServicesConfigurator(dbProps, jahiaConfigInterface).updateConfiguration(sourceWebAppPath + "/WEB-INF/etc/spring/applicationcontext-services.xml", webappPath + "/WEB-INF/etc/spring/applicationcontext-services.xml");
        if ("jboss".equalsIgnoreCase(jahiaConfigInterface.getTargetServerType())) {
            File datasourcePath = new File(jahiaConfigInterface.getTargetServerDirectory(), ServerDeploymentFactory.getInstance().getImplementation(jahiaConfigInterface.getTargetServerType() + jahiaConfigInterface.getTargetServerVersion()).getDeploymentFilePath("jahia-jboss-config.sar/jahia-ds", "xml"));
            if (datasourcePath.exists()) {
                new JBossDatasourceConfigurator(dbProps, jahiaConfigInterface).updateConfiguration(datasourcePath.getPath(), datasourcePath.getPath());
            }
            datasourcePath = new File(sourceWebAppPath, "../jahia-jboss-config/jahia-ds.xml");
            if (datasourcePath.exists()) {
                new JBossDatasourceConfigurator(dbProps, jahiaConfigInterface).updateConfiguration(datasourcePath.getPath(), datasourcePath.getPath());
            }
        }

        new IndexationPolicyConfigurator(dbProps, jahiaConfigInterface).updateConfiguration(sourceWebAppPath + "/WEB-INF/etc/spring/applicationcontext-indexationpolicy.xml", webappPath + "/WEB-INF/etc/spring/applicationcontext-indexationpolicy.xml");
        new JahiaPropertiesConfigurator(dbProps, jahiaConfigInterface).updateConfiguration (sourceWebAppPath + "/WEB-INF/etc/config/jahia.skeleton", webappPath + "/WEB-INF/etc/config/jahia.properties");

    }

    private void setProperties() throws Exception {


        //now set the common properties to both a clustered environment and a standalone one

        webappDir = getWebAppTargetConfigurationDir();
        String sourceWebappPath = webappDir.toString();
        if (jahiaConfig.isConfigureBeforePackaging()) {
            sourceWebappPath = jahiaConfig.getSourceWebAppDir();
            getLogger().info("Configuration before WAR packaging is active, will look for configuration files in directory " + sourceWebappPath + " and store the modified files in " + webappDir);
        }

        if (jahiaConfig.getCluster_activated().equals("true")) {
            getLogger().info(" Deploying in cluster for server in " + webappDir);
            deployOnCluster();
        } else {
            getLogger().info("Deployed in standalone for server in " + webappDir);
        }
        
        String dbUrl = jahiaConfig.getDatabaseUrl();
        if (jahiaConfig.getDatabaseType().equals("derby_embedded") && jahiaConfig.getDatabaseUrl().contains("$context")) {
            dbUrl = StringUtils.replace(dbUrl, "$context", StringUtils.replace(sourceWebappPath, "\\", "/"));
        }
        
        dbProps = new Properties();
        //database script always ends with a .script
        databaseScript = new File(sourceWebappPath + "/WEB-INF/var/db/" + jahiaConfig.getDatabaseType() + ".script");
        FileInputStream is = null;
        try {
            is = new FileInputStream(databaseScript);
            dbProps.load(is);
            // we override these just as the configuration wizard does
            dbProps.put("storeFilesInDB", jahiaConfig.getStoreFilesInDB());
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
        getLogger().info("creating database tables and copying license file");
        try {
            copyLicense(sourceWebappPath + "/WEB-INF/etc/config/licenses/license-free.xml", webappDir.getPath() + "/WEB-INF/etc/config/license.xml");
            if (jahiaConfig.getOverwritedb().equals("true")) {
                if (!databaseScript.exists()) {
                    getLogger().info("cannot find script in " + databaseScript.getPath());
                    throw new Exception("Cannot find script for database " + jahiaConfig.getDatabaseType());
                }
                db.databaseOpen(dbProps.getProperty("jahia.database.driver"), dbUrl, jahiaConfig.getDatabaseUsername(), jahiaConfig.getDatabasePassword());
                if (jahiaConfig.getDatabaseType().equals("mysql")) {
                    getLogger().info("database is mysql trying to drop it and create a new one");
                    cleanDatabase();
                    //you have to reopen the database connection as before you just dropped the database
                    db.databaseOpen(dbProps.getProperty("jahia.database.driver"), jahiaConfig.getDatabaseUrl(), jahiaConfig.getDatabaseUsername(), jahiaConfig.getDatabasePassword());
                }
                createDBTables(databaseScript);
                insertDBCustomContent();
            }

            if (!jahiaConfig.isConfigureBeforePackaging()) {
                deleteRepositoryAndIndexes();
                if ("tomcat".equals(jahiaConfig.getTargetServerType())) {
                    deleteTomcatFiles();
                }
                if (jahiaConfig.getSiteImportLocation() != null) {
                    getLogger().info("copying site Export to the " + webappDir + "/WEB-INF/var/imports");
                    importSites();
                } else {
                    getLogger().info("no site import found ");
                }
            }

        } catch (Exception e) {
            getLogger().error("exception in setting the properties because of " + e, e);
        }
    }

    private void importSites() {
        for (int i = 0; i < jahiaConfig.getSiteImportLocation().size(); i++) {
            try {
                copy(jahiaConfig.getSiteImportLocation().get(i), webappDir + "/WEB-INF/var/imports");
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

    private void deleteTomcatFiles() {

        cleanDirectory(new File(jahiaConfig.getTargetServerDirectory() + "/temp"));
        cleanDirectory(new File(jahiaConfig.getTargetServerDirectory() + "/work"));
        getLogger().info("Finished deleting content of Tomcat's "+jahiaConfig.getTargetServerDirectory() + "/temp and "+jahiaConfig.getTargetServerDirectory() + "/work folders");
    }

    private void deleteRepositoryAndIndexes() {

        try {
            File[] files = new File(webappDir + "/WEB-INF/var/repository")
                    .listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return !"indexing_configuration.xml".equals(name);
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

        cleanDirectory(new File(webappDir + "/WEB-INF/var/search_indexes"));

        File[] templateDirs = new File(jahiaConfig.getJahiaVersion() < 6.5 ?webappDir + "/templates":webappDir + "/modules")
                .listFiles(jahiaConfig.getJahiaVersion() < 6.5 ? (FilenameFilter) new AndFileFilter(
                        new NotFileFilter(new NameFileFilter("default")),
                        DirectoryFileFilter.DIRECTORY)
                        : DirectoryFileFilter.DIRECTORY);
        if (templateDirs != null) {
            for (File templateDir : templateDirs) {
                cleanDirectory(templateDir);
                templateDir.delete();
            }
        }

        getLogger().info("Finished deleting content of the " + webappDir + "/WEB-INF/var/repository and " + webappDir + "+/WEB-INF/var/search_indexes folders");
    }

    //copy method for the license for instance
    private void copyLicense(String fromFileName, String toFileName)
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

        DatabaseScripts scripts = new DatabaseScripts();

// get script runtime...
        try {
            sqlStatements = scripts.getSchemaSQL(dbScript);
        } catch (Exception e) {
            throw e;
        }
// drop each tables (if present) and (re-)create it after...
        for (String line : sqlStatements) {
            final String lowerCaseLine = line.toLowerCase();
            final int tableNamePos = lowerCaseLine.indexOf("create table");
            if (tableNamePos != -1) {
                final String tableName = line.substring("create table".length() +
                        tableNamePos,
                        line.indexOf("(")).trim();
//getLog().info("Creating table [" + tableName + "] ...");
                try {
                    db.query("DROP TABLE " + tableName);
                } catch (Throwable t) {
                    // ignore because if this fails it's ok
                    getLogger().debug("Drop failed on " + tableName + " because of " + t + " but that's acceptable...");
                }
            }
            try {
                db.query(line);
            } catch (Exception e) {
                // first let's check if it is a DROP TABLE query, if it is,
                // we will just fail silently.

                String upperCaseLine = line.toUpperCase().trim();
                String errorMsg = "Error while trying to execute query: " + line + ". Cause: " + e.getMessage();
                if (upperCaseLine.startsWith("DROP ") || upperCaseLine.contains(" DROP ") || upperCaseLine.contains("\nDROP ") || upperCaseLine.contains(" DROP\n") || upperCaseLine.contains("\nDROP\n")) {
                    getLogger().debug(errorMsg, e);
                } else if (upperCaseLine.startsWith("ALTER TABLE") || upperCaseLine.startsWith("CREATE INDEX")){
                    if (getLogger().isDebugEnabled()) {
                        getLogger().warn(errorMsg, e);
                    } else {
                        getLogger().warn(errorMsg);
                    }
                } else {
                    getLogger().error(errorMsg, e);
                    throw e;
                }
            }
        }


    }
// end createDBTables()


    /**
     * Insert database custom data, like root user and properties.
     */
    private void insertDBCustomContent() throws Exception {

        if (jahiaConfig.getJahiaVersion() >= 6.5) return;

        getLogger().debug("Inserting customized settings into database...");

// get two keys...
        final String rootName = "root";

        final String password = encryptPassword(jahiaConfig.getJahiaRootPassword());   //root1234
        getLogger().info("Encrypted root password for jahia is " + jahiaConfig.getJahiaRootPassword());
        final int siteID0 = 0;
        final String rootKey = rootName + ":" + siteID0;
        final String grpKey0 = "administrators" + ":" + siteID0;
        final String grpKey1 = "users" + ":" + siteID0;
        final String grpKey2 = "guest" + ":" + siteID0;

// query insert root user...
        db.queryPreparedStatement("INSERT INTO jahia_users(id_jahia_users, name_jahia_users, password_jahia_users, key_jahia_users) VALUES(0,?,?,?)",
                new Object[]{rootName, password, rootKey});

// query insert root first name...
        db.queryPreparedStatement("INSERT INTO jahia_user_prop(id_jahia_users, name_jahia_user_prop, value_jahia_user_prop, provider_jahia_user_prop, userkey_jahia_user_prop) VALUES(0, 'firstname', ?, 'jahia',?)",
                new Object[]{"root", rootKey});

// query insert root last name...
        db.queryPreparedStatement("INSERT INTO jahia_user_prop(id_jahia_users, name_jahia_user_prop, value_jahia_user_prop, provider_jahia_user_prop, userkey_jahia_user_prop) VALUES(0, 'lastname', ?, 'jahia',?)",
                new Object[]{"", rootKey});

// query insert root e-mail address...
        db.queryPreparedStatement("INSERT INTO jahia_user_prop(id_jahia_users, name_jahia_user_prop, value_jahia_user_prop, provider_jahia_user_prop, userkey_jahia_user_prop) VALUES(0, 'email', ?, 'jahia',?)",
                new Object[]{(String) "", rootKey});

// query insert administrators group...
        db.queryPreparedStatement("INSERT INTO jahia_grps(id_jahia_grps, name_jahia_grps, key_jahia_grps, siteid_jahia_grps) VALUES(0,?,?,null)",
                new Object[]{"administrators", grpKey0});

// query insert users group...
        db.queryPreparedStatement("INSERT INTO jahia_grps(id_jahia_grps, name_jahia_grps, key_jahia_grps, siteid_jahia_grps) VALUES(1,?,?,null)",
                new Object[]{"users", grpKey1});

// query insert guest group...
        db.queryPreparedStatement("INSERT INTO jahia_grps(id_jahia_grps, name_jahia_grps, key_jahia_grps, siteid_jahia_grps) VALUES(2,?,?,null)",
                new Object[]{"guest", grpKey2});


// query insert administrators group access...
        db.queryPreparedStatement("INSERT INTO jahia_grp_access(id_jahia_member, id_jahia_grps, membertype_grp_access) VALUES(?,?,1)",
                new Object[]{rootKey, grpKey0});

// create guest user
        db.queryPreparedStatement("INSERT INTO jahia_users(id_jahia_users, name_jahia_users, password_jahia_users, key_jahia_users) VALUES(1,?,?,?)",
                new Object[]{"guest", "*", "guest:0"});

//db.queryPreparedStatement("INSERT INTO jahia_version(install_number, build, release_number, install_date) VALUES(0, ?,?,?)",
//new Object[] { new Integer(Jahia.getBuildNumber()), Jahia.getReleaseNumber() + "." + Jahia.getPatchNumber(), new Timestamp(System.currentTimeMillis()) } );
    }
// end insertDBCustomContent()

    /**
         * Copy the external config
         *
         * @throws IOException
         * @return true if an external config has been found

         */
        private boolean copyExternalConfig()
                throws Exception {
            if (jahiaConfig.getExternalConfigPath() == null) {
                getLogger().info("External jahia config. not specified.");
                return false;

            }
            File externalConfigDirectory = new File(jahiaConfig.getExternalConfigPath());
            if (!externalConfigDirectory.exists()) {
                getLogger().warn("Not copying external jahia config. Directory[" + externalConfigDirectory.getAbsolutePath()
                        + "] does not exist!");
                return false;
            }

            getLogger().info("Copying external jahia config. directory [" + externalConfigDirectory.getAbsolutePath() + "] to [" + getWebappDeploymentDir().getAbsolutePath() + "]");
            String[] fileNames = getFilesToCopy(externalConfigDirectory);
            for (int i = 0; i < fileNames.length; i++) {
                copyFile(new File(externalConfigDirectory, fileNames[i]), new File(getWebappDeploymentDir(), fileNames[i]));
            }
            return true;
        }

        /**
         * Returns a list of filenames that should be copied
         * over to the destination directory.
         *
         * @param directory the parent diretory to be scanned
         * @return the array of filenames, relative to the sourceDir
         */
        private String[] getFilesToCopy(File directory) {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(directory);
            scanner.addDefaultExcludes();
            scanner.scan();
            return scanner.getIncludedFiles();
        }


        /**
         * Copy file from source to destination
         *
         * @param source
         * @param destination
         * @return
         * @throws IOException
         */
        private boolean copyFile(File source, File destination) {
            try {
                boolean doOverride = destination.exists();
                FileUtils.copyFile(source.getCanonicalFile(), destination);
                // preserve timestamp
                destination.setLastModified(source.lastModified());

                if (!doOverride) {
                    getLogger().debug(" + [" + source.getPath() + "] has been copied to [" + destination.getAbsolutePath()+"]");
                } else {
                    getLogger().debug(" o [" + destination.getAbsolutePath() + "] has been overrided by " + source.getPath());
                }
            } catch (Exception e) {
                getLogger().error(" + Unable to copy" + source.getPath(),e);

            }
            return true;
        }



    public String encryptPassword(String password) {
        if (password == null) {
            return null;
        }

        if (password.length() == 0) {
            return null;
        }

        String result = null;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            if (md != null) {
                md.reset();
                md.update(password.getBytes());
                result = new String(Base64.encodeBase64(md.digest()));
            }
            md = null;
        } catch (NoSuchAlgorithmException ex) {

            result = null;
        }

        return result;
    }

    protected String getWebappDeploymentDirName() {
        return jahiaConfig.getWebAppDirName() != null ? jahiaConfig.getWebAppDirName() : "jahia";
    }

    /**
     * Get the folder on the application server where the jahia webapp is unpacked
     */
    protected File getWebappDeploymentDir() throws Exception {
        return new File(jahiaConfig.getTargetServerDirectory(), ServerDeploymentFactory.getInstance()
                .getImplementation(jahiaConfig.getTargetServerType() + jahiaConfig.getTargetServerVersion()).getDeploymentDirPath(getWebappDeploymentDirName(), "war"));
    }

    /**
     * We override this method so that we can have, in the same project, both a targetServerDirectory and a
     * targetConfigurationDirectory that are not the same, so that we can configure in a directory, and then deploy
     * to another directory.
     * @return if the target server directory and configuration directory are the same, the targetServerDirectory value
     * is returned, otherwise the targetConfigurationDirectory value is return.
     * @throws Exception can be raised if there is a problem initializing the deployer classes.
     */
    public File getWebAppTargetConfigurationDir () throws Exception {
        if (jahiaConfig.getTargetServerDirectory().equals(jahiaConfig.getTargetConfigurationDirectory())) {
            return getWebappDeploymentDir();
        } else {
            return new File(jahiaConfig.getTargetConfigurationDirectory());
        }
    }

    public static void main(String[] args) {
        AbstractLogger logger = new ConsoleLogger(ConsoleLogger.LEVEL_INFO);
        logger.info("Started Jahia global configurator");
        try {
            new JahiaGlobalConfigurator(logger, getConfiguration(args.length > 0 ? new File(args[0]) : null)).execute();
        } catch (Exception e) {
            logger.error("Error during execution of a configurator. Cause: " + e.getMessage(), e);
        }
        logger.info("... finished job of Jahia global configurator.");
    }

    protected static JahiaConfigInterface getConfiguration(File configFile) throws IOException, IllegalAccessException,
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
            BeanUtils.populate(config, props);
        }
        return config;
    }
}
