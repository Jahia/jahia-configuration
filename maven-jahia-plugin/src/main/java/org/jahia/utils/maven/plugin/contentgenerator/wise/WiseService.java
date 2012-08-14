package org.jahia.utils.maven.plugin.contentgenerator.wise;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jahia.utils.maven.plugin.contentgenerator.OutputService;
import org.jahia.utils.maven.plugin.contentgenerator.SiteService;
import org.jahia.utils.maven.plugin.contentgenerator.UserGroupService;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.UserBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.CollectionBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.DocspaceBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.FileBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.FolderBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.WiseBO;
import org.jdom.Document;

public class WiseService {
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

	public WiseBO generateWiseInstance(ExportBO wiseExport) {
		WiseBO wise = null;
		if (wiseExport.getNbDocspaces() > 0) {
			List<DocspaceBO> docspaces = docspaceService.generateDocspaces(wiseExport);
			wise = new WiseBO(wiseExport.getWiseInstanceKey(), docspaces);
		}
		return wise;
	}

	public String generateWise(ExportBO wiseExport) throws IOException {
		OutputService os = new OutputService();
		UserGroupService userGroupService = new UserGroupService();
		SiteService siteService = new SiteService();

		List<File> globalFilesToZip = new ArrayList<File>();

		// Clean output directory
		File outputDirectory = new File(wiseExport.getOutputDir());
		FileUtils.deleteDirectory(outputDirectory);
		outputDirectory.mkdir();

		// Wise instance files
		WiseBO wiseInstance = generateWiseInstance(wiseExport);

		File wiseInstanceOutputDir = new File(wiseExport.getOutputDir() + "/wise");
		File wiseInstanceContentDir = new File(wiseInstanceOutputDir + "/content");

		wiseInstanceOutputDir.mkdir();
		File repositoryFile = new File(wiseInstanceOutputDir + "/repository.xml");
		os.writeJdomDocumentToFile(wiseInstance.getDocument(), repositoryFile);

		// create properties file
		siteService.createPropertiesFile(wiseExport.getWiseInstanceKey(), wiseExport.getSiteLanguages(), "templates-docspace", wiseInstanceOutputDir);

		// Zip wise instances files
		List<File> wiseInstanceFiles = new ArrayList<File>(FileUtils.listFiles(wiseInstanceOutputDir, null, false));
		if (wiseInstanceContentDir.exists()) {
			wiseInstanceFiles.add(wiseInstanceContentDir);
		}

		File wiseArchive = os.createSiteArchive("wise.zip", wiseExport.getOutputDir(), wiseInstanceFiles);
		globalFilesToZip.add(wiseArchive);

		// Users
		Integer nbCollectionsPerUser = wiseExport.getNbCollectionsPerUser();
		Integer nbFilesPerCollection = wiseExport.getNbFilesPerCollection();
		// If there is not enough files we use all the files and that's it.
		if (nbFilesPerCollection.compareTo(wiseExport.getFiles().size()) > 0) {
			nbFilesPerCollection = wiseExport.getFiles().size();
		}
		List<UserBO> users = userGroupService.generateUsers(wiseExport.getNumberOfUsers(), nbCollectionsPerUser, nbFilesPerCollection,
				wiseExport.getFiles());
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

		// Generate users list
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
		
		// File collectionsFile = new File(wiseExport.getOutputDir(), "collections.txt");
		// collectionsFile.delete();
		// os.appendPathToFile(collectionsFile, collections);
		
		// Generate files list
		List<String> filePaths = new ArrayList<String>();
		for (DocspaceBO docspace : wiseInstance.getDocspaces()) {
			for (FolderBO folder : docspace.getFolders()) {
				for (FileBO file : folder.getFiles()) {
					filePaths.add(file.getNodePath());
				}
			}
		}
		
		File filePathsFile = new File(wiseExport.getOutputDir(), "filePaths.txt");
		filePathsFile.delete();
		os.appendPathToFile(filePathsFile, filePaths);

		// Zip all of this
		File wiseImportArchive = os.createSiteArchive("wise_instance_generated.zip", wiseExport.getOutputDir(), globalFilesToZip);

		return wiseImportArchive.getAbsolutePath();
	}
}
