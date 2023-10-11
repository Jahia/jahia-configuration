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

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom2.Element;

public class NoteBO {
	
	private Element note;
	
	private String name = "new-note";
	
	private String title = "New note";
	
	private String body = "Note content (generated)";
	
	private String creator = "root";
	
	public NoteBO(String name, String title, String creator, String body) {
		this.name = name;
		this.title = title;
		this.creator = creator;
		this.body = body;
	}
	
	public Element getElement() {
		if (note == null) {
			note = new Element(name);
			note.setAttribute("originWS", "default", ContentGeneratorCst.NS_J);
			note.setAttribute("primaryType", "docnt:note", ContentGeneratorCst.NS_JCR);
			note.setAttribute("title", title, ContentGeneratorCst.NS_JCR);
			note.setAttribute("body", body);
			note.setAttribute("createdBy", creator, ContentGeneratorCst.NS_JCR);
		}
		return note;
	}
}
