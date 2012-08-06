package org.jahia.utils.maven.plugin.contentgenerator.wise.bo;

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

public class FileReferenceBO extends FileBO {
	
	public FileReferenceBO(FileBO file) {
		super(file.getFileName(), file.getNodePath(), file.getCreator(), file.getOwner(), file.getEditor(), file.getReader());
	}

	public Element getElement() {
		if (fileElement == null) {
			super.getElement();
		}
		fileElement.setAttribute("primaryType", "jnt:fileReference", ContentGeneratorCst.NS_JCR);
		fileElement.setAttribute("node", super.nodePath, ContentGeneratorCst.NS_J);
		return fileElement;
	}
}
