package org.jahia.utils.maven.plugin.contentgenerator.wise.bo;

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

public class FileBO {

	private Element fileElement;
	
	private String fileName;
	
	private String documentStatus = "Draft";

	public FileBO(String fileName) {
		this.fileName = fileName;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public Element getElement() {
		if (fileElement == null) {
			fileElement = new Element(this.fileName);
			fileElement.setAttribute("mixinTypes", "docmix:docspaceDocument jmix:accessControlled jmix:document", ContentGeneratorCst.NS_JCR);
			fileElement.setAttribute("primaryType", "jnt:file", ContentGeneratorCst.NS_JCR);
			fileElement.setAttribute("documentStatus", "documentStatus");
		}
		return fileElement;
	}
}
