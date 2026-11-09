package org.jahia.maven.javascript;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;

/**
 * Builds the JavaScript project (typically runs a build script).
 */
@Mojo(name = "yarn-package", defaultPhase = LifecyclePhase.PACKAGE)
public class YarnPackageMojo extends AbstractYarnMojo {

    private static final String DEFAULT_COMMAND = "package";

    @Parameter(property = "jahia.js.yarnPackage.command")
    protected String yarnPackageCommand;

    @Inject
    public YarnPackageMojo(MavenProject mavenProject, MavenSession mavenSession, BuildPluginManager pluginManager) {
        super(mavenProject, mavenSession, pluginManager);
    }

    @Override
    public void execute() throws MojoExecutionException {
        // it is required to package the JS project, so the build should fail if the default command, when used, is not defined
        executeYarnCommand(yarnPackageCommand, DEFAULT_COMMAND, false);
    }
}

