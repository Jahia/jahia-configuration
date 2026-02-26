package org.jahia.maven.javascript;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import javax.inject.Inject;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(name = "install-node-and-corepack", defaultPhase = LifecyclePhase.INITIALIZE)
public class InstallNodeAndCorepackMojo extends AbstractYarnMojo {

    /**
     * Node.js version to install.
     */
    @Parameter(property = "jahia.js.nodeVersion", defaultValue = "v22.21.1")
    protected String nodeVersion;

    /**
     * Corepackversion to install.
     */
    @Parameter(property = "jahia.js.corepackVersion", defaultValue = "0.34.5")
    protected String corepackVersion;

    @Inject
    public InstallNodeAndCorepackMojo(MavenProject mavenProject, MavenSession mavenSession, BuildPluginManager pluginManager) {
        super(mavenProject, mavenSession, pluginManager);
    }

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Installing Node.js " + nodeVersion);

        Xpp3Dom config = configuration(element(name("nodeVersion"), nodeVersion), element(name("corepackVersion"), corepackVersion),
                element(name("workingDirectory"), workingDirectory.getAbsolutePath()));

        executeFrontendPlugin("install-node-and-corepack", config);
    }

}

