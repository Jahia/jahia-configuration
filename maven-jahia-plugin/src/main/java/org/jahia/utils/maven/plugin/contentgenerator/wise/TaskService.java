package org.jahia.utils.maven.plugin.contentgenerator.wise;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
	
	public List<TaskBO> generateTasks(int nbTasks, Integer nbUsers) {
		List<TaskBO> tasks = new ArrayList<TaskBO>();
		
		String assignee = "root";
		String creator = "root";
		int idAssignee;
		int idCreator;
		Random rand = new Random();
		for (int i = 0; i < nbTasks; i++) {
			if (nbUsers != null && (nbUsers.compareTo(0) > 0)) {
				idAssignee = rand.nextInt(nbUsers - 1);
				assignee = "user" + idAssignee;
				
				idCreator = rand.nextInt(nbUsers - 1);
				creator = "user" + idCreator;
			}
			tasks.add(new TaskBO("Task " + i, creator, assignee, "New task " + i));
		}
		return tasks;
	}
}
