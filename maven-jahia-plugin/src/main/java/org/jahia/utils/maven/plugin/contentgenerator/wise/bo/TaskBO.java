/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

public class TaskBO {
	private Element task;
	
	private String name = "new-task";
	
	private String title = "New task";
	
	private String creator = "root";
	
	private String candidate = "root";
	
	private String description = "This task has been created by the Content generator";
	
	private String priority = "low";
	
	private String state = "active";
	
	private String type = "docspace";
	
	private String dueDate = "2013-07-05T00:00:00.000+02:00";
		
	public TaskBO(String title, String creator, String candidate, String description) {
		this.title = title;
		this.creator = creator;
		this.candidate = candidate;
		this.description = description;
	}
	
	public Element getElement() {
		if (task == null) {
			task = new Element(name);
			task.setAttribute("description", description);
			task.setAttribute("createdBy", creator, ContentGeneratorCst.NS_JCR);
			task.setAttribute("assigneeUserKey", "");
			task.setAttribute("candidates", "u:" + candidate);
			task.setAttribute("dueDate", dueDate);
			task.setAttribute("originWS", "default", ContentGeneratorCst.NS_J);
			task.setAttribute("primaryType", "jnt:task", ContentGeneratorCst.NS_JCR);
			task.setAttribute("title", title, ContentGeneratorCst.NS_JCR);
			task.setAttribute("priority", priority);
			task.setAttribute("state", state);
			task.setAttribute("type", type);
		}
		return task;
	}
}
