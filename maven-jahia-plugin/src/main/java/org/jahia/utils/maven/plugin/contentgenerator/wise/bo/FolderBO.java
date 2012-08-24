package org.jahia.utils.maven.plugin.contentgenerator.wise.bo;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

public class FolderBO implements java.io.Serializable, Comparable<FolderBO> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	Element folderElement;

	String folderName;

	List<FolderBO> subFolders;

	Set<FileBO> files;

	public FolderBO(String folderName, List<FolderBO> subFolders, Set<FileBO> files) {
		this.folderName = folderName;
		this.subFolders = subFolders;
		this.files = files;
	}
	
	public Set<FileBO> getFiles() {
		return files;
	}
	
	public String getFolderName() {
		return this.folderName;
	}

	public Element getElement() {
		if (folderElement == null) {
			folderElement = new Element(folderName);
			folderElement.setAttribute("mixinTypes", "docmix:docspaceFolder", ContentGeneratorCst.NS_JCR);
			folderElement.setAttribute("primaryType", "jnt:folder", ContentGeneratorCst.NS_JCR);

			if (files != null) {
				for (Iterator<FileBO> iterator = files.iterator(); iterator.hasNext();) {
					FileBO file = iterator.next();
					folderElement.addContent(file.getElement());
				}
			}
			
			if (subFolders != null) {
				for (Iterator<FolderBO> iterator = subFolders.iterator(); iterator.hasNext();) {
					FolderBO subFolder = iterator.next();
					folderElement.addContent(subFolder.getElement());
				}
			}
			
		}
		return folderElement;
	}

	public int compareTo(FolderBO f) throws NullPointerException {
		return this.folderName.compareTo(f.getFolderName());
	}
}
