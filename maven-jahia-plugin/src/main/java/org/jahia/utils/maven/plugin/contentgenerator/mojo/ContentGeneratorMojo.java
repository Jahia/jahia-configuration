package org.jahia.utils.maven.plugin.contentgenerator.mojo;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jahia.utils.maven.plugin.contentgenerator.ContentGeneratorService;
import org.jahia.utils.maven.plugin.contentgenerator.FileService;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jahia.utils.maven.plugin.contentgenerator.properties.DatabaseProperties;

/**
 * @goal generate
 * @requiresProject false
 * @author Guillaume Lucazeau
 * 
 */
public abstract class ContentGeneratorMojo extends AbstractMojo {

	/**
	 * @parameter expression="${jahia.cg.mysql.host}" default-value="localhost"
	 */
	protected String mysql_host;

	/**
	 * @parameter expression="${jahia.cg.mysql.login}"
	 */

	protected String mysql_login;

	/**
	 * @parameter expression="${jahia.cg.mysql.password}"
	 */
	protected String mysql_password;

	/**
	 * @parameter expression="${jahia.cg.mysql_db}"
	 */
	protected String mysql_db;

	/**
	 * @parameter expression="${jahia.cg.mysql_table}" default-value="articles"
	 */
	protected String mysql_table;

	/**
	 * @parameter expression="${jahia.cg.nbPagesOnTopLevel}" default-value="1"
	 */
	protected Integer nbPagesOnTopLevel;

	/**
	 * @parameter expression="${jahia.cg.nbSubLevels}" default-value="2"
	 */
	protected Integer nbSubLevels;

	/**
	 * @parameter expression="${jahia.cg.nbPagesPerLevel}" default-value="3"
	 */
	protected Integer nbPagesPerLevel;

	/**
	 * @parameter expression="${jahia.cg.outputDirectory}"
	 *            default-value="output"
	 */
	protected String outputDirectory;

	/**
	 * @parameter expression="${jahia.cg.outputFileName}"
	 *            default-value="jahia-cg-output.xml"
	 */
	protected String outputFileName;

	/**
	 * @parameter expression="${jahia.cg.pagesHaveVanity}" default-value="true"
	 */
	protected Boolean pagesHaveVanity;

	/**
	 * @parameter expression="${jahia.cg.siteKey}" default-value="testSite"
	 */
	protected String siteKey;

	/**
	 * @parameter expression="${jahia.cg.siteLanguages}" default-value="en,fr"
	 */
	protected String siteLanguages;

	/**
	 * @parameter expression="${jahia.cg.addFiles}" default-value="none"
	 */
	protected String addFiles;

	/**
	 * @parameter expression="${jahia.cg.poolDirectory}" default-value="files_pool"
	 */
	protected String poolDirectory;

	/**
	 * @parameter expression="${jahia.cg.numberOfFilesToGenerate}" " default-value="0"
	 */
	protected Integer numberOfFilesToGenerate;

	/**
	 * @parameter expression="${jahia.cg.numberOfBigTextPerPage}"
	 *            default-value="1"
	 */
	protected Integer numberOfBigTextPerPage;

	/**
	 * @parameter expression="${jahia.cg.numberOfUsers}" default-value="25"
	 */
	protected Integer numberOfUsers;
	
	/**
	 * @parameter expression="${jahia.cg.numberOfGroups}" default-value="5"
	 */
	protected Integer numberOfGroups;

	/**
	 * @parameter expression="${jahia.cg.numberOfUsersPerGroup}" default-value="5"
	 */
	protected Integer numberOfUsersPerGroup;

    /**
     * @parameter expression="${jahia.cg.groupsAclRatio}" defaule-value="0"
     */
    protected double groupAclRatio;

    /**
     * @parameter expression="${jahia.cg.usersAclRatio}" defaule-value="0"
     */
    protected double usersAclRatio;

    /**
     * @parameter expression="${jahia.cg.numberOfSites}" default-value="1"
     */
    protected Integer numberOfSites;
    
    /**
     * @parameter expression="${jahia.cg.numberOfCategories}" default-value="1"
     */
    protected Integer numberOfCategories;
    
    /**
     * @parameter expression="${jahia.cg.numberOfCategoryLevels}" default-value="1"
     */
    protected Integer numberOfCategoryLevels;
    
    /**
     * @parameter expression="${jahia.cg.numberOfTags}" default-value="1"
     */
    protected Integer numberOfTags;
    
    /**
     * @parameter expression="${jahia.cg.visibilityEnabled}" default-value="false"
     */
    protected Boolean visibilityEnabled;
    
    /**
     * @parameter expression="${jahia.cg.visibilityStartDate}"
     */
    protected String visibilityStartDate;
    
    /**
     * @parameter expression="${jahia.cg.visibilityEndDate}"
     */
    protected String visibilityEndDate;

	public abstract void execute() throws MojoExecutionException, MojoFailureException;

	/**
	 * Get properties and initialize ExportBO
	 * 
	 * @return a new export BO containing all the parameters
	 */
	protected ExportBO initExport() throws MojoExecutionException {
		ExportBO export = new ExportBO();
		ContentGeneratorService contentGeneratorService = ContentGeneratorService.getInstance();

		/**
		 * Database
		 */
        DatabaseProperties.HOSTNAME = mysql_host;

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

        export.setNbPagesTopLevel(nbPagesOnTopLevel);
        export.setNbSubLevels(nbSubLevels);
        export.setNbSubPagesPerPage(nbPagesPerLevel);

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
        export.setPagesHaveVanity(pagesHaveVanity);
        export.setSiteKey(siteKey);
        export.setSiteLanguages(Arrays.asList(siteLanguages.split(",")));
		export.setAddFilesToPage(addFiles);

		// TODO: add "and if called goal is NOT generate-files"
		// because the execution fails if we want to generate files with jahia.cg.addFiles == none
		if (ContentGeneratorCst.VALUE_ALL.equals(export.getAddFilesToPage())
				|| ContentGeneratorCst.VALUE_RANDOM.equals(export.getAddFilesToPage())) {
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

        export.setNumberOfBigTextPerPage(numberOfBigTextPerPage);
        export.setNumberOfUsers(numberOfUsers);
        export.setNumberOfGroups(numberOfGroups);
        export.setNumberOfUsersPerGroup(numberOfUsersPerGroup);
		export.setNumberOfFilesToGenerate(numberOfFilesToGenerate);
        export.setGroupAclRatio(groupAclRatio);
        export.setUsersAclRatio(usersAclRatio);
        export.setNumberOfSites(numberOfSites);
        
        export.setNumberOfCategories(numberOfCategories);
        export.setNumberOfCategoryLevels(numberOfCategoryLevels);
        
        export.setNumberOfTags(numberOfTags);
        
        if (visibilityEnabled == null) {
        	visibilityEnabled = Boolean.FALSE;
        }
       
        export.setVisibilityEnabled(visibilityEnabled);
        export.setVisibilityStartDate(visibilityStartDate);
        export.setVisibilityEndDate(visibilityEndDate);
        
		Integer totalPages = contentGeneratorService.getTotalNumberOfPagesNeeded(nbPagesOnTopLevel, nbSubLevels,
				nbPagesPerLevel);
		export.setTotalPages(totalPages);
		if (export.getTotalPages().compareTo(ContentGeneratorCst.MAX_TOTAL_PAGES) > 0) {
			throw new MojoExecutionException("You asked to generate " + export.getTotalPages()
					+ " pages, the maximum allowed is " + ContentGeneratorCst.MAX_TOTAL_PAGES);
		}

		return export;
	}
}
