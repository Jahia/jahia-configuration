package org.jahia.configuration.cluster.tool;

import org.apache.commons.cli.*;
import org.jahia.configuration.cluster.ClusterConfigGenerator;
import org.jahia.configuration.logging.AbstractLogger;
import org.jahia.configuration.logging.ConsoleLogger;

import java.io.File;
import java.io.IOException;

/**
 * Cluster tool main class
 */
public class ClusterTool {

    private AbstractLogger logger;
    private ClusterConfigGenerator clusterConfigGenerator;
    private ClusterConfigDeployer clusterConfigDeployer;
    private ClusterSequentialTomcatStart clusterSequentialTomcatStart;
    private ClusterExecute clusterExecute;
    private ClusterKill clusterKill;
    private String command = null;

    public ClusterTool(byte loggingLevel, String projectDirectory, String command) throws Exception {
        this.logger = new ConsoleLogger(loggingLevel);
        this.command = command;
        clusterConfigGenerator = new ClusterConfigGenerator(logger, new File(projectDirectory));
        if (command == null) {
            clusterConfigDeployer = new ClusterConfigDeployer(logger, clusterConfigGenerator.getClusterConfigBean());
        } else if ("start".equals(command)) {
            clusterSequentialTomcatStart = new ClusterSequentialTomcatStart(logger, clusterConfigGenerator.getClusterConfigBean());
        } else if ("stop".equals(command)) {
            clusterExecute = new ClusterExecute(logger, clusterConfigGenerator.getClusterConfigBean(), clusterConfigGenerator.getClusterConfigBean().getShutdownCommandLine());
        } else if ("ps".equals(command)) {
            clusterExecute = new ClusterExecute(logger, clusterConfigGenerator.getClusterConfigBean(), clusterConfigGenerator.getClusterConfigBean().getPsCommandLine());
        } else if ("kill".equals(command)) {
            clusterKill = new ClusterKill(logger, clusterConfigGenerator.getClusterConfigBean(), false);
        } else if ("hardkill".equals(command)) {
            clusterKill = new ClusterKill(logger, clusterConfigGenerator.getClusterConfigBean(), false);
        }
    }

    public void run() throws Exception {
        if (command == null) {
            clusterConfigGenerator.generateClusterConfiguration();
            clusterConfigDeployer.execute();
        } else if ("start".equals(command)) {
            clusterSequentialTomcatStart.execute();
        } else if ("stop".equals(command)) {
            clusterExecute.execute();
        } else if ("ps".equals(command)) {
            clusterExecute.execute();
        } else if ("kill".equals(command)) {
            clusterKill.execute();
        } else if ("hardkill".equals(command)) {
            clusterKill.execute();
        }
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
                return;
            }

            String command = null;
            if (lineArgs.length >= 2) {
                command = lineArgs[1];
            }

            byte logLevel = ConsoleLogger.LEVEL_INFO;
            if (line.hasOption('l')) {
                logLevel = Byte.parseByte(line.getOptionValue('l'));
            }
            ClusterTool application = new ClusterTool(logLevel, lineArgs[0], command);
            application.run();
        } catch (ParseException exp) {
            // oops, something went wrong
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
