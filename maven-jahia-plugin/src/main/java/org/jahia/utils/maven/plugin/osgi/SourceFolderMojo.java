package org.jahia.utils.maven.plugin.osgi;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.tika.io.IOUtils;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * A goal that checks the dependencies of a generated OSGi bundle JAR against the project dependencies, and reports
 * any missing packages that weren't found in any dependency export.
 *
 * @goal update-jahia-source-folder
 * @phase package
 */
public class SourceFolderMojo extends AbstractMojo {
    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    private MavenProject project;

    /**
     * Classifier type of the bundle to be installed.  For example, "jdk14".
     * Defaults to none which means this is the project's main bundle.
     *
     * @parameter
     */
    protected String classifier;

    /**
     * @component
     */
    private ArchiverManager archiverManager;

    /**
     * @component
     */
    private ArtifactHandlerManager artifactHandlerManager;

    /**
     * @component
     */
    private MavenProjectHelper mavenProjectHelper;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String extension = project.getPackaging();
        if ("bundle".equals(extension)) {
            extension = "jar";
        }

        String artifactFilePath = project.getBuild().getDirectory() + File.pathSeparator + project.getBuild().getFinalName() + "." + extension;

        File artifactFile = new File(artifactFilePath);
        if (!artifactFile.exists()) {
            throw new MojoExecutionException("No artifact generated for project, was the goal called in the proper phase (should be verify) ?");
        }
        try (JarFile jarFile = new JarFile(artifactFile)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest.getMainAttributes() == null) {
                throw new MojoExecutionException("Error reading OSGi bundle manifest data from artifact " + artifactFile);
            }
            String groupId = project.getGroupId();
            boolean isModule = groupId.equals("org.jahia.module") || groupId.endsWith(".jahia.modules") || manifest.getMainAttributes().containsKey(new Attributes.Name("Jahia-Module-Type"));
            if (isModule) {
                String sourceFolder = "/var/jahia/sources/" + project.getBasedir().getName();
                getLog().info("Updating Jahia-Source-Folders to " + sourceFolder);
                manifest.getMainAttributes().putValue("Jahia-Source-Folders", sourceFolder);
                File expandedJarDirectory = unpackBundle(artifactFile);
                getLog().info("Extract JAR " + artifactFile + " contents to directory " + expandedJarDirectory);
                if (expandedJarDirectory == null) {
                    getLog().error("Error unpacking artifact " + artifactFile + " aborting bundle update");
                    return;
                }
                File manifestFile = new File(expandedJarDirectory, "META-INF/MANIFEST.MF");
                if (manifestFile.exists()) {
                    getLog().info("Overwriting existing META-INF/MANIFEST file");
                } else {
                    getLog().warn("Missing META-INF/MANIFEST.MF file in bundle, how did that happen ?");
                }
                FileOutputStream manifestFileOutputStream = null;
                try {
                    manifestFileOutputStream = new FileOutputStream(manifestFile);
                    manifest.write(manifestFileOutputStream);
                } catch (IOException e) {
                    getLog().error("Error writing new META-INF/MANIFEST.MF file", e);
                } finally {
                    IOUtils.closeQuietly(manifestFileOutputStream);
                }
                packBundle(artifactFile, manifestFile, expandedJarDirectory);

                try {
                    FileUtils.deleteDirectory(expandedJarDirectory);
                    getLog().info("Deleted temporary JAR extraction directory " + expandedJarDirectory);
                } catch (IOException e) {
                    getLog().error("Error purging temporary extracted JAR directory " + expandedJarDirectory, e);
                }

                Artifact mainArtifact = project.getArtifact();

                if ("bundle".equals(mainArtifact.getType())) {
                    // workaround for MNG-1682: force maven to install artifact using the "jar" handler
                    mainArtifact.setArtifactHandler(artifactHandlerManager.getArtifactHandler("jar"));
                }

                if (null == classifier || classifier.trim().length() == 0) {
                    mainArtifact.setFile(artifactFile);
                } else {
                    mavenProjectHelper.attachArtifact(project, artifactFile, classifier);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error reading OSGi bundle manifest data from artifact " + artifactFile, e);
        }
    }

    private File unpackBundle(File jarFile) {
        File outputDir = new File(project.getBuild().getOutputDirectory(), jarFile.getName() + "-" + System.currentTimeMillis());
        if (outputDir.exists()) {
            getLog().error("Problem unpacking " + jarFile + " to " + outputDir + " : directory already exists !");
            return null;
        }

        try {
            /*
             * this directory must exist before unpacking, otherwise the plexus
             * unarchiver decides to use the current working directory instead!
             */
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            UnArchiver unArchiver = archiverManager.getUnArchiver("jar");
            unArchiver.setDestDirectory(outputDir);
            unArchiver.setSourceFile(jarFile);
            unArchiver.extract();
        } catch (Exception e) {
            getLog().error("Problem unpacking " + jarFile + " to " + outputDir, e);
            return null;
        }
        return outputDir;
    }

    private void packBundle(File jarFile, File manifestFile, File contentDirectory) {
        try {
            JarArchiver archiver = (JarArchiver) archiverManager.getArchiver("jar");

            archiver.setManifest(manifestFile);
            archiver.setDestFile(jarFile);

            archiver.addDirectory(contentDirectory, null, null);
            archiver.createArchive();
        } catch (Exception e) {
            getLog().error("Problem packing " + jarFile + " with contents from  " + contentDirectory, e);
        }

    }
}
