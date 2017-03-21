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
package org.jahia.utils.maven.plugin.contentgenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jahia.utils.maven.plugin.contentgenerator.wise.FileAndFolderService;

/**
 * Class to handle files used as attachments in Jahia pages
 * 
 * @author Guillaume Lucazeau
 * 
 */
public class FileService {

	private String sep;

	public FileService() {
		sep = System.getProperty("file.separator");
	}

	/**
	 * Returns a file name randomly picked in the list of file names available
	 * (from the pool directory specified in parameter)
	 * 
	 * @param availableFileNames
	 * @return file name chosen
	 */
	public String getFileName(List<String> availableFileNames) {
		String fileName = null;

		if (ContentGeneratorService.currentFileIndex == availableFileNames.size()) {
			ContentGeneratorService.currentFileIndex = 0;
		}
		fileName = availableFileNames.get(ContentGeneratorService.currentFileIndex);
		ContentGeneratorService.currentFileIndex++;
		return fileName;
	}

	/**
	 * Returns a list of the files that can be used as attachments Return only
	 * filename as String, sorted alphabetically
	 * 
	 * @param filesDirectory
	 *            directory containing the files that will be uploaded into the
	 *            Jahia repository and can be used as attachments
	 * @return list of file names
	 * TODO: get a list of file actually used as attachment and provide them as
	 *        a zip
	 */
	public List<String> getFileNamesAvailable(File filesDirectory) {
		List<String> fileNames = new ArrayList<String>();
		System.out.println(filesDirectory.getAbsolutePath());
		File[] files = filesDirectory.listFiles();
		Arrays.sort(files);

		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				fileNames.add(files[i].getName());
			}
		}
		return fileNames;
	}

	/**
	 * Returns a list of File available for attachment
	 * 
	 * @param filesDirectory
	 * @return Files available
	 */
	public List<File> getFilesAvailable(File filesDirectory) {
		List<String> filenames = getFileNamesAvailable(filesDirectory);
		List<File> fileList = new ArrayList<File>();
		File f;

		for (Iterator<String> iterator = filenames.iterator(); iterator.hasNext();) {
			String fileName = iterator.next();
			f = new File(filesDirectory, fileName);
			fileList.add(f);
		}
		return fileList;
	}

	/**
	 * 
	 * @param filesToCopy
	 * @param destDir
	 * @returns List of new files (copies)
	 * @throws IOException
	 *             if source or destination is invalid
	 * @throws IOException
	 *             if an IO error occurs during copying
	 */
	public List<File> copyFilesForAttachment(List<File> filesToCopy, File destDir) throws IOException {
		List<File> newFiles = new ArrayList<File>();
		File oldFile;
		File newFile;
		for (Iterator<File> iterator = filesToCopy.iterator(); iterator.hasNext();) {
			oldFile = iterator.next();

			// creates a new directory for each file, with the same name
			File newDirForFile = new File(destDir + sep + oldFile.getName());
			newDirForFile.mkdir();

			FileUtils.copyFileToDirectory(oldFile, newDirForFile);
			newFile = new File(destDir, oldFile.getName());
			newFiles.add(newFile);
		}
		return newFiles;
	}

	/**
	 * Creates a file and fill it with XML that we will insert into repository
	 * file, to import files into JCR
	 * 
	 * @param tempXmlFile
	 * @param fileNames
	 * @throws IOException
	 */
	public void createAndPopulateFilesXmlFile(File tempXmlFile, List<File> fileNames) throws IOException {
		GregorianCalendar gc  = (GregorianCalendar) GregorianCalendar.getInstance();
		FileAndFolderService fileAndFolderService = FileAndFolderService.getInstance();
		
		FileUtils.writeStringToFile(tempXmlFile, sep);

		StringBuffer filesXml = new StringBuffer();
		filesXml.append("\t<files jcr:primaryType=\"jnt:folder\">");
		filesXml.append("    <contributed jcr:mixinTypes=\"jmix:accessControlled\" jcr:primaryType=\"jnt:folder\">\n");
		filesXml.append("     <j:acl jcr:primaryType=\"jnt:acl\">\n");
		filesXml.append("        <GRANT_g_site-privileged j:aceType=\"GRANT\" j:principal=\"g:privileged\" j:protected=\"false\" j:roles=\"contributor\" jcr:primaryType=\"jnt:ace\" />\n");
		filesXml.append("     </j:acl>\n");
		
		for (Iterator<File> iterator = fileNames.iterator(); iterator.hasNext();) {
			File file = iterator.next();
			String fileName = file.getName();
			String mimeType = fileAndFolderService.getMimeType(file);
			if (mimeType == null) {
				mimeType = "application/text";
			}
			filesXml.append("          <"
					+ org.apache.jackrabbit.util.ISO9075.encode(fileName)
					+ " jcr:primaryType=\"jnt:file\" jcr:title=\""
                    + fileName
                    + "\">\n");
			filesXml.append("             <jcr:content jcr:mimeType=\"" + mimeType +"\" jcr:primaryType=\"jnt:resource\" />\n");
			filesXml.append("          </" + org.apache.jackrabbit.util.ISO9075.encode(fileName) + ">\n");
		}

		filesXml.append("    </contributed>\n");
		filesXml.append("</files>\n");


		FileUtils.writeStringToFile(tempXmlFile, filesXml.toString());
	}

	/**
	 * Returns afilename without the extension (removes substring following last
	 * dot)
	 * 
	 * @param fileName
	 * @return filename without extension
	 */
	private String getFileNameWithoutExtension(String fileName) {
		return fileName.substring(0, fileName.lastIndexOf('.'));
	}
}
