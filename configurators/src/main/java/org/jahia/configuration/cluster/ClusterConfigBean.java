package org.jahia.configuration.cluster;

import org.jahia.configuration.logging.AbstractLogger;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Cluster configuration bean.
 */
public class ClusterConfigBean {

    protected AbstractLogger logger;

    private String configurationFileName = "cluster.properties";

    private int numberOfNodes = 10;
    private String nodeNamePrefix = "jahiaServer";
    private List<String> externalHostNames = new ArrayList<String>();
    private List<String> internalIPs = new ArrayList<String>();
    private String privateKeyFileLocation = "privatekey.pem";
    private String jahiaAdvancedPropertyRelativeFileLocation = "tomcat" + File.separator+ "webapps"+File.separator+"ROOT"+File.separator+"WEB-INF" + File.separator + "etc" + File.separator + "config" + File.separator + "jahia.advanced.properties";
    private String templateDirectoryName = "template";
    private String nodesDirectoryName = "nodes";

    Properties clusterProperties = new Properties();

    public ClusterConfigBean(AbstractLogger logger, File parentDirectory) throws Exception {
        this.logger = logger;
        File configurationFile = new File(parentDirectory, configurationFileName);
        clusterProperties.load(new FileReader(configurationFile));

        numberOfNodes = getIntProp("numberOfNodes", numberOfNodes);
        nodeNamePrefix = getStringProp("nodeNamePrefix", nodeNamePrefix);
        externalHostNames = getStringListProp("externalHostNames", externalHostNames);
        internalIPs = getStringListProp("internalIPs", internalIPs);
        privateKeyFileLocation = parentDirectory + File.separator + getStringProp("privateKeyFileLocation", privateKeyFileLocation);
        jahiaAdvancedPropertyRelativeFileLocation = getStringProp("jahiaAdvancedPropertyRelativeFileLocation", jahiaAdvancedPropertyRelativeFileLocation);
        templateDirectoryName = parentDirectory + File.separator + getStringProp("templateDirectoryName", templateDirectoryName);
        nodesDirectoryName = parentDirectory + File.separator +getStringProp("nodesDirectoryName", nodesDirectoryName);

        if (externalHostNames.size() != numberOfNodes) {
            throw new Exception("External host name list size is not equal to number of nodes !");
        }

        if (internalIPs.size() != numberOfNodes) {
            throw new Exception("Internal IPs list size is not equal to number of nodes !");
        }

    }

    public int getIntProp(String name, int defaultValue) {
        if (!clusterProperties.containsKey(name)) {
            return defaultValue;
        }
        return Integer.parseInt(clusterProperties.getProperty(name));
    }

    public String getStringProp(String name, String defaultValue) {
        if (!clusterProperties.containsKey(name)) {
            return defaultValue;
        }
        return clusterProperties.getProperty(name);
    }

    public List<String> getStringListProp(String name, List<String> defaultValue) {
        if (!clusterProperties.containsKey(name)) {
            return defaultValue;
        }
        String[] stringArray = clusterProperties.getProperty(name).split(",");
        return Arrays.asList(stringArray);
    }

    public int getNumberOfNodes() {
        return numberOfNodes;
    }

    public String getNodeNamePrefix() {
        return nodeNamePrefix;
    }

    public List<String> getExternalHostNames() {
        return externalHostNames;
    }

    public List<String> getInternalIPs() {
        return internalIPs;
    }

    public String getPrivateKeyFileLocation() {
        return privateKeyFileLocation;
    }

    public String getJahiaAdvancedPropertyRelativeFileLocation() {
        return jahiaAdvancedPropertyRelativeFileLocation;
    }

    public String getTemplateDirectoryName() {
        return templateDirectoryName;
    }

    public String getNodesDirectoryName() {
        return nodesDirectoryName;
    }
}
