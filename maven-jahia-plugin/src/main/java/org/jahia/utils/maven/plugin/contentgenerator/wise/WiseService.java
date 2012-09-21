package org.jahia.utils.maven.plugin.contentgenerator.wise;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jahia.utils.maven.plugin.contentgenerator.CategoryService;
import org.jahia.utils.maven.plugin.contentgenerator.OutputService;
import org.jahia.utils.maven.plugin.contentgenerator.SiteService;
import org.jahia.utils.maven.plugin.contentgenerator.UserGroupService;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.TagBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.UserBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.CollectionBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.DocspaceBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.FileBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.FolderBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.WiseBO;
import org.jdom.Document;
import org.jdom.Element;

public class WiseService {

	private Log logger = new SystemStreamLog();

	public static WiseService instance;

	private DocspaceService docspaceService;

	private WiseService() {
		docspaceService = DocspaceService.getInstance();
	}

	public static WiseService getInstance() {
		if (instance == null) {
			instance = new WiseService();
		}
		return instance;
	}

	public WiseBO generateWiseInstance(ExportBO wiseExport, List<UserBO> users) {
		WiseBO wise = null;
		if (wiseExport.getNbDocspaces() > 0) {
			List<DocspaceBO> docspaces = docspaceService.generateDocspaces(wiseExport);
			wise = new WiseBO(wiseExport.getWiseInstanceKey(), docspaces, users);
		}
		return wise;
	}

	public String generateWise(ExportBO wiseExport) throws IOException {
		OutputService os = new OutputService();
		UserGroupService userGroupService = new UserGroupService();
		SiteService siteService = new SiteService();

		List<File> globalFilesToZip = new ArrayList<File>();

		// Wise instance files
		// Light users for the privileges groups 
		List<UserBO> users = userGroupService.generateUsers(wiseExport.getNumberOfUsers(), 0, 0, 0);
		WiseBO wiseInstance = generateWiseInstance(wiseExport, users);

		File wiseInstanceOutputDir = new File(wiseExport.getOutputDir() + "/wise");
		File wiseInstanceContentDir = new File(wiseInstanceOutputDir + "/content");

		wiseInstanceOutputDir.mkdir();
		logger.info("Creating repository file");

		// System site and categories
		Document systemSiteRepository = siteService.createSystemSiteRepository();
		insertWiseInstanceIntoSiteRepository(systemSiteRepository, wiseInstance.getElement());

		CategoryService cs = new CategoryService();
		Element categories = cs.createCategories(wiseExport.getNumberOfCategories(), wiseExport.getNumberOfCategoryLevels(), wiseExport);
		cs.insertCategoriesIntoSiteRepository(systemSiteRepository, categories);
		
		File repositoryFile = new File(wiseInstanceOutputDir + "/repository.xml");
		os.writeJdomDocumentToFile(systemSiteRepository, repositoryFile);

		// create properties file
		logger.info("Creating site properties file");
		siteService.createPropertiesFile(wiseExport.getWiseInstanceKey(), wiseExport.getSiteLanguages(), "templates-wise", wiseInstanceOutputDir);

		// Zip wise instances files
		List<File> wiseInstanceFiles = new ArrayList<File>(FileUtils.listFiles(wiseInstanceOutputDir, null, false));
		if (wiseInstanceContentDir.exists()) {
			wiseInstanceFiles.add(wiseInstanceContentDir);
		}

		logger.info("Creating Wise site archive");
		File wiseArchive = os.createSiteArchive("wise.zip", wiseExport.getOutputDir(), wiseInstanceFiles);
		globalFilesToZip.add(wiseArchive);

		// Users
		Integer nbCollectionsPerUser = wiseExport.getNbCollectionsPerUser();
		Integer nbFilesPerCollection = wiseExport.getNbFilesPerCollection();
		// If there is not enough files we use all the files and that's it.
		if (nbFilesPerCollection.compareTo(wiseExport.getNbFilesPerFolder()) > 0) {
			nbFilesPerCollection = wiseExport.getNbFilesPerFolder();
		}
		logger.info("Creating users");
		users = userGroupService.generateUsers(wiseExport.getNumberOfUsers(), nbCollectionsPerUser, nbFilesPerCollection,
				wiseExport.getNbFilesPerFolder());
		File tmpUsers = new File(wiseExport.getOutputDir(), "users");
		tmpUsers.mkdir();

		File repositoryUsers = new File(tmpUsers, "repository.xml");
		Document usersRepositoryDocument = userGroupService.createUsersRepository(users);
		os.writeJdomDocumentToFile(usersRepositoryDocument, repositoryUsers);

		List<File> filesToZip = new ArrayList<File>();
		File contentUsers = userGroupService.createFileTreeForUsers(users, tmpUsers);

		filesToZip.add(repositoryUsers);
		filesToZip.add(contentUsers);
		File usersArchive = os.createSiteArchive("users.zip", wiseExport.getOutputDir(), filesToZip);
		globalFilesToZip.add(usersArchive);

		// Zip all of this
		logger.info("Creating Wise instance archive");
		File wiseImportArchive = os.createSiteArchive("wise_instance_generated.zip", wiseExport.getOutputDir(), globalFilesToZip);

		// Generate users list
		logger.info("Generating users, files and tags lists");
		List<String> userNames = new ArrayList<String>();
		List<String> collections = new ArrayList<String>();
		for (UserBO user : users) {
			userNames.add(user.getName());

			for (CollectionBO collection : user.getCollections()) {
				collections.add(collection.getTitle());
			}
		}
		File usersFile = new File(wiseExport.getOutputDir(), "users.txt");
		usersFile.delete();
		os.appendPathToFile(usersFile, userNames);

		// File collectionsFile = new File(wiseExport.getOutputDir(),
		// "collections.txt");
		// collectionsFile.delete();
		// os.appendPathToFile(collectionsFile, collections);

		// Generate files list
		List<String> filePaths = new ArrayList<String>();
		for (DocspaceBO docspace : wiseInstance.getDocspaces()) {
			for (FolderBO folder : docspace.getFolders()) {
				for (FileBO file : folder.getFiles()) {
					String nodePath = StringUtils.chomp(file.getNodePath(), "/");
					filePaths.add(nodePath);
				}
			}
		}

		File filePathsFile = new File(wiseExport.getOutputDir(), "files.txt");
		filePathsFile.delete();
		os.appendPathToFile(filePathsFile, filePaths);

		// Generate files list
		List<String> tags = new ArrayList<String>();
		for (TagBO tag : wiseExport.getTags()) {
			tags.add(tag.getTagName());
		}

		File tagsFile = new File(wiseExport.getOutputDir(), "tags.txt");
		tagsFile.delete();
		os.appendPathToFile(tagsFile, tags);

		return wiseImportArchive.getAbsolutePath();
	}

	public Document insertWiseInstanceIntoSiteRepository(Document repository, Element wiseInstance) {
		logger.info("Add Wise instance to the system site repository");
		Element sites = repository.getRootElement().getChild("sites");
		sites.addContent(wiseInstance);
		return repository;
	}
}
