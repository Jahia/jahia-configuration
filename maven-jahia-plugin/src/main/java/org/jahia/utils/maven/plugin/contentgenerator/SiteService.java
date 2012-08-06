package org.jahia.utils.maven.plugin.contentgenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jahia.utils.maven.plugin.contentgenerator.bo.SiteBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Document;
import org.jdom.Element;

public class SiteService {
	private Log logger = new SystemStreamLog();
	private String sep;

	public SiteService() {
		sep = System.getProperty("file.separator");
	}

	public File createAndPopulateRepositoryFile(File tempOutputDir,
			SiteBO site, File pagesFile, File filesFile, File groupsFile, File tagsFile)
			throws IOException {
		File repositoryFile = new File(tempOutputDir, "repository.xml");

		FileOutputStream output = new FileOutputStream(repositoryFile, true);
		IOUtils.write(site.getHeader(), output);
		
		// there is an XML files for attachments only if we requested some
		if (filesFile != null) {
			IOUtils.copy(new FileInputStream(filesFile), output);
		}
		IOUtils.copy(new FileInputStream(groupsFile), output);
		IOUtils.copy(new FileInputStream(tagsFile), output);
		IOUtils.copy(new FileInputStream(pagesFile), output);
		
		IOUtils.write(site.getFooter(), output);

		return repositoryFile;
	}

	/**
	 * Creates temporary directory where we will put all resources to zip If the
	 * directory already exists, we empty it
	 * 
	 * @param siteKey
	 * @param destDir
	 * @return new directory created
	 * @throws IOException
	 *             can not delete dir
	 */
	public File createSiteDirectory(String siteKey, File destDir)
			throws IOException {
		File tempOutputDir = new File(destDir, siteKey);
		if (tempOutputDir.exists()) {
			FileUtils.deleteDirectory(tempOutputDir);
		}
		tempOutputDir.mkdir();
		logger.debug("temp directory for site export: "
				+ tempOutputDir.getAbsolutePath());
		return tempOutputDir;
	}

	/**
	 * Copies the XML file generated into a temporary dir and renames it to
	 * "repository.xml" (or other name defined in the constant)
	 * 
	 * @param pagesFile
	 * @return new File created
	 * @throws IOException
	 */
	public File copyPagesFile(File pagesFile, File tempOutputDir)
			throws IOException {
		FileUtils.copyFileToDirectory(pagesFile, tempOutputDir);
		File copy = new File(tempOutputDir, pagesFile.getName());
		File renamedCopy = new File(tempOutputDir,
				ContentGeneratorCst.REPOSITORY_FILENAME);
		copy.renameTo(renamedCopy);
		logger.debug("new file containing pages: " + renamedCopy);
		return renamedCopy;
	}

	/**
	 * Create the tree that will contains all files used as attachments in the
	 * new site
	 * 
	 * @param siteKey
	 * @param tempOutputDir
	 * @return new File created
	 * @throws IOException
	 *             one dir can not be created
	 */
	public File createFilesDirectoryTree(String siteKey, File tempOutputDir)
			throws IOException {
		String treePath = tempOutputDir + sep + "content" + sep + "sites" + sep
				+ siteKey + sep + "files" + sep + "contributed";
		File treeFile = new File(treePath);
		FileUtils.forceMkdir(treeFile);
		return treeFile;
	}

	/**
	 * Creates properties for the site created and write them to the default
	 * properties file (creates it as well)
	 * 
	 * @param siteKey
	 * @param tempOutputDir
	 * @return
	 * @throws FileNotFoundException
	 *             file
	 * @throws IOException
	 */
	public File createPropertiesFile(String siteKey, List<String> languages, String templateSet,
			File tempOutputDir) throws FileNotFoundException, IOException {
		Properties siteProp = new Properties();

		siteProp.setProperty("sitetitle", siteKey);
		siteProp.setProperty("siteservername",
				ContentGeneratorCst.SITE_SERVER_NAME_DEFAULT);
		siteProp.setProperty("sitekey", siteKey);
		siteProp.setProperty("description",
				ContentGeneratorCst.DESCRIPTION_DEFAULT);
		siteProp.setProperty("templatePackageName",
				templateSet);
		siteProp.setProperty("mixLanguage", Boolean.TRUE.toString());
		siteProp.setProperty("defaultLanguage", languages.get(0));
		siteProp.setProperty("installedModules.1", "default");
		siteProp.setProperty("installedModules.2", templateSet);
		for (String language : languages) {
			siteProp.setProperty("language." + language + ".activated",
					Boolean.TRUE.toString());
			siteProp.setProperty("language." + language + ".mandatory",
					Boolean.FALSE.toString());
		}

		String sep = System.getProperty("file.separator");
		File propFile = new File(tempOutputDir,
				ContentGeneratorCst.SITE_PROPERTIES_FILENAME);

		siteProp.store(new FileOutputStream(propFile),
				ContentGeneratorCst.DESCRIPTION_DEFAULT);

		return propFile;
	}

	public Document insertGroupsIntoSiteRepository(Document repository,
			String siteKey, Element groups) {
		Element siteNode = repository.getRootElement().getChild("sites")
				.getChild(siteKey);
		siteNode.addContent(groups);
		return repository;
	}

	/**
	 * Creates an XML document containing the basic structure of the systemSite
	 * repository
	 * 
	 * @return new repository created
	 */
	public Document createSystemSiteRepository() {
		logger.info("Initialization of system site repository");
		Document systemSiteRepository = new Document();
		Element contentNode = new Element("content");
		systemSiteRepository.setRootElement(contentNode);

		Element sitesNode = new Element("sites");
		sitesNode.setAttribute("primaryType", "jnt:virtualsitesFolder",
				ContentGeneratorCst.NS_JCR);
		contentNode.addContent(sitesNode);

		Element systemSiteNode = new Element("systemsite");
		systemSiteNode.setAttribute("primaryType", "jnt:virtualsite",
				ContentGeneratorCst.NS_JCR);
		sitesNode.addContent(systemSiteNode);

		return systemSiteRepository;
	}
}
