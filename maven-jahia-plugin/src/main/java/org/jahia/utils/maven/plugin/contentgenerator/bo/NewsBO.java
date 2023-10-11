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

import java.util.List;
import java.util.Random;

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom2.Element;

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
