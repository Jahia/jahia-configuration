package org.jahia.utils.maven.plugin.contentgenerator.wise.bo;

import java.util.ArrayList;
import java.util.List;

import org.jahia.utils.maven.plugin.contentgenerator.bo.AceBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.AclBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

public class FileBO {

	protected Element fileElement;
	
	protected String nodePath;
	
	protected String fileName;
	
	protected String creator;
	
	protected String owner;
	
	protected String reader;
	
	protected String editor;
	
	protected String documentStatus = "Draft";

	public FileBO(String fileName, String nodePath, String creator, String owner, String editor, String reader) {
		this.fileName = fileName;
		this.nodePath = nodePath;
		this.creator = creator;
		this.owner = owner;
		this.editor = editor;
		this.reader = reader;
	}
	
	public String getFileName() {
		return fileName;
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

	public Element getElement() {
		if (fileElement == null) {
			fileName =  org.apache.jackrabbit.util.ISO9075.encode(fileName);
			fileElement = new Element(this.fileName);
			// mixinTypes = jmix:image or jmix:document, depending on the type of document. We should generate that dynamically
			fileElement.setAttribute("mixinTypes", "docmix:docspaceDocument jmix:accessControlled ", ContentGeneratorCst.NS_JCR);
			fileElement.setAttribute("primaryType", "jnt:file", ContentGeneratorCst.NS_JCR);
			fileElement.setAttribute("createdBy", creator, ContentGeneratorCst.NS_JCR);
			fileElement.setAttribute("documentStatus", documentStatus);
			fileElement.setAttribute("extractedText", "Dummy content from Jahia Content Generator", ContentGeneratorCst.NS_J);
			
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
}
