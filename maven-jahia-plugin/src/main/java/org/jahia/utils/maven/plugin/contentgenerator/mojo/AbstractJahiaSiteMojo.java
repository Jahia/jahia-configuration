package org.jahia.utils.maven.plugin.contentgenerator.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;

/**
 * Group common parameters for Jahia site and Wise instance
 * @author Guillaume Lucazeau
 *
 */
public abstract class AbstractJahiaSiteMojo extends AbstractContentGeneratorMojo {
	/**
	 * Site key of your site. A trailing number will be added if you generate more than one site.
	 * @parameter expression="${jahia.cg.siteKey}" default-value="testSite"
	 * @required
	 */
	protected String siteKey;
	
	/**
	 * Number of users to generate
	 * @parameter expression="${jahia.cg.numberOfUsers}" default-value="25"
	 * @required
	 */
	protected Integer numberOfUsers;
	
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

	public abstract void execute() throws MojoExecutionException, MojoFailureException;

	/**
	 * Get properties and initialize ExportBO
	 * 
	 * @return a new export BO containing all the parameters
	 */
	protected ExportBO initExport() throws MojoExecutionException {
		ExportBO export = super.initExport();
		super.initFilesProperties(export);
		export.setSiteKey(siteKey);
		export.setNumberOfUsers(numberOfUsers);
		export.setNumberOfTags(numberOfTags);
		export.setNumberOfCategories(numberOfCategories);
		export.setNumberOfCategoryLevels(numberOfCategoryLevels);
		return export;
	}
}
