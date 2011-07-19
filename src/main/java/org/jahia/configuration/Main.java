package org.jahia.configuration;

import org.jahia.configuration.configurators.JahiaGlobalConfigurator;
import org.jahia.configuration.logging.AbstractLogger;
import org.jahia.configuration.logging.ConsoleLogger;
import org.jahia.configuration.modules.ModuleDeployer;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        AbstractLogger logger = new ConsoleLogger(ConsoleLogger.LEVEL_INFO);
        logger.info("\nJahia 6.5 Configuration Tool");
        logger.info("Copyright 2002-2011 - Jahia Solutions Group SA http://www.jahia.com - All Rights Reserved\n");
        if (args.length > 0) {
            if ((args[0].equals("--deploy-module") || args[0].equals("-dm")) && args.length > 2) {
                try {
                    String target = args[args.length-1];
    
                    File output = new File(target, "WEB-INF/var/shared_modules");
    
                    if (!output.exists()) {
                        logger.error("Target does not seem to be a valid jahia root folder.");
                        System.exit(-1);
                    }
    
                    ModuleDeployer deployer = new ModuleDeployer(output, logger);
    
                    try {
                        logger.info("Deploying modules to Jahia Web application at " + target + "\n");
                        for (int i = 1; i < args.length - 1; i++) {
                            String arg = args[i];
                            File f = new File(arg);
                            if (f.exists()) {
                                deployer.deployModule(f);
                            } else {
                                logger.error("Cannot find file : "+f.getName());
                            }
                        }
                        logger.info("\n...module deployment done.");
                    } catch (IOException e) {
                        logger.error("", e);
                    }
                } catch (Exception e) {
                    logger.error("Error during execution of a configurator. Cause: " + e.getMessage(), e);
                }
                return;
            } else if (args[0].equals("--configure") || args[0].equals("-c")) {
                logger.info("Started Jahia global configurator");
                try {
                    new JahiaGlobalConfigurator(logger, JahiaGlobalConfigurator.getConfiguration(args.length > 1 ? new File(args[1]) : null, logger)).execute();
                } catch (Exception e) {
                    logger.error("Error during execution of a configurator. Cause: " + e.getMessage(), e);
                }
                logger.info("...finished job of Jahia global configurator.");
                return;
            }
        }
        
        showUsage(logger);
    }

    private static void showUsage(AbstractLogger logger) {
        logger.info("Usage: java -jar configurators-x.yy-standalone.jar [command] [parameters(s)]");
        logger.info("\nCommands:");
        logger.info(" -c,--configure"+"\t\t"+"Performs configuration of an installed Jahia server.");
        logger.info("\t\t\t"+"Expects a path to a properties file with configuration");
        logger.info("\t\t\t"+"settings as a parameter.");

        logger.info(" -dm,--deploy-module"+"\t"+"Deploys provided module to a specified Jahia server.");
        logger.info("\t\t\t"+"Expects one or more paths to module WAR files followed");
        logger.info("\t\t\t"+"by a path to the Jahia Web application folder.");

        logger.info("\nExamples:");
        logger.info(" java -jar configurators-x.yy-standalone.jar --configure /opt/jahia/install.properties");
        logger.info(" java -jar configurators-x.yy-standalone.jar --deploy-module blog-1.1.war forum-1.1.war /opt/jahia-6.5/tomcat/webapps/ROOT");
    }

}
