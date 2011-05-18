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
        if (args[0].equals("--deploy-module")) {
            try {
                String target = args[args.length-1];

                File output = new File(target, "WEB-INF/var/shared_modules");

                if (!output.exists()) {
                    logger.error("Target does not seem to be a valid jahia root folder.");
                    System.exit(-1);
                }

                ModuleDeployer deployer = new ModuleDeployer(output, logger);

                try {
                    for (int i = 1; i < args.length - 1; i++) {
                        String arg = args[i];
                        File f = new File(arg);
                        if (f.exists()) {
                            deployer.deployModule(f);
                        } else {
                            logger.error("Cannot find file : "+f.getName());
                        }
                    }
                } catch (IOException e) {
                    logger.error("", e);
                }
            } catch (Exception e) {
                logger.error("Error during execution of a configurator. Cause: " + e.getMessage(), e);
            }

        } else if (args[0].equals("--configure")) {
            logger.info("Started Jahia global configurator");
            try {
                new JahiaGlobalConfigurator(logger, JahiaGlobalConfigurator.getConfiguration(args.length > 1 ? new File(args[1]) : null, logger)).execute();
            } catch (Exception e) {
                logger.error("Error during execution of a configurator. Cause: " + e.getMessage(), e);
            }
            logger.info("... finished job of Jahia global configurator.");
        }
    }

}
