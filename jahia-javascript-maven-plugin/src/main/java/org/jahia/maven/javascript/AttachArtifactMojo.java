package org.jahia.maven.javascript;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;
import java.io.File;

/**
 * Attaches the built package (<code>*.tgz</code> file) as the Maven artifact.
 */
@Mojo(name = "attach-artifact", defaultPhase = LifecyclePhase.PACKAGE)
public class AttachArtifactMojo extends AbstractYarnMojo {

    protected static final String DEFAULT_PACKAGE_PATH = "dist/package.tgz";

    /**
     * Path to the package.tgz file.
     * Can be:
     * - Relative to the working directory (e.g., "dist/package.tgz" or "custom/output.tgz")
     * - An absolute path (e.g., "/absolute/path/to/package.tgz")
     */
    @Parameter(property = "jahia.js.packageFile")
    protected String packageFile;

    @Inject
    public AttachArtifactMojo(MavenProject mavenProject, MavenSession mavenSession, BuildPluginManager pluginManager) {
        super(mavenProject, mavenSession, pluginManager);
    }

    @Override
    public void execute() throws MojoExecutionException {
        // Determine the package file path
        String packagePath = (packageFile != null) ? packageFile : DEFAULT_PACKAGE_PATH;

        // Resolve the file - if it's relative, make it relative to workingDirectory
        File resolvedPackageFile = new File(packagePath);
        if (!resolvedPackageFile.isAbsolute()) {
            resolvedPackageFile = new File(workingDirectory, packagePath);
        }

        if (!resolvedPackageFile.exists()) {
            throw new MojoExecutionException("Package file not found: " + resolvedPackageFile.getAbsolutePath());
        }

        // Set the artifact file on the project
        mavenProject.getArtifact().setFile(resolvedPackageFile);

        getLog().info("Artifact " + resolvedPackageFile.getAbsolutePath() + " attached successfully");
    }
}

