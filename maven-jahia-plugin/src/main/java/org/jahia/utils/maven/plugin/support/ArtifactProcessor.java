package org.jahia.utils.maven.plugin.support;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.jahia.utils.osgi.parsers.ParsingContext;

/**
 * Created by loom on 27.11.14.
 */
public interface ArtifactProcessor {

    public ParsingContext enterArtifact(Artifact artifact, boolean optional, boolean external, ParsingContext parentParsingContext, String logPrefix, int depth) throws MojoExecutionException;

    public boolean exitArtifact(Artifact artifact, boolean optional, boolean external, String logPrefix, ParsingContext parsingContext, int depth) throws MojoExecutionException;
}
