package org.jahia.utils.maven.plugin.contentgenerator.wise;

import java.util.ArrayList;
import java.util.List;

import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.NoteBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.TaskBO;

public class TaskService {
	private static TaskService instance;
	
	private TaskService() {

	}

	public static TaskService getInstance() {
		if (instance == null) {
			instance = new TaskService();
		}
		return instance;
	}
	
	public List<TaskBO> generateTasks(int nbTasks) {
		List<TaskBO> tasks = new ArrayList<TaskBO>();

		for (int i = 0; i < nbTasks; i++) {
			tasks.add(new TaskBO("Task " + i, "root", "New task " + i));
		}
		return tasks;
	}
}
