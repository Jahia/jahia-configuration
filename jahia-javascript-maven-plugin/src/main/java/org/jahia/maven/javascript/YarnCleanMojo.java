package org.jahia.maven.javascript;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;

@Mojo(name = "yarn-clean", defaultPhase = LifecyclePhase.CLEAN)
public class YarnCleanMojo extends AbstractYarnMojo {

    private static final String DEFAULT_COMMAND = "clean";
    @Parameter(property = "yarnClean.command")
    protected String command;

    @Inject
    public YarnCleanMojo(MavenProject mavenProject, MavenSession mavenSession, BuildPluginManager pluginManager) {
        super(mavenProject, mavenSession, pluginManager);
    }

    @Override
    public void execute() throws MojoExecutionException {
        executeYarnCommand(command, DEFAULT_COMMAND);
    }
}

