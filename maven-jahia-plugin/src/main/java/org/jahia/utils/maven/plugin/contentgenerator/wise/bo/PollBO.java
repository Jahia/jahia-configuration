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
