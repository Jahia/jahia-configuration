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

import java.util.List;
import java.util.Map;

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom2.Element;

public class PageBO extends ContentBO {

    private Boolean hasVanity;
    private String pageTemplate;


    public PageBO(final String namePrefix, Map<String, ArticleBO> articles, final List<ContentBO> subPages, Boolean hasVanity,
            String siteKey, String fileName, Integer numberBigText,
            Map<String, List<String>> acls, Integer idCategory, Integer idTag, Boolean visibilityEnabled, String visibilityStartDate, String visibilityEndDate, String description, String pageTemplate,
            String cmisSite, List<String> externalFilePaths, boolean personalized, int minPersonalizationVariants, int maxPersonalizationVariants) {

        super(namePrefix, articles, subPages, siteKey, fileName, numberBigText, acls, idCategory, idTag, visibilityEnabled,
                visibilityStartDate,
                visibilityEndDate, description, cmisSite, externalFilePaths, personalized, minPersonalizationVariants,
                maxPersonalizationVariants);
        this.hasVanity = hasVanity;
        this.pageTemplate = pageTemplate;
        buildPageElement();
    }

    private void buildPageElement() {

        element.addNamespaceDeclaration(ContentGeneratorCst.NS_JCR);
        element.addNamespaceDeclaration(ContentGeneratorCst.NS_NT);
        element.addNamespaceDeclaration(ContentGeneratorCst.NS_JNT);
        element.addNamespaceDeclaration(ContentGeneratorCst.NS_TEST);
        element.addNamespaceDeclaration(ContentGeneratorCst.NS_SV);
        element.addNamespaceDeclaration(ContentGeneratorCst.NS_JMIX);
        element.addNamespaceDeclaration(ContentGeneratorCst.NS_J);
        element.addNamespaceDeclaration(ContentGeneratorCst.NS_SV);
        element.addNamespaceDeclaration(ContentGeneratorCst.NS_REP);
        element.addNamespaceDeclaration(ContentGeneratorCst.NS_WEM);

        element.setAttribute("changefreq", "monthly");
        element.setAttribute("templateName", pageTemplate, ContentGeneratorCst.NS_J);
        element.setAttribute("primaryType", "jnt:page", ContentGeneratorCst.NS_JCR);
        element.setAttribute("priority", "0.5");

        String mixinTypes = "jmix:sitemap";
        if (hasVanity) {
            mixinTypes = mixinTypes + " jmix:vanityUrlMapped";
        }
        element.setAttribute("mixinTypes", mixinTypes, ContentGeneratorCst.NS_JCR);

        Element listNode = new Element("listA");
        listNode.setAttribute("primaryType", "jnt:contentList", ContentGeneratorCst.NS_JCR);
        buildPersonalizedElements(listNode);

        element.addContent(listNode);

        if (hasVanity) {
            Element vanityNode = new Element("vanityUrlMapping");
            vanityNode.setAttribute("primaryType", "jnt:vanityUrls", ContentGeneratorCst.NS_JCR);

            Element vanitySubNode = new Element(namePrefix);
            vanitySubNode.setAttribute("active", "true", ContentGeneratorCst.NS_J);
            vanitySubNode.setAttribute("default", "true", ContentGeneratorCst.NS_J);
            vanitySubNode.setAttribute("url", "/" + namePrefix, ContentGeneratorCst.NS_J);
            vanitySubNode.setAttribute("language", "en", ContentGeneratorCst.NS_JCR);
            vanitySubNode.setAttribute("primaryType", "jnt:vanityUrl", ContentGeneratorCst.NS_JCR);

            vanityNode.addContent(vanitySubNode);
            element.addContent(vanityNode);
        }

    }

}
