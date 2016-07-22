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
import java.util.List;

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
     * Contains the full list of projects in the reactor.
     * @parameter default-value="${reactorProjects}"
     * @readonly
     * @required
     */
    private List<MavenProject> reactorProjects;

    /**
     * @parameter expression="${maven.buildNumber.allBranches}" default-value="false"
     */
    protected boolean allBranches = false;

    /**
     * @parameter expression="${maven.buildNumber.baseBuildNumber}" default-value="0"
     */
    protected int baseBuildNumber = 0;

    /**
     * @parameter expression="${maven.buildNumber.baseGitRevision}" default-value=""
     */
    protected String baseGitRevision = "";

    /**
     * You can rename the buildNumber property name to another property name if desired.
     *
     * @parameter expression="${maven.buildNumber.buildNumberPropertyName}" default-value="buildNumber"
     */
    private String buildNumberPropertyName;

    /**
     * If set to true, will get the scm revision once for all modules of a multi-module project instead of fetching once
     * for each module.
     *
     * @parameter expression="${maven.buildNumber.getRevisionOnlyOnce}" default-value="false"
     */
    private boolean getRevisionOnlyOnce;

    /**
     * @parameter expression="${maven.buildNumber.scmDirectory}" default-value="${basedir}"
     */
    protected File scmDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (project.getProperties().get("maven.gitBuildNumber.alreadySet") != null) {
            return;
        }
        ScmLogger logger = new DefaultLog();
        ScmFileSet scmFileSet = new ScmFileSet(scmDirectory);
        RevisionCountConsumer consumer = new RevisionCountConsumer(logger);
        CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();

        Commandline cli = GitCommandLineUtils.getBaseGitCommandLine(scmFileSet.getBasedir(), "rev-list");
        if (allBranches) {
            cli.createArg().setValue("--all");

            String currentRevision = getCurrentRevision(logger, stderr, scmFileSet.getBasedir());
            consumer.setStartCountFrom(currentRevision);
            if (baseGitRevision != null && !"".equals(baseGitRevision)) {
                consumer.setStopCountAt(baseGitRevision);
            }
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

                if (getRevisionOnlyOnce && reactorProjects != null) {
                    for (MavenProject mavenProject : reactorProjects) {
                        mavenProject.getProperties().put( this.buildNumberPropertyName, revision );
                        mavenProject.getProperties().put( "maven.gitBuildNumber.alreadySet", true );
                    }
                }

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
        private String stopCountAt;

        public RevisionCountConsumer(ScmLogger logger) {
            super(logger);
            count = 0;
            startCountFrom = null;
            stopCountAt = null;
        }

        public void setStartCountFrom(String startCountFrom) {
            this.startCountFrom = startCountFrom;
        }

        public void setStopCountAt(String stopCountAt) {
            this.stopCountAt = stopCountAt;
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
            if (stopCountAt != null) {
                if (s.equals(stopCountAt) || "stopped".equals(stopCountAt)) {
                    stopCountAt = "stopped";
                    return;
                }
            }
            count ++;
        }

        public int getCount() {
            return count;
        }
    }
}
