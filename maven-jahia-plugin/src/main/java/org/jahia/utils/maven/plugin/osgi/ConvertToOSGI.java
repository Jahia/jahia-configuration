package org.jahia.utils.maven.plugin.osgi;


import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.jahia.utils.maven.plugin.AbstractManagementMojo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jahia server deployment mojo.
 *
 * @goal convert-to-osgi
 * @requiresDependencyResolution runtime
 */
public class ConvertToOSGI extends AbstractManagementMojo {

    private static Map<String, String> jahiaManifestAttributes = new HashMap<String, String>();

    static {
        // depends, module-type and root-folder are not here to handle their default values
        jahiaManifestAttributes.put("definitions", "Jahia-Definitions");
        jahiaManifestAttributes.put("initial-imports", "Jahia-Initial-Imports");
        jahiaManifestAttributes.put("resource-bundle", "Jahia-Resource-Bundle");
        jahiaManifestAttributes.put("deploy-on-site", "Jahia-Deploy-On-Site");
    }

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        if (!checkProjectParent(project, "org.jahia.modules", "jahia-modules")) {
            throw new MojoExecutionException("Project must inherit from org.jahia.modules:jahia-modules");
        }

        File webapp = new File(baseDir, "src/main/webapp");
        File resources = new File(baseDir, "src/main/resources");
        try {
            if (resources.exists()) {
                File oldWorkflowDir = new File(resources, "org/jahia/services/workflow");
                File newWorkflowDirParent = new File(resources, "org/jahia/modules/custom");
                if (oldWorkflowDir.exists()) {
                    FileUtils.moveDirectoryToDirectory(oldWorkflowDir, newWorkflowDirParent, true);
                }
            }
            if (webapp.exists()) {
                FileUtils.deleteQuietly(new File(webapp, "WEB-INF/web.xml"));
                moveWithMerge(webapp, resources);
            }

            parsePom();
        } catch (DocumentException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private boolean checkProjectParent(MavenProject p, String groupId, String artifactId) {
        MavenProject parent = p.getParent();
        if (parent == null) {
            return false;
        }
        if (groupId.equals(parent.getGroupId()) && artifactId.equals(parent.getArtifactId())) {
            return true;
        } else {
            return checkProjectParent(parent, groupId, artifactId);
        }
    }

    private void moveWithMerge(File src, File dst) throws IOException {
        // @todo this doesn't handle SVN directories properly

        File[] files = src.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return !name.startsWith(".");
            }
        });

        for (File file : files) {
            if (file.isDirectory()) {
                File subDir = new File(dst, file.getName());
                if (subDir.exists()) {
                    moveWithMerge(file, subDir);
                } else {
                    FileUtils.moveDirectoryToDirectory(file, dst, true);
                }
            } else {
                FileUtils.moveFileToDirectory(file, dst, true);
            }
        }
    }

    private void parsePom() throws DocumentException, IOException {
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

        generateBundlePlugin(manifestEntries, instructions, instructionsTemplate);
        if (!instructions.elements().isEmpty()) {
            plugins.add(pluginTemplate.detach());
        }

        // Export pom
        XMLWriter writer = new XMLWriter(new FileOutputStream(pom), OutputFormat.createPrettyPrint());
        writer.write(pomDocument);
        writer.close();
    }

    private void generateBundlePlugin(Element manifestEntries, Element instructions, Element instructionsTemplate) {

        Element depends = manifestEntries.element("depends");
        String dependsStr = null;
        if (depends != null) {
            dependsStr = depends.getText();
        }
        if (dependsStr != null && !dependsStr.trim().equals("default") && !dependsStr.trim().equals("Default Jahia Templates")) {
            instructions.addElement("Jahia-Depends").setText(dependsStr);
        }

        Element moduleType = manifestEntries.element("module-type");
        String moduleTypeStr = null;
        if (moduleType != null) {
            moduleTypeStr = moduleType.getText();
        }
        if (moduleTypeStr != null && !moduleTypeStr.equals("module")) {
            instructions.addElement("Jahia-Module-Type").setText(moduleTypeStr);
        }

        for (String name : jahiaManifestAttributes.keySet()) {
            Element element = manifestEntries.element(name);
            if (element != null) {
                element.setName(jahiaManifestAttributes.get(name));
                instructions.add(element.detach());
            }
        }
    }
}
