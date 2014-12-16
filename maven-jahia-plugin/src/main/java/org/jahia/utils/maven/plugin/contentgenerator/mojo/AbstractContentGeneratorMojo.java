package org.jahia.utils.maven.plugin.contentgenerator.mojo;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jahia.utils.maven.plugin.contentgenerator.FileService;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jahia.utils.maven.plugin.contentgenerator.properties.DatabaseProperties;

public abstract class AbstractContentGeneratorMojo extends AbstractMojo {

	/**
	 * MySQL server hosting the Wikipedia articles database
	 * @parameter expression="${jahia.cg.mysql.host}"  default-value="localhost"
	 * @required
	 * 
	 */
	protected String mysql_host;

	/**
	 * MySQL user to connect to the Wikipedia articles database
	 * @parameter expression="${jahia.cg.mysql.login}"
	 * @required
	 */
	protected String mysql_login;

	/**
	 * MySQL password for the user
	 * @parameter expression="${jahia.cg.mysql.password}"
	 * @required
	 */
	protected String mysql_password;

	/**
	 * Port on which to connect to the MySQL database
	 * @parameter expression="${jahia.cg.mysql.port}"
	 */
	protected String mysql_port = "3306";

	/**
	 * MySQL database name
	 * @parameter expression="${jahia.cg.mysql_db}"
	 * @required
	 */
	protected String mysql_db;

	/**
	 * MySQL table
	 * @parameter expression="${jahia.cg.mysql_table}" default-value="articles"
	 * @required
	 */
	protected String mysql_table;

	/**
	 * Local directory where generated files will be written
	 * @parameter expression="${jahia.cg.outputDirectory}"
	 *            default-value="output"
	 * @required
	 */
	protected String outputDirectory;

	/**
	 * 
	 * @parameter expression="${jahia.cg.outputFileName}"
	 *            default-value="jahia-cg-output.xml"
	 */
	protected String outputFileName;

	/**
	 * Local directory that contains files to be included in Jahia pages or Wise instances
	 * @parameter expression="${jahia.cg.poolDirectory}" default-value="files_pool"
	 */
	protected String poolDirectory;

	public abstract void execute() throws MojoExecutionException, MojoFailureException;

	/**
	 * Get properties and initialize ExportBO
	 * 
	 * @return a new export BO containing all the parameters
	 */
	protected ExportBO initExport() throws MojoExecutionException {
		ExportBO export = new ExportBO();

		/**
		 * Database
		 */
        DatabaseProperties.HOSTNAME = mysql_host;

		DatabaseProperties.PORT = mysql_port;

		if (mysql_db == null) {
			throw new MojoExecutionException("No database name provided");
		}
		DatabaseProperties.DATABASE = mysql_db;

		if (mysql_login == null) {
			throw new MojoExecutionException("No database user provided");
		}
		DatabaseProperties.USER = mysql_login;

		if (mysql_password == null) {
			throw new MojoExecutionException("No database user password provided");
		}
		DatabaseProperties.PASSWORD = mysql_password;

		if (mysql_table == null) {
			getLog().info(
					"No MySQL table name provided, uses default \"" + ContentGeneratorCst.MYSQL_TABLE_DEFAULT + "\"");
			DatabaseProperties.TABLE = "articles";
		} else {
			DatabaseProperties.TABLE = mysql_table;
		}

		if (outputDirectory == null) {
			throw new MojoExecutionException("outputDirectory property can not be null");
		}
		File fOutputDirectory = new File(outputDirectory);
		if (!fOutputDirectory.exists()) {
			fOutputDirectory.mkdirs();
		} else {
			// Clean output directory
			try {
				getLog().info("Deleting output directory");
				FileUtils.deleteDirectory(fOutputDirectory);
			} catch (IOException e) {
				getLog().error("Can not delete output directory");
				e.printStackTrace();
			}
			fOutputDirectory.mkdir();
		}

		File outputFile = new File(outputDirectory, outputFileName);
		export.setOutputFile(outputFile);
		export.setOutputDir(outputDirectory);

		String sep = System.getProperty("file.separator");
		File tmp = new File(outputDirectory + sep + "tmp");
		tmp.mkdir();
		ExportBO.tmp = tmp;
		
		File outputMapFile = new File(outputDirectory, "sitemap.txt");
		export.setMapFile(outputMapFile);

		if (! export.isDisableInternalFileReference()) {
			if (poolDirectory == null) {
				throw new MojoExecutionException("Pool directory property can not be null");
			}
			File fPoolDirectory = new File(poolDirectory);
			if (!fPoolDirectory.exists()) {
				fPoolDirectory.mkdirs();
			}
			export.setFilesDirectory(fPoolDirectory);

			FileService fileService = new FileService();
			List<String> filesNamesAvailable = fileService.getFileNamesAvailable(export.getFilesDirectory());
			export.setFileNames(filesNamesAvailable);
		}
		return export;
	}
	
	protected ExportBO initFilesProperties(ExportBO export) throws MojoExecutionException {
		// TODO: add "and if called goal is NOT generate-files"
		// because the execution fails if we want to generate files with
		// jahia.cg.addFiles == none

		return export;
	}
}
