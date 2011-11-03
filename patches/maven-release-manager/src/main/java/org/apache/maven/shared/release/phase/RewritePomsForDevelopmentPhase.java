package org.apache.maven.shared.release.phase;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.scm.ScmTranslator;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * Rewrite POMs for future development
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RewritePomsForDevelopmentPhase
    extends AbstractRewritePomsPhase
{
    /**
     * SCM URL translators mapped by provider name.
     */
    private Map<String, ScmTranslator> scmTranslators;

    protected void transformScm( MavenProject project, Element rootElement, Namespace namespace,
                                 ReleaseDescriptor releaseDescriptor, String projectId, ScmRepository scmRepository,
                                 ReleaseResult result, String commonBasedir )
        throws ReleaseExecutionException
    {
        // If SCM is null in original model, it is inherited, no mods needed
        if ( project.getScm() != null )
        {
            Element scmRoot = rootElement.getChild( "scm", namespace );
            if ( scmRoot != null )
            {
                Map originalScmInfo = releaseDescriptor.getOriginalScmInfo();
                // check containsKey, not == null, as we store null as a value
                if ( !originalScmInfo.containsKey( projectId ) )
                {
                    throw new ReleaseExecutionException(
                        "Unable to find original SCM info for '" + project.getName() + "'" );
                }

                ScmTranslator translator = scmTranslators.get( scmRepository.getProvider() );
                if ( translator != null )
                {
                    Scm scm = (Scm) originalScmInfo.get( projectId );

                    if ( scm != null )
                    {
                        rewriteElement( "connection", scm.getConnection(), scmRoot, namespace );
                        rewriteElement( "developerConnection", scm.getDeveloperConnection(), scmRoot, namespace );
                        rewriteElement( "url", scm.getUrl(), scmRoot, namespace );
                        rewriteElement( "tag", translator.resolveTag( scm.getTag() ), scmRoot, namespace );
                    }
                    else
                    {
                        // cleanly remove the SCM element
                        rewriteElement( "scm", null, rootElement, namespace );
                    }
                }
                else
                {
                    String message = "No SCM translator found - skipping rewrite";
                    result.appendDebug( message );
                    getLogger().debug( message );
                }
            }
        }
    }

    protected Map getOriginalVersionMap( ReleaseDescriptor releaseDescriptor, List reactorProjects, boolean simulate )
    {
        return simulate
            ? releaseDescriptor.getOriginalVersions( reactorProjects )
            : releaseDescriptor.getReleaseVersions();
    }

    protected Map getNextVersionMap( ReleaseDescriptor releaseDescriptor )
    {
        return releaseDescriptor.getDevelopmentVersions();
    }

    protected String getResolvedSnapshotVersion( String artifactVersionlessKey, Map resolvedSnapshotsMap )
    {
        Map versionsMap = (Map) resolvedSnapshotsMap.get( artifactVersionlessKey );
        if (versionsMap == null && artifactVersionlessKey != null && artifactVersionlessKey.indexOf(":") != -1)
        {
            versionsMap = (Map) resolvedSnapshotsMap.get(StringUtils.substringBeforeLast(artifactVersionlessKey, ":") + ":*");
            if (versionsMap == null ) {
                versionsMap = (Map) resolvedSnapshotsMap.get("*:" + StringUtils.substringAfterLast(artifactVersionlessKey, ":"));
            }
        }

        if ( versionsMap != null )
        {
            return (String) ( versionsMap.get( ReleaseDescriptor.DEVELOPMENT_KEY ) );
        }
        else
        {
            return null;
        }
    }
}
