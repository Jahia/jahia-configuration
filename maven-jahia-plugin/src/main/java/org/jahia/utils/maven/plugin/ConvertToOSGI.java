package org.jahia.utils.maven.plugin;


import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.*;

/**
 * Jahia server deployment mojo.
 * @goal convertToOSGI
 * @requiresDependencyResolution runtime
 */
public class ConvertToOSGI extends AbstractManagementMojo {

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        File webapp = new File(baseDir,"src/main/webapp");
        File resources = new File(baseDir,"src/main/resources");
        Set<String> rootFolders = new HashSet<String>();
        try {
            if (webapp.exists()) {
                File[] files = webapp.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return !name.startsWith(".");
                    }
                });

                for (File file : files) {
                    if (file.isDirectory()) {
                        rootFolders.add(file.getName());
                        FileUtils.moveDirectoryToDirectory(file, resources, true);
                    } else {
                        FileUtils.moveFileToDirectory(file, resources, true);
                    }
                }
            }

            final Set<String> allNonEmptyDirectories = new HashSet<String>();
            File directory = new File(baseDir, "src/main/java");
            if (directory.exists() && directory.isDirectory()) {
                Collection<File> packages = FileUtils.listFiles(directory, null, true);
                for (File file : packages) {
                    allNonEmptyDirectories.add(file.getPath().substring(directory.getPath().length()));
                }
            }

            parsePom(allNonEmptyDirectories, rootFolders);
        } catch (DocumentException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void parsePom(Set<String> allNonEmptyDirectories, Set<String> rootFolders) throws DocumentException, IOException {
        SAXReader reader = new SAXReader();
        File pom = new File(baseDir, "pom.xml");
        Document pomDocument = reader.read(pom);

        Document bundleModuleDocument = reader.read(getClass().getClassLoader().getResourceAsStream("bundleModule.xml"));

        Element root = pomDocument.getRootElement();

        // Set packaging
        Element packaging = root.element("packaging");
        if (packaging == null) {
            root.addElement("packaging");
        }
        packaging.setText("bundle");

        // Copy template dependencies
        Element dependencies = root.element("dependencies");
        if (dependencies == null) {
            dependencies = root.addElement("dependencies");
        }
        List dependenciesTemplate = bundleModuleDocument.selectNodes("/project/*[local-name()='dependencies']/*");
        for (Element dep : (Iterable<? extends Element>) dependenciesTemplate) {
            dependencies.add(dep.detach());
        }

        // Generate plugin instructions
        Element plugins = (Element) pomDocument.selectSingleNode("/project/*[local-name()='build']/*[local-name()='plugins']");
        Element previousPluginConfig = (Element) plugins.selectSingleNode("//*[local-name()='artifactId'][text()='maven-war-plugin']").getParent().detach();
        Element manifestEntries = previousPluginConfig.element("configuration").element("archive").element("manifestEntries");

        Element pluginTemplate = (Element) bundleModuleDocument.selectSingleNode("/project/*[local-name()='build']/*[local-name()='plugins']/*[local-name()='plugin']");
        Element instructionsTemplate = (Element) pluginTemplate.element("configuration").element("instructions").detach();
        Element instructions = pluginTemplate.element("configuration").addElement("instructions");
        plugins.add(pluginTemplate.detach());

        xxxx(root, manifestEntries, instructions, instructionsTemplate, allNonEmptyDirectories, rootFolders);

        // Export pom
        XMLWriter writer = new XMLWriter(new FileOutputStream(pom), OutputFormat.createPrettyPrint());
        writer.write(pomDocument);
        writer.close();
    }

    private void xxxx(Element root, Element manifestEntries, Element instructions, Element instructionsTemplate, Set<String> allNonEmptyDirectories, Set<String> rootFolders) {
        List<String> classPathEntries = new ArrayList<String>();

        if (classPathEntries.size() > 0) {
            StringBuffer bundleClassPath = new StringBuffer(".");
            for (String classPathEntry : classPathEntries) {
                bundleClassPath.append(",");
                bundleClassPath.append(classPathEntry);
            }
            instructions.addElement("Bundle-ClassPath").setText(bundleClassPath.toString());
        }

        Element version = root.element("version");
        if (version == null) {
            version = root.element("parent").element("version");
        }
        if (version != null) {
            String versionStr = version.getText();
            int dashPos = versionStr.indexOf("-");
            if (dashPos > -1) {
                versionStr = versionStr.substring(0, dashPos);
            }
            int underScorePos = versionStr.indexOf("_");
            if (underScorePos > -1) {
                versionStr = versionStr.substring(0, underScorePos);
            }
            instructions.addElement("Bundle-Version").setText(versionStr);
        }

        Set<String> exportPackageExcludes = new TreeSet<String>(new Comparator<String>() {
            public int compare(String s, String s1) {
                if (s.length() > s1.length()) {
                    return -1;
                } else if (s.length() < s1.length()) {
                    return 1;
                } else {
                    return s.compareTo(s1);
                }
            }
        });

        // calculate export package exclusions that are all non-empty directories minus the ones that contain
        // resources to export.
        exportPackageExcludes.addAll(allNonEmptyDirectories);

        String artifactId = root.element("artifactId").getText();
        String rootFolder = artifactId;
        String name = root.element("name").getText();
        if (rootFolder != null) {
            String packagePrefix = rootFolder.replaceAll("[ -]", "");

            instructions.addElement("Bundle-SymbolicName").setText(rootFolder);
            StringBuilder exportPackage = new StringBuilder("");
//            if (exportPackageIncludes.size() > 0) {
//                for (String exportPackageInclude : exportPackageIncludes) {
//                    exportPackage.append(exportPackageInclude);
//                    exportPackage.append(",");
//                }
//            }
            /*
            if (exportPackageExcludes.size() > 0) {
                for (String exportPackageExclude : exportPackageExcludes) {
                    exportPackage.append("!");
                    exportPackage.append(exportPackageExclude);
                    exportPackage.append(".*,");
                }
                //exportPackage.append("*,");
            } else {
                //exportPackage.append("*,");
            }
            */
            exportPackage.append(name.replaceAll("[ -]", ""));
            exportPackage.append(",");
            exportPackage.append(packagePrefix);
            instructions.addElement("Export-Package").setText(exportPackage.toString());

            Element depends = manifestEntries.element("depends");
            String dependsStr = "";
            if (depends != null) {
                dependsStr = depends.getText();
            }
            if (!dependsStr.contains("default") && !dependsStr.contains("Default Jahia Templates") && !rootFolder.equals("assets") && !rootFolder.equals("default")) {
                if (!dependsStr.equals("")) {
                    dependsStr += ",";
                }
                dependsStr += "default";
            }


            String[] dependsArray = dependsStr.split(",");
            StringBuilder importPackage = new StringBuilder("*");

            for (String dep : dependsArray) {
                if (!"".equals(dep)) {
                    importPackage.append(",");
                    dep = dep.replaceAll("[ -]", "");
                    importPackage.append(dep);
                }
            }

            Set<String> templateImportPackages = new TreeSet<String>();
            templateImportPackages.addAll(Arrays.asList(instructionsTemplate.element("Import-Package").getText().split(",")));

            List<String> alreadyImportedPackages = new ArrayList<String>(Arrays.asList(importPackage.toString().split(",")));
            for (String curImportPackage : templateImportPackages) {
                if (!alreadyImportedPackages.contains(curImportPackage)) {
                    importPackage.append(",");
                    importPackage.append(curImportPackage);
                    alreadyImportedPackages.add(curImportPackage);
                }
            }

            instructions.addElement("Import-Package").setText(importPackage.toString());

            StringBuilder staticResources = new StringBuilder("");
            for (String folder : rootFolders) {
                if (!folder.equals("META-INF") && !folder.equals("WEB-INF")) {
                    staticResources.append(",/");
                    staticResources.append(folder);
                }
            }
            if (staticResources.length() > 0) {
                instructions.addElement("Jahia-Static-Resources").setText(staticResources.substring(1));
            }
        }

        for (Element element : (Iterable<? extends Element>) instructionsTemplate.elements()) {
            if (instructions.element(element.getName()) == null) {
                instructions.add(element.detach());
            }
        }
        for (Element element : (Iterable<? extends Element>) manifestEntries.elements()) {
            if (instructions.element(element.getName()) == null) {
                instructions.add(element.detach());
            }
        }
    }
}
