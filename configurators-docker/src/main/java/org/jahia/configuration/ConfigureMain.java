/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2023 Jahia Solutions Group SA. All rights reserved.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class ConfigureMain {
    private static final Logger logger = LoggerFactory.getLogger(ConfigureMain.class);

    public static void main(String[] args) {
        try (InputStream is = ConfigureMain.class.getClassLoader().getResourceAsStream("header.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            System.out.println(reader.lines().collect(Collectors.joining(System.lineSeparator())) + System.lineSeparator());
        } catch (IOException e) {
            // ignore if the header file is not found
        }

        if (args.length > 0) {
            if (args[0].equals("--configure") || args[0].equals("-c")) {
                logger.info("Jahia docker configurator started");
                try {
                    new JahiaGlobalConfigurator(JahiaGlobalConfigurator.getConfiguration(args.length > 1 ? new File(args[1]) : null)).execute();
                } catch (Exception e) {
                    logger.error("Error during execution of a configurator. Cause: " + e.getMessage(), e);
                    System.exit(1);
                }
                logger.info("Jahia docker configurator finished successfully");
                return;
            }
        }

        logger.info("Usage: java -cp \"configurators-docker-6.13.0-standalone.jar:/path/to/tomcat/lib/*\" org.jahia.configuration.ConfigureMain [command] [parameters(s)]");
        logger.info("\nCommands:");
        logger.info(" -c,--configure"+"\t\t"+"Performs configuration of an installed Jahia server.");
        logger.info("\t\t\t"+"Expects a path to a properties file with configuration");
        logger.info("\t\t\t"+"settings as a parameter.");

        logger.info("\nExamples:");
        logger.info(" java -cp \"configurators-docker-standalone.jar:/path/to/tomcat/lib/*\" org.jahia.configuration.ConfigureMain --configure /opt/jahia/install.properties");
    }
}
