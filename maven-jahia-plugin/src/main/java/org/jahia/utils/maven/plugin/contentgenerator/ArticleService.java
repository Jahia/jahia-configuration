/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2023 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.contentgenerator;

import java.util.List;
import java.util.Random;

import org.jahia.utils.maven.plugin.contentgenerator.bo.ArticleBO;

public class ArticleService {
	private static ArticleService instance;
	
	private ArticleService() {
		
	}
	
	public static ArticleService getInstance() {
		if (instance == null) {
			instance = new ArticleService();
		}
		return instance;
	}
	
	/**
	 * Chooses an article in the list of available articles. As long as not used
	 * yet articles remain, chooses the next one. When all the articles have
	 * been used, randomly picks another one.
	 * 
	 * @param articles
	 *            All articles BO available
	 * @param maxArticleIndex
	 * @return selected article
	 */
	public ArticleBO getArticle(List<ArticleBO> articles) {
		Random generator = new Random();
		int maxIndex = articles.size() - 1;
		int index;
		if (ContentGeneratorService.currentPageIndex <= maxIndex) {
			index = ContentGeneratorService.currentPageIndex;
		} else {
			index = generator.nextInt(maxIndex);
		}
		return articles.get(index);
	}
}
