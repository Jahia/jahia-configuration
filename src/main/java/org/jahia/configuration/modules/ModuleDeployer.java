/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */
package org.jahia.configuration.modules;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.jahia.configuration.logging.AbstractLogger;

/**
 * Deployment utility that is responsible for correctly deploying all resources of a module into the server runtime.
 */
public class ModuleDeployer {

    private AbstractLogger logger;
    private File output;

    public ModuleDeployer(File output, AbstractLogger logger) {
        this.output = output;
        this.logger = logger;
    }

    private void copyDbScripts(File warFile, File targetDir) {
        JarFile war = null;
        try {
            war = new JarFile(warFile);
            if (war.getJarEntry("META-INF/db") != null) {
                war.close();
                ZipUnArchiver unarch = new ZipUnArchiver(warFile);
                File tmp = new File(FileUtils.getTempDirectory(), String.valueOf(System.currentTimeMillis()));
                tmp.mkdirs();
                File destDir = new File(targetDir, "db/sql/schema");
                try {
                    unarch.extract("META-INF/db", tmp);
					FileUtils.copyDirectory(new File(tmp, "META-INF/db"), destDir);
                } finally {
                    FileUtils.deleteQuietly(tmp);
                }
                logger.info("Copied database scripts from " + warFile.getName() + " to " + destDir);
            }
        } catch (Exception e) {
            logger.error("Error copying database scripts for module " + warFile, e);
        } finally {
            if (war != null) {
                try {
                    war.close();
                } catch (Exception e) {
                    logger.warn("Unable to close the JAR file " + warFile, e);
                }
            }
        }
    }

    public void deployModule(File file) throws IOException {
        FileUtils.copyFileToDirectory(file, output);
        logger.info("Copied " + file + " to " + output);
        File targetDir = new File(output, "../");
        copyDbScripts(file, targetDir);
    }
}
