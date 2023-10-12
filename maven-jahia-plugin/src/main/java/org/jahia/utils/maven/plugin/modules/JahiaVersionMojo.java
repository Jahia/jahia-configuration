/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2023 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
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
