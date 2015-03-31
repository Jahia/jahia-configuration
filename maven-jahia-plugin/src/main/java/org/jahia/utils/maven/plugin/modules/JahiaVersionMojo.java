package org.jahia.utils.maven.plugin.modules;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Used to add Jahia version in module context
 *
 * @goal jahia-version
 * @phase prepare-package
 */
public class JahiaVersionMojo extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        MavenProject p = project.getParent();
        while (p != null && !StringUtils.equals(p.getArtifactId(), "jahia-modules")) {
            p = p.getParent();
        }
        if (p != null) {
            project.getProperties().put("jahia.version", p.getVersion());
            if (!project.getProperties().containsKey("jahia-download-sources-available")) {
                project.getProperties()
                        .put("jahia-download-sources-available", isProjectProtected() ? "false" : "true");
            }
        } else {
            for (Dependency dep : project.getDependencies()) {
                if (StringUtils.equals(dep.getArtifactId(), "jahia-impl")) {
                    project.getProperties().put("jahia.version", dep.getVersion());
                    return;
                }
            }
        }
    }

    private boolean isProjectProtected() {
        String id = project.getDistributionManagement() != null
                && project.getDistributionManagement().getRepository() != null ? project.getDistributionManagement()
                .getRepository().getId() : null;

        return id != null
                && ("jahia-enterprise-releases".equals(id) || "jahia-internal-releases".equals(id) || "workspace-factory-releases"
                        .equals(id));
    }

}
