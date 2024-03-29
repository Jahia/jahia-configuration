/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2023 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.utils.maven.plugin.contentgenerator.wise.bo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.jahia.utils.maven.plugin.contentgenerator.bo.AceBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.AclBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.SiteBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.UserBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom2.Document;
import org.jdom2.Element;

public class WiseBO extends SiteBO {
	Element wiseElement;

	String wiseInstanceName;

	List<DocspaceBO> docspaces;
	
	List<UserBO> users;

	public WiseBO(String wiseInstanceKey, List<DocspaceBO> docspaces, List<UserBO> users) {
		this.setSiteKey(wiseInstanceKey);
		this.docspaces = docspaces;
		this.users = users;
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
		
		for (Iterator<UserBO> iterator = users.iterator(); iterator.hasNext();) {
			UserBO user = iterator.next();
			membersElement.addContent(user.getUserMemberXml());
		}		

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
