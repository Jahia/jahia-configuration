/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.contentgenerator.bo;

import java.util.Iterator;
import java.util.List;

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom2.Element;

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
