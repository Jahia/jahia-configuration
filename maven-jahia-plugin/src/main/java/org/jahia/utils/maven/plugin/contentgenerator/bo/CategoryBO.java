package org.jahia.utils.maven.plugin.contentgenerator.bo;

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

/**
 * Object that represent a category and its XML code
 * @author Guillaume Lucazeau
 *
 */
public class CategoryBO {
	private String categoryNamePrefix = "category";

	private Integer idCategory; 
	
	private String categoryName;

	private Element categoryElement;

	public CategoryBO(Integer idCategory) {
		this.categoryName = categoryNamePrefix + idCategory;
	}

	public String getTagName() {
		return this.categoryName;
	}

	/**
	 * Returns XML Element representing the category object
	 * 
	 * @return XML code of the category
	 */
	public Element getCategoryElement() {
		if (this.categoryElement == null) {
			this.categoryElement = new Element(this.categoryName);

			this.categoryElement.setAttribute("published", "true", ContentGeneratorCst.NS_J);
			this.categoryElement.setAttribute("primaryType", "jnt:category",
					ContentGeneratorCst.NS_JCR);

			// TODO: manage languages
			Element translation_fr = new Element("translation_fr",
					ContentGeneratorCst.NS_J);
			translation_fr.setAttribute("published", "true",
					ContentGeneratorCst.NS_J);
			translation_fr.setAttribute("language", "fr",
					ContentGeneratorCst.NS_JCR);
			translation_fr.setAttribute("mixinTypes", "mix:title",
					ContentGeneratorCst.NS_JCR);
			translation_fr.setAttribute("primaryType", "jnt:translation",
					ContentGeneratorCst.NS_JCR);
			translation_fr.setAttribute("title", "Categorie " + idCategory,
					ContentGeneratorCst.NS_JCR);
			this.categoryElement.addContent(translation_fr);

			Element translation_en = new Element("translation_en",
					ContentGeneratorCst.NS_J);
			translation_en.setAttribute("published", "true",
					ContentGeneratorCst.NS_J);
			translation_en.setAttribute("language", "en",
					ContentGeneratorCst.NS_JCR);
			translation_en.setAttribute("mixinTypes", "mix:title",
					ContentGeneratorCst.NS_JCR);
			translation_en.setAttribute("primaryType", "jnt:translation",
					ContentGeneratorCst.NS_JCR);
			translation_en.setAttribute("title", "Category " + idCategory,
					ContentGeneratorCst.NS_JCR);
			
			this.categoryElement.addContent(translation_en);
		}

		return this.categoryElement;
	}
}
