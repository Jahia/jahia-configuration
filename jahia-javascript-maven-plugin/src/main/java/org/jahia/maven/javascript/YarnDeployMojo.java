package org.jahia.maven.javascript;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;

/**
 * Publishes the package to a npm registry.
 */
@Mojo(name = "yarn-deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class YarnDeployMojo extends AbstractYarnMojo {

    private static final String BASE_COMMAND = "npm publish --provenance";

    /**
     * Access level for npm publish. Valid values: "public" or "private".
     */
    @Parameter(property = "jahia.js.yarnDeploy.access", defaultValue = "public")
    protected String yarnDeployAccess;

    /**
     * Tag to use for SNAPSHOT versions. Default is "alpha".
     */
    @Parameter(property = "jahia.js.yarnDeploy.snapshotTag", defaultValue = "alpha")
    protected String yarnDeploySnapshotTag;

    @Inject
    public YarnDeployMojo(MavenProject mavenProject, MavenSession mavenSession, BuildPluginManager pluginManager) {
        super(mavenProject, mavenSession, pluginManager);
    }

    @Override
    public void execute() throws MojoExecutionException {
        // Validate access parameter
        if (!"public".equals(yarnDeployAccess) && !"private".equals(yarnDeployAccess)) {
            throw new MojoExecutionException("Invalid access value: " + yarnDeployAccess + ". Must be 'public' or 'private'.");
        }
        boolean isSnapshot = isSnapshot();
        String tag = isSnapshot ? yarnDeploySnapshotTag : "latest";

        // Execute the constructed command
        String command = String.format("%s --access %s --tag %s", BASE_COMMAND, yarnDeployAccess, tag);
        executeYarnCommand(command, null, false);
    }

    /**
     * Checks if the project version ends with "-SNAPSHOT".
     *
     * @return true if the version ends with "-SNAPSHOT", false otherwise
     */
    private boolean isSnapshot() {
        return mavenProject.getArtifact().getVersion().contains(Artifact.SNAPSHOT_VERSION);
    }
}

