package org.jahia.utils.maven.plugin.contentgenerator.bo;

import java.util.Iterator;
import java.util.List;

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
	
	private List<String> siteLanguages;

	public CategoryBO(Integer idCategory, List<String> siteLanguages) {
		this.idCategory = idCategory;
		this.categoryName = categoryNamePrefix + idCategory;
		this.siteLanguages = siteLanguages;
	}

	public String getCategoryName() {
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

			for (Iterator<String> iterator = siteLanguages.iterator(); iterator.hasNext();) {
				String language = (String) iterator.next();
				
				Element translation = new Element("translation_" + language,
						ContentGeneratorCst.NS_J);
				translation.setAttribute("published", "true",
						ContentGeneratorCst.NS_J);
				translation.setAttribute("language", language,
						ContentGeneratorCst.NS_JCR);
				translation.setAttribute("mixinTypes", "mix:title",
						ContentGeneratorCst.NS_JCR);
				translation.setAttribute("primaryType", "jnt:translation",
						ContentGeneratorCst.NS_JCR);
				translation.setAttribute("title", "Category " + this.idCategory + " (" + language + ")",
						ContentGeneratorCst.NS_JCR);
				
				this.categoryElement.addContent(translation);
			}
		}

		return this.categoryElement;
	}
}
