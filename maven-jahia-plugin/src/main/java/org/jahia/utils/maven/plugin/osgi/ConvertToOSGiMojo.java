package org.jahia.utils.maven.plugin.osgi;


import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.manager.BasicScmManager;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.git.gitexe.GitExeScmProvider;
import org.apache.maven.scm.provider.svn.svnexe.SvnExeScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.tika.io.IOUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.jahia.commons.Version;
import org.jahia.utils.maven.plugin.AbstractManagementMojo;
import org.jahia.utils.migration.Migrators;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jahia utility goal to help with the migration of Jahia modules to OSGi packaging.
 *
 * @goal convert-to-osgi
 * @requiresDependencyResolution runtime
 */
public class ConvertToOSGiMojo extends AbstractManagementMojo {

    private static Map<String, String> jahiaManifestAttributes = new HashMap<String, String>();

    static {
        // depends, module-type and root-folder are not here to handle their default values
        jahiaManifestAttributes.put("definitions", "Jahia-Definitions");
        jahiaManifestAttributes.put("initial-imports", "Jahia-Initial-Imports");
        jahiaManifestAttributes.put("resource-bundle", "Jahia-Resource-Bundle");
        jahiaManifestAttributes.put("deploy-on-site", "Jahia-Deploy-On-Site");
    }

    /**
     * @parameter expression="${jahia.osgi.conversion.performMigration}"
     *
     */
    private boolean performMigration = false;

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {

        ScmManager scmManager = new BasicScmManager();
        scmManager.setScmProvider("svn", new SvnExeScmProvider());
        scmManager.setScmProvider("git", new GitExeScmProvider());
        File pomXmlFile = new File(baseDir, "pom.xml");
        String scmURL = null;
        Reader reader = null;
        ScmRepository scmRepository = null;
        try {
            reader = new FileReader(pomXmlFile);
            Model model = new MavenXpp3Reader().read(reader);
            scmURL = model.getScm().getConnection();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(reader);
        }
        try {
            scmRepository = scmManager.makeScmRepository(scmURL);
        } catch (ScmRepositoryException e) {
            e.printStackTrace();
        } catch (NoSuchScmProviderException e) {
            e.printStackTrace();
        }
        File webapp = new File(baseDir, "src/main/webapp");
        File resources = new File(baseDir, "src/main/resources");
        try {
            if (resources.exists()) {
                File oldWorkflowDir = new File(resources, "org/jahia/services/workflow");
                File newWorkflowDirParent = new File(resources, "org/jahia/modules/custom");
                if (oldWorkflowDir.exists()) {
                    ScmFileSet filesToRemove = new ScmFileSet(oldWorkflowDir,null,null);
                    getLog().info("Moving " + oldWorkflowDir + " to " + newWorkflowDirParent + "...");
                    FileUtils.moveDirectoryToDirectory(oldWorkflowDir, newWorkflowDirParent, true);
                    scmManager.remove(scmRepository, filesToRemove, "remove workflow dir");
                    scmManager.add(scmRepository, new ScmFileSet(newWorkflowDirParent,null,null));
                }
            }
            if (webapp.exists()) {
                List<File> filesToRemove = listFilesAndDirectories(webapp);
                getLog().info("Removing " + new File(webapp, "WEB-INF/web.xml") + " no longer needed...");
                File webXml = new File(webapp, "WEB-INF/web.xml");
                FileUtils.deleteQuietly(webXml);
                getLog().info("Moving contents of directory " + webapp + " into directory " + resources + "...");
                moveWithMerge(webapp, resources);
                scmManager.add(scmRepository, new ScmFileSet(new File(""), resources));
                List<File> filesToAdd = listFilesAndDirectories(resources);
                scmManager.add(scmRepository, new ScmFileSet(resources,filesToAdd), "add resources files");
                scmManager.remove(scmRepository, new ScmFileSet(webapp,filesToRemove), "remove webapps files");
            }

            getLog().info("Performing Maven project modifications...");
            parsePom();
            scmManager.add(scmRepository, new ScmFileSet( new File( "" ), pomXmlFile ));

            if (performMigration) {
                getLog().info("Performing needed migration modifications");
            } else {
                getLog().info("Checking for migration issues...");
            }
            List<String> messages = checkForMigrationIssues(baseDir, performMigration);
            if (messages.size() > 0) {
                getLog().info("=====================================================================================================================");
                getLog().info("Transformation messages:");
                getLog().info("---------------------------------------------------------------------------------------------------------------------");
                for (String message : messages) {
                    getLog().info(message);
                }
                getLog().info("---------------------------------------------------------------------------------------------------------------------");
                if (!performMigration) {
                    getLog().info("None of the source code files were modified. If you would like to have the goal convert the files, please");
                    getLog().info("relaunch the convert-to-osgi goal using the following parameter : ");
                    getLog().info("   mvn jahia:convert-to-osgi -Djahia.osgi.conversion.performMigration=true");
                    getLog().info("---------------------------------------------------------------------------------------------------------------------");
                } else {
                    getLog().info("Source files were modified. Please review all code changes to make sure everything is ok as detection may have ");
                    getLog().info("matched false statements (100% automatic conversion is not guaranteed by this tool.");
                    getLog().info("---------------------------------------------------------------------------------------------------------------------");
                }
            }
        } catch (DocumentException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScmException e) {
            e.printStackTrace();
        }
    }

    private List<File> listFilesAndDirectories(File rootFolder) {
        List<File> files = new ArrayList<File>();
        for (File f : rootFolder.listFiles()) {
            files.add(f);
            if (f.isDirectory()) {
                files.addAll(listFilesAndDirectories(f));
            }
        }
        return files;
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
        } else {
            if (packaging.getTextTrim().toLowerCase().equals("war")) {
                packaging.setText("bundle");
            } else {
                getLog().info("Non WAR packaging found : " + packaging.getTextTrim() + ", not modifying it to bundle, but you might want to double-check this.");
            }
        }

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
        if (plugins != null) {
            Element mavenWarPluginArtifactId = (Element) plugins.selectSingleNode("//*[local-name()='artifactId'][text()='maven-war-plugin']");
            if (mavenWarPluginArtifactId != null) {
                Element previousPluginConfig = (Element) mavenWarPluginArtifactId.getParent().detach();
                Element manifestEntries = previousPluginConfig.element("configuration").element("archive").element("manifestEntries");

                Element pluginTemplate = (Element) bundleModuleDocument.selectSingleNode("/project/*[local-name()='build']/*[local-name()='plugins']/*[local-name()='plugin']");
                if (pluginTemplate != null) {
                    Element instructionsTemplate = (Element) pluginTemplate.element("configuration").element("instructions").detach();
                    Element instructions = pluginTemplate.element("configuration").addElement("instructions");

                    generateBundlePlugin(manifestEntries, instructions, instructionsTemplate);
                    if (!instructions.elements().isEmpty()) {
                        plugins.add(pluginTemplate.detach());
                    }
                }
            }
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

    public List<String> checkForMigrationIssues(File currentFile, boolean performMigration) throws FileNotFoundException {
        List<String> messages = new ArrayList<String>();
        if (currentFile.isFile()) {
            FileInputStream fileInputStream = new FileInputStream(currentFile);
            ByteArrayOutputStream byteArrayOutputStream = null;
            if (performMigration) {
                // we use byte arrays as we will be writing to the same file as the input file
                byteArrayOutputStream = new ByteArrayOutputStream();
            }
            messages.addAll(Migrators.getInstance().migrate(fileInputStream, byteArrayOutputStream, currentFile.getPath(), new Version("6.6"), new Version("7.0"), performMigration));
            IOUtils.closeQuietly(fileInputStream);
            if (performMigration && messages.size() > 0 && byteArrayOutputStream.size() > 0) {
                getLog().info("Renaming existing file " + currentFile + " to " + currentFile.getName() + ".backup");
                if (!currentFile.renameTo(new File(currentFile.getPath() + ".backup"))) {
                    getLog().error("Error renaming " + currentFile + "!");
                }
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                getLog().info("Writing modified file " + currentFile + "...");
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
                FileOutputStream fileOutputStream = new FileOutputStream(currentFile);
                long bytesCopied = 0;
                try {
                    bytesCopied = IOUtils.copyLarge(byteArrayInputStream, fileOutputStream);
                    fileOutputStream.flush();
                } catch (IOException e) {
                    getLog().error("Error writing to file " + currentFile, e);
                }
                getLog().info("Wrote " + bytesCopied + " bytes to file " + currentFile);
                IOUtils.closeQuietly(fileOutputStream);
            }
            return messages;
        }
        if (!currentFile.isDirectory()) {
            getLog().warn("Found non-file or directory at " + currentFile + ",ignoring...");
            return messages;
        }
        File[] currentChildren = currentFile.listFiles();
        for (File currentChild : currentChildren) {
            messages.addAll(checkForMigrationIssues(currentChild, performMigration));
        }
        return messages;
    }
}
