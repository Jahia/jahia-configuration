package org.jahia.utils.maven.plugin.contentgenerator.wise;

import java.util.ArrayList;
import java.util.List;

import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.PollBO;
import org.jdom.Element;

public class PollService {
	private static PollService instance;
	
	private PollService() {

	}

	public static PollService getInstance() {
		if (instance == null) {
			instance = new PollService();
		}
		return instance;
	}
	
	public List<PollBO> generatePolls(int nbPolls) {
		List<PollBO> polls = new ArrayList<PollBO>();

		for (int i = 0; i < nbPolls; i++) {			
			List<String> answers = new ArrayList<String>();
			answers.add("Answer" + i + "-1");
			answers.add("Answer" + i + "-2");
			answers.add("Answer" + i + "-3");
			polls.add(new PollBO("Question " + i, answers));
		}
		return polls;
	}
}
