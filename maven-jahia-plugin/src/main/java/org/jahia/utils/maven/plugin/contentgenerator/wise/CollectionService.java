/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.CollectionBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.FileBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.FileReferenceBO;

public class CollectionService {

	private static CollectionService instance;

	private Log logger = new SystemStreamLog();

	private Random rand = new Random();

	private CollectionService() {

	}

	public static CollectionService getInstance() {
		if (instance == null) {
			instance = new CollectionService();
		}
		return instance;
	}

	public List<CollectionBO> generateCollections(int nbCollections, int nbFilesPerCollection, int nbGeneratedFiles, String currentUsername) {
		List<CollectionBO> collections = new ArrayList<CollectionBO>();

		for (int i = 1; i <= nbCollections; i++) {
			logger.info("Generating collection " + i + "/" + nbCollections + " containing " + nbFilesPerCollection + " files for user " + currentUsername);
			CollectionBO collection = new CollectionBO("Collection" + i, getRandomFilesReferences(nbFilesPerCollection, nbGeneratedFiles));
			collections.add(collection);
		}
		return collections;
	}

	public List<FileReferenceBO> getRandomFilesReferences(int nbFilesPerCollection, int nbGeneratedFiles) {
		List<FileReferenceBO> fileReferences = new ArrayList<FileReferenceBO>();

		int fileIndex;
		for (int i = 0; i < nbFilesPerCollection; i++) {
			fileIndex = rand.nextInt(nbGeneratedFiles - 1);
			FileBO file = getRandomFileFromTmpDir(fileIndex);
			FileReferenceBO fileReference = new FileReferenceBO(file);
			fileReferences.add(fileReference);
		}
		return fileReferences;
	}

	public FileBO getRandomFileFromTmpDir(int index) {
		FileBO file = null;
		String sep = System.getProperty("file.separator");
		try {
			FileInputStream fichier = new FileInputStream(ExportBO.tmp + sep + ContentGeneratorCst.TMP_DIR_WISE_FILES + sep + index + ".ser");
			ObjectInputStream ois = new ObjectInputStream(fichier);
			file = (FileBO) ois.readObject();
			ois.close();
		} catch (java.io.IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return file;
	}
}
