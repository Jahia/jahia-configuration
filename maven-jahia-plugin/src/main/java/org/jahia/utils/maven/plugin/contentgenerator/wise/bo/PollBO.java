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
import java.util.Random;

import org.jahia.utils.maven.plugin.contentgenerator.OutputService;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

public class PollBO {

	private Element poll;

	private String question = "Why ?";
	
	private String questionElementName = "why?";

	private String status = "open";
	
	private List<String> answers = new ArrayList<String>();

	public PollBO(String question, List<String> answers) {
		OutputService os = new OutputService();
		this.questionElementName = os.formatStringForXml(question);
		this.question = question;
		this.answers = answers;
	}
	
	public PollBO(String question, String questionElementName, List<String> answers) {
		this.questionElementName = questionElementName;
		this.question = question;
		this.answers = answers;
	}

	public Element getElement() {
		Random rand = new Random();
		
		if (poll == null) {
			poll = new Element(questionElementName);
			poll.setAttribute("mixinTypes", "docmix:docspaceObject", ContentGeneratorCst.NS_JCR);
			poll.setAttribute("primaryType", "jnt:poll", ContentGeneratorCst.NS_JCR);
			poll.setAttribute("question", question);
			poll.setAttribute("status", status);
			
			Element answersElement = new Element("answers");
			answersElement.setAttribute("primaryType", "jnt:answersList", ContentGeneratorCst.NS_JCR);
			
			for (Iterator<String> iterator = answers.iterator(); iterator.hasNext();) {
				String answer = iterator.next();
				Element answerElement = new Element(answer);
				answerElement.setAttribute("primaryType", "jnt:answer", ContentGeneratorCst.NS_JCR);
				
				answerElement.setAttribute("label", answer);
				answerElement.setAttribute("nbOfVotes", String.valueOf(rand.nextInt(10)));
				answersElement.addContent(answerElement);
			}
			poll.addContent(answersElement);
		}		
		return poll;
	}
}
