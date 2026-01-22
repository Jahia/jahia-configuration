package org.jahia.maven.javascript;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;
import java.io.File;

@Mojo(name = "attach-artifact", defaultPhase = LifecyclePhase.PACKAGE)
public class AttachArtifactMojo extends AbstractMojo {

    /**
     * Path to the package.tgz file (can be relative to the working directory or absolute).
     */
    @Parameter(property = "packageFile", defaultValue = "${project.basedir}/dist/package.tgz")
    protected File packageFile;

    private final MavenProject mavenProject;

    @Inject
    public AttachArtifactMojo(MavenProject mavenProject) {
        this.mavenProject = mavenProject;
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (!packageFile.exists()) {
            throw new MojoExecutionException("Package file not found: " + packageFile.getAbsolutePath());
        }

        // Set the artifact file on the project
        mavenProject.getArtifact().setFile(packageFile);

        getLog().info("Artifact " + packageFile.getAbsolutePath() + " attached successfully");
    }
}

