package org.jahia.configuration.archivers;

import org.codehaus.plexus.archiver.AbstractArchiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.dir.DirectoryArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
import org.jahia.configuration.deployers.ServerDeploymentFactory;

import java.io.File;
import java.io.IOException;

/**
 * Handles copying/archiving of files with filtering, based on the target server
 * type.
 * 
 * @author Serge Huber
 * @author Sergiy Shyrkov
 */
public class MainArchiver {

	private static void execute(File source, File target, String excludes,
			boolean archive, boolean verbose) throws IOException,
			ArchiverException {
		AbstractArchiver archiver = archive ? new ZipArchiver()
				: new DirectoryArchiver();
		if (verbose) {
			archiver.enableLogging(new ConsoleLogger(Logger.LEVEL_DEBUG,
					"console"));
		}
		File absoluteDestFile = new File(target.getAbsolutePath());
		archiver.setDestFile(absoluteDestFile);
		archiver.addDirectory(source, null,
				excludes != null ? excludes.split(",") : null);
		archiver.createArchive();
	}

	public static void main(String[] args) throws IOException,
			ArchiverException {
		if (args.length < 2 || args[0].equals("-m") && args.length < 3) {
			System.out
					.println("Usage is:\n\t[-cmv] source target [serverType]");
			System.out
					.println("\tOption -c indicates that the target will be an archive file, i.e. the source will be compressed into an archive file.");
			System.out
					.println("\tOption -m performs \"move\" operation for the source folder"
							+ " into the archive or target folder, i.e. the source folder is actually deleted after completing the operation.");
			System.out
					.println("\tOption -v does verbose output when performing operation.");
			System.out
					.println("For example:\n\t-cm /tmp/build/jahia-directory /opt/deployments/jahia.war"
							+ " tomcat\nWill move the files from jahia-directory into an archive jahia.war"
							+ " filtering out resources for Apache Tomcat as a target server type.");
            System.exit(1);
			return;
		}

		File target = null;
		File source = null;
		String serverType = null;
		String excludes = null;
		boolean performMove = false;
		boolean archive = false;
		boolean verbose = false;
		if (args[0].startsWith("-")) {
			performMove = args[0].contains("m");
			archive = args[0].contains("c");
			verbose = args[0].contains("v");
			source = new File(args[1]);
			target = new File(args[2]);
			serverType = args.length > 3 ? args[3] : null;
		} else {
			source = new File(args[0]);
			target = new File(args[1]);
			serverType = args.length > 2 ? args[2] : null;
		}

		if (serverType != null) {
			ServerDeploymentFactory.setTargetServerDirectory("/tmp/");
			excludes = ServerDeploymentFactory.getInstance()
					.getImplementation(serverType).getWarExcludes();
		}
		execute(source, target, excludes, archive, verbose);

		if (performMove) {
			// need to delete the original directory
			try {
				FileUtils.deleteDirectory(source);
			} catch (IOException e) {
				e.printStackTrace();
                System.exit(1);
			}
		}
	}
}
