/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2011 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.configuration.server.tomcat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;

/**
 * Jahia specific Tomcat lifecycle listener that checks for modules to be deployed and does the deployment of their classes and JAR files
 * before the Jahia Web Application is started.
 * 
 * @author Sergiy Shyrkov
 */
public class ModuleClassesHostLifecycleListener implements LifecycleListener {

    private static final FileFilter DIR_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    };

    private static Logger log = Logger.getLogger(ModuleClassesHostLifecycleListener.class.getName());

    /**
     * From org.apache.commons.io.IOUtils.copyLarge(InputStream, OutputStream)
     */
    protected static long copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    protected void deployModuleClasses(File jahiaDir, File module) throws IOException {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Deploying classes of module " + module);
        }
        int count = 0;
        JarFile jar = new JarFile(module);
        Enumeration<JarEntry> entries = jar.entries();
        try {
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!entry.isDirectory()
                        && (name.startsWith("WEB-INF/classes/") || name.startsWith("WEB-INF/lib/"))) {
                    if (log.isLoggable(Level.FINE)) {
                        log.log(Level.FINE, "    deploying " + name);
                    }
                    File target = new File(jahiaDir, name);
                    File targetFolder = target.getParentFile();
                    if (!targetFolder.exists()) {
                        if (!targetFolder.mkdirs()) {
                            log.log(Level.SEVERE, "Unable to create folder " + targetFolder.getPath()
                                    + ". Skip copying entry " + name);
                            continue;
                        }
                    }
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target));
                    InputStream in = jar.getInputStream(entry);
                    try {
                        copy(in, out);
                    } finally {
                        try {
                            out.close();
                        } catch (Exception e) {
                            // ignore
                        }
                        try {
                            in.close();
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                    long lastModified = entry.getTime();
                    if (lastModified > 0) {
                        target.setLastModified(lastModified);
                    }
                    count++;
                }
            }
        } finally {
            jar.close();
        }

        log.info(count + " item(s) deployed for module " + module);
    }

    protected void deploySharedTemplatePackages(File jahiaDir) {
        File sharedModulesDir = new File(jahiaDir, "WEB-INF/var/shared_modules");
        log.info("Scanning shared modules directory (" + sharedModulesDir
                + ") for new or updated modules set packages ...");

        File[] warFiles = getModulesToDeploy(jahiaDir, sharedModulesDir);

        log.info("Detected " + warFiles.length + " new/updated modules to be deployed");

        for (File module : warFiles) {
            try {
                deployModuleClasses(jahiaDir, module);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Cannot deploy classes of module: " + module.getName(), e);
            }
        }

        log.info("...finished scanning shared modules directory.");
    }

    protected File detectJahiaWebAppPath(File file) {
        File jahiaDir = null;
        if (!file.isDirectory() || file.list() == null) {
            return null;
        }

        File[] apps = file.listFiles(DIR_FILTER);
        for (File app : apps) {
            if (new File(app, "WEB-INF/classes/jahia-startup-intro.txt").isFile()) {
                // found Jahia Web application
                jahiaDir = app;
                break;
            }
        }

        return jahiaDir;
    }

    protected File[] getModulesToDeploy(File jahiaDir, File sharedModulesDir) {
        File[] packageFiles;

        final File modulesDir = new File(jahiaDir, "modules");

        if (!sharedModulesDir.isDirectory() || !modulesDir.isDirectory()) {
            return new File[0];
        }

        packageFiles = sharedModulesDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                File file = new File(dir, name);
                if (!file.isFile()
                        && !(file.getName().toLowerCase().endsWith(".war") || file.getName().toLowerCase()
                                .endsWith(".jar"))) {
                    return false;
                }
                JarFile jar = null;
                try {
                    jar = new JarFile(file);
                    Manifest manifest = jar.getManifest();
                    String rootFolder = null;
                    if (manifest != null) {
                        rootFolder = (String) manifest.getMainAttributes().get(
                                new Attributes.Name("root-folder"));
                    }
                    if (rootFolder == null) {
                        rootFolder = file.getName().substring(0, file.getName().length() - 4);
                    }
                    if (rootFolder != null) {
                        File deployedModule = new File(modulesDir, rootFolder);
                        if (!deployedModule.exists() || deployedModule.lastModified() < file.lastModified()) {
                            return true;
                        }
                    }
                } catch (IOException e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                } finally {
                    if (jar != null) {
                        try {
                            jar.close();
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }

                return false;
            }
        });

        return packageFiles;
    }

    public void lifecycleEvent(LifecycleEvent event) {
        if (Lifecycle.BEFORE_START_EVENT.equals(event.getType())) {
            if (!(event.getLifecycle() instanceof StandardHost)) {
                log.log(Level.SEVERE, "This listener should be nested inside <Host/> element");
                return;
            }

            long timer = System.currentTimeMillis();

            StandardHost host = (StandardHost) event.getLifecycle();
            StandardEngine engine = (StandardEngine) host.getParent();

            File jahiaWebApp = detectJahiaWebAppPath(new File(engine.getBaseDir(), host.getAppBase()));
            if (jahiaWebApp == null) {
                log.log(Level.WARNING, "Jahia Web application not found. Skipping.");
                return;
            }
            log.info("Jahia Web application found at " + jahiaWebApp);

            deploySharedTemplatePackages(jahiaWebApp);

            log.info("...done checking modules in " + (System.currentTimeMillis() - timer) + " ms");
        }
    }

}
