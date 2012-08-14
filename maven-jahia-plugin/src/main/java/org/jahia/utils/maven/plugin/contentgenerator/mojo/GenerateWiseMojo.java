package org.jahia.utils.maven.plugin.contentgenerator.mojo;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
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
			wiseRepositoryFile = wiseService.generateWise(wiseExport);
			getLog().info("Wise instance archive created and available here: " + wiseRepositoryFile);
		} catch (IOException e) {
			getLog().error("Error writing output file");
			e.printStackTrace();
		}
	}
}
