package org.jahia.utils.maven.plugin.contentgenerator;

import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jahia.utils.maven.plugin.contentgenerator.bo.CategoryBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jdom.Document;
import org.jdom.Element;

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
