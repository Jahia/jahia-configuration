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
    private String deploymentUserName = "ec2-user";
    private String deploymentTargetPath = "/home/ec2-user/Jahia";
    private String jahiaAdvancedPropertyRelativeFileLocation = "tomcat" + File.separator+ "webapps"+File.separator+"ROOT"+File.separator+"WEB-INF" + File.separator + "etc" + File.separator + "config" + File.separator + "jahia.advanced.properties";
    private String templateDirectoryName = "template";
    private String nodesDirectoryName = "nodes";
    private String logsDirectoryName = "logs";
    private String remoteLogDirectory = "/home/ec2-user/Jahia/tomcat/log";

    private String waitForStartupURL = "http://${hostname}:8080/welcome";
    private String startupCommandLine = "/home/ec2-user/Jahia/tomcat/bin/startup.sh";
    private String shutdownCommandLine = "/home/ec2-user/Jahia/tomcat/bin/startup.sh";
    private String getPidCommandLine = "cat /home/ec2-user/Jahia/tomcat/temp/tomcat.pid";
    private String psCommandLine = "ps aux | grep bootstrap.jar";
    private String killCommandLine = "kill ${tomcatpid}";
    private String hardKillCommandLine = "kill -9 ${tomcatpid}";
    private String dumpThreadsCommandLine = "kill -QUIT ${tomcatpid}";

    private String awsAccessKey = null;
    private String awsSecretKey = null;

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
        deploymentUserName = getStringProp("deploymentUserName", deploymentUserName);
        deploymentTargetPath = getStringProp("deploymentTargetPath", deploymentTargetPath);

        jahiaAdvancedPropertyRelativeFileLocation = getStringProp("jahiaAdvancedPropertyRelativeFileLocation", jahiaAdvancedPropertyRelativeFileLocation);
        templateDirectoryName = parentDirectory + File.separator + getStringProp("templateDirectoryName", templateDirectoryName);
        nodesDirectoryName = parentDirectory + File.separator +getStringProp("nodesDirectoryName", nodesDirectoryName);
        logsDirectoryName = parentDirectory + File.separator + getStringProp("logsDirectoryName", logsDirectoryName);
        remoteLogDirectory = getStringProp("remoteLogDirectory", remoteLogDirectory);

        waitForStartupURL = getStringProp("waitForStartupURL", waitForStartupURL);
        startupCommandLine = getStringProp("startupCommandLine", startupCommandLine);
        shutdownCommandLine = getStringProp("shutdownCommandLine", shutdownCommandLine);
        getPidCommandLine = getStringProp("getPidCommandLine", getPidCommandLine);
        psCommandLine = getStringProp("psCommandLine", psCommandLine);
        killCommandLine = getStringProp("killCommandLine", killCommandLine);
        hardKillCommandLine = getStringProp("hardKillCommandLine", hardKillCommandLine);
        dumpThreadsCommandLine = getStringProp("dumpThreadsCommandLine", dumpThreadsCommandLine);

        awsAccessKey = getStringProp("awsAccessKey", awsAccessKey);
        awsSecretKey = getStringProp("awsSecretKey", awsSecretKey);

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

    public String getDeploymentUserName() {
        return deploymentUserName;
    }

    public String getDeploymentTargetPath() {
        return deploymentTargetPath;
    }

    public String getWaitForStartupURL() {
        return waitForStartupURL;
    }

    public String getStartupCommandLine() {
        return startupCommandLine;
    }

    public String getShutdownCommandLine() {
        return shutdownCommandLine;
    }

    public String getGetPidCommandLine() {
        return getPidCommandLine;
    }

    public String getPsCommandLine() {
        return psCommandLine;
    }

    public String getKillCommandLine() {
        return killCommandLine;
    }

    public String getHardKillCommandLine() {
        return hardKillCommandLine;
    }

    public String getDumpThreadsCommandLine() {
        return dumpThreadsCommandLine;
    }

    public String getLogsDirectoryName() {
        return logsDirectoryName;
    }

    public String getRemoteLogDirectory() {
        return remoteLogDirectory;
    }

    public String getAwsAccessKey() {
        return awsAccessKey;
    }

    public String getAwsSecretKey() {
        return awsSecretKey;
    }
}
