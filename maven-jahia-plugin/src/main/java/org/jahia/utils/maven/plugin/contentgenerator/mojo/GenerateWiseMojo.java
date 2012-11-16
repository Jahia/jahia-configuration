package org.jahia.utils.maven.plugin.contentgenerator.mojo;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jahia.utils.maven.plugin.contentgenerator.wise.WiseService;

/**
 * Generates a Wise instance ready to be imported into a Wise installation
 * Also generates a map containing all the included files
 * @goal generate-wise
 * @requiresProject false
 * @author Guillaume Lucazeau
 * 
 */
public class GenerateWiseMojo extends AbstractJahiaSiteMojo {

	/**
	 * Number of docspace to create
	 * N.B: it currently works only for one
	 * @parameter expression="${jahia.cg.wise.nbDocspaces}" default-value="1"
	 * @required
	 */
	private Integer nbDocspaces;
	
	/**
	 * Number of Polls
	 * @parameter expression="${jahia.cg.wise.nbPolls}" default-value="10"
	 */
	private Integer nbPolls;
	
	/**
	 * Number of Notes
	 * Notes contain a random Wikipedia article
	 * @parameter expression="${jahia.cg.wise.nbNotes}" default-value="10"
	 */
	private Integer nbNotes;
	
	/**
	 * Number of tasks
	 * Tasks have a random user as creator and assignee
	 * @parameter expression="${jahia.cg.wise.nbTasks}" default-value="10"
	 */
	private Integer nbTasks;
	
	/**
	 * Number subfolder per folder
	 * @parameter expression="${jahia.cg.wise.nbFoldersPerLevel}" default-value="3"
	 * @required
	 */
	private Integer nbFoldersPerLevel;

	/**
	 * Folder depth
	 * @parameter expression="${jahia.cg.wise.foldersDepth}" default-value="2"
	 * @required
	 */
	private Integer foldersDepth;
	
	/**
	 * Number of files per folder
	 * Files are randomly picked from the files pool
	 * You need to have enough files in the fool as they can't be used twice
	 * @parameter expression="${jahia.cg.wise.nbFilesPerFolder}" default-value="3"
	 * @required
	 */
	private Integer nbFilesPerFolder;
	
	/**
	 * Number of collections per user
	 * @parameter expression="${jahia.cg.wise.nbCollectionsPerUser}" default-value="3"
	 */
	private Integer nbCollectionsPerUser;
	
	/**
	 * Number of files per collection
	 * @parameter expression="${jahia.cg.wise.nbFilesPerCollection}" default-value="3"
	 */
	private Integer nbFilesPerCollection;
	
	/**
	 * Number of users with the role "docspace-owner"
	 * They come first (user 0 to userN)
	 * @parameter expression="${jahia.cg.numberOfOwners}" default-value="5"
	 * @required
	 */
	protected Integer numberOfOwners;
	
	/**
	 * Number of users with the role "docspace-editor"
	 * They come after the owners
	 * @parameter expression="${jahia.cg.numberOfEditors}" default-value="10"
	 * @required
	 */
	protected Integer numberOfEditors;
	
	/**
	 * Number of users with the role "docspace-collaborator"
	 * They come after the editors
	 * @parameter expression="${jahia.cg.numberOfCollaborators}" default-value="15"
	 * @required
	 */
	protected Integer numberOfCollaborators;
	
	/**
	 * Start date for the range used to create a random creation date assigned to files in the JCR
	 * It actually doesn't work as during the import the date is replaced with the current date
	 * @parameter expression="${jahia.cg.startCreationDateRange}" default-value="2010-01-01"
	 */
	protected String startCreationDateRange;
	
	/**
	 * End date for the range used to create a random creation date assigned to files in the JCR
	 * It actually doesn't work as during the import the date is replaced with the current date
	 * @parameter expression="${jahia.cg.endCreationDateRange}" default-value="2012-10-01"
	 */
	protected String endCreationDateRange;
    
	/**
	 * Wise instance key
	 * @parameter expression="${jahia.cg.wiseInstanceKey}"
	 * @required
	 */
	protected String wiseInstanceKey;
    
	private ExportBO initExport() throws MojoExecutionException {
		boolean filesRequired = false;
		if (nbFilesPerFolder > 0) {
			filesRequired = true;
		}
		ExportBO wiseExport = super.initExport(filesRequired);
		
		wiseExport.setNbDocspaces(nbDocspaces);
		wiseExport.setNbPolls(nbPolls);
		wiseExport.setNbNotes(nbNotes);
		wiseExport.setNbFoldersPerLevel(nbFoldersPerLevel);
		wiseExport.setFoldersDepth(foldersDepth);
		wiseExport.setNbFilesPerFolder(nbFilesPerFolder);
		wiseExport.setNbTasks(nbTasks);
		wiseExport.setNbCollectionsPerUser(nbCollectionsPerUser);
		wiseExport.setNbFilesPerCollection(nbFilesPerCollection);
		wiseExport.setNumberOfOwners(numberOfOwners);
		wiseExport.setNumberOfEditors(numberOfEditors);
		wiseExport.setNumberOfCollaborators(numberOfCollaborators);
		wiseExport.setWiseInstanceKey(wiseInstanceKey);
		
		// site language hard coded to English
        List<String> languages = new ArrayList<String>();
        languages.add("en");
        wiseExport.setSiteLanguages(languages);
		
		DateFormat df = new SimpleDateFormat(ContentGeneratorCst.DATE_RANGE_FORMAT);
		Date dStartCreationDateRange = null;
		Date dEndCreationDateRange = null;
		try {
			dStartCreationDateRange = df.parse(startCreationDateRange);
		} catch (ParseException e) {
			throw new MojoExecutionException("startCreationDateRange is malformed, please use this format: " + ContentGeneratorCst.DATE_RANGE_FORMAT);
		}
		try {
			dEndCreationDateRange = df.parse(endCreationDateRange);
		} catch (ParseException e) {
			throw new MojoExecutionException("endCreationdateRange is malformed, please use this format: " + ContentGeneratorCst.DATE_RANGE_FORMAT);
		}
		wiseExport.setStartCreationDateRange(dStartCreationDateRange);
		wiseExport.setEndCreationDateRange(dEndCreationDateRange);
		
		// Trick to init file pool directory. Should clean this.
		if (wiseExport.getNbFilesPerFolder() > 0) {
			wiseExport.setAddFilesToPage(ContentGeneratorCst.VALUE_ALL);
		}
		return wiseExport;
	}
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		WiseService wiseService = WiseService.getInstance();
		ExportBO wiseExport = this.initExport();

		getLog().info("Jahia content generator for Wise starts");
		getLog().info(wiseExport.getSiteKey() + " instance will be created");

		String wiseRepositoryFile;
		try {
			int nbOwners = wiseExport.getNumberOfOwners();
			int nbEditors = wiseExport.getNumberOfEditors();
			int nbCollaborators = wiseExport.getNumberOfCollaborators();
			
			wiseRepositoryFile = wiseService.generateWise(wiseExport);
			getLog().info("Each word of this list has been used for the description of " + ContentGeneratorCst.OFTEN_USED_DESCRIPTION_WORDS_COUNTER + " files: " + ContentGeneratorCst.OFTEN_USED_DESCRIPTION_WORDS);
			getLog().info("Each word of this list has been used for the description of " + ContentGeneratorCst.SELDOM_USED_DESCRIPTION_WORDS_COUNTER + " files: " + ContentGeneratorCst.SELDOM_USED_DESCRIPTION_WORDS);
			getLog().info(wiseExport.getNumberOfUsers() + " users created:");
			getLog().info("- user0 to user" + (nbOwners -1) + " have docspace-owner role (" + nbOwners + " owners)");
			getLog().info("- user" + nbOwners + " to user" + (nbOwners + nbEditors -1) + " have docspace-editor role (" + nbEditors + " editors)");
			getLog().info("- user" + (nbOwners + nbEditors) + " to user" + (nbOwners + nbEditors + nbCollaborators -1) + " have docspace-collaborator role (" + nbCollaborators + " collaborators)");
			getLog().info("Wise instance archive created and available here: " + wiseRepositoryFile);
		} catch (IOException e) {
			getLog().error("Error writing output file");
			e.printStackTrace();
		}
	}
}
