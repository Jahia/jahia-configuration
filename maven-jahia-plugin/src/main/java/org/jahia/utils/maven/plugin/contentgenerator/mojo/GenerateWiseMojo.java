package org.jahia.utils.maven.plugin.contentgenerator.mojo;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jahia.utils.maven.plugin.contentgenerator.wise.WiseService;

/**
 * @goal generate-wise
 * @requiresProject false
 * @author Guillaume Lucazeau
 * 
 */
public class GenerateWiseMojo extends ContentGeneratorMojo {

	/**
	 * @parameter expression="${jahia.cg.wise.nbDocspaces}" default-value="1"
	 */
	private Integer nbDocspaces;
	
	/**
	 * @parameter expression="${jahia.cg.wise.nbPolls}" default-value="10"
	 */
	private Integer nbPolls;
	
	/**
	 * @parameter expression="${jahia.cg.wise.nbNotes}" default-value="10"
	 */
	private Integer nbNotes;
	
	/**
	 * @parameter expression="${jahia.cg.wise.nbTasks}" default-value="10"
	 */
	private Integer nbTasks;
	
	/**
	 * @parameter expression="${jahia.cg.wise.nbFoldersPerLevel}" default-value="3"
	 */
	private Integer nbFoldersPerLevel;

	/**
	 * @parameter expression="${jahia.cg.wise.foldersDepth}" default-value="2"
	 */
	private Integer foldersDepth;
	
	/**
	 * @parameter expression="${jahia.cg.wise.nbFilesPerFolder}" default-value="3"
	 */
	private Integer nbFilesPerFolder;
	
	/**
	 * @parameter expression="${jahia.cg.wise.nbCollectionsPerUser}" default-value="3"
	 */
	private Integer nbCollectionsPerUser;
	
	/**
	 * @parameter expression="${jahia.cg.wise.nbFilesPerCollection}" default-value="3"
	 */
	private Integer nbFilesPerCollection;
	
	/**
	 * @parameter expression="${jahia.cg.numberOfOwners}" default-value="5"
	 */
	protected Integer numberOfOwners;
	
	/**
	 * @parameter expression="${jahia.cg.numberOfEditors}" default-value="10"
	 */
	protected Integer numberOfEditors;
	
	/**
	 * @parameter expression="${jahia.cg.numberOfCollaborators}" default-value="15"
	 */
	protected Integer numberOfCollaborators;
	
	/**
	 * @parameter expression="${jahia.cg.startCreationDateRange}" default-value="2010-01-01"
	 */
	protected String startCreationDateRange;
	
	/**
	 * @parameter expression="${jahia.cg.endCreationDateRange}" default-value="2012-10-01"
	 */
	protected String endCreationDateRange;

	
	public ExportBO initExport() throws MojoExecutionException {
		ExportBO wiseExport = super.initExport();
		
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
