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
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jahia.utils.maven.plugin.contentgenerator.DatabaseService;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ArticleBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.DocspaceBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.FolderBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.NoteBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.PollBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.TaskBO;

public class DocspaceService {
	private static DocspaceService instance;

	private Log logger = new SystemStreamLog();

	private DocspaceService() {

	}

	public static DocspaceService getInstance() {
		if (instance == null) {
			instance = new DocspaceService();
		}
		return instance;
	}

	public List<DocspaceBO> generateDocspaces(ExportBO wiseExport) {
		// Articles are used for Polls and Notes
		Integer numberOfArticles = null;
		if (wiseExport.getNbNotes().compareTo(wiseExport.getNbPolls()) > 0) {
			numberOfArticles = wiseExport.getNbNotes();
		} else {
			numberOfArticles = wiseExport.getNbPolls();
		}

		List<ArticleBO> articles = new ArrayList<ArticleBO>();
		if (numberOfArticles.compareTo(0) > 0) {
			articles = DatabaseService.getInstance().selectArticles(wiseExport, numberOfArticles);
		}

		PollService pollService = PollService.getInstance();
		List<PollBO> polls = pollService.generatePolls(wiseExport.getNbPolls(), articles);

		NoteService noteService = NoteService.getInstance();
		List<NoteBO> notes = noteService.generateNotes(wiseExport.getNbNotes(), wiseExport.getNumberOfUsers(), articles);

		TaskService taskService = TaskService.getInstance();
		List<TaskBO> tasks = taskService.generateTasks(wiseExport.getNbTasks(), wiseExport.getNumberOfUsers());

		FileAndFolderService fileAndFolderService = FileAndFolderService.getInstance();

		List<DocspaceBO> docspaces = new ArrayList<DocspaceBO>();
		for (int i = 1; i <= wiseExport.getNbDocspaces(); i++) {
			String docspaceName = "Docspace" + i;

			logger.info("Generating docspace " + i + "/" + wiseExport.getNbDocspaces());

			fileAndFolderService.generateFolders(docspaceName, wiseExport);

			// read folders from tmp directory

			docspaces.add(new DocspaceBO(docspaceName, polls, notes, tasks, getFoldersBO(), wiseExport.getNumberOfUsers(), wiseExport.getNumberOfOwners(), wiseExport.getNumberOfCollaborators(), wiseExport.getNumberOfEditors()));
		}
		return docspaces;
	}

	public List<FolderBO> getFoldersBO() {
		List<FolderBO> folders = new ArrayList<FolderBO>();
		FolderBO folderBO = null;
		String sep = System.getProperty("file.separator");

		File tmpDir = new File(ExportBO.tmp + sep + ContentGeneratorCst.TMP_DIR_TOP_FOLDERS);
		for (File f : tmpDir.listFiles()) {
			try {
				FileInputStream fichier = new FileInputStream(f);
				ObjectInputStream ois = new ObjectInputStream(fichier);
				folderBO = (FolderBO) ois.readObject();
				folders.add(folderBO);
				ois.close();
			} catch (java.io.IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		Collections.sort(folders);
		return folders;
	}
}
