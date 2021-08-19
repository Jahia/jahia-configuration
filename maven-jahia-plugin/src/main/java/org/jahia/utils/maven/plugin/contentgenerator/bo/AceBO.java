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

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom2.Element;

public class AceBO {
	
	private Element aceElement;
	
	private String permission; // GRANT, DENY
	
	private String type; //u, g
	
	private String userOrGroupName;
	
	private String userOrGroupPrincipal;
	
	private String roles;
	
	public AceBO(String userOrGroupName, String userOrGroupPrincipal, String type, String permission,  String roles) {
		this.permission = permission;
		this.type = type;
		this.userOrGroupName = userOrGroupName;
		this.roles = roles;
		this.userOrGroupPrincipal = userOrGroupPrincipal;
	}
	
  public Element getElement() {
	  String elementName = permission + "_" + type + "_" + userOrGroupName;
	  if (aceElement == null) {
		  aceElement = new Element(elementName);
		  aceElement.setAttribute("primaryType", "jnt:ace", ContentGeneratorCst.NS_JCR);
		  aceElement.setAttribute("aceType", permission, ContentGeneratorCst.NS_J);
		  aceElement.setAttribute("principal", type + ":" + userOrGroupPrincipal, ContentGeneratorCst.NS_J);
		  aceElement.setAttribute("protected", "false", ContentGeneratorCst.NS_J);
		  aceElement.setAttribute("roles", roles, ContentGeneratorCst.NS_J);
	  }
	  
	  return aceElement;
  }
}
