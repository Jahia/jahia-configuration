/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2023 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.contentgenerator.bo;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.FileBO;

/**
 * Contains all the parameters used to configure the export
 *
 * @author Guillaume Lucazeau
 *
 */
public class ExportBO {

    public static File tmp;

    protected File outputFile;
    protected String outputDir;
    protected String siteType;
    protected Integer nbPagesTopLevel;
    protected Integer nbSubLevels;
    protected Integer nbPagesPerLevel;
    protected Integer totalPages;
    protected Integer nbFoldersPerLevel;
    protected String rootPageName;
    protected String rootFolderName;
    protected Integer maxArticleIndex;
    protected File mapFile;
    protected Boolean pagesHaveVanity;
    protected String siteKey;
    protected List<String> siteLanguages;
    protected File filesDirectory;
    protected List<String> fileNames;
    protected List<FileBO> files =new ArrayList<>();
    protected Integer numberOfFilesToGenerate;
    protected Integer numberOfBigTextPerPage;
    protected Integer numberOfUsers;
    protected Integer numberOfOwners;
    protected Integer numberOfEditors;
    protected Integer numberOfCollaborators;
    protected Integer numberOfGroups;
    protected Integer numberOfUsersPerGroup;
    protected double groupAclRatio;
    protected double usersAclRatio;
    protected Integer numberOfSites;
    protected Integer numberOfCategories;
    protected Integer numberOfCategoryLevels;
    protected Integer numberOfTags;
    protected List<TagBO> tags;
    protected Boolean visibilityEnabled;
    protected String visibilityStartDate;
    protected String visibilityEndDate;
    protected Integer nbPolls;
    protected Integer nbTasks;
    protected Integer nbNotes;
    protected Integer nbDocspaces;
    protected Integer foldersDepth;
    protected Integer nbFilesPerFolder;
    protected Integer nbCollectionsPerUser;
    protected Integer nbFilesPerCollection;
    protected Date startCreationDateRange;
    protected Date endCreationDateRange;
    protected Integer percentagePagesWithTplQuery;
    protected Integer percentagePagesWithTplList;
    private Integer nbPagesWithTplList;
    private Integer nbPagesWithTplQuery;
    private String cmisUrl;
    private String cmisUser;
    private String cmisPassword;
    private String cmisRepositoryId;
    private String cmisSiteName;
    private String cmisServerType;
    private boolean disableInternalFileReference;
    private boolean disableExternalFileReference;
    private int pcPersonalizedPages;
    private int minPersonalizationVariants;
    private int maxPersonalizationVariants;
    private String jahiaRelease;
    private String buildNumber;

    public String getJahiaRelease() {
        return jahiaRelease;
    }

    public void setJahiaRelease(String jahiaRelease) {
        this.jahiaRelease = jahiaRelease;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getCmisUrl() {
        return cmisUrl;
    }

    public void setCmisUrl(String cmisUrl) {
        this.cmisUrl = cmisUrl;
    }

    public String getCmisUser() {
        return cmisUser;
    }

    public void setCmisUser(String cmisUser) {
        this.cmisUser = cmisUser;
    }

    public String getCmisPassword() {
        return cmisPassword;
    }

    public void setCmisPassword(String cmisPassword) {
        this.cmisPassword = cmisPassword;
    }

    public String getCmisServerType() {
        return cmisServerType;
    }

    public void setCmisServerType(String cmisServerType) {
        this.cmisServerType = cmisServerType;
    }

    public String getCmisRepositoryId() {
        return cmisRepositoryId;
    }

    public void setCmisRepositoryId(String cmisRepositoryId) {
        this.cmisRepositoryId = cmisRepositoryId;
    }

    public String getCmisSiteName() {
        return cmisSiteName;
    }

    public void setCmisSiteName(String cmisSiteName) {
        this.cmisSiteName = cmisSiteName;
    }

    public Integer getPercentagePagesWithTplQuery() {
        return percentagePagesWithTplQuery;
    }

    public void setPercentagePagesWithTplQuery(Integer percentagePagesWithTplQuery) {
        this.percentagePagesWithTplQuery = percentagePagesWithTplQuery;
    }

    public Integer getPercentagePagesWithTplList() {
        return percentagePagesWithTplList;
    }

    public void setPercentagePagesWithTplList(Integer percentagePagesWithTplList) {
        this.percentagePagesWithTplList = percentagePagesWithTplList;
    }

    public Integer getNbPagesWithTplList() {
        return nbPagesWithTplList;
    }

    public void setNbPagesWithTplList(Integer nbPagesWithTplList) {
        this.nbPagesWithTplList = nbPagesWithTplList;
    }

    public Integer getNbPagesWithTplQuery() {
        return nbPagesWithTplQuery;
    }

    public void setNbPagesWithTplQuery(Integer nbPagesWithTplQuery) {
        this.nbPagesWithTplQuery = nbPagesWithTplQuery;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public void setSiteType(String siteType) {
        this.siteType = siteType;
    }

    public String getSiteType() {
        return siteType;
    }

    public Integer getNbPagesTopLevel() {
        return nbPagesTopLevel;
    }

    public void setNbPagesTopLevel(Integer nbPagesTopLevel) {
        this.nbPagesTopLevel = nbPagesTopLevel;
    }

    public Integer getNbSubLevels() {
        return nbSubLevels;
    }

    public void setNbSubLevels(Integer nbSubLevels) {
        this.nbSubLevels = nbSubLevels;
    }

    public Integer getNbPagesPerLevel() {
        return nbPagesPerLevel;
    }

    public void setNbPagesPerLevel(Integer nbPagesPerLevel) {
        this.nbPagesPerLevel = nbPagesPerLevel;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public String getRootPageName() {
        return rootPageName;
    }

    public void setRootPageName(String rootPageName) {
        this.rootPageName = rootPageName;
    }

    public String getRootFolderName() {
        return rootFolderName;
    }

    public void setRootFolderName(String rootFolderName) {
        this.rootFolderName = rootFolderName;
    }

    public Integer getMaxArticleIndex() {
        return maxArticleIndex;
    }

    public void setMaxArticleIndex(Integer maxArticleIndex) {
        this.maxArticleIndex = maxArticleIndex;
    }

    public File getMapFile() {
        return mapFile;
    }

    public void setMapFile(File mapFile) {
        this.mapFile = mapFile;
    }

    public Boolean getPagesHaveVanity() {
        return pagesHaveVanity;
    }

    public void setPagesHaveVanity(Boolean pagesHaveVanity) {
        this.pagesHaveVanity = pagesHaveVanity;
    }

    public String getSiteKey() {
        return siteKey;
    }

    public void setSiteKey(String siteKey) {
        this.siteKey = siteKey;
    }

    public List<String> getSiteLanguages() {
        return siteLanguages;
    }

    public void setSiteLanguages(List<String> siteLanguages) {
        this.siteLanguages = siteLanguages;
    }

    public File getFilesDirectory() {
        return filesDirectory;
    }

    public void setFilesDirectory(File filesDirectory) {
        this.filesDirectory = filesDirectory;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }

    public List<FileBO> getFiles() {
        return files;
    }

    public void setFiles(List<FileBO> files) {
        this.files = files;
    }

    public Integer getNumberOfFilesToGenerate() {
        return numberOfFilesToGenerate;
    }

    public void setNumberOfFilesToGenerate(Integer numberOfFilesToGenerate) {
        this.numberOfFilesToGenerate = numberOfFilesToGenerate;
    }

    public Integer getNumberOfBigTextPerPage() {
        return numberOfBigTextPerPage;
    }

    public void setNumberOfBigTextPerPage(Integer numberOfBigTextPerPage) {
        this.numberOfBigTextPerPage = numberOfBigTextPerPage;
    }

    public Integer getNumberOfUsers() {
        return numberOfUsers;
    }

    public void setNumberOfUsers(Integer numberOfUsers) {
        this.numberOfUsers = numberOfUsers;
    }

    public Integer getNumberOfOwners() {
        return numberOfOwners;
    }

    public void setNumberOfOwners(Integer numberOfOwners) {
        this.numberOfOwners = numberOfOwners;
    }

    public Integer getNumberOfEditors() {
        return numberOfEditors;
    }

    public void setNumberOfEditors(Integer numberOfEditors) {
        this.numberOfEditors = numberOfEditors;
    }

    public Integer getNumberOfCollaborators() {
        return numberOfCollaborators;
    }

    public void setNumberOfCollaborators(Integer numberOfCollaborators) {
        this.numberOfCollaborators = numberOfCollaborators;
    }

    public Integer getNumberOfGroups() {
        return numberOfGroups;
    }

    public void setNumberOfGroups(Integer numberOfGroups) {
        this.numberOfGroups = numberOfGroups;
    }

    public Integer getNumberOfUsersPerGroup() {
        return numberOfUsersPerGroup;
    }

    public void setNumberOfUsersPerGroup(Integer numberOfUsersPerGroup) {
        this.numberOfUsersPerGroup = numberOfUsersPerGroup;
    }

    public double getGroupAclRatio() {
        return groupAclRatio;
    }

    public void setGroupAclRatio(double groupAclRatio) {
        this.groupAclRatio = groupAclRatio;
    }

    public double getUsersAclRatio() {
        return usersAclRatio;
    }

    public void setUsersAclRatio(double usersAclRatio) {
        this.usersAclRatio = usersAclRatio;
    }

    public Integer getNumberOfSites() {
        return numberOfSites;
    }

    public void setNumberOfSites(Integer numberOfSites) {
        this.numberOfSites = numberOfSites;
    }

    public Integer getNumberOfCategories() {
        return numberOfCategories;
    }

    public void setNumberOfCategories(Integer numberOfCategories) {
        this.numberOfCategories = numberOfCategories;
    }

    public Integer getNumberOfCategoryLevels() {
        return numberOfCategoryLevels;
    }

    public void setNumberOfCategoryLevels(Integer numberOfCategoryLevels) {
        this.numberOfCategoryLevels = numberOfCategoryLevels;
    }

    public Integer getNumberOfTags() {
        return numberOfTags;
    }

    public void setNumberOfTags(Integer numberOfTags) {
        this.numberOfTags = numberOfTags;
    }

    public List<TagBO> getTags() {
        return tags;
    }

    public void setTags(List<TagBO> tags) {
        this.tags = tags;
    }

    public Boolean getVisibilityEnabled() {
        return visibilityEnabled;
    }

    public void setVisibilityEnabled(Boolean visibilityEnabled) {
        this.visibilityEnabled = visibilityEnabled;
    }

    public String getVisibilityStartDate() {
        return visibilityStartDate;
    }

    public void setVisibilityStartDate(String visibilityStartDate) {
        this.visibilityStartDate = visibilityStartDate;
    }

    public String getVisibilityEndDate() {
        return visibilityEndDate;
    }

    public void setVisibilityEndDate(String visibilityEndDate) {
        this.visibilityEndDate = visibilityEndDate;
    }

    public Integer getNbPolls() {
        return nbPolls;
    }

    public void setNbPolls(Integer nbPolls) {
        this.nbPolls = nbPolls;
    }

    public Integer getNbTasks() {
        return nbTasks;
    }

    public void setNbTasks(Integer nbTasks) {
        this.nbTasks = nbTasks;
    }

    public Integer getNbNotes() {
        return nbNotes;
    }

    public void setNbNotes(Integer nbNotes) {
        this.nbNotes = nbNotes;
    }

    public Integer getNbDocspaces() {
        return nbDocspaces;
    }

    public void setNbDocspaces(Integer nbDocspaces) {
        this.nbDocspaces = nbDocspaces;
    }

    public Integer getNbFoldersPerLevel() {
        return nbFoldersPerLevel;
    }

    public void setNbFoldersPerLevel(Integer nbFoldersPerLevel) {
        this.nbFoldersPerLevel = nbFoldersPerLevel;
    }

    public Integer getFoldersDepth() {
        return foldersDepth;
    }

    public void setFoldersDepth(Integer foldersDepth) {
        this.foldersDepth = foldersDepth;
    }

    public Integer getNbFilesPerFolder() {
        return nbFilesPerFolder;
    }

    public void setNbFilesPerFolder(Integer nbFilesPerFolder) {
        this.nbFilesPerFolder = nbFilesPerFolder;
    }

    public Integer getNbCollectionsPerUser() {
        return nbCollectionsPerUser;
    }

    public void setNbCollectionsPerUser(Integer nbCollectionsPerUser) {
        this.nbCollectionsPerUser = nbCollectionsPerUser;
    }

    public Integer getNbFilesPerCollection() {
        return nbFilesPerCollection;
    }

    public void setNbFilesPerCollection(Integer nbFilesPerCollection) {
        this.nbFilesPerCollection = nbFilesPerCollection;
    }

    public Date getStartCreationDateRange() {
        return startCreationDateRange;
    }

    public void setStartCreationDateRange(Date startCreationDateRange) {
        this.startCreationDateRange = startCreationDateRange;
    }

    public Date getEndCreationDateRange() {
        return endCreationDateRange;
    }

    public void setEndCreationDateRange(Date endCreationDateRange) {
        this.endCreationDateRange = endCreationDateRange;
    }

    public boolean isDisableInternalFileReference() {
        return disableInternalFileReference;
    }

    public void setDisableInternalFileReference(boolean disableInternalFileReference) {
        this.disableInternalFileReference = disableInternalFileReference;
    }

    public boolean isDisableExternalFileReference() {
        return disableExternalFileReference;
    }

    public void setDisableExternalFileReference(boolean disableExternalFileReference) {
        this.disableExternalFileReference = disableExternalFileReference;
    }

    public int getPcPersonalizedPages() {
        return pcPersonalizedPages;
    }

    public void setPcPersonalizedPages(int pcPersonalizedPages) {
        this.pcPersonalizedPages = pcPersonalizedPages;
    }

    public int getMinPersonalizationVariants() {
        return minPersonalizationVariants;
    }

    public void setMinPersonalizationVariants(int minPersonalizationVariants) {
        this.minPersonalizationVariants = minPersonalizationVariants;
    }

    public int getMaxPersonalizationVariants() {
        return maxPersonalizationVariants;
    }

    public void setMaxPersonalizationVariants(int maxPersonalizationVariants) {
        this.maxPersonalizationVariants = maxPersonalizationVariants;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<!-- export information -->\n");
        sb.append("<!-- top level pages: " + this.getNbPagesTopLevel() + " -->\n");
        sb.append("<!-- sub levels: " + this.getNbSubLevels() + " -->\n");
        sb.append("<!-- sub pages per page: " + this.getNbPagesPerLevel() + " -->\n");
        sb.append("<!-- total pages: " + this.getTotalPages() + " -->\n");
        sb.append("<!-- site key: " + this.getSiteKey() + " -->\n");
        sb.append("<!-- big text per page: " + this.getNumberOfBigTextPerPage() + " -->\n");
        return sb.toString();
    }

}
