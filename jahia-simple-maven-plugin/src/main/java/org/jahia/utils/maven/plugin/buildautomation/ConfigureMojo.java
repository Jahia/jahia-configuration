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
package org.jahia.utils.maven.plugin.buildautomation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jahia.configuration.configurators.JahiaConfigInterface;
import org.jahia.utils.maven.plugin.AbstractManagementMojo;
import org.jahia.utils.maven.plugin.MojoLogger;
import org.jahia.configuration.configurators.JahiaGlobalConfigurator;

/**
 * Configure the deployed Jahia instance
 * Basically set everything that can be found in the jahia.properties and jahia.advance.properties files.
 *
 * @goal configure
 * @requiresDependencyResolution runtime
 * @requiresProject false
 */
public class ConfigureMojo extends AbstractManagementMojo implements JahiaConfigInterface {

    /**
     * @parameter default-value="${jahia.data.dir}/modules/"
     */
    protected String jahiaModulesDiskPath;
    /**
     * properties file path
     *
     * @parameter default-value="http://127.0.0.1:8080/manager/html/"
     */
    protected String jahiaWebAppsDeployerBaseURL;
    /**
     * properties file path
     *
     * @parameter expression="${jahia.configure.cluster_activated}" default-value="false"
     */
    protected String cluster_activated;
    /**
     * properties file path
     *
     * @parameter expression="${jahia.configure.cluster_node_serverId}"
     */
    protected String cluster_node_serverId;

    /**
     * properties file path
     *
     * @parameter expression="${jahia.configure.clusterHazelcastBindPort}"
     */
    protected String clusterHazelcastBindPort;

    /**
     * @parameter expression="${jahia.configure.jahiaRootUsername}" default-value="root"
     */
    protected String jahiaRootUsername;
    /**
     * properties jahiaRootPassword
     *
     * @parameter expression="${jahia.configure.jahiaRootPassword}" default-value="root1234"
     */
    protected String jahiaRootPassword;
    /**
     * @parameter expression="${jahia.configure.jahiaRootPreferredLang}" default-value="en"
     */
    protected String jahiaRootPreferredLang;
    /**
     * @parameter expression="${jahia.configure.jahiaRootFirstname}" default-value=""
     */
    protected String jahiaRootFirstname;
    /**
     * @parameter expression="${jahia.configure.jahiaRootLastname}" default-value=""
     */
    protected String jahiaRootLastname;
    /**
     * @parameter expression="${jahia.configure.jahiaRootEmail}" default-value=""
     */
    protected String jahiaRootEmail;
    /**
     * properties file path
     *
     * @parameter expression="${jahia.configure.processingServer}" default-value="true"
     */
    protected String processingServer;
    /**
     * properties file path
     *
     * @parameter default-value="${jahia.data.dir}/imports/"
     */
    protected String jahiaImportsDiskPath;

    /**
     * The TCP bind address to start server socket on
     *
     * @parameter expression="${jahia.configure.clusterTCPBindAddress}"
     */
    protected String clusterTCPBindAddress;
    
    /**
     * The TCP port to bind to
     *
     * @parameter expression="${jahia.configure.clusterTCPBindPort}"
     */
    protected String clusterTCPBindPort;
    
    /**
     * Database type used
     *
     * @parameter expression="${jahia.configure.databaseType}" default-value="derby_embedded"
     */
    protected String databaseType;

    /**
     * URL to connect to the database
     *
     * @parameter expression="${jahia.configure.databaseUrl}" default-value="jdbc:derby:directory:jahia"
     */
    protected String databaseUrl;

    /**
     * List of nodes in the cluster.
     *
     * @parameter expression="${jahia.configure.databaseUsername}" default-value=""
     */
    protected String databaseUsername;

    /**
     * List of nodes in the cluster.
     *
     * @parameter expression="${jahia.configure.databasePassword}" default-value=""
     */
    protected String databasePassword;

    /**
     * @parameter
     */
    protected List<String> siteImportLocation;

    /**
     * @parameter expression="${jahia.configure.overwritedb}" default-value="true"
     * <p/>
     * This property is here for instance when we are in a clustered mode we don not want the database scripts to be
     * executed for every node
     */
    protected String overwritedb;

    /**
     * @parameter expression="${jahia.configure.deleteFiles}" default-value="true"
     *
     * Should we delete existing files
     */
    protected String deleteFiles;

    /**
     * properties storeFilesInDB
     *
     * @parameter expression="${jahia.configure.storeFilesInDB}" default-value="false"
     */
    protected String storeFilesInDB;

    /**
     * properties storeFilesInDB
     *
     * @parameter expression="${jahia.configure.storeFilesInAWS}" default-value="false"
     */
    protected String storeFilesInAWS;

    /**
     * The directory that will be used to store the configured Jahia in if the target server directory has to be overridden.
     * 
     * @parameter expression="${jahia.deploy.targetConfigurationDirectory}"
     */
    protected String targetConfigurationDirectory;

    /**
     * @parameter expression="${jahia.configure.mailServer}" default-value=""
     */
    protected String mailServer;
    
    /**
     * @parameter expression="${jahia.configure.mailAdministrator}" default-value=""
     */
    protected String mailAdministrator;
    
    /**
     * @parameter expression="${jahia.configure.mailFrom}" default-value=""
     */
    protected String mailFrom;
    
    /**
     * @parameter expression="${jahia.configure.mailParanoia}" default-value="Disabled"
     */
    protected String mailParanoia;
    
    /**
     * @parameter expression="${jahia.configure.toolManagerUsername}" default-value="jahia"
     */
    protected String jahiaToolManagerUsername;

    /**
     * @parameter expression="${jahia.configure.toolManagerPassword}" default-value="password"
     */
    protected String jahiaToolManagerPassword;

    /**
     * @parameter expression="${jahia.configure.ldapActivated}" default-value="false"
     */
    protected String ldapActivated;

    /**
     * @parameter expression="${jahia.configure.groupLdapProviderProperties}"
     */
    protected Map<String, String> groupLdapProviderProperties;

    /**
     * @parameter expression="${jahia.configure.userLdapProviderProperties}"
     */
    protected Map<String, String> userLdapProviderProperties;

    /**
     * @parameter expression="${jahia.configure.operatingMode}" default-value="development"
     */
    protected String operatingMode;


    /**
     * Activates configuration externalization. Make sure to specify a value for the externalizedConfigTargetPath that
     * will indicate where the configuration JAR should be generated.
     *
     * @parameter expression="${jahia.configure.externalizedActivated}" default-value="true"
     */
    protected boolean externalizedConfigActivated;
    
    /**
     * If active, the externalized configuration is deployed exploded.
     * 
     * @parameter expression="${jahia.configure.externalizedExploded}" default-value="true"
     */
    protected boolean externalizedConfigExploded;

    /**
     * The location at which the externalized configuration JAR will be generated.
     * @parameter expression="${jahia.configure.externalizedTargetPath}"
     */
    protected String externalizedConfigTargetPath;

    /**
     * Allows to specify a classifier on the configuration, usually used to identify cluster node configurations, such
     * as jahiaNode1, jahiaNode2, etc...
     * @parameter expression="${jahia.configure.externalizedClassifier}" default-value=""
     */
    protected String externalizedConfigClassifier;

    /**
     * The name of the JAR file (without the extension)
     *
     * @parameter expression="${jahia.configure.externalizedFinalName}" default-value="jahia-config"
     */
    protected String externalizedConfigFinalName;

    /**
     * The location of a JEE application to be configured for deployment in EAR format. If this is
     * null or empty this means we are not using EAR deployment for Jahia but WAR deployment.
     *
     * @parameter expression="${jahia.configure.jeeApplicationLocation}" default-value=""
     */
    protected String jeeApplicationLocation;

    /**
     * The list of modules to be setup in the JEE application.xml deployment descriptor.
     * List is comma separated, and each module has the following format:
     * type:id:arg1:arg2:...
     *
     * The arguments are different for each module type. Usually it is just a relative URI to the location of a JAR
     * or a SAR/RAR but in the case of a web module it is a bit different.
     *
     * For a WAR, the format is:
     *
     * web:webid:weburi:contextroot
     *
     * which will then become in the xml:
     *
     * <module id="webid">
     *     <web>
     *         <web-uri>weburi</web-uri>
     *         <context-root>contextroot</context-root>
     *     </web>
     * </module>
     *
     * The ID is an identifier used to name the module so that we can rewrite the XML more easily, and keep existing
     * structure should they exist already.
     *
     * @parameter expression="${jahia.configure.jeeApplicationModuleList}" default-value=""
     */
    protected String jeeApplicationModuleList;

    /**
     * A filesystem path to the folder, where Jackrabbit FileDataStore puts the binary content.
     *
     * @parameter expression="${jahia.configure.fileDataStorePath}" default-value=""
     */
    protected String fileDataStorePath;

    /**
     * @parameter expression="${jahia.configure.jahiaAdvancedProperties}"
     */
    protected Map<String, String> jahiaAdvancedProperties;

    /**
     * @parameter expression="${jahia.configure.jahiaProperties}"
     */
    protected Map<String, String> jahiaProperties;

    /**
     * Path to an existing license file to be used by Jahia instead of a default trial one.
     *
     * @parameter expression="${jahia.configure.licenseFile}"
     */
    private String licenseFile;


    public void doExecute() throws MojoExecutionException, MojoFailureException {
        try {
            new JahiaGlobalConfigurator(new MojoLogger(getLog()), this).execute();
        } catch (Exception e) {
            throw new MojoExecutionException("Error while configuring Jahia", e);
        }
    }

    public String getCluster_activated() {
        return cluster_activated;
    }

    public String getCluster_node_serverId() {
        return cluster_node_serverId;
    }

    public String getClusterHazelcastBindPort() {
        return clusterHazelcastBindPort;
    }

    public String getClusterTCPBindAddress() {
        return clusterTCPBindAddress;
    }

    public String getClusterTCPBindPort() {
        return clusterTCPBindPort;
    }

    public String getDatabasePassword() {
        return StringUtils.defaultString(databasePassword);
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public String getDatabaseUsername() {
        return StringUtils.defaultString(databaseUsername);
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public String getTargetServerDirectory() {
        return targetServerDirectory;
    }

    public String getTargetServerType() {
        return targetServerType;
    }

    public String getTargetServerVersion() {
        return targetServerVersion;
    }

    public String getWebAppDirName() {
        return webAppDirName;
    }

    public String getJahiaImportsDiskPath() {
        return jahiaImportsDiskPath;
    }

    public String getJahiaWebAppsDeployerBaseURL() {
        return jahiaWebAppsDeployerBaseURL;
    }

    public String getJahiaRootPassword() {
        return jahiaRootPassword;
    }

    public String getDb_script() {
        return getDatabaseType() + ".script";
    }

    public String getOverwritedb() {
        return overwritedb;
    }

    public String getDeleteFiles() {
        return deleteFiles;
    }

    public String getProcessingServer() {
        return processingServer;
    }

    public List<String> getSiteImportLocation() {
        return siteImportLocation;
    }

    public String getStoreFilesInDB() {
        return storeFilesInDB;
    }

    public String getStoreFilesInAWS() {
        return storeFilesInAWS;
    }

    public String getTargetConfigurationDirectory() {
        return targetConfigurationDirectory;
    }

    public void setTargetConfigurationDirectory(String targetConfigurationDirectory) {
        this.targetConfigurationDirectory = targetConfigurationDirectory;
    }

    public String getJahiaRootEmail() {
        return jahiaRootEmail;
    }

    public String getJahiaRootFirstname() {
        return jahiaRootFirstname;
    }

    public String getJahiaRootLastname() {
        return jahiaRootLastname;
    }

    public String getJahiaRootUsername() {
        return jahiaRootUsername;
    }

    public String getMailAdministrator() {
        return mailAdministrator;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public String getMailParanoia() {
        return mailParanoia;
    }

    public String getMailServer() {
        return mailServer;
    }

    public String getJahiaModulesDiskPath() {
        return jahiaModulesDiskPath;
    }

    public String getJahiaToolManagerUsername() {
        return jahiaToolManagerUsername;
    }

    public String getJahiaToolManagerPassword() {
        return jahiaToolManagerPassword;
    }

    public String getLdapActivated() {
        return ldapActivated;
    }

    public Map<String, String> getGroupLdapProviderProperties() {
        return groupLdapProviderProperties;
    }

    public Map<String, String> getUserLdapProviderProperties() {
        return userLdapProviderProperties;
    }

    public void setGroupLdapProviderProperties(String groupLdapProviderProperties) {
        if (groupLdapProviderProperties != null) {
            this.groupLdapProviderProperties = JahiaGlobalConfigurator.fromJSON(groupLdapProviderProperties);
        }
    }

    public void setUserLdapProviderProperties(String userLdapProviderProperties) {
        if (userLdapProviderProperties != null) {
            this.userLdapProviderProperties = JahiaGlobalConfigurator.fromJSON(userLdapProviderProperties);
        }
    }
    
    public String getOperatingMode() {
        return operatingMode;
    }

    public boolean isExternalizedConfigActivated() {
        return externalizedConfigActivated;
    }

    public String getExternalizedConfigTargetPath() {
        if (externalizedConfigTargetPath == null && externalizedConfigActivated) {
            initConfigTargetDir();
        }
        return externalizedConfigTargetPath;
    }

    private void initConfigTargetDir() {
        // initialize configuration directory location
        String path = null;
        boolean isTomcat = false;
        boolean isJBoss = false;
        if (targetServerType != null) {
            if (targetServerType.startsWith("jboss")) {
                isJBoss = true;
                path = "${jahiaWebAppRoot}/../../digital-factory-config.jar/";
            } else if (targetServerType.startsWith("tomcat")) {
                isTomcat = true;
                path = "${jahiaWebAppRoot}/../../digital-factory-config/";
            }
        }
        if (path == null) {
            throw new IllegalArgumentException("Externalized configuration is activated,"
                    + " but the target directory could not be detected. Please, specify it explicitly.");
        }
        externalizedConfigTargetPath = JahiaGlobalConfigurator.resolveDataDir(path,
                getWebappDeploymentDir().getAbsolutePath()).getAbsolutePath();
        getLog().info("Configuration directory path resolved to: " + externalizedConfigTargetPath);
        
        try {
            if (isTomcat) {
                adjustCatalinaProperties();
            } else if (isJBoss) {
                FileUtils.touch(new File(externalizedConfigTargetPath, "../digital-factory-config.jar.dodeploy"));
            }
        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
        }
    }

    private void adjustCatalinaProperties() throws IOException {
        // check if the catalina.properties have to be adjusted
        File catalinaProps = new File(targetServerDirectory, "conf/catalina.properties");
        String content = FileUtils.readFileToString(catalinaProps);
        if (!content.contains("${catalina.home}/digital-factory-config")) {
            List<String> lines = IOUtils.readLines(new StringReader(content));
            List<String> modifiedLines = new LinkedList<String>();
            for (String line : lines) {
                modifiedLines.add(line.startsWith("common.loader") ? addPathToCommonLoader(line) : line);
            }
            FileWriter fileWriter = new FileWriter(catalinaProps);
            try {
                IOUtils.writeLines(modifiedLines, null, fileWriter);
                fileWriter.flush();
                getLog().info(
                        "Adjusted common.loader value in the catalina.properties"
                                + " to reference digital-factory-config directory");
            } finally {
                IOUtils.closeQuietly(fileWriter);
            }
        }
    }

    private static String addPathToCommonLoader(String line) {
        line = line.trim();
        if (line.endsWith("\\")) {
            line = line.substring(0, line.length() - 2) + ",${catalina.home}/digital-factory-config,\\";
        } else {
            line = line + ",${catalina.home}/digital-factory-config";
        }
        return line;
    }

    public String getExternalizedConfigClassifier() {
        return externalizedConfigClassifier;
    }

    public String getExternalizedConfigFinalName() {
        return externalizedConfigFinalName;
    }

    public String getJeeApplicationLocation() {
        return jeeApplicationLocation;
    }

    public String getJeeApplicationModuleList() {
        return jeeApplicationModuleList;
    }

    public String getFileDataStorePath() {
        return fileDataStorePath;
    }

    public Map<String, String> getJahiaAdvancedProperties() {
        return jahiaAdvancedProperties;
    }

    public Map<String, String> getJahiaProperties() {
        return jahiaProperties;
    }

    public void setJahiaAdvancedProperties(String jahiaAdvancedProperties) {
        if (jahiaAdvancedProperties != null) {
            this.jahiaAdvancedProperties = JahiaGlobalConfigurator.fromJSON(jahiaAdvancedProperties);
        }
    }

    public void setJahiaProperties(String jahiaProperties) {
        if (jahiaProperties != null) {
            this.jahiaProperties = JahiaGlobalConfigurator.fromJSON(jahiaProperties);
        }
    }

    @Override
    public String getJahiaRootPreferredLang() {
        return jahiaRootPreferredLang;
    }

    @Override
    public String getLicenseFile() {
        return licenseFile;
    }

    @Override
    public boolean isExternalizedConfigExploded() {
        return externalizedConfigExploded;
    }

}
