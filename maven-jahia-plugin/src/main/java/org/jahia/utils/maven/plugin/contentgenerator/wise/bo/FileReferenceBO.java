package org.jahia.utils.maven.plugin.contentgenerator.wise.bo;

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

public class FileReferenceBO extends FileBO {
	
	public FileReferenceBO(String fileName, String creator) {
		super(fileName, creator);
	}

	public Element getElement() {
		if (fileElement == null) {
			super.getElement();
		}
		fileElement.setAttribute("primaryType", "jnt:fileReference", ContentGeneratorCst.NS_JCR);
		return fileElement;
	}
}
