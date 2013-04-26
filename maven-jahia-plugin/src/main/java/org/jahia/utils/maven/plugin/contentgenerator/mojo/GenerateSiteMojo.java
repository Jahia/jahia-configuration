package org.jahia.utils.maven.plugin.contentgenerator.mojo;

import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;
import org.jahia.utils.maven.plugin.contentgenerator.ContentGeneratorService;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.w3c.dom.DOMException;

/**
 * Generate a Jahia site (ZIP file) ready to be imported
 * @goal generate-site
 * @requiresProject false
 * @author Guillaume Lucazeau
 * 
 */
public class GenerateSiteMojo extends AbstractJahiaSiteMojo {
	
	/**
	 * Number of big text container per page
	 * @parameter expression="${jahia.cg.numberOfBigTextPerPage}"
	 *            default-value="1"
	 */
	protected Integer numberOfBigTextPerPage;
	
	/**
	 * @parameter expression="${jahia.cg.numberOfGroups}" default-value="5"
	 */
	protected Integer numberOfGroups;

	/**
	 * @parameter expression="${jahia.cg.numberOfUsersPerGroup}" default-value="5"
	 */
	protected Integer numberOfUsersPerGroup;

    /**
     * @parameter expression="${jahia.cg.groupsAclRatio}" default-value="0"
     */
    protected double groupAclRatio;

    /**
     * @parameter expression="${jahia.cg.usersAclRatio}" default-value="0"
     */
    protected double usersAclRatio;

    /**
     * @parameter expression="${jahia.cg.numberOfSites}" default-value="1"
     */
    protected Integer numberOfSites;
    
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
    
	/**
	 * Choose if you want to add a vanity URL to your page (example: "page123")
	 * @parameter expression="${jahia.cg.pagesHaveVanity}" default-value="true"
	 */
	protected Boolean pagesHaveVanity;

	/**
	 * Site language(s), a comma-separated list
	 * @parameter expression="${jahia.cg.siteLanguages}" default-value="en,fr"
	 * @required
	 */
	protected String siteLanguages;

	/**
	 * Choose if you want to add file containers to your pages
	 * Possible values: all, random, none.
	 * @parameter expression="${jahia.cg.addFiles}" default-value="none"
	 */
	protected String addFiles;
	
	/**
	 * Number of pages on the top level (root pages)
	 * @parameter expression="${jahia.cg.nbPagesOnTopLevel}" default-value="1"
	 */
	protected Integer nbPagesOnTopLevel;

	/**
	 * Pages depth
	 * @parameter expression="${jahia.cg.nbSubLevels}" default-value="2"
	 */
	protected Integer nbSubLevels;

	/**
	 * Number of sub-pages per page
	 * @parameter expression="${jahia.cg.nbPagesPerLevel}" default-value="3"
	 * @required
	 */
	protected Integer nbPagesPerLevel;
    
	private ExportBO initExport() throws MojoExecutionException {
		ContentGeneratorService contentGeneratorService = ContentGeneratorService.getInstance();
		boolean filesRequired = false;
		if (ContentGeneratorCst.VALUE_ALL.equals(addFiles) || ContentGeneratorCst.VALUE_RANDOM.equals(addFiles)) {
			filesRequired = true;
		}
			
		ExportBO export = super.initExport(filesRequired);
		
		export.setAddFilesToPage(addFiles);
        export.setNbPagesTopLevel(nbPagesOnTopLevel);
        export.setNbSubLevels(nbSubLevels);
        export.setNbSubPagesPerPage(nbPagesPerLevel);
        export.setNumberOfBigTextPerPage(numberOfBigTextPerPage);        
        export.setNumberOfGroups(numberOfGroups);
        export.setNumberOfUsersPerGroup(numberOfUsersPerGroup);
        export.setGroupAclRatio(groupAclRatio);
        export.setUsersAclRatio(usersAclRatio);
        export.setNumberOfSites(numberOfSites);
        export.setPagesHaveVanity(pagesHaveVanity);
        
        String[] aLanguages = StringUtils.split(siteLanguages, ",");
        export.setSiteLanguages(Arrays.asList(aLanguages));
        
        if (visibilityEnabled == null) {
        	visibilityEnabled = Boolean.FALSE;
        }
        export.setVisibilityEnabled(visibilityEnabled);
        export.setVisibilityStartDate(visibilityStartDate);
        export.setVisibilityEndDate(visibilityEndDate);
        
        Integer totalPages = contentGeneratorService.getTotalNumberOfPagesNeeded(nbPagesOnTopLevel, nbSubLevels,
				nbPagesPerLevel);
		export.setTotalPages(totalPages);
		return export;
	}
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		ContentGeneratorService contentGeneratorService = ContentGeneratorService.getInstance();
		
		ExportBO export = this.initExport();
		if (export.getTotalPages().compareTo(ContentGeneratorCst.MAX_TOTAL_PAGES) > 0) {
			throw new MojoExecutionException("You asked to generate " + export.getTotalPages()
					+ " pages, the maximum allowed is " + ContentGeneratorCst.MAX_TOTAL_PAGES);
		}

		getLog().info("Jahia content generator starts");
		getLog().info(export.getSiteKey() + " site will be created");

		String zipFilePath = null;
		try {
			zipFilePath = contentGeneratorService.generateSites(export);
		} catch (DOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		getLog().info("Site archive created and available here: " + zipFilePath);
	}
}
