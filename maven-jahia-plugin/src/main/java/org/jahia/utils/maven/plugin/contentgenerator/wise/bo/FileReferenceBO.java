package org.jahia.utils.maven.plugin.contentgenerator.wise.bo;

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

public class FileReferenceBO extends FileBO {
	private Element fileReferenceElement;
	
	public FileReferenceBO(FileBO file) {
		super(file.getFileName(), file.getNodePath(), file.getCreator(), file.getOwner(), file.getEditor(), file.getReader());
	}

	public Element getElement() {
		if (fileReferenceElement == null) {
			fileName =  org.apache.jackrabbit.util.ISO9075.encode(fileName);
			fileReferenceElement = new Element(fileName);
			
			fileReferenceElement.setAttribute("primaryType", "jnt:fileReference", ContentGeneratorCst.NS_JCR);
			fileReferenceElement.setAttribute("node", super.nodePath, ContentGeneratorCst.NS_J);
			fileReferenceElement.setAttribute("originWS", "default", ContentGeneratorCst.NS_J);
		}
		return fileReferenceElement;
	}
}
