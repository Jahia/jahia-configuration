package org.jahia.configuration.modules;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.jahia.configuration.logging.AbstractLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModuleDeployer {
    private File output;
    private AbstractLogger logger;
    private boolean deployModuleForOSGiTransformation;

    public ModuleDeployer(File output, AbstractLogger logger, boolean deployModuleForOSGiTransformation) {
        this.output = output;
        this.logger = logger;
        this.deployModuleForOSGiTransformation = deployModuleForOSGiTransformation;
    }

    public void deployModule(File file) throws IOException {
        if (deployModuleForOSGiTransformation) {
            logger.info("Copy module to OSGi transformation directory");
        } else {
            logger.info("Copy modules JAR " + file.getName() + " to shared modules folder");
        }
        FileUtils.copyFileToDirectory(file, output);
        if (!deployModuleForOSGiTransformation) {
            copyJars(file, new File(output,"../../.."));
            copyDbScripts(file, new File(output,"../../.."));
        }
    }

    /**
     * Checks if the jar is already deployed
     * and we don't try to deploy a new version (using "last modified time")
     * @todo should be put in a central location to be reused in the mojos and listeners
     * @param entry
     * @param targetDir
     * @return isNewer
     */
    private boolean isClassNewer(JarEntry entry, File targetDir) {
    	File fEntry = new File(targetDir, entry.getName());
    	if (fEntry.exists() && fEntry.lastModified() >= entry.getTime()) {
    		return false;
    	}
    	return true;
    }
    
    public void copyJars(File warFile, File targetDir) {
        JarFile war = null;
        try {
            war = new JarFile(warFile);
            int deployed = 0;
            if (war.getJarEntry("WEB-INF/lib") != null) {
                Enumeration<JarEntry> entries = war.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    
                    if (isClassNewer(entry, targetDir)) {
	                    if (entry.getName().startsWith("WEB-INF/lib/") && entry.getName().endsWith(".jar")) {
	                        deployed++;
	                        InputStream source = war.getInputStream(entry);
	                        File libsDir = new File(targetDir, "WEB-INF/lib");
	                        if (!libsDir.exists()) {
	                            libsDir.mkdirs();
	                        }
	                        File targetFile = new File(targetDir, entry.getName());
	                        FileOutputStream target = new FileOutputStream(targetFile);
	                        IOUtils.copy(source, target);
	                        IOUtils.closeQuietly(source);
	                        target.flush();
	                        IOUtils.closeQuietly(target);
	                        if (entry.getTime() > 0) {
	                            targetFile.setLastModified(entry.getTime());
	                        }
	                    }
                    } else {
                    	logger.info(entry.getName() + " is already deployed and newer than the current entry");
                    }
                }
            }
            if (deployed > 0) {
                logger.info("Copied " + deployed + " JARs from " + warFile.getName() + " to WEB-INF/lib");
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

    private void copyDbScripts(File warFile, File targetDir) {
        JarFile war = null;
        try {
            war = new JarFile(warFile);
            if (war.getJarEntry("META-INF/db") != null) {
            	war.close();
            	ZipUnArchiver unarch = new ZipUnArchiver(warFile);
            	File tmp = new File(targetDir, String.valueOf(System.currentTimeMillis()));
            	tmp.mkdirs();
            	unarch.extract("META-INF/db", tmp);
            	FileUtils.copyDirectory(new File(tmp, "META-INF/db"), new File(targetDir, "WEB-INF/var/db/sql/schema"));
            	FileUtils.deleteDirectory(tmp);
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


}
