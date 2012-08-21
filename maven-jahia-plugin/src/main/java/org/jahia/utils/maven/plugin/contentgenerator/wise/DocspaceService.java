package org.jahia.utils.maven.plugin.contentgenerator.wise;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jahia.utils.maven.plugin.contentgenerator.DatabaseService;
import org.jahia.utils.maven.plugin.contentgenerator.TagService;
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

		TagService tagService = new TagService();
		wiseExport.setTags(tagService.createTagsBO(wiseExport.getNumberOfTags()));

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

			docspaces.add(new DocspaceBO(docspaceName, polls, notes, tasks, getFoldersBO(), wiseExport.getNumberOfUsers()));
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
		return folders;
	}
}
