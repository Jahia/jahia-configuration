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

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jahia.utils.maven.plugin.contentgenerator.bo.CategoryBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jdom2.Document;
import org.jdom2.Element;

public class CategoryService {

	private Log logger = new SystemStreamLog();

	private int counterCategories;

	public CategoryService() {

	}

	/**
	 * Creates a new category and call itself to add sub-categories
	 * 
	 * @param level
	 * @param siteLanguages languages requested for the new site
	 * @return
	 */
	private Element addCategory(int level, List<String> siteLanguages) {
		CategoryBO category = new CategoryBO(counterCategories, siteLanguages);
		Element categoryElement = category.getCategoryElement();

		counterCategories--;
		level--;

		if (level > 0 && counterCategories > 0) {
			categoryElement.addContent(addCategory(level, siteLanguages));
		}
		return categoryElement;
	}

	/**
	 * Creates all categories requested
	 * 
	 * @param nbCategories
	 * @param nbLevelsCategories
	 * @return
	 */
	public Element createCategories(Integer nbCategories,
			Integer nbLevelsCategories, ExportBO export) {
		logger.info("Creation of " + nbCategories + " categories on "
				+ nbLevelsCategories + " levels");
		counterCategories = nbCategories - 1; // we'll start with category0
		Element categories = new Element("categories");

		List<String> siteLanguages = export.getSiteLanguages();
		
		while (counterCategories >= 0) {
			int level = nbLevelsCategories;
			Element category = addCategory(level, siteLanguages);
			categories.addContent(category);
		}

		return categories;
	}

	public Document insertCategoriesIntoSiteRepository(Document repository,
			Element categories) {
		logger.info("Add categories to the system site repository");
		Element systemSite = repository.getRootElement().getChild("sites")
				.getChild("systemsite");
		systemSite.addContent(categories);
		return repository;
	}
}
