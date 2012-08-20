package org.jahia.utils.maven.plugin.contentgenerator.wise;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jahia.utils.maven.plugin.contentgenerator.ArticleService;
import org.jahia.utils.maven.plugin.contentgenerator.ContentGeneratorService;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ArticleBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.NoteBO;

public class NoteService {
	private static NoteService instance;

	private Log logger = new SystemStreamLog();

	private NoteService() {

	}

	public static NoteService getInstance() {
		if (instance == null) {
			instance = new NoteService();
		}
		return instance;
	}

	public List<NoteBO> generateNotes(Integer nbNotes, Integer nbUsers, List<ArticleBO> articles) {
		List<NoteBO> notes = new ArrayList<NoteBO>();

		ArticleService articleService = ArticleService.getInstance();

		String creator = "root";
		int idCreator;
		Random rand = new Random();
		if (nbUsers.compareTo(0) <= 0) {
			logger.error("Notes can note be created without users.");
		} else {
			for (int i = 1; i <= nbNotes; i++) {
				logger.info("Generating note " + i + "/" + nbNotes);

				if (nbUsers != null && (nbUsers.compareTo(0) > 0)) {
					idCreator = rand.nextInt(nbUsers - 1);
					creator = "user" + idCreator;
				}
				ArticleBO article = articleService.getArticle(articles);
				ContentGeneratorService.currentPageIndex++;
				notes.add(new NoteBO("note" + i, "Note " + i, creator, article.getContent()));
			}
		}

		return notes;
	}
}
