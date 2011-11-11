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


    public ClusterTool(byte loggingLevel) throws Exception {
        logger = new ConsoleLogger(loggingLevel);
        clusterConfigGenerator = new ClusterConfigGenerator(logger, new File("."));
    }

    public void run() throws Exception {
        clusterConfigGenerator.generateClusterConfiguration();
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
                formatter.printHelp( "cluster-tools [options] project_directory", options );
                return;
            }

            byte logLevel = ConsoleLogger.LEVEL_INFO;
            if (line.hasOption('l')) {
                logLevel = Byte.parseByte(line.getOptionValue('l'));
            }
            ClusterTool application = new ClusterTool(logLevel);
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
