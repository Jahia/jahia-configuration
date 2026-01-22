package org.jahia.maven.javascript;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import javax.inject.Inject;
import java.io.File;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Base class for all Jahia JavaScript mojos.
 * Provides common configuration and utilities.
 */
public abstract class AbstractYarnMojo extends AbstractMojo {

    private static final String FRONTEND_PLUGIN_GROUP_ID = "com.github.eirslett";
    private static final String FRONTEND_PLUGIN_ARTIFACT_ID = "frontend-maven-plugin";

    /**
     * The Maven project.
     */
    protected final MavenProject mavenProject;

    /**
     * The Maven session.
     */
    protected final MavenSession mavenSession;

    /**
     * The build plugin manager.
     */
    protected final BuildPluginManager pluginManager;

    /**
     * The plugin descriptor for this plugin (injected by Maven).
     */
    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    protected PluginDescriptor pluginDescriptor;

    /**
     * Working directory where node and yarn will be installed.
     */
    @Parameter(defaultValue = "${project.basedir}")
    protected File workingDirectory;

    /**
     * Constructor with dependency injection.
     *
     * @param mavenProject  the Maven project
     * @param mavenSession  the Maven session
     * @param pluginManager the build plugin manager
     */
    @Inject
    protected AbstractYarnMojo(MavenProject mavenProject, MavenSession mavenSession, BuildPluginManager pluginManager) {
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
        this.pluginManager = pluginManager;
    }

    protected void executeYarnCommand(String command) throws MojoExecutionException {
        if (StringUtils.isEmpty(command)) {
            throw new MojoExecutionException("No command specified!");
        }
        executeYarnCommand(command, null);
    }

    protected void executeYarnCommand(String command, String defaultCommand) throws MojoExecutionException {
        if (command == null) {
            getLog().debug("No command specified, using default command: " + defaultCommand);
            command = defaultCommand;
        }
        Xpp3Dom config = configuration(element(name("workingDirectory"), workingDirectory.getAbsolutePath()),
                element(name("arguments"), command));

        executeFrontendPlugin("yarn", config);
    }

    /**
     * Gets the version of the frontend-maven-plugin from this plugin's own dependencies.
     *
     * @return the version string, or a fallback version if not found
     */
    private String getFrontendPluginVersion() throws MojoExecutionException {
        // Get the version from this plugin's own dependencies
        return pluginDescriptor.getArtifacts().stream()
                .filter(artifact -> FRONTEND_PLUGIN_GROUP_ID.equals(artifact.getGroupId()) && FRONTEND_PLUGIN_ARTIFACT_ID.equals(
                        artifact.getArtifactId())).findFirst().map(org.apache.maven.artifact.Artifact::getVersion).orElseThrow(
                        () -> new MojoExecutionException(
                                FRONTEND_PLUGIN_GROUP_ID + ":" + FRONTEND_PLUGIN_ARTIFACT_ID + " not found in plugin dependencies!"));
    }

    /**
     * Executes a frontend-maven-plugin goal.
     *
     * @param goal          the goal to execute
     * @param configuration the configuration for the goal
     * @throws MojoExecutionException if execution fails
     */
    protected void executeFrontendPlugin(String goal, Xpp3Dom configuration) throws MojoExecutionException {
        getLog().debug("Executing frontend-maven-plugin goal '" + goal + "'...");
        executeMojo(plugin(groupId(FRONTEND_PLUGIN_GROUP_ID), artifactId(FRONTEND_PLUGIN_ARTIFACT_ID), version(getFrontendPluginVersion())),
                goal(goal), configuration, executionEnvironment(mavenProject, mavenSession, pluginManager));
    }
}

