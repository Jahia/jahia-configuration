package org.jahia.utils.maven.plugin.contentgenerator.bo;

import java.util.Iterator;
import java.util.List;

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

public class AclBO {

	private Element aclElement;

	private List<AceBO> aces;

	public AclBO(List<AceBO> aces) {
		this.aces = aces;
	}

	public Element getElement() {
		if (aclElement == null) {
			aclElement = new Element("acl", ContentGeneratorCst.NS_J);
			aclElement.setAttribute("primaryType", "jnt:acl",
					ContentGeneratorCst.NS_JCR);
			for (Iterator<AceBO> iterator = aces.iterator(); iterator.hasNext();) {
				AceBO aceBO = iterator.next();
				aclElement.addContent(aceBO.getElement());
			}
		}
		return aclElement;
	}
}
