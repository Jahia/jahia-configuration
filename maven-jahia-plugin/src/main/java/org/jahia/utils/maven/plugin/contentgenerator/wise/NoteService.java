package org.jahia.utils.maven.plugin.contentgenerator.wise;

import java.util.ArrayList;
import java.util.List;

import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.NoteBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.PollBO;

public class NoteService {
	private static NoteService instance;
	
	private NoteService() {

	}

	public static NoteService getInstance() {
		if (instance == null) {
			instance = new NoteService();
		}
		return instance;
	}
	
	public List<NoteBO> generateNotes(int nbNotes) {
		List<NoteBO> notes = new ArrayList<NoteBO>();

		for (int i = 0; i < nbNotes; i++) {
			notes.add(new NoteBO());
		}
		return notes;
	}
}
