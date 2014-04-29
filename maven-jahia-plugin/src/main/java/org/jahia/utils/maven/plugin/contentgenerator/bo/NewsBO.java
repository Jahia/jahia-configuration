package org.jahia.utils.maven.plugin.contentgenerator.bo;

import java.util.List;
import java.util.Random;

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

public class NewsBO {
	private Element newsElement;
	
	private final String newsContent = "Bacon ipsum dolor sit amet cow brisket meatloaf shank, venison beef tail prosciutto sausage jowl rump spare ribs tenderloin. Chuck ham tongue sirloin. Corned beef shoulder andouille kevin. Tail sausage ground round tongue. Brisket bresaola porchetta turkey cow tri-tip. Pork loin salami shank pastrami cow turkey beef meatloaf ham hock fatback biltong landjaeger.";
	
	private String newsName;
	
	private List<String> languages;
	
	public NewsBO(String newsName, List<String> languages) {
		this.newsName = newsName;
		this.languages = languages;
	}
	
	public Element getElement() {
		if (newsElement == null) {
			Random generator = new Random();
			
			newsElement = new Element(newsName);
			newsElement.setAttribute("primaryType", "jnt:news", ContentGeneratorCst.NS_JCR);
			
			for (String language : languages) {
				Element translationNode = new Element("translation_" + language, ContentGeneratorCst.NS_J);
				translationNode.setAttribute("language", language, ContentGeneratorCst.NS_JCR);
				translationNode.setAttribute("mixinTypes", "mix:title", ContentGeneratorCst.NS_JCR);
				translationNode.setAttribute("primaryType", "jnt:translation", ContentGeneratorCst.NS_JCR);
				translationNode.setAttribute("title", newsName, ContentGeneratorCst.NS_JCR);
				translationNode.setAttribute("date", "2013-12-12T11:35:00.000+01:00");
				
				int r = generator.nextInt(3) + 1;
				for (int i = 1; i <= r; i++) {
					translationNode.setAttribute("desc", newsContent);
				}			
				
				newsElement.addContent(translationNode);
	        }
		}
		return newsElement;
	}
}
