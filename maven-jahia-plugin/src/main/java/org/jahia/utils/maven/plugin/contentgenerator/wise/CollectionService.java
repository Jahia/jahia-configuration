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

	public List<CollectionBO> generateCollections(int nbCollections, int nbFilesPerCollection, int nbGeneratedFiles) {
		List<CollectionBO> collections = new ArrayList<CollectionBO>();

		for (int i = 1; i <= nbCollections; i++) {
			logger.info("Generating collection " + i + "/" + nbCollections + " containing " + nbFilesPerCollection + " files");
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
