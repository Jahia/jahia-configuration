package org.jahia.utils.maven.plugin.contentgenerator.wise.bo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.jahia.utils.maven.plugin.contentgenerator.bo.AceBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.AclBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.SiteBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Document;
import org.jdom.Element;

public class WiseBO extends SiteBO {
	Element wiseElement;

	String wiseInstanceName;

	List<DocspaceBO> docspaces;

	public WiseBO(String wiseInstanceKey, List<DocspaceBO> docspaces) {
		this.setSiteKey(wiseInstanceKey);
		this.docspaces = docspaces;
	}
	
	public List<DocspaceBO> getDocspaces() {
		return docspaces;
	}

	public Element getElement() {
		if (wiseElement == null) {
			wiseElement = new Element(this.getSiteKey());
			wiseElement.setAttribute("mixinTypes", "jmix:accessControlled", ContentGeneratorCst.NS_JCR);
			wiseElement.setAttribute("primaryType", "jnt:virtualsite", ContentGeneratorCst.NS_JCR);

			wiseElement.addContent(getGroupsElement());
			wiseElement.addContent(getAclElement());
			
			Element filesElement = new Element("files");
			filesElement.setAttribute("primaryType", "jnt:folder", ContentGeneratorCst.NS_JCR);
			filesElement.setAttribute("publicationStatus", "3", ContentGeneratorCst.NS_J);

			filesElement.addContent(getContributedElement());

			if (CollectionUtils.isNotEmpty(docspaces)) {
				Element docspacesElement = new Element("docspaces");
				docspacesElement.setAttribute("mixinTypes", "jmix:workflowRulesable", ContentGeneratorCst.NS_JCR);
				docspacesElement.setAttribute("primaryType", "jnt:folder", ContentGeneratorCst.NS_JCR);

				for (Iterator<DocspaceBO> iterator = docspaces.iterator(); iterator.hasNext();) {
					DocspaceBO docspace = iterator.next();
					docspacesElement.addContent(docspace.getElement());
				}
				filesElement.addContent(docspacesElement);
			}
			wiseElement.addContent(filesElement);
		}
		return wiseElement;
	}

	public Document getDocument() {
		Document doc = new Document();
		Element contentNode = new Element("content");


		doc.setRootElement(contentNode);

		Element sitesNode = new Element("sites");
		sitesNode.setAttribute("primaryType", "jnt:virtualsitesFolder", ContentGeneratorCst.NS_JCR);
		sitesNode.addContent(this.getElement());	        
	        
		contentNode.addContent(sitesNode);
		return doc;
	}

	private Element getContributedElement() {
		Element contributedElement = new Element("contributed");
		contributedElement.setAttribute("originWS", "default", ContentGeneratorCst.NS_J);
		contributedElement.setAttribute("createdBy", "system", ContentGeneratorCst.NS_JCR);
		contributedElement.setAttribute("mixinTypes", "jmix:accessControlled", ContentGeneratorCst.NS_JCR);
		contributedElement.setAttribute("primaryType", "jnt:folder", ContentGeneratorCst.NS_JCR);

		AceBO sitePrivileged = new AceBO("site-privileged", "privileged", "g", "GRANT", "contributor");

		List<AceBO> aces = new ArrayList<AceBO>();
		aces.add(sitePrivileged);
		AclBO acl = new AclBO(aces);

		Element aclElement = acl.getElement();
		contributedElement.addContent(aclElement);
		return contributedElement;
	}

	private Element getGroupsElement() {
		Element groupElement = new Element("groups");
		groupElement.setAttribute("primaryType", "jnt:groupsFolder", ContentGeneratorCst.NS_JCR);

		Element sitePrivileged = new Element("site-privileged");
		sitePrivileged.setAttribute("external", "false", ContentGeneratorCst.NS_J);
		sitePrivileged.setAttribute("hidden", "false", ContentGeneratorCst.NS_J);
		sitePrivileged.setAttribute("mixinTypes", "jmix:systemNode", ContentGeneratorCst.NS_JCR);
		sitePrivileged.setAttribute("primaryType", "jnt:group", ContentGeneratorCst.NS_JCR);

		Element membersElement = new Element("members", ContentGeneratorCst.NS_J);
		membersElement.setAttribute("primaryType", "jnt:members", ContentGeneratorCst.NS_JCR);

		Element siteAdministratorsElement = new Element("site-administrators___3");
		siteAdministratorsElement.setAttribute("primaryType", "jnt:member", ContentGeneratorCst.NS_JCR);
		siteAdministratorsElement.setAttribute("member", "/sites/" + this.getSiteKey() + "/groups/site-administrators", ContentGeneratorCst.NS_J);

		membersElement.setContent(siteAdministratorsElement);
		sitePrivileged.setContent(membersElement);
		groupElement.setContent(sitePrivileged);
		return groupElement;
	}
	
	private Element getAclElement() {
		AceBO sitePrivileged = new AceBO("site-privileged", "site-privileged", "g", "GRANT", "docspace-site-member privileged");
		AceBO privileged = new AceBO("privileged", "privileged", "g", "DENY", "privileged");
		AceBO siteAdministrators = new AceBO("site-administrators", "site-administrators", "g", "GRANT", "site-administrator");
		List<AceBO> aces = new ArrayList<AceBO>();
		aces.add(sitePrivileged);
		aces.add(privileged);
		aces.add(siteAdministrators);
		
		AclBO acl = new AclBO(aces);
		return acl.getElement();
	}
}
