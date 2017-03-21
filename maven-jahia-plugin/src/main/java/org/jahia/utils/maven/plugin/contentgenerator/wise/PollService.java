/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
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
