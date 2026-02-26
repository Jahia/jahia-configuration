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
 * Runs verification tasks (tests, linting, etc.).
 */
@Mojo(name = "yarn-verify", defaultPhase = LifecyclePhase.VERIFY)
public class YarnVerifyMojo extends AbstractYarnMojo {

    private static final String DEFAULT_COMMAND = "verify";

    @Parameter(property = "jahia.js.yarnVerify.command")
    protected String yarnVerifyCommand;

    @Inject
    public YarnVerifyMojo(MavenProject mavenProject, MavenSession mavenSession, BuildPluginManager pluginManager) {
        super(mavenProject, mavenSession, pluginManager);
    }

    @Override
    public void execute() throws MojoExecutionException {
        executeYarnCommand(yarnVerifyCommand, DEFAULT_COMMAND, true);
    }
}

