package org.jahia.configuration.cluster.tool;

import org.jahia.configuration.configurators.PropertiesManager;
import org.jahia.configuration.logging.AbstractLogger;

import java.io.*;
import java.util.*;

/**
 * Cluster configuration bean.
 */
public class ClusterConfigBean {

    protected AbstractLogger logger;

    private String configurationFileName = "cluster.properties";

    private List<String> nodeTypes = new ArrayList<String>();
    private String browsingNodeNamePrefix = "browsing";
    private String contributionNodeNamePrefix = "contribution";
    private String processingNodeNamePrefix = "processing";
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
    private List<String> filesToFilter = new ArrayList<String>();
    private String deleteFileNamePrefix = "CLUSTER_DELETE_ME_";

    private String waitForStartupURL = "http://${hostname}:8080/welcome";
    private String startupCommandLine = "/home/ec2-user/Jahia/tomcat/bin/startup.sh";
    private String shutdownCommandLine = "/home/ec2-user/Jahia/tomcat/bin/startup.sh";
    private String getPidCommandLine = "cat /home/ec2-user/Jahia/tomcat/temp/tomcat.pid";
    private String psCommandLine = "ps aux | grep bootstrap.jar";
    private String killCommandLine = "kill ${tomcatpid}";
    private String hardKillCommandLine = "kill -9 ${tomcatpid}";
    private String dumpThreadsCommandLine = "kill -QUIT ${tomcatpid}";
    private String tailLogsCommandLine = "tail -f -n 200 /home/ec2-user/Jahia/tomcat/logs/catalina.out";

    private String awsAccessKey = null;
    private String awsSecretKey = null;
    private List<String> awsInstanceNamesToIgnore = new ArrayList<String>();

    private String dbDriverClass = "";
    private String dbUrl = "";
    private String dbUser = "jahia";
    private String dbPassword = "jahia";
    private String dbLocalRevisionsTableName="JR_J_LOCAL_REVISIONS";

    private PropertiesManager clusterProperties;
    private File parentDirectory;
    private File configurationFile;

    private int processingServerCount = 0;
    private int contributionServercount = 0;
    private int browsingServerCount = 0;
    private List<String> generatedNodeIds = new ArrayList<String>();

    public ClusterConfigBean(AbstractLogger logger, File parentDirectory) throws Exception {
        this.logger = logger;
        this.parentDirectory = parentDirectory;
        this.configurationFile = new File(parentDirectory, configurationFileName);

        FileInputStream configStream = new FileInputStream(configurationFile);
        clusterProperties = new PropertiesManager(configStream);
        clusterProperties.setUnmodifiedCommentingActivated(false);

        nodeTypes = getStringListProp("nodeTypes", nodeTypes);
        browsingNodeNamePrefix = getStringProp("browsingNodeNamePrefix", browsingNodeNamePrefix);
        contributionNodeNamePrefix = getStringProp("contributionNodeNamePrefix", contributionNodeNamePrefix);
        processingNodeNamePrefix = getStringProp("", processingNodeNamePrefix);
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
        filesToFilter = getStringListProp("filesToFilter", filesToFilter);
        deleteFileNamePrefix = getStringProp("deleteFileNamePrefix", deleteFileNamePrefix);

        waitForStartupURL = getStringProp("waitForStartupURL", waitForStartupURL);
        startupCommandLine = getStringProp("startupCommandLine", startupCommandLine);
        shutdownCommandLine = getStringProp("shutdownCommandLine", shutdownCommandLine);
        getPidCommandLine = getStringProp("getPidCommandLine", getPidCommandLine);
        psCommandLine = getStringProp("psCommandLine", psCommandLine);
        killCommandLine = getStringProp("killCommandLine", killCommandLine);
        hardKillCommandLine = getStringProp("hardKillCommandLine", hardKillCommandLine);
        dumpThreadsCommandLine = getStringProp("dumpThreadsCommandLine", dumpThreadsCommandLine);
        tailLogsCommandLine = getStringProp("tailLogsCommandLine", tailLogsCommandLine);

        awsAccessKey = getStringProp("awsAccessKey", awsAccessKey);
        awsSecretKey = getStringProp("awsSecretKey", awsSecretKey);
        awsInstanceNamesToIgnore = getStringListProp("awsInstanceNamesToIgnore", awsInstanceNamesToIgnore);

        dbDriverClass = getStringProp("dbDriverClass", dbDriverClass);
        dbUrl = getStringProp("dbUrl", dbUrl);
        dbUser = getStringProp("dbUser", dbUser);
        dbPassword = getStringProp("dbPassword", dbPassword);
        dbLocalRevisionsTableName = getStringProp("dbLocalRevisionsTableName", dbLocalRevisionsTableName);

    }

    public void checkSizeConsistency() throws Exception {
        if (externalHostNames.size() != nodeTypes.size()) {
            throw new Exception("External host name list size is not equal to number of nodes !");
        }

        if (internalIPs.size() != nodeTypes.size()) {
            throw new Exception("Internal IPs list size is not equal to number of nodes !");
        }
    }

    public void store() throws IOException {
        FileInputStream configStream = new FileInputStream(configurationFile);
        clusterProperties.storeProperties(configStream, configurationFile.getPath());
    }

    public int getIntProp(String name, int defaultValue) {
        if (!clusterProperties.getPropertiesObject().containsKey(name)) {
            return defaultValue;
        }
        return Integer.parseInt(clusterProperties.getProperty(name));
    }

    public String getStringProp(String name, String defaultValue) {
        if (!clusterProperties.getPropertiesObject().containsKey(name)) {
            return defaultValue;
        }
        return clusterProperties.getProperty(name);
    }

    public List<String> getStringListProp(String name, List<String> defaultValue) {
        if (!clusterProperties.getPropertiesObject().containsKey(name)) {
            return defaultValue;
        }
        String[] stringArray = clusterProperties.getProperty(name).split(",");
        return Arrays.asList(stringArray);
    }

    public void setStringListProp(String name, List<String> stringList) {
        StringBuffer newValue = new StringBuffer();
        int i=0;
        for (String curString : stringList) {
            newValue.append(curString);
            if (i < (stringList.size() -1)) {
                newValue.append(",");
            }
            i++;
        }
        clusterProperties.setProperty(name, newValue.toString());
    }

    public List<String> getNodeTypes() {
        return nodeTypes;
    }

    public String getBrowsingNodeNamePrefix() {
        return browsingNodeNamePrefix;
    }

    public String getContributionNodeNamePrefix() {
        return contributionNodeNamePrefix;
    }

    public String getProcessingNodeNamePrefix() {
        return processingNodeNamePrefix;
    }

    public List<String> getExternalHostNames() {
        return externalHostNames;
    }

    public List<String> getInternalIPs() {
        return internalIPs;
    }

    public void setExternalHostNames(List<String> externalHostNames) {
        this.externalHostNames = externalHostNames;
        setStringListProp("externalHostNames", externalHostNames);
    }

    public void setInternalIPs(List<String> internalIPs) {
        this.internalIPs = internalIPs;
        setStringListProp("internalIPs", internalIPs);
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

    public List<String> getAwsInstanceNamesToIgnore() {
        return awsInstanceNamesToIgnore;
    }

    public String getTailLogsCommandLine() {
        return tailLogsCommandLine;
    }

    public String getDbDriverClass() {
        return dbDriverClass;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public String getDbLocalRevisionsTableName() {
        return dbLocalRevisionsTableName;
    }

    public List<String> getFilesToFilter() {
        return filesToFilter;
    }

    public String getDeleteFileNamePrefix() {
        return deleteFileNamePrefix;
    }

    private String generateNewNodeId(String nodeType) {
        if ("processing".equals(nodeType)) {
            processingServerCount++;
            return getProcessingNodeNamePrefix() + processingServerCount;
        } else if ("contribution".equals(nodeType)) {
            contributionServercount++;
            return getContributionNodeNamePrefix() + contributionServercount;
        } else {
            browsingServerCount++;
            return getBrowsingNodeNamePrefix() + browsingServerCount;
        }
    }

    public String getNodeType(int index) {
        return getNodeTypes().get(index);
    }

    public String getNodeId(int index) {
        while (generatedNodeIds.size() < getNodeTypes().size()) {
            generatedNodeIds.add(null);
        }
        String nodeId = generatedNodeIds.get(index);
        if (nodeId != null) {
            return nodeId;
        } else {
            nodeId = generateNewNodeId(getNodeType(index));
            generatedNodeIds.set(index, nodeId);
            return nodeId;
        }
    }

    public int getNumberOfNodes() {
        return getNodeTypes().size();
    }

}
