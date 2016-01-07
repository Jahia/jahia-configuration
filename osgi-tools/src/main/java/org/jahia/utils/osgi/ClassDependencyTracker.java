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
package org.jahia.utils.osgi;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Packages;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.Manifest;

/**
 * This dependency tracker utility uses BND to track the package dependencies inside a JAR.
 * So this will only extract the dependencies from Java code, unless BND plugins are added
 * (not possible through the API yet).
 */
public class ClassDependencyTracker {

    public static Set<String> findDependencyInJar(final File jarFile, final String packageToFind, List<String> classPathElements) throws IOException {
        return findDependencyInJarUsingBND(jarFile, packageToFind, classPathElements);
    }

    public static Set<String> findDependencyInJarUsingBND(final File jarFile, final String packageToFind, List<String> classPathElements) throws IOException {
        Set<String> classesThatHaveDependency = new TreeSet<String>();
        Analyzer analyzer = new Analyzer();
        Jar bin = new Jar(jarFile);  // where our data is
        analyzer.setJar(bin);                // give bnd the contents

        // You can provide additional class path entries to allow
        // bnd to pickup export version from the packageinfo file,
        // Version annotation, or their manifests.
        // analyzer.addClasspath( new File("jar/spring.jar") );
        if (classPathElements != null) {
            for (String classPathElement : classPathElements) {
                File classPathElementFile = new File(classPathElement);
                if (classPathElementFile.exists()) {
                    analyzer.addClasspath(new File(classPathElement));
                } else {
                    // System.out.println("Ignoring inexisting class path element " + classPathElement);
                }
            }
        }

        // we just set dummy properties here for BND but we don't need them since we are only looking for a package use
        analyzer.setProperty("Bundle-SymbolicName", "org.osgi.core");
        analyzer.setProperty("Export-Package",
                "org.osgi.framework,org.osgi.service.event");
        analyzer.setProperty("Bundle-Version", "1.0");

        // this macro is the main work horse
        analyzer.setProperty("Jahia-Imports-Package", "${classes;IMPORTING;" + packageToFind + "}");

        // There are no good defaults so make sure you set the
        // Import-Package
        analyzer.setProperty("Import-Package", "*");

        // Calculate the manifest
        try {
            Manifest manifest = analyzer.calcManifest();

            Set<String> usedBy = getUsedBy(analyzer, packageToFind);
            String classNames = manifest.getMainAttributes().getValue("Jahia-Imports-Package");
            if (classNames != null) {
                String[] classNameArray = classNames.split(",");
                /*
                for (String className : classNameArray) {
                    System.out.println(jarFile + ": Class " + className + " uses package " + packageToFind);
                }
                */
                classesThatHaveDependency.addAll(Arrays.asList(classNameArray));
            }

            // dumpAnalyzerImports(jarFile, analyzer);

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return classesThatHaveDependency;
    }

    private static void dumpAnalyzerImports(File jarFile, Analyzer analyzer) {
        Packages imports = analyzer.getImports();
        System.out.println("Imports for " + jarFile + ":");
        StringBuilder importBuffer = new StringBuilder();
        Set<Descriptors.PackageRef> packageNames = new TreeSet<Descriptors.PackageRef>(imports.keySet());
        for (Descriptors.PackageRef packageName : packageNames) {
            importBuffer.append("- " + packageName.getFQN());
            for (Map.Entry<String, String> importEntryEntry : imports.get(packageName).entrySet()) {
                importBuffer.append(";" + importEntryEntry.getKey() + "=\"" + importEntryEntry.getValue() + "\"");
            }
            importBuffer.append("\n");
        }
        System.out.println(importBuffer.toString());
    }

    public static Set<String> getUsedBy(Analyzer analyzer, String packageToFind) {
        Set<String> set = new TreeSet<String>();
        for (Iterator<Map.Entry<Descriptors.PackageRef, List<Descriptors.PackageRef>>> i = analyzer.getUses().entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<Descriptors.PackageRef, List<Descriptors.PackageRef>> entry = i.next();
            List<Descriptors.PackageRef> used = entry.getValue();
            for (Descriptors.PackageRef packageRef : used) {
                if (packageRef.getFQN().equals(packageToFind)) {
                    set.add(entry.getKey().getFQN());
                }
            }
        }
        return set;
    }


}
