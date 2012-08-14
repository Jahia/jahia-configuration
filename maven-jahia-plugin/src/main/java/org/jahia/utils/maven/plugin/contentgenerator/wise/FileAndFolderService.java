package org.jahia.utils.maven.plugin.contentgenerator.wise;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.FileBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.FolderBO;

public class FileAndFolderService {

	private Log logger = new SystemStreamLog();

	private static FileAndFolderService instance;

	String sep = System.getProperty("file.separator");
	
	private static List<String> oftenUsedDescriptionWords;
	
	private static List<String> seldomUsedDescriptionWords;
	
	private static Integer currentOftenUsedDescriptionWordIndex;
	
	private static Integer currentSeldomUsedDescriptionWordIndex;
	
	private static Integer nbOfFilesUsedForOftenUsedDescriptionWords;
	
	private static Integer nbOfFilesUsedForSeldomUsedDescriptionWords;
	
	private static Integer nbOfOftenUsedDescriptionWords;
	
	private static Integer nbOfSeldomUsedDescriptionWords;

	private FileAndFolderService() {
		oftenUsedDescriptionWords = Arrays.asList(ContentGeneratorCst.OFTEN_USED_DESCRIPTION_WORDS.split("\\s*,\\s*"));
		seldomUsedDescriptionWords = Arrays.asList(ContentGeneratorCst.SELDOM_USED_DESCRIPTION_WORDS.split("\\s*,\\s*"));
		currentOftenUsedDescriptionWordIndex = 0;
		currentSeldomUsedDescriptionWordIndex = 0;
		nbOfFilesUsedForOftenUsedDescriptionWords = ContentGeneratorCst.OFTEN_USED_DESCRIPTION_WORDS_COUNTER;
		nbOfFilesUsedForSeldomUsedDescriptionWords = ContentGeneratorCst.SELDOM_USED_DESCRIPTION_WORDS_COUNTER;
		nbOfOftenUsedDescriptionWords = oftenUsedDescriptionWords.size();
		nbOfSeldomUsedDescriptionWords = seldomUsedDescriptionWords.size();
	}

	public static FileAndFolderService getInstance() {
		if (instance == null) {
			instance = new FileAndFolderService();
		}
		return instance;
	}

	public List<FolderBO> generateFolders(String docspaceName, ExportBO wiseExport) {
		Double totalFolders = Math.pow(wiseExport.getNbFoldersPerLevel().doubleValue(), wiseExport.getFoldersDepth().doubleValue());
		Double totalFiles = totalFolders * wiseExport.getNbFilesPerFolder();
		logger.info("Folders generation is starting, " + totalFolders.intValue() + " folders to create, containing a total of " + totalFiles.intValue() + " files.");
		
		String currentPath = initializeContentFolder(wiseExport.getOutputDir() + sep + "wise", wiseExport.getWiseInstanceKey(), docspaceName);
		String currentNodePath = sep + "sites" + sep + wiseExport.getWiseInstanceKey() + sep + "files" + sep + "docspaces" + sep + "docspaceName";
		
		return generateFolders(1, currentPath, currentNodePath, wiseExport);
	}

	private List<FolderBO> generateFolders(Integer currentDepth, String currentPath, String currentNodePath, ExportBO wiseExport) {

		Integer nbFoldersPerLevel = wiseExport.getNbFoldersPerLevel();
		Integer foldersDepth = wiseExport.getFoldersDepth();
		Integer filesPerFolder = wiseExport.getNbFilesPerFolder();
		List<String> fileNames = wiseExport.getFileNames();
		File filesDirectory = wiseExport.getFilesDirectory();
	
		String depthName;

		switch (currentDepth) {
		case 1:
			depthName = "aaa";
			break;
		case 2:
			depthName = "bbb";
			break;
		case 3:
			depthName = "ccc";
			break;
		case 4:
			depthName = "ddd";
			break;
		case 5:
			depthName = "eee";
			break;
		case 6:
			depthName = "fff";
			break;
		case 7:
			depthName = "ggg";
			break;
		case 8:
			depthName = "hhh";
			break;
		case 9:
			depthName = "iii";
			break;
		default:
			depthName = "aaa";
			break;
		}

		List<FolderBO> folders = new ArrayList<FolderBO>();
		for (int i = 1; i <= nbFoldersPerLevel; i++) {
			if (currentDepth == 1) {
				logger.info("Generating top folder " + i + "/" + nbFoldersPerLevel);
			} else {
				//logger.debug("Generating sub folder ");
			}
			List<FolderBO> subFolders = null;
			List<FileBO> files = generateFiles(filesPerFolder, currentNodePath, fileNames, wiseExport.getNumberOfUsers(), filesDirectory);
			// we store all generated files to use them in the collections
			List<FileBO> filesTmp = wiseExport.getFiles();
			filesTmp.addAll(files);
			wiseExport.setFiles(filesTmp);

			if (currentDepth < foldersDepth) {
				subFolders = generateFolders(currentDepth + 1, currentPath + sep + depthName + i, currentNodePath + sep + depthName, wiseExport);
			}
			folders.add(new FolderBO(depthName + i, subFolders, files));

			// create physical folder
			File newFolder = new File(currentPath + sep + depthName + i);
			newFolder.mkdirs();

			// copy files into the new folder
			for (Iterator<FileBO> iterator = files.iterator(); iterator.hasNext();) {
				FileBO fileBO = (FileBO) iterator.next();
				File sourceFile = new File(filesDirectory + sep + fileBO.getFileName());
				File targetFile = new File(newFolder + sep + fileBO.getFileName());
				try {
					FileUtils.copyFile(sourceFile, targetFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return folders;
	}

	public List<FileBO> generateFiles(Integer nbFiles, String currentNodePath, List<String> fileNames, Integer nbUsers, File filesDirectory) {
		//logger.debug("Generating " + nbFiles + " files");
		List<FileBO> files = new ArrayList<FileBO>();
		Random rand = new Random();

		String imageExtensions[] = { ".png", ".gif", ".jpeg", ".jpg" };
		String officeDocumentExtensions[] = { ".doc", ".xls", ".ppt", ".docx", ".xlsx", ".pptx" };

		String creator = "root";
		String owner = "root";
		String editor = "root";
		String reader = "root";
		int idCreator;
		int idOwner;
		int idEditor;
		int idReader;

		for (int i = 0; i < nbFiles; i++) {
			String fileName = fileNames.get(rand.nextInt(fileNames.size() - 1));
			String mixin = "";

			if (nbUsers != null && (nbUsers.compareTo(0) > 0)) {
				idCreator = rand.nextInt(nbUsers - 1);
				creator = "user" + idCreator;

				idOwner = rand.nextInt(nbUsers - 1);
				owner = "user" + idOwner;

				idEditor = rand.nextInt(nbUsers - 1);
				editor = "user" + idEditor;

				idReader = rand.nextInt(nbUsers - 1);
				reader = "user" + idReader;
			}

			// Choose correct mixin depending on the file extension
			String fileExtension = getFileExtension(fileName);
			if (Arrays.asList(imageExtensions).contains(fileExtension)) {
				mixin = " jmix:image";
			} else if (Arrays.asList(officeDocumentExtensions).contains(fileExtension)) {
				mixin = " jmix:document";
			}

			// Detect MIME type
			File f = new File(filesDirectory + sep + fileName);
			Tika tikaParser = new Tika();
			String mimeType = "";
			try {
				mimeType = tikaParser.detect(f);
			} catch (IOException e) {
				logger.error("Impossible to detect the MIME type for file " + f.getAbsoluteFile());
				e.printStackTrace();
			}

			// Extract file content
			Metadata metadata = new Metadata();
			if (mimeType != null) {
				metadata.set(Metadata.CONTENT_TYPE, mimeType);
			}

			String extractedContent = "";
			
			try {
	            extractedContent = new Tika().parseToString(f);				
			} catch (FileNotFoundException e) {
				logger.error("File not found during text extraction " + f.getAbsoluteFile());
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  catch (TikaException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 
			String description = getCurrentOftenDescriptionWord() + " " + getCurrentSeldomDescriptionWord();
			FileBO newFile = new FileBO(fileName, mixin, mimeType, currentNodePath + sep + fileName, creator, owner, editor, reader, extractedContent, description);
			// logger.debug("New FileBO: " + newFile.toString());
			files.add(newFile);
		}
		return files;
	}

	public String initializeContentFolder(String outputDirPath, String wiseInstanceName, String docpaceKey) {
		File contentdirectory = new File(outputDirPath + sep + "content" + sep + "sites" + sep + wiseInstanceName + sep + "files" + sep + "docspaces"
				+ sep + docpaceKey);
		contentdirectory.mkdirs();
		return contentdirectory.getAbsolutePath();
	}

	public String getFileExtension(String fileName) {
		return fileName.substring(fileName.lastIndexOf('.'), fileName.length());
	}
	
	private String getCurrentOftenDescriptionWord() {
		String descriptionWord = "";
		if (currentOftenUsedDescriptionWordIndex < nbOfOftenUsedDescriptionWords) {
			//logger.debug(nbOfFilesUsedForOftenUsedDescriptionWords.toString());
			descriptionWord = oftenUsedDescriptionWords.get(currentOftenUsedDescriptionWordIndex);
			nbOfFilesUsedForOftenUsedDescriptionWords--;

			if (nbOfFilesUsedForOftenUsedDescriptionWords == 0) {
				currentOftenUsedDescriptionWordIndex++;
				nbOfFilesUsedForOftenUsedDescriptionWords = ContentGeneratorCst.OFTEN_USED_DESCRIPTION_WORDS_COUNTER;
			}
		}
		return descriptionWord;
	}
	
	private String getCurrentSeldomDescriptionWord() {
		String descriptionWord = "";
		if (currentSeldomUsedDescriptionWordIndex < nbOfSeldomUsedDescriptionWords) {
			//logger.debug(nbOfFilesUsedForSeldomUsedDescriptionWords.toString());
			descriptionWord = seldomUsedDescriptionWords.get(currentSeldomUsedDescriptionWordIndex);
			nbOfFilesUsedForSeldomUsedDescriptionWords--;

			if (nbOfFilesUsedForSeldomUsedDescriptionWords == 0) {
				currentSeldomUsedDescriptionWordIndex++;
				nbOfFilesUsedForSeldomUsedDescriptionWords = ContentGeneratorCst.SELDOM_USED_DESCRIPTION_WORDS_COUNTER;
			}
		}
		return descriptionWord;
	}
}
