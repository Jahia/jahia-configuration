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
     * @parameter expression="${maven.buildNumber.allBranches}" default-value="false"
     */
    protected boolean allBranches = false;

    /**
     * @parameter expression="${maven.buildNumber.baseBuildNumber}" default-value="0"
     */
    protected int baseBuildNumber = 0;

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
        if (allBranches) {
            cli.createArg().setValue("--all");

            String currentRevision = getCurrentRevision(logger, stderr, scmFileSet.getBasedir());
            consumer.setStartCountFrom(currentRevision);
        } else {
            cli.createArg().setValue("HEAD");
        }

        try {
            int exitCode = GitCommandLineUtils.execute(cli, consumer, stderr, logger);
            if (exitCode == 0) {
                String revision = null;
                if (allBranches) {
                    revision = Integer.toString(consumer.getCount() + baseBuildNumber);
                } else {
                    String currentBranch = GitBranchCommand.getCurrentBranch(logger, null, scmFileSet);
                    revision = currentBranch + "-" + (consumer.getCount() + baseBuildNumber);
                }
                getLog().info("Setting build number property " + buildNumberPropertyName + " to revision " + revision);
                project.getProperties().put(buildNumberPropertyName, revision);
            }
        } catch (ScmException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private String getCurrentRevision(ScmLogger logger, CommandLineUtils.StringStreamConsumer stderr, File basedir) throws MojoExecutionException {
        try {
            Commandline cli = GitCommandLineUtils.getBaseGitCommandLine(basedir, "rev-parse");
            cli.createArg().setValue("HEAD");
            LineConsumer lineConsumer = new LineConsumer(logger);
            int exitCode = GitCommandLineUtils.execute(cli, lineConsumer, stderr, logger);
            if (exitCode == 0) {
                return lineConsumer.getResult();
            }
        } catch (ScmException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        return null;
    }

    private static class LineConsumer extends AbstractConsumer {
        private String result;

        public LineConsumer(ScmLogger logger) {
            super(logger);
        }

        @Override
        public void consumeLine(String s) {
            result = s;
        }

        public String getResult() {
            return result;
        }
    }

    private static class RevisionCountConsumer extends AbstractConsumer {
        private int count;
        private String startCountFrom;

        public RevisionCountConsumer(ScmLogger logger) {
            super(logger);
            count = 0;
            startCountFrom = null;
        }

        public void setStartCountFrom(String startCountFrom) {
            this.startCountFrom = startCountFrom;
        }

        @Override
        public void consumeLine(String s) {
            if (startCountFrom != null) {
                if (!s.equals(startCountFrom)) {
                    return;
                } else {
                    startCountFrom = null;
                }
            }

            count ++;
        }

        public int getCount() {
            return count;
        }
    }
}
