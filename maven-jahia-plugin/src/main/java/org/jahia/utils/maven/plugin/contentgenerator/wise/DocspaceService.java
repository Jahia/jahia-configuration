package org.jahia.utils.maven.plugin.contentgenerator.wise;

import java.util.ArrayList;
import java.util.List;

import org.jahia.utils.maven.plugin.contentgenerator.DatabaseService;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ArticleBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.DocspaceBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.FolderBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.NoteBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.PollBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.TaskBO;

public class DocspaceService {
private static DocspaceService instance;
	
	private DocspaceService() {

	}

	public static DocspaceService getInstance() {
		if (instance == null) {
			instance = new DocspaceService();
		}
		return instance;
	}
	
	public List<DocspaceBO> generateDocspaces(ExportBO wiseExport) {
		PollService pollService = PollService.getInstance();
		List<PollBO> polls = pollService.generatePolls(wiseExport.getNbPolls());
		
		List<ArticleBO> articles = DatabaseService.getInstance()
				.selectArticles(wiseExport, wiseExport.getNbNotes());
		
		NoteService noteService = NoteService.getInstance();
		List<NoteBO> notes = noteService.generateNotes(wiseExport.getNbNotes(), wiseExport.getNumberOfUsers(), articles);
		
		TaskService taskService = TaskService.getInstance();
		List<TaskBO> tasks = taskService.generateTasks(wiseExport.getNbTasks(), wiseExport.getNumberOfUsers());
		
		FileAndFolderService fileAndFolderService = FileAndFolderService.getInstance();
		
		List<DocspaceBO> docspaces = new ArrayList<DocspaceBO>();
		for (int i = 1; i <= wiseExport.getNbDocspaces(); i++) {
			String docspaceName = "docspace" + i;
						
			List<FolderBO> folders = fileAndFolderService.generateFolders(docspaceName, wiseExport);
					
			docspaces.add(new DocspaceBO(docspaceName, polls, notes, tasks, folders));
		}
		return docspaces;
	}
}
