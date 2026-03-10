package org.jahia.maven.javascript;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Base class for all Jahia JavaScript mojos.
 * Provides common configuration and utilities.
 */
public abstract class AbstractYarnMojo extends AbstractMojo {

    private static final String FRONTEND_PLUGIN_GROUP_ID = "com.github.eirslett";
    private static final String FRONTEND_PLUGIN_ARTIFACT_ID = "frontend-maven-plugin";

    protected final MavenProject mavenProject;
    protected final MavenSession mavenSession;
    protected final BuildPluginManager pluginManager;

    /**
     * The plugin descriptor for this plugin (injected by Maven).
     */
    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    protected PluginDescriptor pluginDescriptor;

    /**
     * Working directory where node and yarn will be installed.
     */
    @Parameter(property = "jahia.js.workingDirectory", defaultValue = "${project.basedir}")
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

    protected void executeYarnCommand(String command, String defaultCommand, boolean canSkipIfDefaultCommandNotDefined)
            throws MojoExecutionException {
        if (command == null) {
            getLog().debug("No command specified, using default command: " + defaultCommand);
            command = defaultCommand;
            if (canSkipIfDefaultCommandNotDefined && !hasPackageJsonScript(command)) {
                // it is fine if the default command does not exist when no command is specified, simply skip the execution
                getLog().info("Skipping execution as no script '" + command + "' is defined in package.json");
                return;
            }
        }
        String fullCommand = "yarn " + command;
        Xpp3Dom config = configuration(element(name("workingDirectory"), workingDirectory.getAbsolutePath()),
                element(name("arguments"), fullCommand));

        executeFrontendPlugin("corepack", config);
    }

    /**
     * Gets the version of the frontend-maven-plugin from this plugin's own dependencies.
     *
     * @return the version string, or throws an exception if it cannot be found
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

    /**
     * Checks if a script exists in package.json.
     *
     * @param scriptName the script name to check
     * @return true if the script exists, false otherwise
     */
    private boolean hasPackageJsonScript(String scriptName) {
        File packageJsonFile = new File(workingDirectory, "package.json");

        if (!packageJsonFile.exists()) {
            getLog().debug("package.json not found at: " + packageJsonFile.getAbsolutePath());
            return false;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(packageJsonFile);
            JsonNode scripts = root.get("scripts");

            if (scripts != null && scripts.has(scriptName)) {
                return true;
            }

            getLog().debug("Script '" + scriptName + "' not found in package.json");
            return false;
        } catch (IOException e) {
            getLog().warn("Failed to read package.json: " + e.getMessage());
            return false;
        }
    }
}

