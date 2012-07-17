package org.jahia.utils.maven.plugin.contentgenerator.wise.bo;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.jdom.Document;
import org.jdom.Element;

import org.jahia.utils.maven.plugin.contentgenerator.bo.SiteBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;

public class WiseBO extends SiteBO {
	Element wiseElement;
	
	String wiseInstanceName;
	
	List<DocspaceBO> docspaces;
	
	public WiseBO(String wiseInstanceKey, List<DocspaceBO> docspaces) {
		this.setSiteKey(wiseInstanceKey);
		this.docspaces = docspaces;
	}
	
	public Element getElement() {
		if (wiseElement == null) {
			wiseElement = new Element(this.getSiteKey());
			wiseElement.setAttribute("mixinTypes", "jmix:accessControlled", ContentGeneratorCst.NS_JCR);
			wiseElement.setAttribute("primaryType", "jnt:virtualsite", ContentGeneratorCst.NS_JCR);
			
			if (CollectionUtils.isNotEmpty(docspaces)) {
				Element docspacesElement = new Element("docspaces");
				docspacesElement.setAttribute("mixinTypes", "jmix:workflowRulesable", ContentGeneratorCst.NS_JCR);
				docspacesElement.setAttribute("primaryType", "jnt:folder", ContentGeneratorCst.NS_JCR);
				
				for (Iterator<DocspaceBO> iterator = docspaces.iterator(); iterator.hasNext();) {
					DocspaceBO docspace = iterator.next();
					docspacesElement.addContent(docspace.getElement());
				}
				wiseElement.addContent(docspacesElement);
			}
		}
		return wiseElement;
	}
	
	public Document getDocument() {
        Document doc = new Document();
        Element contentNode = new Element("content");
        contentNode.addNamespaceDeclaration(ContentGeneratorCst.NS_JCR);
        contentNode.addNamespaceDeclaration(ContentGeneratorCst.NS_JNT);
        contentNode.addNamespaceDeclaration(ContentGeneratorCst.NS_JMIX);
        contentNode.addNamespaceDeclaration(ContentGeneratorCst.NS_J);
        contentNode.addNamespaceDeclaration(ContentGeneratorCst.NS_NT);
        contentNode.addNamespaceDeclaration(ContentGeneratorCst.NS_SV);
        contentNode.addNamespaceDeclaration(ContentGeneratorCst.NS_MIX);
        contentNode.addNamespaceDeclaration(ContentGeneratorCst.NS_DOCNT);
        
        doc.setRootElement(contentNode);

        Element sitesNode = new Element("sites");
        sitesNode.setAttribute("primaryType", "jnt:virtualsitesFolder", ContentGeneratorCst.NS_JCR);
        sitesNode.addContent(this.getElement());
        
        contentNode.addContent(sitesNode);
        return doc;
	}
}
