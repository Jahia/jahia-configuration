/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.contentgenerator.bo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.CollectionBO;
import org.jdom.Element;

public class UserBO {
	private String name;

	private String password;

	private String jcrPath;

    private String email;
    
    private List<CollectionBO> collections = new ArrayList<CollectionBO>();

	public UserBO(String name, String password) {
		this.name = name;
		this.password = password;
        this.email = this.name + "@example.com";
	}
	
	public UserBO(String name, String password, String pathJcr, List<CollectionBO> collections) {
		this.name = name;
		this.password = password;
        this.email = this.name + "@example.com";
		this.jcrPath = pathJcr;
		this.collections = collections;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPathJcr() {
		return jcrPath;
	}

	public void setPathJcr(String pathJcr) {
		this.jcrPath = pathJcr;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	public List<CollectionBO> getCollections() {
		return collections;
	}

	public String getDirectoryName(int indexDir) {
		String dirname = null;
		if (this.jcrPath != null) {
			String[] directories = StringUtils.split(this.jcrPath, "/");
			dirname = directories[indexDir];
		}
		return dirname;
	}

	public Element getJcrXml() {
		Element userElement = new Element(this.name);
		userElement.setAttribute("email", this.email);
		userElement.setAttribute("emailNotificationsDisabled", Boolean.FALSE.toString());
		userElement.setAttribute("accountLocked", Boolean.FALSE.toString(), ContentGeneratorCst.NS_J);
		userElement.setAttribute("email", this.email, ContentGeneratorCst.NS_J);
		userElement.setAttribute("external", Boolean.FALSE.toString(), ContentGeneratorCst.NS_J);
		userElement.setAttribute("firstName", this.name + " firstname", ContentGeneratorCst.NS_J);
		userElement.setAttribute("lastName", this.name + " lastname", ContentGeneratorCst.NS_J);
		userElement.setAttribute("organization", "Organization", ContentGeneratorCst.NS_J);
		userElement.setAttribute("password", this.password, ContentGeneratorCst.NS_J); // W6ph5Mm5Pz8GgiULbPgzG37mj9g
		//userElement.setAttribute("picture", this.jcrPath + "/files/profile/publisher.png", ContentGeneratorCst.NS_J); //

		userElement.setAttribute("published", Boolean.TRUE.toString(), ContentGeneratorCst.NS_J);
		userElement.setAttribute("firstName", this.name + " firstname", ContentGeneratorCst.NS_J);

		userElement.setAttribute("mixinTypes", "jmix:accessControlled", ContentGeneratorCst.NS_JCR);
		userElement.setAttribute("primaryType", "jnt:user", ContentGeneratorCst.NS_JCR);

		// userElement.setAttribute("password.history.1242739225417", null);
		userElement.setAttribute("preferredLanguage", "en");

		if (CollectionUtils.isNotEmpty(collections)) {
			Element collectionsElement = new Element("collections");
			collectionsElement.setAttribute("primaryType", "docnt:collections", ContentGeneratorCst.NS_JCR);
			collectionsElement.setAttribute("createdBy", name, ContentGeneratorCst.NS_JCR);
			
			for (Iterator<CollectionBO> iterator = collections.iterator(); iterator.hasNext();) {
				CollectionBO collection = iterator.next();
				collectionsElement.addContent(collection.getElement());
			}
			userElement.addContent(collectionsElement);
		}
		
		// add subfolders				
		Element files = new Element("files");
		files.setAttribute("primaryType", "jnt:folder", ContentGeneratorCst.NS_JCR);
		
		Element privateElement = new Element("private");
		privateElement.setAttribute("primaryType", "jnt:folder", ContentGeneratorCst.NS_JCR); 
		privateElement.setAttribute("mixinTypes", "jmix:accessControlled", ContentGeneratorCst.NS_JCR);
		
		Element jAcl = new Element("acl", ContentGeneratorCst.NS_J);
		jAcl.setAttribute("primaryType", "jnt:acl", ContentGeneratorCst.NS_JCR); 

		AceBO grantUser = new AceBO("user", this.name, "u", "GRANT", "owner");
		
		jAcl.addContent(grantUser.getElement());
		privateElement.addContent(jAcl);
		
		Element imports = new Element("imports");
		imports.setAttribute("primaryType", "jnt:importDropBox", ContentGeneratorCst.NS_JCR);
		
		Element profile = new Element("profile");
		profile.setAttribute("primaryType", "jnt:folder", ContentGeneratorCst.NS_JCR);
	
		files.addContent(privateElement);
		privateElement.addContent(imports);
		files.addContent(profile);

		Element contents = new Element("contents");
		contents.setAttribute("primaryType", "jnt:contentFolder", ContentGeneratorCst.NS_JCR);
		
		Element portlets = new Element("portlets");
		portlets.setAttribute("primaryType", "jnt:portletFolder", ContentGeneratorCst.NS_JCR);
		
		Element preferences = new Element("preferences");
		preferences.setAttribute("primaryType", "jnt:preferences", ContentGeneratorCst.NS_JCR);
		
		userElement.addContent(preferences);
		userElement.addContent(files);
		userElement.addContent(contents);
		userElement.addContent(portlets);
		
				
		List<AceBO> aces = new ArrayList<AceBO>();
		AceBO aceOwner = new AceBO(this.name, this.name, "u", "GRANT", "owner");
		aces.add(aceOwner);
		AclBO acl = new AclBO(aces);
		userElement.addContent(acl.getElement());
		
		if (this.jcrPath == null) {
			return userElement;
		}
		
		String dir1 = getDirectoryName(1);
		String dir2 = getDirectoryName(2);
		String dir3 = getDirectoryName(3);

		Element root = new Element(dir1);
		root.setAttribute("primaryType", "jnt:usersFolder", ContentGeneratorCst.NS_JCR);

		Element subElement1 = new Element(dir2);
		subElement1.setAttribute("primaryType", "jnt:usersFolder", ContentGeneratorCst.NS_JCR);
		root.addContent(subElement1);

		Element subElement2 = new Element(dir3);
		subElement2.setAttribute("primaryType", "jnt:usersFolder", ContentGeneratorCst.NS_JCR);
		subElement1.addContent(subElement2);

		subElement2.addContent(userElement);
		return root;
	}
	
	/**
	 * Return XML code to add this user as a member of a group
	 * Example:  <user1 j:member="/users/gg/if/fh/user1" jcr:primaryType="jnt:member"/>
	 * @return Element userName
	 */
	public Element getUserMemberXml() {
		Element lightElement =  new Element(this.name);
		lightElement.setAttribute("member", jcrPath, ContentGeneratorCst.NS_J);
		lightElement.setAttribute("primaryType", "jnt:member", ContentGeneratorCst.NS_JCR);
		return lightElement;
	}
}
