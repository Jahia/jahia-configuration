package org.jahia.configuration.cluster.tool;

import org.apache.commons.cli.*;
import org.jahia.configuration.cluster.ClusterConfigBean;
import org.jahia.configuration.cluster.ClusterConfigGenerator;
import org.jahia.configuration.cluster.tool.operations.*;
import org.jahia.configuration.logging.AbstractLogger;
import org.jahia.configuration.logging.ConsoleLogger;
import org.jahia.configuration.logging.SLF4JLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Cluster tool main class
 */
public class ClusterTool {

    private static final Logger slf4jLogger = LoggerFactory.getLogger(ClusterTool.class);

    private AbstractLogger logger;
    private ClusterConfigBean clusterConfigBean;
    private ClusterConfigGenerator clusterConfigGenerator;
    private String command = null;
    private HashMap<String, AbstractClusterOperation> operations = new HashMap<String, AbstractClusterOperation>();

    public ClusterTool(String projectDirectory, String command) throws Exception {
        this.logger = new SLF4JLogger(slf4jLogger);
        this.command = command;
        clusterConfigBean = new ClusterConfigBean(logger, new File(projectDirectory));
        clusterConfigGenerator = new ClusterConfigGenerator(logger, clusterConfigBean);
        operations.put("default", new ClusterConfigDeployer(logger, clusterConfigGenerator.getClusterConfigBean()));
        operations.put("start", new ClusterSequentialTomcatStart(logger, clusterConfigGenerator.getClusterConfigBean()));
        operations.put("stop",new ClusterExecute(logger, clusterConfigGenerator.getClusterConfigBean(), clusterConfigGenerator.getClusterConfigBean().getShutdownCommandLine()));
        operations.put("ps", new ClusterExecute(logger, clusterConfigGenerator.getClusterConfigBean(), clusterConfigGenerator.getClusterConfigBean().getPsCommandLine()));
        operations.put("kill", new ClusterKill(logger, clusterConfigGenerator.getClusterConfigBean(), false));
        operations.put("hardkill", new ClusterKill(logger, clusterConfigGenerator.getClusterConfigBean(), true));
        operations.put("dumpthreads", new ClusterDumpThreads(logger, clusterConfigGenerator.getClusterConfigBean()));
        operations.put("getlogs", new ClusterGetLogs(logger, clusterConfigGenerator.getClusterConfigBean()));
        operations.put("awsgetinstances", new AWSGetClusterInstances(logger, clusterConfigGenerator.getClusterConfigBean()));
        operations.put("taillogs", new ClusterTailLogs(logger, clusterConfigBean));
        operations.put("updatelocalrevisions", new ClusterUpdateLocalRevisions(logger, clusterConfigBean));
    }

    public void run() throws Exception {
        long startTime = System.currentTimeMillis();
        if (command == null) {
            command = "default";
            clusterConfigGenerator.generateClusterConfiguration();
        }
        AbstractClusterOperation operation = operations.get(command);
        if (operation == null) {
            operation = new ClusterExecute(logger, clusterConfigBean, command);
        }
        operation.execute();
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Operations completed in " + totalTime + "ms");
    }

    public static Options buildOptions() {

        Option threads = OptionBuilder.withArgName("level")
                .hasArg()
                .withDescription("Logging level")
                .withLongOpt("loglevel")
                .create("l");


        Option help =
                OptionBuilder.withDescription("Prints this help screen")
                .withLongOpt("help")
                .create("h");

        Options options = new Options();
        options.addOption(threads);
        options.addOption(help);
        return options;
    }

    public static void main(String[] args) {

        // create the parser
        CommandLineParser parser = new PosixParser();
        try {
            // parse the command line arguments
            Options options = buildOptions();
            CommandLine line = parser.parse(options, args);
            String[] lineArgs = line.getArgs();
            if (lineArgs.length < 1) {
                // automatically generate the help statement
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "cluster-tools [options] project_directory [command]", options );
                System.out.println("where command may be one of :");
                System.out.println("- [none] : will generate the configuration for all nodes and deploy to the corresponding servers");
                System.out.println("- start : will start all Tomcat instances on all the nodes");
                System.out.println("- stop : will stop (or attempt to stop) all Tomcat instances on all the nodes");
                System.out.println("- kill : will kill all Tomcat instances on all the nodes");
                System.out.println("- hardkill : will kill -9 all Tomcat instances on all the nodes");
                System.out.println("- dumpthreads : will instruct all JVM instances on all the nodes to generate a thread dump in the logs");
                System.out.println("- getlogs : will retrieve all the logs from all the servers");
                System.out.println("- awsgetinstances : will retrieve all AWS instances public DNS names and private IP addresses and display them. Useful to quickly populate the cluster.properties file.");
                System.out.println("- taillogs : will issue a tail for the Tomcat logs on all the cluster instances");
                System.out.println("- updatelocalrevisions : will connect to the database to update all node local revisions to the highest found");
                System.out.println("- [anyother] : will launch any other command on all cluster node instances.");
                return;
            }

            String command = null;
            if (lineArgs.length >= 2) {
                StringBuffer commandBuffer = new StringBuffer();
                for (int i=1; i<lineArgs.length; i++) {
                    commandBuffer.append(lineArgs[i]);
                }
                command = commandBuffer.toString();
                if ("".equals(command)) {
                    command = null;
                }
            }

            byte logLevel = ConsoleLogger.LEVEL_INFO;
            if (line.hasOption('l')) {
                logLevel = Byte.parseByte(line.getOptionValue('l'));
            }
            ClusterTool application = new ClusterTool(lineArgs[0], command);
            application.run();
        } catch (ParseException exp) {
            // oops, something went wrong
            slf4jLogger.error("Parsing failed.  Reason: ", exp);
        } catch (IOException e) {
            slf4jLogger.error("Error", e);
        } catch (Exception e) {
            slf4jLogger.error("Error", e);
        }
    }

}
