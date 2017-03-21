/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
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
	 * Number of notes to generate. 
	 * Each note contains a random Wikipedia article
	 * @parameter expression="${jahia.cg.wise.nbNotes}" default-value="10"
	 */
	private Integer nbNotes;
	
	/**
	 * Number of tasks to generate.
	 * Each task has a random user as creator and assignee
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
	 * Number of users with the role "docspace-owner". 
	 * They come first (user 0 to userN)
	 * @parameter expression="${jahia.cg.numberOfOwners}" default-value="5"
	 * @required
	 */
	protected Integer numberOfOwners;
	
	/**
	 * Number of users with the role "docspace-editor". 
	 * They come after the owners.
	 * @parameter expression="${jahia.cg.numberOfEditors}" default-value="10"
	 * @required
	 */
	protected Integer numberOfEditors;
	
	/**
	 * Number of users with the role "docspace-collaborator". 
	 * They come after the editors.
	 * @parameter expression="${jahia.cg.numberOfCollaborators}" default-value="15"
	 * @required
	 */
	protected Integer numberOfCollaborators;
	
	/**
	 * Start date for the range used to create a random creation date assigned to files in the JCR. 
	 * It actually doesn't work as during the import the date is replaced with the current date.
	 * @parameter expression="${jahia.cg.startCreationDateRange}" default-value="2010-01-01"
	 */
	protected String startCreationDateRange;
	
	/**
	 * End date for the range used to create a random creation date assigned to files in the JCR. 
	 * It actually doesn't work as during the import the date is replaced with the current date.
	 * @parameter expression="${jahia.cg.endCreationDateRange}" default-value="2012-10-01"
	 */
	protected String endCreationDateRange;
    
	protected ExportBO initExport() throws MojoExecutionException {
		boolean filesRequired = false;
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
