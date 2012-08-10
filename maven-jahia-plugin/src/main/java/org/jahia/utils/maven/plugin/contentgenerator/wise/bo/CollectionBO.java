package org.jahia.utils.maven.plugin.contentgenerator.wise.bo;

import java.util.Iterator;
import java.util.List;

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

public class CollectionBO {

	private Element collection;
	
	private String title;
	
	private List<FileReferenceBO> fileReferences;
	
	public CollectionBO(String title, List<FileReferenceBO> fileReferences) {
		this.title = title;
		this.fileReferences = fileReferences;
	}
	
	public Element getElement() {
		if (collection == null) {
			collection = new Element(title);
			collection.setAttribute("primaryType", "docnt:collection", ContentGeneratorCst.NS_DOCNT);
			collection.setAttribute("title", title, ContentGeneratorCst.NS_JCR);
			collection.setAttribute("originWS", "default", ContentGeneratorCst.NS_J);
			
			for (Iterator<FileReferenceBO> iterator = fileReferences.iterator(); iterator.hasNext();) {
				FileReferenceBO fileReference = iterator.next();
				collection.setContent(fileReference.getElement());
			}
		}
		return collection;
	}
}
