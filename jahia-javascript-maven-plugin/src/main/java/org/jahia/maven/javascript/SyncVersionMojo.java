package org.jahia.maven.javascript;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

/**
 * Syncs the package.json version with the Maven project version.
 */
@Mojo(name = "sync-version", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class SyncVersionMojo extends AbstractYarnMojo {

    private static final String VERSION_FIELD = "version";

    @Inject
    public SyncVersionMojo(MavenProject mavenProject, MavenSession mavenSession, BuildPluginManager pluginManager) {
        super(mavenProject, mavenSession, pluginManager);
    }

    @Override
    public void execute() throws MojoExecutionException {
        String mavenVersion = mavenProject.getVersion();
        getLog().debug("Syncing package.json version to: " + mavenVersion);

        File packageJsonFile = new File(workingDirectory, "package.json");

        if (!packageJsonFile.exists()) {
            throw new MojoExecutionException("package.json not found at: " + packageJsonFile.getAbsolutePath());
        }

        try {
            // Read package.json
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(packageJsonFile);

            if (!(rootNode instanceof ObjectNode)) {
                throw new MojoExecutionException("package.json is not a valid JSON object");
            }
            ObjectNode packageJson = (ObjectNode) rootNode;

            if (!packageJson.has(VERSION_FIELD)) {
                throw new MojoExecutionException("package.json does not contain a version field");
            }
            String currentVersion = packageJson.get(VERSION_FIELD).asText();
            if (currentVersion.equals(mavenVersion)) {
                getLog().info("package.json version is already in sync: " + mavenVersion);
                return;
            }

            // Update version in the package.json
            packageJson.put(VERSION_FIELD, mavenVersion);
            mapper.writerWithDefaultPrettyPrinter().writeValue(packageJsonFile, packageJson);
            getLog().info("Updated package.json version from " + currentVersion + " to " + mavenVersion);

        } catch (IOException e) {
            throw new MojoExecutionException("Failed to update package.json version", e);
        }
    }
}

