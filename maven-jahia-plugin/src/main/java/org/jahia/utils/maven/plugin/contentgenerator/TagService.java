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
package org.jahia.utils.maven.plugin.contentgenerator;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jahia.utils.maven.plugin.contentgenerator.bo.TagBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

/**
 * Tags service
 * 
 * @author Guillaume Lucazeau
 * 
 */
public class TagService {
	private Log logger = new SystemStreamLog();

	public TagService() {

	}

	/** 
	 * Creates list of XML Element "tag" from 0 to nbTags-1
	 * @param nbTags
	 * @return list of elements created
	 */
	public List<Element> createTags(Integer nbTags) {
		logger.info("Creation of " + nbTags + " tags");
		List<Element> tags = new ArrayList<Element>();
		TagBO tag = null;
		for (int i = 0; i < nbTags; i++) {
			tag = new TagBO(i);
			tags.add(tag.getTagElement());
			logger.debug("Tag '" + tag.getTagName() + "' created");
		}

		return tags;
	}
	
	
	/** 
	 * Creates list of Tg BO from 0 to nbTags-1
	 * @param nbTags
	 * @return list of tags object created
	 */
	public List<TagBO> createTagsBO(Integer nbTags) {
		logger.info("Creation of " + nbTags + " tags");
		List<TagBO> tags = new ArrayList<TagBO>();
		TagBO tag = null;
		for (int i = 0; i < nbTags; i++) {
			tag = new TagBO(i);
			tags.add(tag);
			logger.debug("Tag '" + tag.getTagName() + "' created");
		}

		return tags;
	}

	/**
	 * Creates XML Element "tags" that will contains tags children
	 * 
	 * @return new tag list element created
	 */
	public Element createTagListElement() {
		logger.info("Creation of the tag list element");
		Element tagList = new Element("tags");
		tagList.setAttribute("published", "true", ContentGeneratorCst.NS_J);
		tagList.setAttribute("primaryType", "jnt:tagList",
				ContentGeneratorCst.NS_JCR);

		return tagList;
	}
}
