package org.jahia.utils.maven.plugin.contentgenerator.wise.bo;

import java.util.ArrayList;
import java.util.List;

import org.jahia.utils.maven.plugin.contentgenerator.bo.AceBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.AclBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

public class FileBO implements java.io.Serializable, Comparable<FileBO> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected Element fileElement;

	protected String mixinFileType;

	protected String mimeType;

	protected String nodePath;

	protected String fileName;

	protected String jcrFilename;

	protected String creator;

	protected String owner;

	protected String reader;

	protected String editor;

	protected String documentStatus = "Draft";

	protected String extractedContent;

	protected String description;

	protected String tag;

	protected String wiseInstanceName;
	
	protected String creationDate;

	public FileBO(String fileName, String mixinFileType, String mimeType, String nodePath, String creator, String owner, String editor,
			String reader, String extractedContent, String description, String tag, String wiseInstanceName, String creationDate) {
		;
		this.fileName = fileName;
		this.jcrFilename = org.apache.jackrabbit.util.ISO9075.encode(fileName);
		this.mixinFileType = mixinFileType;
		this.mimeType = mimeType;
		this.nodePath = nodePath;
		this.creator = creator;
		this.owner = owner;
		this.editor = editor;
		this.reader = reader;
		this.extractedContent = extractedContent;
		this.description = description;
		this.tag = tag;
		this.wiseInstanceName = wiseInstanceName;
		this.creationDate = creationDate;
	}

	public FileBO(String fileName, String nodePath) {
		;
		this.fileName = fileName;
		this.nodePath = nodePath;
	}

	public long getSerialVersionUID() {
		return serialVersionUID;
	}

	public String getFileName() {
		return fileName;
	}

	public String getJcrFileName() {
		return jcrFilename;
	}

	public String getMixinFileType() {
		return mixinFileType;
	}

	public String getMimeType() {
		return mimeType;
	}

	public String getNodePath() {
		return nodePath;
	}

	public String getCreator() {
		return creator;
	}

	public String getOwner() {
		return owner;
	}

	public String getReader() {
		return reader;
	}

	public String getEditor() {
		return editor;
	}

	public String getExtractedContent() {
		return extractedContent;
	}

	public Element getElement() {
		if (fileElement == null) {
			fileElement = new Element(jcrFilename);

			String mixin = "docmix:docspaceDocument jmix:accessControlled " + mixinFileType;
			if (tag != null) {
				// fileElement.setAttribute("newTag", tag,
				// ContentGeneratorCst.NS_J);
				fileElement.setAttribute("tags", "/sites/" + wiseInstanceName + "/tags/" + tag, ContentGeneratorCst.NS_J);
				mixin = mixin + " jmix:tagged";
			}

			fileElement.setAttribute("mixinTypes", mixin, ContentGeneratorCst.NS_JCR);
			fileElement.setAttribute("primaryType", "jnt:file", ContentGeneratorCst.NS_JCR);
			fileElement.setAttribute("createdBy", creator, ContentGeneratorCst.NS_JCR);
			fileElement.setAttribute("documentStatus", documentStatus);
			fileElement.setAttribute("created", creationDate, ContentGeneratorCst.NS_JCR);

			Element jTranslation = new Element("translation_en", ContentGeneratorCst.NS_J);
			jTranslation.setAttribute("description", description, ContentGeneratorCst.NS_JCR);
			jTranslation.setAttribute("language", "en", ContentGeneratorCst.NS_JCR);
			jTranslation.setAttribute("primaryType", "jnt:translation", ContentGeneratorCst.NS_JCR);
			fileElement.addContent(jTranslation);

			Element jcrContentElement = new Element("content", ContentGeneratorCst.NS_JCR);
			jcrContentElement.setAttribute("primaryType", "jnt:resource", ContentGeneratorCst.NS_JCR);
			jcrContentElement.setAttribute("mimeType", mimeType, ContentGeneratorCst.NS_JCR);
			jcrContentElement.setAttribute("extractedText", extractedContent, ContentGeneratorCst.NS_J);
			jcrContentElement.setAttribute("lastExtractionDate", "2012-08-14T23:35:10.629+02:00", ContentGeneratorCst.NS_J);
			jcrContentElement.setAttribute("lastModified", "2012-08-14T23:35:10.629+02:00", ContentGeneratorCst.NS_JCR);

			fileElement.addContent(jcrContentElement);

			AceBO aceOwnerRoot = new AceBO("root", "root", "u", "GRANT", "docspace-owner");
			AceBO aceOwner = new AceBO(owner, owner, "u", "GRANT", "docspace-owner");
			AceBO aceEditor = new AceBO(editor, editor, "u", "GRANT", "docspace-editor");
			AceBO aceReader = new AceBO(reader, reader, "u", "GRANT", "docspace-reader");
			List<AceBO> aces = new ArrayList<AceBO>();
			aces.add(aceOwner);
			aces.add(aceOwnerRoot);
			aces.add(aceEditor);
			aces.add(aceReader);

			AclBO acl = new AclBO(aces);
			fileElement.addContent(acl.getElement());
		}
		return fileElement;
	}

	public String toString() {
		return fileName + " - description: " + description;
	}

	@Override
	public int hashCode() {
		return Integer.valueOf(jcrFilename.charAt(0));
	}

	@Override
	public boolean equals(Object arg0) {
		FileBO f = (FileBO) arg0;
		// return this.fileName.equals(f.getFileName());
		return this.fileName.intern() == f.getFileName().intern();
	}

	public int compareTo(FileBO f) throws NullPointerException {
		int nameDiff = this.fileName.compareToIgnoreCase(f.getFileName());
		return nameDiff;
	}
}
