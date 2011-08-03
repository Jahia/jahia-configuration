package org.jahia.utils.maven.plugin.contentgenerator.bo;

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

/**
 * Object that represent a tag and its XML code
 * @author Guillaume Lucazeau
 *
 */
public class TagBO {
	private String tagNamePrefix = "tag";
	
	private String tagName;
	
	private Element tagElement;

	public TagBO(Integer idTag) {
		this.tagName = tagNamePrefix + idTag;
	}
	
	public String getTagName() {
		return this.tagName;
	}
	
	/**
	 * Returns XML Element representing the tag object
	 * @return XML code of the tag
	 */
	public Element getTagElement() {
		if (this.tagElement == null) {
			this.tagElement = new Element(this.tagName);
			this.tagElement.setAttribute("published", "true", ContentGeneratorCst.NS_J);
			this.tagElement.setAttribute("primaryType", "jnt:tag", ContentGeneratorCst.NS_JCR);
		}
		
		return this.tagElement;
	}
}
