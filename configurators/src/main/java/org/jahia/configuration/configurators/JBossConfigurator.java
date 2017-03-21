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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.StringUtils;
import org.jahia.configuration.deployers.ServerDeploymentInterface;
import org.jahia.configuration.logging.AbstractLogger;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.Format.TextMode;
import org.jdom.output.XMLOutputter;

/**
 * JBoss DB datasource configurator.
 * 
 * @author Sergiy Shyrkov
 */
public class JBossConfigurator extends AbstractXMLConfigurator {

    private static final String BACKGROUND_VALIDATION_MILLIS = "600000";

    private static final Namespace DS_NS_62 = Namespace.getNamespace("urn:jboss:domain:datasources:1.1");
    private static final Namespace DS_NS_63 = Namespace.getNamespace("urn:jboss:domain:datasources:1.2");

    private static final Map<String, String> EXCEPTION_SORTERS;

    private static final String IDLE_TIMEOUT_MINUTES = "30";

    private static final String MAX_POOL_SIZE = "200";

    private static final String MIN_POOL_SIZE = "10";

    private static final Namespace WEB_NS_62 = Namespace.getNamespace("urn:jboss:domain:web:1.5");
    private static final Namespace WEB_NS_63 = Namespace.getNamespace("urn:jboss:domain:web:2.1");
    private static final Namespace WEB_NS_64 = Namespace.getNamespace("urn:jboss:domain:web:2.2");

    static {
        EXCEPTION_SORTERS = new HashMap<String, String>(6);

        EXCEPTION_SORTERS.put("derby", "org.jboss.jca.adapters.jdbc.extensions.novendor.NullExceptionSorter");
        EXCEPTION_SORTERS.put("derby_embedded", "org.jboss.jca.adapters.jdbc.extensions.novendor.NullExceptionSorter");
        EXCEPTION_SORTERS.put("mssql", "org.jboss.jca.adapters.jdbc.extensions.mssql.MSSQLValidConnectionChecker");
        EXCEPTION_SORTERS.put("mysql", "org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLExceptionSorter");
        EXCEPTION_SORTERS.put("oracle", "org.jboss.jca.adapters.jdbc.extensions.oracle.OracleExceptionSorter");
        EXCEPTION_SORTERS
                .put("postgresql", "org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter");

    }

    private String dbType;

    private ServerDeploymentInterface deployer;
    
    private boolean isJBoss63;
    
    private boolean isJBoss64;
    
    private Namespace datasourceNs;
    
    private Namespace webNs;

    public JBossConfigurator(Map<?, ?> dbProperties, JahiaConfigInterface jahiaConfigInterface,
            ServerDeploymentInterface deployer, AbstractLogger logger) {
        super(dbProperties, jahiaConfigInterface, logger);
        dbType = jahiaConfigInterface.getDatabaseType();
        this.deployer = deployer;
    }

    private void configureConnector(Element profile) {
        Namespace ns = WEB_NS_62;
        Element web = profile.getChild("subsystem", ns);
        if (web == null) {
            ns = WEB_NS_63;
            web = profile.getChild("subsystem", ns);
        }
        if (web == null) {
            ns = WEB_NS_64;
            web = profile.getChild("subsystem", ns);
        }
        if (web != null) {
            for (Object child : web.getChildren("connector", ns)) {
                Element connector = (Element) child;
                Attribute name = connector.getAttribute("name");
                if (name != null && "http".equals(name.getValue())) {
                    connector.setAttribute("protocol", "org.apache.coyote.http11.Http11NioProtocol");
                }
            }
        }
    }

    private void configureDatasource(Element datasources) throws JDOMException {
        Element ds = getElement(datasources, "xp:datasource[@jndi-name=\"java:/jahiaDS\"]", datasourceNs.getURI());
        if (ds == null) {
            ds = new Element("datasource", datasourceNs).setAttribute("jndi-name", "java:/jahiaDS")
                    .setAttribute("pool-name", "jahiaDS").setAttribute("enabled", "true")
                    .setAttribute("use-java-context", "true");
            datasources.addContent(0, ds);
        }
        getChildCreate(ds, "connection-url").setText(getValue(dbProperties, "jahia.database.url"));
        getChildCreate(ds, "driver").setText("jahia." + dbType);
        Element pool = getChildCreate(ds, "pool");
        getChildCreate(pool, "min-pool-size").setText(MIN_POOL_SIZE);
        getChildCreate(pool, "max-pool-size").setText(MAX_POOL_SIZE);

        Element security = getChildCreate(ds, "security");
        getChildCreate(security, "user-name").setText(getValue(dbProperties, "jahia.database.user"));
        getChildCreate(security, "password").setText(getValue(dbProperties, "jahia.database.pass"));

        Element validation = getChildCreate(ds, "validation");
        getChildCreate(validation, "valid-connection-checker").setAttribute("class-name",
                "org.jboss.jca.adapters.jdbc.extensions.novendor.JDBC4ValidConnectionChecker");
        getChildCreate(validation, "validate-on-match").setText("false");
        getChildCreate(validation, "background-validation").setText("true");
        getChildCreate(validation, "background-validation-millis").setText(BACKGROUND_VALIDATION_MILLIS);
        getChildCreate(validation, "exception-sorter").setAttribute("class-name", EXCEPTION_SORTERS.get(dbType));

        getChildCreate(getChildCreate(ds, "timeout"), "idle-timeout-minutes").setText(IDLE_TIMEOUT_MINUTES);
    }

    private void configureDriver(Element datasources) throws JDOMException {
        Element drivers = getChildCreate(datasources, "drivers");
        Element driver = getElement(drivers, "xp:driver[@name=\"jahia." + dbType + "\"]", datasourceNs.getURI());
        if (driver == null) {
            driver = new Element("driver", datasourceNs).setAttribute("name", "jahia." + dbType).setAttribute("module",
                    "org.jahia.jdbc." + dbType);
            Element driverClazz = new Element("driver-class", datasourceNs);
            driverClazz.addContent(getDBProperty("jahia.database.driver"));
            
            driver.addContent(driverClazz);
            drivers.addContent(driver);
        }
    }

    private void disableDefaultWelcomeWebApp(Element profile) {
        Element web = profile.getChild("subsystem", webNs);
        if (web != null) {
            Element virtualServer = web.getChild("virtual-server", webNs);
            if (virtualServer != null) {
                virtualServer.setAttribute("enable-welcome-root", "false");
            }
        }
    }

    private Element getChildCreate(Element parent, String childName) {
        Element child = parent.getChild(childName, datasourceNs);
        if (child == null) {
            child = new Element(childName, datasourceNs);
            parent.addContent(child);
        }
        return child;
    }

    private String getDbPropForCLI(String prop) {
        return StringUtils.replace(getValue(dbProperties, prop), "=", "\\=");
    }

    private Format getOutputFormat() {
        Format customFormat = Format.getRawFormat();
        customFormat.setTextMode(TextMode.TRIM);
        customFormat.setIndent("    ");
        customFormat.setLineSeparator(System.getProperty("line.separator"));
        return customFormat;
    }

    private Element getProfile(Element root, ConfigFile sourceConfigFile) {
        Element profile = root.getChild("profile", root.getNamespace());
        if (profile == null) {
            throw new IllegalArgumentException("Cannot find <profile> element in the file" + sourceConfigFile);
        }

        return profile;
    }

    private boolean isRootContext() {
        String moduleList = jahiaConfigInterface.getJeeApplicationModuleList();
        if (moduleList == null || moduleList.length() == 0) {
            return true;
        }
        boolean isRoot = false;
        for (String moduleConfig : moduleList.split(",")) {
            if (moduleConfig.contains("jahia.war")) {
                String[] moduleParams = moduleConfig.split(":");
                isRoot = moduleParams.length < 4 || StringUtils.isEmpty(moduleParams[3]);
                break;
            }
        }
        return isRoot;
    }

    public void updateConfiguration(ConfigFile sourceConfigFile, String destFileName) throws Exception {
        getLogger().info("Processing file " + sourceConfigFile.getURI());
        FileWriter out = null;
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            InputStreamReader fileReader = new InputStreamReader(sourceConfigFile.getInputStream());
            org.jdom.Document jdomDocument = saxBuilder.build(fileReader);

            Element root = jdomDocument.getRootElement();
            isJBoss63 = root.getNamespace().getURI().equals("urn:jboss:domain:1.6");
            isJBoss64 = root.getNamespace().getURI().equals("urn:jboss:domain:1.7");
            getLogger().info("Detected JBoss EAP version " + (isJBoss63 ? "6.3.x" : (isJBoss64 ? "6.4.x" : "6.2.x")));
            datasourceNs = isJBoss63 || isJBoss64 ? DS_NS_63 : DS_NS_62;
            webNs = isJBoss63 ? WEB_NS_63 : (isJBoss64 ? WEB_NS_64 : WEB_NS_62);
            Element profile = getProfile(root, sourceConfigFile);
            Element datasources = getChildCreate(getChildCreate(profile, "subsystem"), "datasources");

            configureDriver(datasources);

            configureDatasource(datasources);

            if (jahiaConfigInterface.getWebAppDirName().equals("ROOT")) {
                disableDefaultWelcomeWebApp(profile);
            }
            
            configureConnector(profile);

            out = new FileWriter(destFileName);
            getLogger().info("Writing output to " + destFileName);
            new XMLOutputter(getOutputFormat()).output(jdomDocument, out);

        } catch (JDOMException jdome) {
            throw new Exception("Error while updating configuration file " + sourceConfigFile, jdome);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public void updateDriverModule() throws IOException {
        if (deployer == null) {
            getLogger().info("No deployer provided. Skipping driver module update.");
            return;
        }

        getLogger().info("Updating driver module");
        File targetDir = new File(jahiaConfigInterface.getTargetServerDirectory(), "modules/org/jahia/jdbc/" + dbType
                + "/main");

        if (!targetDir.isDirectory()) {
            getLogger().info(
                    "Target driver module directory " + targetDir + " cannot be found. Skipping driver module update.");
            return;
        }
        File moduleXml = new File(targetDir, "module.xml");

        if (moduleXml.isFile()) {
            getLogger().info(moduleXml + " is already present. Skipping driver module update.");
            return;
        }

        for (File driver : FileUtils.listFiles(targetDir, new String[] { "jar" }, false)) {
            getLogger().info("Deploying JDBC driver " + driver);
            deployer.deployJdbcDriver(driver);
        }
    }

    public void writeCLIConfiguration(File dest, String profile) throws Exception {
    	String profilePath = profile != null ? "/profile=" + profile : "";
        StringBuilder cli = new StringBuilder(512);

        // connect
        cli.append("connect\n");
        cli.append("\n");

        // add driver
        cli.append(profilePath);
        cli.append("/subsystem=datasources/jdbc-driver=jahia.");
        cli.append(dbType);
        cli.append(":add(driver-module-name=org.jahia.jdbc.");
        cli.append(dbType);
        cli.append(", driver-name=jahia.");
        cli.append(dbType);
        cli.append(", driver-class-name=");
        cli.append(getDbPropForCLI("jahia.database.driver"));
        cli.append(")\n");
        cli.append("\n");

        // add datasource configuration
        cli.append("data-source add");
        if (profile != null) {
        	cli.append(" --profile=").append(profile);
        }
        cli.append(" --name=jahiaDS --jndi-name=java:/jahiaDS --enabled=true --use-java-context=true \\\n");
        cli.append("--driver-name=jahia.").append(dbType).append(" \\\n");
        cli.append("--connection-url=\"").append(getDbPropForCLI("jahia.database.url")).append("\" \\\n");
        String v = getDbPropForCLI("jahia.database.user");
        if (v != null && v.length() > 0) {
            cli.append("--user-name=\"").append(v).append("\" \\\n");
        }
        v = getDbPropForCLI("jahia.database.pass");
        if (v != null && v.length() > 0) {
            cli.append("--password=\"").append(v).append("\" \\\n");
        }

        cli.append("--min-pool-size=" + MIN_POOL_SIZE + " \\\n");
        cli.append("--max-pool-size=" + MAX_POOL_SIZE + " \\\n");
        cli.append("--validate-on-match=false \\\n");
        cli.append("--background-validation=true \\\n");
        cli.append("--background-validation-millis=" + BACKGROUND_VALIDATION_MILLIS + " \\\n");
        cli.append("--valid-connection-checker-class-name=org.jboss.jca.adapters.jdbc.extensions.novendor.JDBC4ValidConnectionChecker \\\n");
        cli.append("--exception-sorter-class-name=").append(EXCEPTION_SORTERS.get(dbType)).append(" \\\n");
        cli.append("--idle-timeout-minutes=" + IDLE_TIMEOUT_MINUTES + "\n");
        cli.append("\n");

        if (isRootContext()) {
            cli.append(profilePath);
            cli.append("/subsystem=web/virtual-server=default-host:write-attribute(name=enable-welcome-root,value=false)\n");
            cli.append("\n");
        }
        
        // enable HTTP NIO connector
        cli.append(profilePath);
        cli.append("/subsystem=web/connector=http:write-attribute(name=protocol,value=org.apache.coyote.http11.Http11NioProtocol)\n\n");

        if (profile == null) {
        	cli.append("reload\n");
        }

        getLogger().info("Writing output to " + dest);
        FileUtils.writeStringToFile(dest, cli.toString());
    }

}
