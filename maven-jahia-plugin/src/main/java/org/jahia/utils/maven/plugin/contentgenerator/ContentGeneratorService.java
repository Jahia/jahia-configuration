package org.jahia.utils.maven.plugin.contentgenerator;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jahia.utils.maven.plugin.contentgenerator.bo.*;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.w3c.dom.DOMException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ContentGeneratorService {

    private Log logger = new SystemStreamLog();

    public static ContentGeneratorService instance;

    public static int currentPageIndex = 0;

    public static int currentFileIndex = 0;

    private ContentGeneratorService() {
    }

    public static ContentGeneratorService getInstance() {
        if (instance == null) {
            instance = new ContentGeneratorService();
        }
        return instance;
    }

    /**
     * Generates an XML files containing the pages tree and that can be imported
     * into a Jahia website
     *
     * @param export
     */
    public void generatePages(ExportBO export) throws MojoExecutionException, IOException {
        if (!ContentGeneratorCst.VALUE_NONE.equals(export.getAddFilesToPage()) && export.getFileNames().isEmpty()) {
            throw new MojoExecutionException(
                    "Directory containing files to include is empty, use jahia-cg:generate-files first");
        }

        List<ArticleBO> articles = DatabaseService.getInstance().selectArticles(export, export.getTotalPages());

        PageService pageService = new PageService();
        pageService.createTopPages(export, articles);
    }

    /**
     * Generates text files that can be used as attachments, with random content
     * from the articles database
     *
     * @param export
     * @throws MojoExecutionException
     */
    public void generateFiles(ExportBO export) throws MojoExecutionException {
        logger.info("Jahia files generator starts");

        Integer numberOfFilesToGenerate = export.getNumberOfFilesToGenerate();
        if (numberOfFilesToGenerate == null) {
            throw new MojoExecutionException("numberOfFilesToGenerate parameter is null");
        }

        List<ArticleBO> articles = DatabaseService.getInstance().selectArticles(export,
                export.getNumberOfFilesToGenerate());
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
                throw new MojoExecutionException("Can't create new file: " + e.getMessage());
            }

            indexArticle++;
        }

        FileService fileService = new FileService();
        logger.debug(export.getFilesDirectory().getAbsolutePath());
        List<String> filesNamesAvailable = fileService.getFileNamesAvailable(export.getFilesDirectory());
        export.setFileNames(filesNamesAvailable);

    }

    /**
     * Generates pages and then creates a ZIP archive containing those pages,
     * the files needed for attachments and site.properties
     *
     *
     * @param export
     * @return Absolute path of ZIP file created
     * @throws MojoExecutionException
     * @throws ParserConfigurationException
     * @throws DOMException
     */
    public String generateSites(ExportBO export) throws MojoExecutionException, DOMException,
            ParserConfigurationException {
        String globalArchivePath = null;

        OutputService os = new OutputService();
        UserGroupService userGroupService = new UserGroupService();

        List<File> globalFilesToZip = new ArrayList<File>();

        try {
            if (!ContentGeneratorCst.VALUE_NONE.equals(export.getAddFilesToPage()) && export.getFileNames().isEmpty()) {
                generateFiles(export);
            }

            List<UserBO> users = userGroupService.generateUsers(export.getNumberOfUsers());
            File tmpUsers = new File(export.getOutputDir(), "users");
            tmpUsers.mkdir();

            File repositoryUsers = new File(tmpUsers, "repository.xml");
            Document usersRepositoryDocument = userGroupService.createUsersRepository(users);
            os.writeJdomDocumentToFile(usersRepositoryDocument, repositoryUsers);

            List<File> filesToZip = new ArrayList<File>();
            File contentUsers = userGroupService.createFileTreeForUsers(users, tmpUsers);

            filesToZip.add(repositoryUsers);
            filesToZip.add(contentUsers);
            File usersArchive = os.createSiteArchive("users.zip", export.getOutputDir(), filesToZip);
            globalFilesToZip.add(usersArchive);

            List<String> userNames = new ArrayList<String>();
            for (UserBO user : users) {
                userNames.add(user.getName());
            }
            File f = new File(export.getOutputDir(), "users.txt");
            f.delete();
            os.appendPathToFile(f, userNames);

            String baseSiteKey = export.getSiteKey();

            export.getMapFile().delete();

            SiteService siteService = new SiteService();            
            for (int i = 0; i < export.getNumberOfSites(); i++) {
                // as we create a full site we will need a home page
                export.setRootPageName(ContentGeneratorCst.ROOT_PAGE_NAME);
                SiteBO site = new SiteBO();
                String siteKey = baseSiteKey + (i > 0 ? i+1 : "");
                export.setSiteKey(siteKey);
                site.setSiteKey(siteKey);

                

                generatePages(export);

                logger.debug("Pages generated, now site");
                filesToZip = new ArrayList<File>();

                // create temporary dir in output dir (siteKey)
                File tempOutputDir = siteService.createSiteDirectory(siteKey, new File(export.getOutputDir()));

                // create properties file
                File propertiesFile = siteService.createPropertiesFile(siteKey, export.getSiteLanguages(), tempOutputDir);
                filesToZip.add(propertiesFile);

                // create tree dirs for files attachments (if files are not at
                // "none")
                File filesFile = null;
                if (!ContentGeneratorCst.VALUE_NONE.equals(export.getAddFilesToPage())) {
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

                List<GroupBO> groups = userGroupService.generateGroups(export.getNumberOfGroups(), export.getNumberOfUsersPerGroup(), users);
                Element groupsNode = userGroupService.generateJcrGroups(siteKey, groups);
                Document groupsDoc = new Document(groupsNode);
                File groupsFile = new File(export.getOutputDir(), "groups.xml");
                os.writeJdomDocumentToFile(groupsDoc, groupsFile);

                // 2 - copy pages => repository.xml
                File repositoryFile = siteService.createAndPopulateRepositoryFile(tempOutputDir, site,
                        export.getOutputFile(), filesFile, groupsFile);

                filesToZip.add(repositoryFile);

                String zipFileName = siteKey + ".zip";
                File siteArchive = os.createSiteArchive(zipFileName, export.getOutputDir(), filesToZip);

                filesToZip.clear();
                
             // Global site archive
                globalFilesToZip.add(siteArchive);
            }

            // system site
            Document systemSiteRepository = siteService.createSystemSiteRepository();
                        
            // TODO SI CATEGORIES
            Element categories = siteService.createCategories(111, 5);
            siteService.insertCategoriesIntoSiteRepository(systemSiteRepository, categories);
            
            String systemSiteRepositoryFileName = "repository.xml";
            File systemSiteRepositoryFile = new File(export.getOutputDir(), systemSiteRepositoryFileName);
            os.writeJdomDocumentToFile(systemSiteRepository, systemSiteRepositoryFile);

            // zip systemsite
            filesToZip.add(systemSiteRepositoryFile);
            File systemSiteArchive = os.createSiteArchive("systemsite.zip", export.getOutputDir(), filesToZip);          
            globalFilesToZip.add(systemSiteArchive);
            logger.info("System site archive created");

            File globalArchive = os.createSiteArchive("import.zip", export.getOutputDir(), globalFilesToZip);
            globalArchivePath = globalArchive.getAbsolutePath();

        } catch (IOException e) {
            throw new MojoExecutionException("Exception while creating the website ZIP archive: " + e);
        }
        return globalArchivePath;
    }

    /**
     * Calculates the number of pages needed, used to know how much articles we
     * will need
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
     * Format a date for inclusion in JCR XML file If date is null, current date
     * is used Format used: http://www.day.com/specs/jcr/1.0/6.2.5.1_Date.html
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

    // @TODO a supprimer
    private Document readXmlFile(File xmlFile) {
        SAXBuilder builder = new SAXBuilder();

        Document document = null;
        try {
            document = builder.build(xmlFile);
        } catch (JDOMException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return document;
    }
}
