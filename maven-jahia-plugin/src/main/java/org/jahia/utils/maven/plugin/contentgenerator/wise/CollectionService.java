package org.jahia.utils.maven.plugin.contentgenerator.wise;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.CollectionBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.FileBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.FileReferenceBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.PollBO;

public class CollectionService {
	
	private static CollectionService instance;
	
	private Random rand = new Random();
	
	private CollectionService() {

	}

	public static CollectionService getInstance() {
		if (instance == null) {
			instance = new CollectionService();
		}
		return instance;
	}
	
	public List<CollectionBO> generateCollections(int nbCollections, int nbFilesPerCollection, List<FileBO> files) {
		List<CollectionBO> collections = new ArrayList<CollectionBO>();

		for (int i = 0; i < nbCollections; i++) {
			CollectionBO collection = new CollectionBO("Collection" + i, getRandomFilesReferences(nbFilesPerCollection, files));
			collections.add(collection);
		}
		return collections;
	}
	
	public List<FileReferenceBO> getRandomFilesReferences(int nbFilesPerCollection, List<FileBO> files) {
		 
		List<FileReferenceBO> fileReferences = new  ArrayList<FileReferenceBO>();
		
		int nbFiles = files.size();
		
		 for (int i = 0; i < nbFilesPerCollection; i++) {
			FileBO file = files.get(nbFiles - 1);
			FileReferenceBO fileReference = new FileReferenceBO(file);
			fileReferences.add(fileReference);
		}
		return fileReferences;
	}
}
