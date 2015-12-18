/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
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
package org.jahia.utils.maven.plugin.support;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.jahia.utils.osgi.PackageUtils;
import org.jahia.utils.osgi.parsers.PackageInfo;
import org.jahia.utils.osgi.parsers.ParsingContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Aether helper related utility methods.
 * 
 * @author Sergiy Shyrkov
 */
public final class MavenAetherHelperUtils {

    public static boolean doesJarHavePackageName(File jarFile, String packageName, Log log) {
        JarInputStream jarInputStream = null;
        if (jarFile == null) {
            log.warn("File is null !");
            return false;
        }
        if (!jarFile.exists()) {
            log.warn("File " + jarFile + " does not exist !");
            return false;
        }
        log.debug("Scanning JAR " + jarFile + "...");
        try {
            jarInputStream = new JarInputStream(new FileInputStream(jarFile));
            JarEntry jarEntry = null;
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                String jarPackageName = jarEntry.getName().replaceAll("/", ".");
                if (jarPackageName.endsWith(".")) {
                    jarPackageName = jarPackageName.substring(0, jarPackageName.length() - 1);
                }
                if (jarPackageName.equals(packageName)) {
                    return true;
                }
            }
        } catch (IOException e) {
            log.error(e);
            ;
        } finally {
            IOUtils.closeQuietly(jarInputStream);
        }
        return false;
    }

    public static Set<PackageInfo> getJarPackages(File jarFile, boolean optionalJar, String version, ParsingContext parsingContext, Log log) {
        JarInputStream jarInputStream = null;
        Set<PackageInfo> packageInfos = new LinkedHashSet<PackageInfo>();
        if (jarFile == null) {
            log.warn("File is null !");
            return packageInfos;
        }
        if (!jarFile.exists()) {
            log.warn("File " + jarFile + " does not exist !");
            return packageInfos;
        }
        log.debug("Scanning JAR " + jarFile + "...");
        try {
            jarInputStream = new JarInputStream(new FileInputStream(jarFile));
            JarEntry jarEntry = null;
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                String jarPackageName = jarEntry.getName().replaceAll("/", ".");
                if (jarPackageName.endsWith(".")) {
                    jarPackageName = jarPackageName.substring(0, jarPackageName.length() - 1);
                }
                if (jarPackageName.startsWith("META-INF") ||
                        jarPackageName.startsWith("WEB-INF") ||
                        jarPackageName.startsWith("OSGI-INF")) {
                    continue;
                }
                packageInfos.addAll(PackageUtils.getPackagesFromClass(jarPackageName, optionalJar, version, jarFile.getCanonicalPath(), parsingContext));
            }
        } catch (IOException e) {
            log.error(e);
            ;
        } finally {
            IOUtils.closeQuietly(jarInputStream);
        }
        return packageInfos;
    }

    public static String getCoords(Artifact artifact) {
        String artifactCoords = artifact.getGroupId() + ":" + artifact.getArtifactId();
        if (StringUtils.isNotEmpty(artifact.getType()) && !("*".equals(artifact.getType()))) {
            artifactCoords += ":" + artifact.getType();
        }
        if (StringUtils.isNotEmpty(artifact.getClassifier())) {
            artifactCoords += ":" + artifact.getClassifier();
        }
        if (StringUtils.isNotEmpty(artifact.getBaseVersion()) && !("*".equals(artifact.getBaseVersion()))) {
            artifactCoords += ":" + artifact.getBaseVersion();
        }

        return artifactCoords;
    }

    public static String getDiskPath(Artifact artifact) {
        String artifactDiskPath = artifact.getGroupId();
        artifactDiskPath = artifactDiskPath.replaceAll("\\.", "/");
        artifactDiskPath += "/" + artifact.getArtifactId();
        if (StringUtils.isNotEmpty(artifact.getBaseVersion()) && !("*".equals(artifact.getBaseVersion()))) {
            artifactDiskPath += "/" + artifact.getBaseVersion();
        }
        artifactDiskPath += "/" + artifact.getArtifactId();
        if (StringUtils.isNotEmpty(artifact.getBaseVersion()) && !("*".equals(artifact.getBaseVersion()))) {
            artifactDiskPath += "-" + artifact.getBaseVersion();
        }
        if (StringUtils.isNotEmpty(artifact.getClassifier())) {
            artifactDiskPath += "-" + artifact.getClassifier();
        }
        if (StringUtils.isNotEmpty(artifact.getType()) && !("*".equals(artifact.getType()))) {
            artifactDiskPath += "." + artifact.getType();
        }
        return artifactDiskPath;
    }

    static String getTrail(List<String> dependencyTrail) {
        return StringUtils.join(dependencyTrail, " -> ");
    }

    static String getTrailPadding(Deque<String> dependencyTrail) {
        StringBuffer padding = new StringBuffer();
        for (int i=0; i < dependencyTrail.size(); i++) {
            padding.append("  ");
        }
        padding.append(dependencyTrail.peek());
        padding.append(" ");
        return padding.toString();
    }

}
