package org.jahia.utils.maven.plugin.contentgenerator.wise;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.NoteBO;

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
	
	public List<NoteBO> generateNotes(Integer nbNotes, Integer nbUsers) {
		List<NoteBO> notes = new ArrayList<NoteBO>();
		
		String creator = "root";
		int idCreator;
		Random rand = new Random();
		for (int i = 0; i < nbNotes; i++) {
			if (nbUsers != null && (nbUsers.compareTo(0) > 0)) {			
				idCreator = rand.nextInt(nbUsers - 1);
				creator = "user" + idCreator;
			}
			notes.add(new NoteBO("note" + i, "Note " + i, creator));
		}
		
		return notes;
	}
}
