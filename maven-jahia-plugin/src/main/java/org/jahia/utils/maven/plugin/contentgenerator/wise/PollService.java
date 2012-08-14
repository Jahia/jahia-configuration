package org.jahia.utils.maven.plugin.contentgenerator.wise;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jahia.utils.maven.plugin.contentgenerator.ArticleService;
import org.jahia.utils.maven.plugin.contentgenerator.ContentGeneratorService;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ArticleBO;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.PollBO;

public class PollService {
	private static PollService instance;
	
	private Log logger = new SystemStreamLog();
	
	private PollService() {

	}

	public static PollService getInstance() {
		if (instance == null) {
			instance = new PollService();
		}
		return instance;
	}
	
	public List<PollBO> generatePolls(int nbPolls, List<ArticleBO> articles) {
		List<PollBO> polls = new ArrayList<PollBO>();

		ArticleService articleService = ArticleService.getInstance();
		
		for (int i = 1; i <= nbPolls; i++) {
			logger.info("Generating poll " + i + "/" + nbPolls);
			
			List<String> answers = new ArrayList<String>();
			answers.add("Answer" + i + "-1");
			answers.add("Answer" + i + "-2");
			answers.add("Answer" + i + "-3");
			
			ArticleBO article = articleService.getArticle(articles);
			ContentGeneratorService.currentPageIndex++;
			
			polls.add(new PollBO(article.getTitle() + "?",  "question" + i, answers));
		}
		return polls;
	}
}
