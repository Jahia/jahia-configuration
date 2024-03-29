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
package org.jahia.utils.maven.plugin.contentgenerator.bo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom2.Element;

public class GroupBO {
	
	private String name;

	private List<UserBO> users;

    public GroupBO(String name, List<UserBO> users) {
		this.name = name;
		this.users = users;
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getNbUsers() {
		return Integer.valueOf(users.size());
	}

	public List<UserBO> getUsers() {
		return users;
	}

	public void setUsers(List<UserBO> users) {
		this.users = users;
	}

	/**
	 * returns user names contained in this group
	 * 
	 * @return user names
	 */
	public List<String> getUserNames() {
		List<String> userNames = new ArrayList<String>();
		for (Iterator<UserBO> iterator = users.iterator(); iterator.hasNext();) {
			UserBO user = iterator.next();
			userNames.add(user.getName());
		}
		return userNames;
	}

	public Element getJcrXml() {

		Element groupNode = new Element(this.name);
		groupNode.setAttribute("primaryType", "jnt:group", ContentGeneratorCst.NS_JCR);
        groupNode.setAttribute("hidden","false", ContentGeneratorCst.NS_J);
		Element users = new Element("members", ContentGeneratorCst.NS_J);
		users.setAttribute("primaryType", "jnt:members", ContentGeneratorCst.NS_JCR);
        groupNode.addContent(users);
		for (Iterator<UserBO> iterator = this.users.iterator(); iterator.hasNext();) {
			UserBO user = iterator.next();
			Element userNode = new Element(user.getName());
			userNode.setAttribute("member", user.getPathJcr(), ContentGeneratorCst.NS_J);
			userNode.setAttribute("primaryType", "jnt:member", ContentGeneratorCst.NS_JCR);
			users.addContent(userNode);
		}
		return groupNode;
	}
}
