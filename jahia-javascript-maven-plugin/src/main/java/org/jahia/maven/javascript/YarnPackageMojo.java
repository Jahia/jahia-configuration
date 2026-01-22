package org.jahia.maven.javascript;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;

@Mojo(name = "yarn-package", defaultPhase = LifecyclePhase.PACKAGE)
public class YarnPackageMojo extends AbstractYarnMojo {

    private static final String DEFAULT_COMMAND = "build";
    @Parameter(property = "yarnPackage.command")
    protected String command;

    @Inject
    public YarnPackageMojo(MavenProject mavenProject, MavenSession mavenSession, BuildPluginManager pluginManager) {
        super(mavenProject, mavenSession, pluginManager);
    }

    @Override
    public void execute() throws MojoExecutionException {
        executeYarnCommand(command, DEFAULT_COMMAND);
    }
}

