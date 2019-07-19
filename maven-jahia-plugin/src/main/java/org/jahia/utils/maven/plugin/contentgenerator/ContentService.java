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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ArticleBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ContentBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.FolderBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.PageBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jahia.utils.maven.plugin.support.RandomUtils;
import org.jdom.Document;

public class ContentService {

    private int nbOftenKeywordsAlreadyAssigned;
    private int nbSeldomKeywordsAlreadyAssigned;
    private Map<String, List<CmisDirectoryPath>> cmisFilePaths;
    private final Random random = new Random();

    private static final List<String> OFTEN_USED_DESCRIPTION_WORDS = Arrays.asList(ContentGeneratorCst.OFTEN_USED_DESCRIPTION_WORDS.split("\\s*,\\s*"));
    private static final List<String> SELDOM_USED_DESCRIPTION_WORDS = Arrays.asList(ContentGeneratorCst.SELDOM_USED_DESCRIPTION_WORDS.split("\\s*,\\s*"));
    private static final Log LOGGER = new SystemStreamLog();

    public ContentService() {
        initCmisFilePath();
    }

    /**
     * Main method, create top pages and for each calls the sub pages generation. Each time a top page and its sub pages have been
     * generated, they are sent to the writer to avoid out of memory error with a big number of pages.
     *
     * @param export
     *            Export BO contains all the parameters chose by the user to configure his/her content generation
     * @param articles
     *            List of articles selected from database
     * @throws IOException
     *             Error while writing generated XML to the output file
     */
    public void createTopContents(ExportBO export, List<ArticleBO> articles) throws IOException {

        OutputService os = new OutputService();
        ArticleService articleService = ArticleService.getInstance();
        String rootContentName;
        if (export.getSiteType().equals("Headless")) {
            rootContentName = export.getRootFolderName();
        }
        else {
            rootContentName = export.getRootPageName();
        }
        LOGGER.info("Creating top contents");


        Map<String, ArticleBO> articlesMap = new HashMap<>();
        for (String language : export.getSiteLanguages()) {
            articlesMap.put(language, articleService.getArticle(articles));
        }

        OutputService outService = new OutputService();
        outService.initOutputFile(export.getOutputFile());
        outService.appendStringToFile(export.getOutputFile(), export.toString());

        List<ContentBO> listeTopContents = new ArrayList<>();
        for (int i = 1; i <= export.getNbPagesTopLevel().intValue(); i++) {
            for (String language : export.getSiteLanguages()) {
                articlesMap.put(language, articleService.getArticle(articles));
            }
            ContentBO contentTopLevel = null;

            contentTopLevel = createNewContent(export, null, articlesMap, export.getNbSubLevels() + 1,
                    createSubContents(export, articles, export.getNbSubLevels(), export.getMaxArticleIndex()));

            outService.appendContentToFile(export.getOutputFile(), contentTopLevel);

            listeTopContents.add(contentTopLevel);
        }

        LOGGER.info("Contents path are being written to the map file");
        ContentBO rootContent = createNewContent(export, rootContentName, articlesMap, export.getNbSubLevels() + 1, listeTopContents);
        List<String> contentsPath = getContentsPath(listeTopContents, "/sites/" + export.getSiteKey() + "/" + rootContent.getName());
        outService.appendPathToFile(export.getMapFile(), contentsPath);
        outService.appendStringToFile(export.getOutputFile(), rootContent.getJcrXml());
        Document document = new Document(rootContent.getElement());
        os.writeJdomDocumentToFile(document, export.getOutputFile());
    }

    /**
     * Recursive method that will generate all sub contents of a content, and call itself as much as necessary to reach the number of levels
     * requested
     *
     * @param export
     *            Export BO contains all the parameters chose by the user to configure his/her content generation
     * @param articles
     *            List of articles selected from database
     * @param level
     *            Current level in the tree decrease each time, top level == number of levels requested by the user)
     * @param maxArticleIndex
     *            Index of the last article
     * @return A list of Content BO, containing their sub pages (if they have some)
     */
    public List<ContentBO> createSubContents(ExportBO export, List<ArticleBO> articles, Integer level, Integer maxArticleIndex) {

        ArticleService articleService = ArticleService.getInstance();
        List<ContentBO> listeContents = new ArrayList<>();
        Map<String, ArticleBO> articlesMap;

        int nbFoldersPerLevel = export.getNbPagesPerLevel();

        listeContents.clear();
        if (level.intValue() > 0) {
            for (int i = 0; i < nbFoldersPerLevel; i++) {
                articlesMap = new HashMap<>();
                for (String language : export.getSiteLanguages()) {
                    articlesMap.put(language, articleService.getArticle(articles));
                }
                ContentBO content = createNewContent(export, null, articlesMap, level,
                        createSubContents(export, articles, level.intValue() - 1, maxArticleIndex + 1));
                listeContents.add(content);
            }
        }
        return listeContents;
    }

    /**
     * Create a new page or folder object
     *
     * @param export
     *            Export BO contains all the parameters chose by the user to configure his/her content generation
     * @param contentName
     *            Used only for root page, or to specify a page name If null, creates a unique page name from concatenation of page name
     *            prefix and unique ID
     * @param articlesMap
     * @param level
     *            Current level in the tree
     * @param subContents
     *            List of sub contents related
     * @return
     */
    public ContentBO createNewContent(ExportBO export, String contentName, Map<String, ArticleBO> articlesMap, int level,
            List<ContentBO> subContents) {

        ContentGeneratorService.currentPageIndex = ContentGeneratorService.currentPageIndex + 1;

        // choose template (query, list, external)
        String template = ContentGeneratorCst.PAGE_TPL_DEFAULT;

        int indexPagesWithList = export.getNbPagesWithTplList();
        int indexPagesWithQuery = indexPagesWithList + export.getNbPagesWithTplQuery();

        // the remaining is distributed between pages with external file references, internal file references and just text
        int remainingNbPages = export.getTotalPages() - (export.getNbPagesWithTplList() + export.getNbPagesWithTplQuery());

        int nbPagesWithExternalFileReference = (export.isDisableExternalFileReference() ? 0
                : Math.round(remainingNbPages * ContentGeneratorCst.PERCENTAGE_PAGES_WITH_EXTERNAL_FILE_REF));
        int indexPagesWithExternalFileReference = indexPagesWithQuery + nbPagesWithExternalFileReference;

        int nbPagesWithInternalFileReference = (export.isDisableInternalFileReference() ? 0
                : Math.round(remainingNbPages * ContentGeneratorCst.PERCENTAGE_PAGES_WITH_INTERNAL_FILE_REF));
        int indexPagesWithInternalFileReference = indexPagesWithExternalFileReference + nbPagesWithInternalFileReference;

        // pages with qa-list template
        if (ContentGeneratorService.currentPageIndex <= indexPagesWithList) {
            template = ContentGeneratorCst.PAGE_TPL_QALIST;
        }

        // pages with qa-query template
        if (ContentGeneratorService.currentPageIndex > indexPagesWithList
                && ContentGeneratorService.currentPageIndex <= indexPagesWithQuery) {
            template = ContentGeneratorCst.PAGE_TPL_QAQUERY;
        }

        List<String> externalFilePaths = new ArrayList<>();
        if (ContentGeneratorService.currentPageIndex > indexPagesWithQuery
                && ContentGeneratorService.currentPageIndex <= indexPagesWithExternalFileReference) {
            contentName = ContentGeneratorCst.PAGE_TPL_QAEXTERNAL + ContentGeneratorService.currentPageIndex;
            externalFilePaths.add(getRandomCmisFilePath(ContentGeneratorCst.CMIS_PICTURES_DIR));
            externalFilePaths.add(getRandomCmisFilePath(ContentGeneratorCst.CMIS_TEXT_DIR));
        }

        String fileName = null;
        if (ContentGeneratorService.currentPageIndex > indexPagesWithExternalFileReference
                && ContentGeneratorService.currentPageIndex <= indexPagesWithInternalFileReference) {
            contentName = ContentGeneratorCst.PAGE_TPL_QAINTERNAL + ContentGeneratorService.currentPageIndex;
            FileService fileService = new FileService();
            fileName = fileService.getFileName(export.getFileNames());
        }

        if (contentName == null) {
            contentName = template + ContentGeneratorService.currentPageIndex;
        }

        LOGGER.debug("Creating new content level " + level + ": " + contentName);

        HashMap<String, List<String>> acls = new HashMap<>();
        if (random.nextFloat() < export.getGroupAclRatio() && export.getNumberOfGroups() > 0) {
            acls.put("g:group" + random.nextInt(export.getNumberOfGroups()), Arrays.asList("editor"));
        }
        if (random.nextFloat() < export.getUsersAclRatio() && export.getNumberOfUsers() > 0) {
            acls.put("u:user" + random.nextInt(export.getNumberOfUsers()), Arrays.asList("editor"));
        }

        // mapping to category
        Integer idCategory = null;
        float firstThird = export.getTotalPages() / 3;
        if (ContentGeneratorService.currentPageIndex <= firstThird && export.getNumberOfCategories() > 0) {
            idCategory = random.nextInt(export.getNumberOfCategories());
            LOGGER.debug("Add " + contentName + " to category " + idCategory);
        }

        Integer idTag = null;
        if (ContentGeneratorService.currentPageIndex <= firstThird && export.getNumberOfTags() > 0) {
            idTag = random.nextInt(export.getNumberOfTags());
            LOGGER.debug("Tag " + contentName + " with tag " + idTag);
        }

        // visibility
        Boolean visibilityOnPage = Boolean.FALSE;
        if (ContentGeneratorService.currentPageIndex <= firstThird && export.getVisibilityEnabled()) {
            visibilityOnPage = Boolean.TRUE;
            LOGGER.debug("Visibility enabled");
        }

        // add description for the first pages
        String oftenKeywords = getOftenKeywords(export.getTotalPages());
        String seldomKeywords = getSeldomKeywords(export.getTotalPages());
        String description = oftenKeywords + " " + seldomKeywords;

        ContentBO content = null;
        if (export.getSiteType().equals("Headless")) {
            content = new FolderBO(contentName, articlesMap, subContents, export.getSiteKey(), fileName,
                    export.getNumberOfBigTextPerPage(), acls, idCategory, idTag, visibilityOnPage, export.getVisibilityStartDate(),
                    export.getVisibilityEndDate(), description, export.getCmisSiteName(), externalFilePaths,
                    RandomUtils.isRandomOccurrence(export.getPcPersonalizedPages()), export.getMinPersonalizationVariants(),
                    export.getMaxPersonalizationVariants());
        }
        else {
            content = new PageBO(contentName, articlesMap, subContents, export.getPagesHaveVanity(), export.getSiteKey(),
                    fileName,
                    export.getNumberOfBigTextPerPage(), acls, idCategory, idTag, visibilityOnPage, export.getVisibilityStartDate(),
                    export.getVisibilityEndDate(), description, template, export.getCmisSiteName(), externalFilePaths,
                    RandomUtils.isRandomOccurrence(export.getPcPersonalizedPages()), export.getMinPersonalizationVariants(),
                    export.getMaxPersonalizationVariants());
        }

        return content;
    }


    /**
     * getPagesPathrecursively retrieves absolute paths for each page, from the top page. If choosen by the user, a map of this path will be
     * generated. It can be used to run performance tests.
     *
     * @param pages
     *            list of the top pages
     * @param path
     *            this method is recursive, this is the path generated for the pages above
     * @return String containing all the generated paths, one per line
     */
    public static List<String> getContentsPath(List<ContentBO> contents, String path) {

        List<String> siteMap = new ArrayList<>();

        if (path == null) {
            path = "";
        }
        for (Iterator<ContentBO> iterator = contents.iterator(); iterator.hasNext();) {
            ContentBO content = iterator.next();
            String newPath = path + ContentGeneratorCst.PAGE_PATH_SEPARATOR + content.getName();
            siteMap.add(newPath);

            if (content.getSubContents() != null) {
                siteMap.addAll(getContentsPath(content.getSubContents(), newPath));
            }
        }
        return siteMap;
    }


    private String getOftenKeywords(int nbOfPagesToCreate) {

        Integer nbKeywordsAvailable = OFTEN_USED_DESCRIPTION_WORDS.size();
        double ratio =  (ContentGeneratorCst.OFTEN_USED_DESCRIPTION_WORDS_COUNTER * (double) nbKeywordsAvailable / nbOfPagesToCreate);

        double nbKeywordsToGet = ((ContentGeneratorService.currentPageIndex + 1) * ratio) - nbOftenKeywordsAlreadyAssigned;
        nbKeywordsToGet = Math.floor(nbKeywordsToGet);


        Set<String> keywords = new HashSet<>();
        if (nbKeywordsAvailable <= nbKeywordsToGet) {
            keywords = new HashSet<>(OFTEN_USED_DESCRIPTION_WORDS);
        } else {
            int i = 1;
            while (i <= nbKeywordsToGet) {
                int randomId = random.nextInt(nbKeywordsAvailable - 1);
                boolean added = keywords.add(OFTEN_USED_DESCRIPTION_WORDS.get(randomId));
                if (added) {
                    i++;
                }
            }
        }
        StringBuffer sb = new StringBuffer();
        for (Iterator<String> iterator = keywords.iterator(); iterator.hasNext();) {
            sb = sb.append(iterator.next() + " ");
        }

        nbOftenKeywordsAlreadyAssigned += (int) nbKeywordsToGet;
        return sb.toString();
    }

    private String getSeldomKeywords(int nbOfPagesToCreate) {

        Integer nbKeywordsAvailable = SELDOM_USED_DESCRIPTION_WORDS.size();
        double ratio =  (ContentGeneratorCst.SELDOM_USED_DESCRIPTION_WORDS_COUNTER * (double) nbKeywordsAvailable / nbOfPagesToCreate);

        double nbKeywordsToGet = ((ContentGeneratorService.currentPageIndex + 1) * ratio) - nbSeldomKeywordsAlreadyAssigned;
        nbKeywordsToGet = Math.floor(nbKeywordsToGet);

        Set<String> keywords = new HashSet<>();if (nbKeywordsAvailable <= nbKeywordsToGet) {
            keywords = new HashSet<>(SELDOM_USED_DESCRIPTION_WORDS);
        } else {
            int i = 1;
            while (i <= nbKeywordsToGet) {
                int randomId = random.nextInt(nbKeywordsAvailable - 1);
                boolean added = keywords.add(SELDOM_USED_DESCRIPTION_WORDS.get(randomId));
                if (added) {
                    i++;
                }
            }
        }

        StringBuffer sb = new StringBuffer();
        for (Iterator<String> iterator = keywords.iterator(); iterator.hasNext();) {
            sb = sb.append(iterator.next() + " ");
        }

        nbSeldomKeywordsAlreadyAssigned += (int) nbKeywordsToGet;
        return sb.toString();
    }

    private String getRandomCmisFilePath(String type) {

        String randomFilePath;
        List<CmisDirectoryPath> pathsList = cmisFilePaths.get(type);
        int randomPathIndex = random.nextInt(pathsList.size() - 1);
        CmisDirectoryPath pathObject = pathsList.get(randomPathIndex);

        int randomFileId = random.nextInt(pathObject.getNbFiles() - 1);
        String randomFileName = randomFileId + pathObject.getFileSuffix();
        randomFilePath = pathObject.getDirectoryPath() + "/" + randomFileName;
        return randomFilePath;
    }

    private void initCmisFilePath() {

        List<CmisDirectoryPath> textPaths = new ArrayList<>();
        List<CmisDirectoryPath> picturesPaths = new ArrayList<>();

        int nbFiles = 100;
        for (int i = 0; i < 10; i++) {
            textPaths.add(new CmisDirectoryPath("/" + ContentGeneratorCst.CMIS_TEXT_DIR + "/directory-" + nbFiles + "-" + i, ".sample.txt", nbFiles));
            picturesPaths.add(new CmisDirectoryPath("/" + ContentGeneratorCst.CMIS_PICTURES_DIR + "/directory-" + nbFiles + "-" + i, ".sample.png", nbFiles));
        }

        nbFiles = 500;
        textPaths.add(new CmisDirectoryPath("/" + ContentGeneratorCst.CMIS_TEXT_DIR + "/directory-" + nbFiles + "-0", ".sample.txt", nbFiles));
        picturesPaths.add(new CmisDirectoryPath("/" + ContentGeneratorCst.CMIS_PICTURES_DIR + "/directory-" + nbFiles + "-0", ".sample.png", nbFiles));

        nbFiles = 1000;
        textPaths.add(new CmisDirectoryPath("/" + ContentGeneratorCst.CMIS_TEXT_DIR + "/directory-" + nbFiles + "-0", ".sample.txt", nbFiles));
        picturesPaths.add(new CmisDirectoryPath("/" + ContentGeneratorCst.CMIS_PICTURES_DIR + "/directory-" + nbFiles + "-0", ".sample.png", nbFiles));

        cmisFilePaths = new HashMap<>();
        cmisFilePaths.put(ContentGeneratorCst.CMIS_PICTURES_DIR, textPaths);
        cmisFilePaths.put(ContentGeneratorCst.CMIS_TEXT_DIR, picturesPaths);
    }

    private static class CmisDirectoryPath {

        private String directoryPath;
        private String fileSuffix;
        private int nbFiles;

        private CmisDirectoryPath(String directoryPath, String fileSuffix, int nbFiles) {
            this.directoryPath = "/documentLibrary" + directoryPath;
            this.fileSuffix = fileSuffix;
            this.nbFiles = nbFiles;
        }

        public String getDirectoryPath() {
            return directoryPath;
        }

        public String getFileSuffix() {
            return fileSuffix;
        }

        public int getNbFiles() {
            return nbFiles;
        }
    }

}
