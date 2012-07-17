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
