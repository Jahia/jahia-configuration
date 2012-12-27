/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2012 Jahia Solutions Group SA. All rights reserved.
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.jahia.configuration.logging.AbstractLogger;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Deployment utility that is responsible for correctly deploying all resources of a module into the server runtime.
 */
public class ModuleDeployer {

    private class NewerFileFilter implements FileFilter {
        
        private int count; 
        private File dest;
        private File src;
        
        NewerFileFilter(File src, File dest) {
            super();
            this.src = src;
            this.dest = dest;
        }
        
        public boolean accept(File fileToCopy) {
            if (fileToCopy.isDirectory()) {
                return true;
            } else {
                boolean newer = isNewer(fileToCopy);
                if (newer) {
                    count++;
                }
                return newer;
            }
        }

        public int getCount() {
            return count;
        }

        private boolean isNewer(File fileToCopy) {
            File target = new File(dest, fileToCopy.getPath().substring(src.getPath().length()));
            return !target.exists() || FileUtils.isFileNewer(fileToCopy, target);
        }
    };
    
    private boolean deployModuleForOSGiTransformation;
    private AbstractLogger logger;
    private File output;

    public ModuleDeployer(File output, AbstractLogger logger, boolean deployModuleForOSGiTransformation) {
        this.output = output;
        this.logger = logger;
        this.deployModuleForOSGiTransformation = deployModuleForOSGiTransformation;
    }

    private void copyClasses(File warFile, File targetDir) {
        JarFile war = null;
        try {
            war = new JarFile(warFile);
            if (war.getJarEntry("WEB-INF/classes") != null) {
                war.close();
                ZipUnArchiver unarch = new ZipUnArchiver(warFile);
                File tmp = new File(targetDir, String.valueOf(System.currentTimeMillis()));
                tmp.mkdirs();
                File srcDir = new File(tmp, "WEB-INF/classes");
                File destDir = new File(targetDir, "WEB-INF/classes");
                NewerFileFilter filter = new NewerFileFilter(srcDir, destDir);
                try {
                    unarch.extract("WEB-INF/classes", tmp);
                    FileUtils.copyDirectory(srcDir, destDir, filter);
                } finally {
                    FileUtils.deleteQuietly(tmp);
                }
                logger.info("Copied " + filter.getCount() + " new/newer class(es) from " + warFile.getName() + " to WEB-INF/classes");
            }
        } catch (Exception e) {
            logger.error("Error copying classes for module " + warFile, e);
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

    private void copyDbScripts(File warFile, File targetDir) {
        JarFile war = null;
        try {
            war = new JarFile(warFile);
            if (war.getJarEntry("META-INF/db") != null) {
                war.close();
                ZipUnArchiver unarch = new ZipUnArchiver(warFile);
                File tmp = new File(targetDir, String.valueOf(System.currentTimeMillis()));
                tmp.mkdirs();
                try {
                    unarch.extract("META-INF/db", tmp);
                    FileUtils.copyDirectory(new File(tmp, "META-INF/db"), new File(targetDir, "WEB-INF/var/db/sql/schema"));
                } finally {
                    FileUtils.deleteQuietly(tmp);
                }
                logger.info("Copied database scripts from " + warFile.getName() + " to WEB-INF/var/db/sql/schema");
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

    public void copyJars(File warFile, File targetDir) {
        JarFile war = null;
        try {
            war = new JarFile(warFile);
            int deployed = 0;
            int found = 0;
            if (war.getJarEntry("WEB-INF/lib") != null) {
                Enumeration<JarEntry> entries = war.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();

                    String entryName = entry.getName();
                    if (!entryName.startsWith("WEB-INF/lib/") || !entryName.endsWith(".jar")) {
                        continue;
                    }
                    if (isNewer(entry, targetDir)) {
                            deployed++;
                            InputStream source = war.getInputStream(entry);
                            File libsDir = new File(targetDir, "WEB-INF/lib");
                            if (!libsDir.exists()) {
                                libsDir.mkdirs();
                            }
                            File targetFile = new File(targetDir, entryName);
                            FileOutputStream target = new FileOutputStream(targetFile);
                            try {
                                IOUtils.copy(source, target);
                            } finally {
                                IOUtils.closeQuietly(source);
                                target.flush();
                                IOUtils.closeQuietly(target);
                            }
                            if (entry.getTime() > 0) {
                                targetFile.setLastModified(entry.getTime());
                            }
                    } else {
                        found++;
                        logger.debug(entryName + " is already deployed and is not older than the current entry");
                    }
                }
            }
            if (found > 0) {
                logger.info("Found " + found + " JARs in " + warFile.getName() + ". Copied " + deployed + " new/newer JARs to WEB-INF/lib");
            }
        } catch (IOException e) {
            logger.error("Error copying JAR files for module " + warFile, e);
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
        if (deployModuleForOSGiTransformation) {
            logger.info("Copy module " + file.getName() + " to OSGi transformation directory");
        } else {
            logger.info("Copy modules JAR " + file.getName() + " to shared modules folder");
        }
        FileUtils.copyFileToDirectory(file, output);
        logger.info("Copied " + file + " to " + output);
        if (!deployModuleForOSGiTransformation) {
            File targetDir = new File(output, "../../..");
            copyJars(file, targetDir);
            copyClasses(file, targetDir);
            copyDbScripts(file, targetDir);
        }
    }

    /**
     * Checks if the jar is already deployed and we don't try to deploy a new version (using "last modified time")
     * 
     * @param entry the Jar file entry
     * @param targetDir the target directory
     * @return the result of the last modified value check
     */
    private boolean isNewer(JarEntry entry, File targetDir) {
        File fEntry = new File(targetDir, entry.getName());
        if (fEntry.exists() && fEntry.lastModified() >= entry.getTime()) {
            return false;
        }
        return true;
    }
}
