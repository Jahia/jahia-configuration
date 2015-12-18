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
 *     This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
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
import org.apache.commons.lang.StringUtils;
import org.jahia.utils.maven.plugin.contentgenerator.bo.AceBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.AclBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

public class DocspaceBO {
	private Element docspaceElement;

	private String docspaceName;

	private String fromEmail = "invitedto@vibes.jahia.com";

	private List<PollBO> polls;

	private List<NoteBO> notes;

	private List<TaskBO> tasks;

	private List<FolderBO> folders;
	
	private Integer nbUsers;
	
	private Integer nbOwners;
	
	private Integer nbEditors;
	
	private Integer nbCollaborators;

	public DocspaceBO(String docspaceName, List<PollBO> polls, List<NoteBO> notes, List<TaskBO> tasks, List<FolderBO> folders, Integer nbUsers, Integer nbOwners, Integer nbEditors, Integer nbCollaborators) {
		this.docspaceName = docspaceName;
		this.polls = polls;
		this.notes = notes;
		this.tasks = tasks;
		this.folders = folders;
		this.nbUsers = nbUsers;
		this.nbOwners = nbOwners;
		this.nbEditors = nbEditors;
		this.nbCollaborators = nbCollaborators;
	}

	public List<PollBO> getPolls() {
		return polls;
	}

	public List<NoteBO> getNotes() {
		return notes;
	}

	public List<TaskBO> getTasks() {
		return tasks;
	}

	public List<FolderBO> getFolders() {
		return folders;
	}

	public Element getElement() {
		if (docspaceElement == null) {
			docspaceElement = new Element(StringUtils.lowerCase(docspaceName));
			docspaceElement.setAttribute("fromEmail", fromEmail);
			docspaceElement.setAttribute("mixinTypes", "docmix:docspace jmix:accessControlled", ContentGeneratorCst.NS_JCR);
			docspaceElement.setAttribute("primaryType", "jnt:folder", ContentGeneratorCst.NS_JCR);

			Element translationEn = new Element("translation_en", ContentGeneratorCst.NS_J);
			translationEn.setAttribute("primaryType", "jnt:translation", ContentGeneratorCst.NS_JCR);
			translationEn.setAttribute("description", "Created by the Jahia Content Generator", ContentGeneratorCst.NS_JCR);
			translationEn.setAttribute("title", docspaceName, ContentGeneratorCst.NS_JCR);
			translationEn.setAttribute("language", "en", ContentGeneratorCst.NS_JCR);
			docspaceElement.addContent(translationEn);
			
			docspaceElement.addContent(generateDocspaceAcl());

			if (CollectionUtils.isNotEmpty(polls)) {
				Element pollsElement = new Element("polls");
				pollsElement.setAttribute("primaryType", "docnt:pollsContainer", ContentGeneratorCst.NS_JCR);

				for (Iterator<PollBO> iterator = polls.iterator(); iterator.hasNext();) {
					PollBO poll = iterator.next();
					pollsElement.addContent(poll.getElement());
				}
				docspaceElement.addContent(pollsElement);
			}

			if (CollectionUtils.isNotEmpty(notes)) {
				Element notesElement = new Element("notes");
				notesElement.setAttribute("primaryType", "docnt:notesContainer", ContentGeneratorCst.NS_JCR);

				for (Iterator<NoteBO> iterator = notes.iterator(); iterator.hasNext();) {
					NoteBO note = iterator.next();
					notesElement.addContent(note.getElement());
				}
				docspaceElement.addContent(notesElement);
			}

			if (CollectionUtils.isNotEmpty(tasks)) {
				Element tasksElement = new Element("tasks");
				tasksElement.setAttribute("primaryType", "jnt:tasks", ContentGeneratorCst.NS_JCR);

				for (Iterator<TaskBO> iterator = tasks.iterator(); iterator.hasNext();) {
					TaskBO task = iterator.next();
					tasksElement.addContent(task.getElement());
				}
				docspaceElement.addContent(tasksElement);
			}

			if (CollectionUtils.isNotEmpty(folders)) {
				for (Iterator<FolderBO> iterator = folders.iterator(); iterator.hasNext();) {
					FolderBO folder = iterator.next();
					docspaceElement.addContent(folder.getElement());
				}
			}

		}
		return docspaceElement;
	}

	private Element generateDocspaceAcl() {
		List<AceBO> aces = new ArrayList<AceBO>();
		AceBO aceOwnerRoot = new AceBO("root", "root", "u", "GRANT", "docspace-owner");
		aces.add(aceOwnerRoot);
		
		int maxIdEditors = nbEditors + nbOwners;
		int maxIdCollaborators = maxIdEditors + nbCollaborators;
		for (int i = 0; i < nbUsers; i++) {
			String userName = "user" + i;
			
			if (i < nbOwners) {
				AceBO aceOwner = new AceBO(userName, userName, "u", "GRANT", "docspace-owner");
				aces.add(aceOwner);
			} else if (i < maxIdEditors) {
				AceBO aceEditor = new AceBO(userName, userName, "u", "GRANT", "docspace-editor");
				aces.add(aceEditor);
			}else if (i < maxIdCollaborators) {
				AceBO aceCollaborator = new AceBO(userName, userName, "u", "GRANT", "docspace-collaborator");
				aces.add(aceCollaborator);
			}
		}
		AclBO acl = new AclBO(aces);
		return acl.getElement();
	}
}
