package org.jahia.utils.osgi;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Jar;

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
        Map<String, Map<String, String>> imports = analyzer.getImports();
        System.out.println("Imports for " + jarFile + ":");
        StringBuilder importBuffer = new StringBuilder();
        Set<String> packageNames = new TreeSet<String>(imports.keySet());
        for (String packageName : packageNames) {
            importBuffer.append("- " + packageName);
            for (Map.Entry<String, String> importEntryEntry : imports.get(packageName).entrySet()) {
                importBuffer.append(";" + importEntryEntry.getKey() + "=\"" + importEntryEntry.getValue() + "\"");
            }
            importBuffer.append("\n");
        }
        System.out.println(importBuffer.toString());
    }

    public static Set<String> getUsedBy(Analyzer analyzer, String packageToFind) {
        Set<String> set = new TreeSet<String>();
        for (Iterator<Map.Entry<String, Set<String>>> i = analyzer.getUses().entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<String, Set<String>> entry = i.next();
            Set<String> used = entry.getValue();
            if (used.contains(packageToFind))
                set.add(entry.getKey());
        }
        return set;
    }


}
