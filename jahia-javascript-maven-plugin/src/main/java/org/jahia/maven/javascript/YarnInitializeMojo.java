package org.jahia.maven.javascript;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;

@Mojo(name = "yarn-initialize", defaultPhase = LifecyclePhase.INITIALIZE)
public class YarnInitializeMojo extends AbstractYarnMojo {

    private static final String COMMAND = "install";

    @Inject
    public YarnInitializeMojo(MavenProject mavenProject, MavenSession mavenSession, BuildPluginManager pluginManager) {
        super(mavenProject, mavenSession, pluginManager);
    }

    @Override
    public void execute() throws MojoExecutionException {
        executeYarnCommand(COMMAND, null, false);
    }
}

