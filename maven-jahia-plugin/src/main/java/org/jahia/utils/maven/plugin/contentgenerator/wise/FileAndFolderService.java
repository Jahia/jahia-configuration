/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.contentgenerator.wise;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.TagBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.FileBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.FolderBO;

public class FileAndFolderService {

	private Log logger = new SystemStreamLog();
	
	Random rand = new Random();

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

	private static int totalGeneratedFiles = 0;
	
	private static long timestampDifference;
	
	private static long startTimestamp;

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

	public void generateFolders(String docspaceName, ExportBO wiseExport) {
		docspaceName = StringUtils.lowerCase(docspaceName);

		// (N^L-1) / (N-1) * N
		double nbNodes = wiseExport.getNbFoldersPerLevel().doubleValue();
		double depth = wiseExport.getFoldersDepth().doubleValue();

		Double totalFolders = Math.pow(nbNodes, depth) - 1;
		totalFolders = totalFolders / (nbNodes - 1);
		totalFolders = totalFolders * nbNodes;

		Double totalFiles = totalFolders * wiseExport.getNbFilesPerFolder();

		logger.info("Folders generation is starting, " + totalFolders.intValue() + " folders to create, containing a total of "
				+ totalFiles.intValue() + " files.");

		String currentPath = initializeContentFolder(wiseExport.getOutputDir() + sep + "wise", wiseExport.getSiteKey(), docspaceName);
		String currentNodePath = "/" + "sites" + "/" + wiseExport.getSiteKey() + "/" + "files" + "/" + "docspaces" + "/" + docspaceName;

		// if there is not enough physical files available
		// we'll take them all and stop
		Integer nbFilesAvailable = wiseExport.getFileNames().size();
		if (wiseExport.getNbFilesPerFolder().compareTo(nbFilesAvailable) > 0) {
			logger.warn("You asked for " + wiseExport.getNbFilesPerFolder() + " files per folder, but there are only " + nbFilesAvailable
					+ " files in the pool, and we can't use them twice.");
			wiseExport.setNbFilesPerFolder(nbFilesAvailable);
		}

		// create temporary folders to serialize files and folders objects created
		File tmpTopFoldersDir = new File(ExportBO.tmp + sep + ContentGeneratorCst.TMP_DIR_TOP_FOLDERS);
		tmpTopFoldersDir.mkdir();
		File tmpFilesDir = new File(ExportBO.tmp + sep + ContentGeneratorCst.TMP_DIR_WISE_FILES);
		tmpFilesDir.mkdir();
		
		// initialize date range difference for random creation date	
		startTimestamp = wiseExport.getStartCreationDateRange().getTime();
		long endTimestamp = wiseExport.getEndCreationDateRange().getTime();
		timestampDifference = endTimestamp - startTimestamp;
		
		generateFolders(1, currentPath, currentNodePath, wiseExport);	
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
			}

			List<FolderBO> subFolders = null;
			Set<FileBO> files = generateFiles(filesPerFolder, currentNodePath + "/" + depthName + i, fileNames, wiseExport.getNumberOfUsers(),
					filesDirectory, wiseExport.getTags(), wiseExport.getSiteKey());
			
			// we serialize all generated files to use them in the collections
			FileOutputStream tmpFile;
			ObjectOutputStream oos;
			File tmpWiseFilesDir = new File(ExportBO.tmp + sep + ContentGeneratorCst.TMP_DIR_WISE_FILES);
			for (FileBO file : files) {
				try {
					tmpFile = new FileOutputStream(tmpWiseFilesDir + sep + totalGeneratedFiles + ".ser");
					oos = new ObjectOutputStream(tmpFile);
					oos.writeObject(file);
					oos.flush();
					oos.close();
					totalGeneratedFiles = totalGeneratedFiles + 1;
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			

			if (currentDepth < foldersDepth) {
				subFolders = generateFolders(currentDepth + 1, currentPath + sep + depthName + i, currentNodePath + "/" + depthName + i, wiseExport);
			}
			FolderBO folder = new FolderBO(depthName + i, subFolders, files);
			folders.add(folder);

			if (currentDepth == 1) {
				File tmpTopFoldersDir = new File(ExportBO.tmp + sep + ContentGeneratorCst.TMP_DIR_TOP_FOLDERS);
				try {
					tmpFile = new FileOutputStream(tmpTopFoldersDir + sep + i + ".ser");
					oos = new ObjectOutputStream(tmpFile);
					oos.writeObject(folder);
					oos.flush();
					oos.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				folder = null;
			}

			// create physical folder
			File newFolder = new File(currentPath + sep + depthName + i);
			newFolder.mkdirs();

			// copy files into the new folder
			for (Iterator<FileBO> iterator = files.iterator(); iterator.hasNext();) {
				FileBO fileBO = (FileBO) iterator.next();
				File sourceFile = new File(filesDirectory + sep + fileBO.getFileName());
				File targetDirectory = new File(newFolder + sep + fileBO.getFileName());
				// each file is contained in its own directory
				targetDirectory.mkdir();
				File targetFile = new File(targetDirectory + sep + fileBO.getFileName());
				try {
					FileUtils.copyFile(sourceFile, targetFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		Collections.sort(folders);
		return folders;
	}

	public Set<FileBO> generateFiles(Integer nbFilesToGenerate, String currentNodePath, List<String> fileNames, Integer nbUsers, File filesDirectory,
			List<TagBO> tags, String wiseInstanceName) {
		// logger.debug("Generating " + nbFiles + " files");
		SortedSet<FileBO> files = new TreeSet<FileBO>();

		List<String> fileNamesAvailable= new ArrayList<String>(fileNames);
		
		Integer nbAvailableFiles = fileNames.size();
		int currentFilenameIndex = 0;

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
		int nbOfTags = tags.size();
		int randFilenameIndex;
		String extractedContent = "";
		FileBO newFile = null;
		
		while (files.size() < nbFilesToGenerate) {
			 // logger.debug("Generating file " + (files.size() + 1) + "/" + nbFilesToGenerate);

			String fileName = "";
			if (nbFilesToGenerate.compareTo(nbAvailableFiles) >= 0) {
				fileName = fileNames.get(currentFilenameIndex);
				currentFilenameIndex++;
			} else {
				int remainingNbAvailableFiles = fileNamesAvailable.size() - 1;
				randFilenameIndex = rand.nextInt(remainingNbAvailableFiles);
				fileName = fileNamesAvailable.get(randFilenameIndex);
				fileNamesAvailable.remove(randFilenameIndex);
			}

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
			String mimeType = getMimeType(f);
			
			// Extract file content
			Metadata metadata = new Metadata();
			if (mimeType != null) {
				metadata.set(Metadata.CONTENT_TYPE, mimeType);
			}

			try {
				extractedContent = new Tika().parseToString(f);
			} catch (FileNotFoundException e) {
				logger.error("File not found during text extraction " + f.getAbsoluteFile());
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TikaException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String description = getCurrentOftenDescriptionWord() + " " + getCurrentSeldomDescriptionWord();

			// Random choice of tag
			int randomTagIndex = rand.nextInt(nbOfTags - 1);
			TagBO tag = tags.get(randomTagIndex);

			// Random creation date
			String creationDate = getRandomJcrDate(timestampDifference);
			newFile = new FileBO(fileName, mixin, mimeType, currentNodePath + "/" + fileName, creator, owner, editor, reader,
					extractedContent, description, tag.getTagName(), wiseInstanceName, creationDate);
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
			// logger.debug(nbOfFilesUsedForOftenUsedDescriptionWords.toString());
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
			// logger.debug(nbOfFilesUsedForSeldomUsedDescriptionWords.toString());
			descriptionWord = seldomUsedDescriptionWords.get(currentSeldomUsedDescriptionWordIndex);
			nbOfFilesUsedForSeldomUsedDescriptionWords--;

			if (nbOfFilesUsedForSeldomUsedDescriptionWords == 0) {
				currentSeldomUsedDescriptionWordIndex++;
				nbOfFilesUsedForSeldomUsedDescriptionWords = ContentGeneratorCst.SELDOM_USED_DESCRIPTION_WORDS_COUNTER;
			}
		}
		return descriptionWord;
	}
	
	private String getRandomJcrDate(long timestampDifference) {	
		Float f = rand.nextFloat();
		f = f * timestampDifference;
		f = f + startTimestamp;
		Date d = new Date(f.longValue());
		Calendar c = GregorianCalendar.getInstance();
		c.setTime(d);
		return org.apache.jackrabbit.util.ISO8601.format(c); 
	}
	
	public String getMimeType(File f) {
		Tika tikaParser = new Tika();
		String mimeType = "";
		try {
			mimeType = tikaParser.detect(f);
		} catch (IOException e) {
			logger.error("Impossible to detect the MIME type for file " + f.getAbsoluteFile());
			e.printStackTrace();
		}
		return mimeType;
	}
}