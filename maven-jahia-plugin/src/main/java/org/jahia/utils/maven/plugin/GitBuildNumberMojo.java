package org.jahia.utils.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.log.DefaultLog;
import org.apache.maven.scm.log.ScmLogger;
import org.apache.maven.scm.provider.git.gitexe.command.GitCommandLineUtils;
import org.apache.maven.scm.provider.git.gitexe.command.branch.GitBranchCommand;
import org.apache.maven.scm.util.AbstractConsumer;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;

/**
 * Creates a build number from number of git revisions
 *
 * @goal gitbuildnumber
 */
public class GitBuildNumberMojo extends AbstractMojo {


    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    /**
     * You can rename the buildNumber property name to another property name if desired.
     *
     * @parameter expression="${maven.buildNumber.buildNumberPropertyName}" default-value="buildNumber"
     */
    private String buildNumberPropertyName;

    /**
     * @parameter expression="${maven.buildNumber.scmDirectory}" default-value="${basedir}"
     */
    protected File scmDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ScmLogger logger = new DefaultLog();
        ScmFileSet scmFileSet = new ScmFileSet(scmDirectory);
        RevisionCountConsumer consumer = new RevisionCountConsumer(logger);
        CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();

        Commandline cli = GitCommandLineUtils.getBaseGitCommandLine(scmFileSet.getBasedir(), "rev-list");
        cli.createArg().setValue("HEAD");

        try {
            int exitCode = GitCommandLineUtils.execute(cli, consumer, stderr, logger);
            if (exitCode == 0) {
                String currentBranch = GitBranchCommand.getCurrentBranch(logger, null, scmFileSet);
                String revision = currentBranch + "-" + consumer.getCount();
                project.getProperties().put( buildNumberPropertyName, revision );
            }
        } catch (ScmException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static class RevisionCountConsumer extends AbstractConsumer {
        private int count;

        public RevisionCountConsumer(ScmLogger logger) {
            super(logger);
            count = 0;
        }

        @Override
        public void consumeLine(String s) {
            count ++;
        }

        public int getCount() {
            return count;
        }
    }
}
