/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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

import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;
import org.jahia.utils.maven.plugin.contentgenerator.ContentGeneratorService;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;

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
     * For each created page a random float will be compared to this value and if inferior an ACL node with a random group will be added.
     * @parameter expression="${jahia.cg.groupsAclRatio}" default-value="0"
     */
    protected double groupAclRatio;

    /**
     * For each created page a random float will be compared to this value and if inferior an ACL node with a random user will be added.
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

    /**
     * Percentage of pages using the Jahia QA page template "qa-list" (tpl-web-blue, branch qa)
     * @parameter expression="${jahia.cg.percentagePagesWithTplList}" default-value="0"
     */
    protected Integer percentagePagesWithTplList;

    /**
     * Percentage of pages using the Jahia QA page template "qa-query" (tpl-web-blue, branch qa)
     * @parameter expression="${jahia.cg.percentagePagesWithTplQuery}" default-value="0"
     */
    protected Integer percentagePagesWithTplQuery;

    /**
     * Connection URL to the CMIS server
     * @parameter expression="${jahia.cg.cmis.url}"
     */
    protected String cmisUrl;

    /**
     * Connection user to the CMIS server
     * @parameter expression="${jahia.cg.cmis.user}"
     */
    protected String cmisUser;

    /**
     * Connection password to the CMIS server
     * @parameter expression="${jahia.cg.cmis.password}"
     */
    protected String cmisPassword;

    /**
     * CMIS Repository ID on the CMIS server
     * @parameter expression="${jahia.cg.cmis.repository.id}"
     */
    protected String cmisRepositoryId;

    /**
     * Site name on the CMIS server
     * @parameter expression="${jahia.cg.cmis.siteName}"
     */
    protected String cmisSiteName;

    /**
     * Number of files available on the server
     * @description Currently with use this pattern sample.<id>.txt. id goes from 0 to number of files-1
     * @parameter expression="${jahia.cg.cmis.nb.available.files}"
     */
    protected Integer cmisNbAvailableFiles;

    /**
     * Disable reference to internal files on pages
     * @parameter expression="${jahia.cg.disable.internal.filereference}" default-value="false"
     */
    protected boolean disableInternalFileReference;

    /**
     * Disable reference to internal files on pages
     * @parameter expression="${jahia.cg.disable.external.filereference}" default-value="false"
     */
    protected boolean disableExternalFileReference;

    /**
     * How many pages should contain a single piece of personalized content, per cent.
     * @parameter expression="${jahia.cg.pcPersonalizedPages}" default-value="0"
     */
    protected int pcPersonalizedPages;

    /**
     * Minimum number of personalization variants, in case we personalize a piece of content.
     * @parameter expression="${jahia.cg.minPersonalizationVariants}" default-value="2"
     */
    protected int minPersonalizationVariants;

    /**
     * Maximum number of personalization variants, in case we personalize a piece of content.
     * @parameter expression="${jahia.cg.maxPersonalizationVariants}" default-value="3"
     */
    protected Integer maxPersonalizationVariants;

    @Override
    protected ExportBO initExport() throws MojoExecutionException {

        ContentGeneratorService contentGeneratorService = ContentGeneratorService.getInstance();
        ExportBO export = super.initExport();

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
        export.setPercentagePagesWithTplList(percentagePagesWithTplList);
        export.setPercentagePagesWithTplQuery(percentagePagesWithTplQuery);
        export.setCmisUrl(cmisUrl);
        export.setCmisUser(cmisUser);
        export.setCmisPassword(cmisPassword);
        export.setCmisRepositoryId(cmisRepositoryId);
        export.setCmisSiteName(cmisSiteName);
        export.setDisableExternalFileReference(disableExternalFileReference);
        export.setDisableInternalFileReference(disableInternalFileReference);

        String[] aLanguages = StringUtils.split(siteLanguages, ",");
        export.setSiteLanguages(Arrays.asList(aLanguages));

        if (visibilityEnabled == null) {
            visibilityEnabled = Boolean.FALSE;
        }
        export.setVisibilityEnabled(visibilityEnabled);
        export.setVisibilityStartDate(visibilityStartDate);
        export.setVisibilityEndDate(visibilityEndDate);

        Integer totalPages = contentGeneratorService.getTotalNumberOfPagesNeeded(nbPagesOnTopLevel, nbSubLevels, nbPagesPerLevel);
        export.setTotalPages(totalPages);

        if (percentagePagesWithTplList > 0) {
            export.setNbPagesWithTplList(new Integer(totalPages / percentagePagesWithTplList));
        } else {
            export.setNbPagesWithTplList(0);
        }

        if (percentagePagesWithTplQuery > 0) {
            export.setNbPagesWithTplQuery(new Integer(totalPages / percentagePagesWithTplQuery));
        } else {
            export.setNbPagesWithTplQuery(0);
        }

        if (pcPersonalizedPages > 100) {
            throw new RuntimeException("Wrong perconalized content percentage");
        }
        if (minPersonalizationVariants < 0) {
            throw new RuntimeException("Wrong min number of personalization variants");
        }
        if (maxPersonalizationVariants < minPersonalizationVariants) {
            throw new RuntimeException("Wrong max number of personalization variants");
        }
        export.setPcPersonalizedPages(pcPersonalizedPages);
        export.setMinPersonalizationVariants(minPersonalizationVariants);
        export.setMaxPersonalizationVariants(maxPersonalizationVariants);

        return export;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        ContentGeneratorService contentGeneratorService = ContentGeneratorService.getInstance();

        ExportBO export = this.initExport();
        if (export.getTotalPages().compareTo(ContentGeneratorCst.MAX_TOTAL_PAGES) > 0) {
            throw new MojoExecutionException("You asked to generate " + export.getTotalPages() + " pages, the maximum allowed is " + ContentGeneratorCst.MAX_TOTAL_PAGES);
        }

        getLog().info("Jahia content generator starts");
        getLog().info(export.getSiteKey() + " site will be created");

        String zipFilePath = null;
        try {
            zipFilePath = contentGeneratorService.generateSites(export);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

        getLog().info("Site archive created and available here: " + zipFilePath);
    }
}
