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

@Mojo(name = "yarn-deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class YarnDeployMojo extends AbstractYarnMojo {

    private static final String BASE_COMMAND = "npm publish --provenance";

    /**
     * Access level for npm publish. Valid values: "public" or "private".
     */
    @Parameter(property = "yarnDeploy.access", defaultValue = "public")
    protected String access;

    /**
     * Tag to use for SNAPSHOT versions. Default is "alpha".
     */
    @Parameter(property = "yarnDeploy.snapshotTag", defaultValue = "alpha")
    protected String snapshotTag;

    @Inject
    public YarnDeployMojo(MavenProject mavenProject, MavenSession mavenSession, BuildPluginManager pluginManager) {
        super(mavenProject, mavenSession, pluginManager);
    }

    @Override
    public void execute() throws MojoExecutionException {
        // Validate access parameter
        if (!"public".equals(access) && !"private".equals(access)) {
            throw new MojoExecutionException("Invalid access value: " + access + ". Must be 'public' or 'private'.");
        }
        boolean isSnapshot = isSnapshot();
        String tag = isSnapshot ? snapshotTag : "latest";

        // Build the command
        String command = String.format("%s --access %s --tag %s", BASE_COMMAND, access, tag);

        executeYarnCommand(command);
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

