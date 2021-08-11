/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.support;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RepositorySystem;

/**
 * Factory for obtaining an instance of the proper artifact and dependency resolver, depending on the Maven execution environment.
 * 
 * @author Sergiy Shyrkov
 */
public final class AetherHelperFactory {

    /**
     * Obtains an instance of the proper artifact and dependency resolver, depending on the Maven execution environment
     * 
     * @param container
     * @param project
     * @param session
     * @param log
     * @return an instance of the proper artifact and dependency resolver
     * @throws MojoExecutionException
     */
    @SuppressWarnings("unchecked")
    public static AetherHelper create(PlexusContainer container, MavenProject project, MavenSession session, Log log)
            throws MojoExecutionException {
        try {
            if (container.hasComponent("org.eclipse.aether.RepositorySystem")) {
                log.info("Using Aether helper for Maven 3.0.x");
                
                warnMavenVersion(log);
                
                return new Maven30AetherHelper(container.lookup(RepositorySystem.class),
                        session.getRepositorySession(), project.getRemoteProjectRepositories(), log);
            } else if (container.hasComponent(org.eclipse.aether.RepositorySystem.class)) {
                Object repoSession;
                try {
                    repoSession = MavenSession.class.getMethod("getRepositorySession").invoke(session);
                } catch (Exception e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                }
                List<?> remoteRepos = project.getRemoteProjectRepositories();
                log.info("Using Aether helper for Maven 3.1+");
                return new Maven31AetherHelper(container.lookup(org.eclipse.aether.RepositorySystem.class),
                        (RepositorySystemSession) repoSession,
                        (List<org.eclipse.aether.repository.RemoteRepository>) remoteRepos, log);
            }
        } catch (ComponentLookupException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        throw new MojoExecutionException("Unable to find either Sonatype's Aether nor Eclipse's Aether implementations");
    }

    private static void warnMavenVersion(Log log) {
        log.warn("");
        log.warn("************************* DEPRECATION *************************");
        log.warn("*                                                             *");
        log.warn("* The version of Maven (3.0.x), you are using, is deprecated. *");
        log.warn("* Please, switch to a more recent one (e.g. 3.3.x).           *");
        log.warn("*                                                             *");
        log.warn("***************************************************************");
        log.warn("");
    }

}
