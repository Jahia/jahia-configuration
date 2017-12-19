/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
        logger.info("\nDigital Experience Manager 7.2 Configuration Tool");
        logger.info("Copyright 2002-2017 - Jahia Solutions Group SA http://www.jahia.com - All Rights Reserved\n");
        if (args.length > 0) {
            if ((args[0].equals("--deploy-module") || args[0].equals("-dm")) && args.length > 2) {
                try {
                    String target = args[args.length-1];
    
                    File output = new File(target, "modules");
    
                    if (!output.exists()) {
                        logger.error("Target does not seem to be a valid Digital Experience Manager data folder.");
                        System.exit(-1);
                    }
    
                    ModuleDeployer deployer = new ModuleDeployer(output, logger);
    
                    try {
                        logger.info("Deploying modules to Digital Experience Manager application at " + target + "\n");
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
                        logger.error("Error deploying module", e);
                        System.exit(1);
                    }
                } catch (Exception e) {
                    logger.error("Error during execution of a configurator. Cause: " + e.getMessage(), e);
                    System.exit(1);
                }
                return;
            } else if (args[0].equals("--configure") || args[0].equals("-c")) {
                logger.info("Started Jahia global configurator");
                try {
                    new JahiaGlobalConfigurator(logger, JahiaGlobalConfigurator.getConfiguration(args.length > 1 ? new File(args[1]) : null, logger)).execute();
                } catch (Exception e) {
                    logger.error("Error during execution of a configurator. Cause: " + e.getMessage(), e);
                    System.exit(1);
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

        logger.info(" -dm,--deploy-module"+"\t"+"Deploys provided module to a specified Digital Experience Manager server.");
        logger.info("\t\t\t"+"Expects one or more paths to module JAR (OSGi bundle) files followed");
        logger.info("\t\t\t"+"by a path to the Digital Experience Manager data folder.");

        logger.info("\nExamples:");
        logger.info(" java -jar configurators-x.yy-standalone.jar --configure /opt/jahia/install.properties");
        logger.info(" java -jar configurators-x.yy-standalone.jar --deploy-module blog-2.0.jar forum-2.0.jar /opt/jahia-7.0/tomcat/digital-factory-data");
    }

}
