/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.contentgenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ArticleBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.GroupBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.MountPointBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.SiteBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.UserBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Document;
import org.jdom.Element;
import org.w3c.dom.DOMException;

public class ContentGeneratorService {

    public static int currentPageIndex = 0;
    public static int currentFileIndex = 0;

    private static ContentGeneratorService instance;

    private static final Log LOGGER = new SystemStreamLog();

    private ContentGeneratorService() {
    }

    public static ContentGeneratorService getInstance() {
        if (instance == null) {
            instance = new ContentGeneratorService();
        }
        return instance;
    }

    /**
     * Generates an XML files containing the pages tree and that can be imported into a Jahia website
     *
     * @param export
     */
    public void generateContents(ExportBO export, List<ArticleBO> articles) throws MojoExecutionException, IOException {
        if (!export.isDisableInternalFileReference() && export.getFileNames().isEmpty()) {
            throw new MojoExecutionException("Directory containing files to include is empty, use jahia:generate-files first");
        }
        ContentGeneratorService.currentPageIndex = 0;

        ContentService contentService = new ContentService();
        contentService.createTopContents(export, articles);
        LOGGER.debug("Contents generated, now site");
    }


    /**
     * Generates text files that can be used as attachments, with random content from the articles database
     *
     * @param export
     * @throws MojoExecutionException
     */
    public void generateFiles(ExportBO export) throws MojoExecutionException {

        LOGGER.info("Jahia files generator starts");

        Integer numberOfFilesToGenerate = export.getNumberOfFilesToGenerate();
        if (numberOfFilesToGenerate == null) {
            throw new MojoExecutionException("numberOfFilesToGenerate parameter is required");
        }

        List<ArticleBO> articles = DatabaseService.getInstance().selectArticles(export, export.getNumberOfFilesToGenerate());
        int indexArticle = 0;
        File outputFile;

        for (int i = 0; i < numberOfFilesToGenerate; i++) {
            if (indexArticle == articles.size()) {
                indexArticle = 0;
            }

            outputFile = new File(export.getFilesDirectory(), "file." + i + ".txt");

            try {
                FileUtils.writeStringToFile(outputFile, articles.get(indexArticle).getContent());
            } catch (IOException e) {
                throw new MojoExecutionException("Error generating article file", e);
            }

            indexArticle++;
        }

        FileService fileService = new FileService();
        LOGGER.debug(export.getFilesDirectory().getAbsolutePath());
        List<String> filesNamesAvailable = fileService.getFileNamesAvailable(export.getFilesDirectory());
        export.setFileNames(filesNamesAvailable);

    }

    /**
     * Generates pages and then creates a ZIP archive containing those pages, the files needed for attachments and site.properties
     *
     *
     * @param export
     * @return Absolute path of ZIP file created
     * @throws MojoExecutionException
     * @throws ParserConfigurationException
     * @throws DOMException
     */
    public String generateSites(ExportBO export) throws MojoExecutionException, DOMException, ParserConfigurationException {

        OutputService os = new OutputService();
        UserGroupService userGroupService = new UserGroupService();

        List<File> globalFilesToZip = new ArrayList<>();

        try {
            List<UserBO> users = userGroupService.generateUsers(export.getNumberOfUsers(), 0, 0, null);
            File tmpUsers = new File(export.getOutputDir(), "users");
            tmpUsers.mkdir();

            File repositoryUsers = new File(tmpUsers, "repository.xml");
            Document usersRepositoryDocument = userGroupService.createUsersRepository(users);
            os.writeJdomDocumentToFile(usersRepositoryDocument, repositoryUsers);

            List<File> filesToZip = new ArrayList<>();
            File contentUsers = userGroupService.createFileTreeForUsers(users, tmpUsers);

            filesToZip.add(repositoryUsers);
            filesToZip.add(contentUsers);
            File usersArchive = os.createSiteArchive("users.zip", export.getOutputDir(), filesToZip);
            globalFilesToZip.add(usersArchive);

            List<String> userNames = new ArrayList<>();
            for (UserBO user : users) {
                userNames.add(user.getName());
            }
            File usersFile = new File(export.getOutputDir(), "users.txt");
            usersFile.delete();
            os.appendPathToFile(usersFile, userNames);

            String baseSiteKey = export.getSiteKey();

            export.getMapFile().delete();

            SiteService siteService = new SiteService();
            TagService tagService = new TagService();
            CategoryService categoryService = new CategoryService();

            List<ArticleBO> articles = DatabaseService.getInstance().selectArticles(export, export.getTotalPages());

            for (int i = 0; i < export.getNumberOfSites(); i++) {

                LOGGER.debug("Generating site #" + (i + 1));

                SiteBO site = new SiteBO();
                String siteKey = baseSiteKey + (i > 0 ? i + 1 : "");
                export.setSiteKey(siteKey);
                site.setSiteKey(siteKey);

                // as we create a full site we will need a home page or a top folder
                export.setRootFolderName(ContentGeneratorCst.ROOT_FOLDER_NAME);
                export.setRootPageName(ContentGeneratorCst.ROOT_PAGE_NAME);
                generateContents(export, articles);

                filesToZip = new ArrayList<>();

                // create temporary dir in output dir (siteKey)
                File tempOutputDir = siteService.createSiteDirectory(siteKey, new File(export.getOutputDir()));

                // create properties file
                File propertiesFile = siteService.createPropertiesFile(siteKey, export.getSiteLanguages(),
                        ContentGeneratorCst.TEMPLATE_SET_DEFAULT, tempOutputDir);
                filesToZip.add(propertiesFile);

                // create tree dirs for files attachments (if files are not at
                // "none")
                File filesFile = null;
                if (!export.isDisableInternalFileReference()) {
                    FileService fileService = new FileService();
                    File filesDirectory = siteService.createFilesDirectoryTree(siteKey, tempOutputDir);
                    filesToZip.add(new File(tempOutputDir + "/content"));

                    // get all files available in the pool dir
                    List<File> filesToCopy = fileService.getFilesAvailable(export.getFilesDirectory());

                    fileService.copyFilesForAttachment(filesToCopy, filesDirectory);

                    // generates XML code for files
                    filesFile = new File(export.getOutputDir(), "jcrFiles.xml");
                    fileService.createAndPopulateFilesXmlFile(filesFile, filesToCopy);
                }

                // Groups
                File groupsFile = null;
                if (export.getNumberOfUsers() > 0) {
                    List<GroupBO> groups = userGroupService.generateGroups(export.getNumberOfGroups(), export.getNumberOfUsersPerGroup(),
                            users);

                    Element groupsNode = userGroupService.generateJcrGroups(siteKey, groups);
                    Document groupsDoc = new Document(groupsNode);
                    groupsFile = new File(export.getOutputDir(), "groups.xml");
                    os.writeJdomDocumentToFile(groupsDoc, groupsFile);
                }

                // Tags
                Element tagList = tagService.createTagListElement();
                List<Element> tags = tagService.createTags(export.getNumberOfTags());
                tagList.addContent(tags);

                Document tagsDoc = new Document(tagList);
                File tagsFile = new File(export.getOutputDir(), "tags.xml");
                os.writeJdomDocumentToFile(tagsDoc, tagsFile);

                // Copy pages => repository.xml
                File repositoryFile = siteService.createAndPopulateRepositoryFile(tempOutputDir, site, export.getOutputFile(), filesFile,
                        groupsFile, tagsFile);

                filesToZip.add(repositoryFile);

                String zipFileName = siteKey + ".zip";
                File siteArchive = os.createSiteArchive(zipFileName, export.getOutputDir(), filesToZip);

                filesToZip.clear();

                // Global site archive
                globalFilesToZip.add(siteArchive);
            }

            // system site
            Document systemSiteRepository = siteService.createSystemSiteRepository();

            Element categories = categoryService.createCategories(export.getNumberOfCategories(), export.getNumberOfCategoryLevels(),
                    export);
            categoryService.insertCategoriesIntoSiteRepository(systemSiteRepository, categories);

            String systemSiteRepositoryFileName = "repository.xml";
            File systemSiteRepositoryFile = new File(export.getOutputDir(), systemSiteRepositoryFileName);
            os.writeJdomDocumentToFile(systemSiteRepository, systemSiteRepositoryFile);

            // zip systemsite
            filesToZip.add(systemSiteRepositoryFile);
            File systemSiteArchive = os.createSiteArchive("systemsite.zip", export.getOutputDir(), filesToZip);
            globalFilesToZip.add(systemSiteArchive);
            LOGGER.info("System site archive created");

            // Mount points
            if (!export.isDisableExternalFileReference()) {
                LOGGER.info("Generating mount points");
                filesToZip.clear();

                // creating temporary files with all the mount points
                MountPointBO mountPointCmis = new MountPointBO(ContentGeneratorCst.CMIS_MOUNT_POINT_NAME,
                        ContentGeneratorCst.MOUNT_POINT_CMIS, export.getCmisRepositoryId(), export.getCmisUrl(), export.getCmisUser(),
                        export.getCmisPassword(), export.getCmisServerType());
                Document cmisMountDoc = new Document(mountPointCmis.getElement());

                File mountsFile = new File(export.getOutputDir(), "mounts.xml");
                os.writeJdomDocumentToFile(cmisMountDoc, mountsFile);

                // create temporary dir in output dir to generate the repository.xml contained the mounts.zip file
                // we need a temporary dir because the system site repository.xml is already created in export.getOutputDir()
                File tempOutputDir = new File(export.getOutputDir(), "mounts");
                tempOutputDir.mkdir();

                MountPointService mountPointService = new MountPointService();
                File mountPointsRepository = mountPointService.createAndPopulateRepositoryFile(tempOutputDir, mountsFile);

                // zip mount points
                filesToZip.add(mountPointsRepository);
                File mountPointsArchive = os.createSiteArchive("mounts.zip", export.getOutputDir(), filesToZip);
                globalFilesToZip.add(mountPointsArchive);
                LOGGER.info("Mount point archive created");
            }

            // export.properties
            Properties exportProperties = new Properties();
            exportProperties.put("JahiaRelease", export.getJahiaRelease());
            exportProperties.put("BuildNumber", export.getBuildNumber());
            File exportPropertiesFile = new File(export.getOutputDir(), "export.properties");
            os.writePropertiesToFile(exportProperties, exportPropertiesFile);
            globalFilesToZip.add(exportPropertiesFile);

            // global archive, the one that we will import in Jahia
            File globalArchive = os.createSiteArchive("import.zip", export.getOutputDir(), globalFilesToZip);
            return globalArchive.getAbsolutePath();

        } catch (IOException e) {
            throw new MojoExecutionException("Error generating website", e);
        }
    }

    /**
     * Calculates the number of pages needed, used to know how much articles we will need
     *
     * @param nbPagesTopLevel
     * @param nbLevels
     * @param nbPagesPerLevel
     * @return number of pages needed
     */
    public Integer getTotalNumberOfPagesNeeded(Integer nbPagesTopLevel, Integer nbLevels, Integer nbPagesPerLevel) {
        Double nbPages = new Double(0);
        for (double d = nbLevels; d > 0; d--) {
            nbPages += Math.pow(nbPagesPerLevel.doubleValue(), d);
        }
        nbPages = nbPages * nbPagesTopLevel + nbPagesTopLevel;

        return new Integer(nbPages.intValue());
    }

    /**
     * Format a date for inclusion in JCR XML file If date is null, current date is used Format used:
     * http://www.day.com/specs/jcr/1.0/6.2.5.1_Date.html
     *
     * @param date
     * @return formated date
     */
    public String getDateForJcrImport(Date date) {
        GregorianCalendar gc = (GregorianCalendar) GregorianCalendar.getInstance();
        if (date != null) {
            gc.setTime(date);
        }
        StringBuffer sbNewDate = new StringBuffer();
        // 2011-04-01T17:39:59.265+02:00
        sbNewDate.append(gc.get(Calendar.YEAR));
        sbNewDate.append("-");
        sbNewDate.append(gc.get(Calendar.MONTH));
        sbNewDate.append("-");
        sbNewDate.append(gc.get(Calendar.DAY_OF_MONTH));
        sbNewDate.append("T");
        sbNewDate.append(gc.get(Calendar.HOUR_OF_DAY));
        sbNewDate.append(":");
        sbNewDate.append(gc.get(Calendar.MINUTE));
        sbNewDate.append(":");
        sbNewDate.append(gc.get(Calendar.SECOND));
        sbNewDate.append(".");
        sbNewDate.append(gc.get(Calendar.MILLISECOND));
        sbNewDate.append(gc.get(Calendar.ZONE_OFFSET));
        return sbNewDate.toString();
    }
}
