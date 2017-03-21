/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
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

import org.apache.commons.lang.StringUtils;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Attribute;
import org.jdom.Element;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class PageBO {

    private Element pageElement;
    private String namePrefix;
    private Map<String, ArticleBO> articles;
    private List<PageBO> subPages;
    private Boolean hasVanity;
    private String siteKey;
    private String fileName;
    private Integer numberBigText;
    private Map<String, List<String>> acls;
    private Integer idCategory;
    private Integer idTag;
    private Boolean visibilityEnabled;
    private String visibilityStartDate;
    private String visibilityEndDate;
    private String description;
    private String pageTemplate;
    private String cmisSite;
    private List<String> externalFilePaths;
    private boolean personalized;
    private int minPersonalizationVariants;
    private int maxPersonalizationVariants;

    public PageBO(final String namePrefix, Map<String, ArticleBO> articles, final List<PageBO> subPages, Boolean hasVanity, String siteKey, String fileName, Integer numberBigText,
            Map<String, List<String>> acls, Integer idCategory, Integer idTag, Boolean visibilityEnabled, String visibilityStartDate, String visibilityEndDate, String description, String pageTemplate,
            String cmisSite, List<String> externalFilePaths, boolean personalized, int minPersonalizationVariants, int maxPersonalizationVariants) {

        this.articles = articles;
        this.subPages = subPages;
        this.namePrefix = namePrefix;
        this.hasVanity = hasVanity;
        this.siteKey = siteKey;
        this.fileName = fileName;
        this.numberBigText = numberBigText;
        this.acls = acls;
        this.idCategory = idCategory;
        this.idTag = idTag;
        this.visibilityEnabled = visibilityEnabled;
        this.visibilityStartDate = visibilityStartDate;
        this.visibilityEndDate = visibilityEndDate;
        this.description = description;
        this.pageTemplate = pageTemplate;
        this.cmisSite = cmisSite;
        this.externalFilePaths = externalFilePaths;
        this.personalized = personalized;
        this.minPersonalizationVariants = minPersonalizationVariants;
        this.maxPersonalizationVariants = maxPersonalizationVariants;
        buildPageElement();
    }

    public String getName() {
        if (personalized) {
            return namePrefix + "-personalized";
        } else {
            return namePrefix;

        }
    }

    public List<PageBO> getSubPages() {
        return subPages;
    }

    public Element getElement() {
        return pageElement;
    }

    private void buildPageElement() {

        pageElement = new Element(getName());
        pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_JCR);
        pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_NT);
        pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_JNT);
        pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_TEST);
        pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_SV);
        pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_JMIX);
        pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_J);
        pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_SV);
        pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_REP);
        pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_WEM);

        pageElement.setAttribute("changefreq", "monthly");
        pageElement.setAttribute("templateName", pageTemplate, ContentGeneratorCst.NS_J);
        pageElement.setAttribute("primaryType", "jnt:page", ContentGeneratorCst.NS_JCR);
        pageElement.setAttribute("priority", "0.5");

        String mixinTypes = "jmix:sitemap";
        if (hasVanity) {
            mixinTypes = mixinTypes + " jmix:vanityUrlMapped";
        }
        pageElement.setAttribute("mixinTypes", mixinTypes, ContentGeneratorCst.NS_JCR);

        if (idCategory != null) {
            pageElement.setAttribute("jcategorized", StringUtils.EMPTY, ContentGeneratorCst.NS_JMIX);
            pageElement.setAttribute("defaultCategory", "/sites/systemsite/categories/category" + idCategory, ContentGeneratorCst.NS_J);
        }

        if (idTag != null) {
            pageElement.setAttribute("tags", "/sites/" + siteKey + "/tags/tag" + idTag, ContentGeneratorCst.NS_J);
        }

        // articles
        for (Map.Entry<String, ArticleBO> entry : articles.entrySet()) {
            Element translationNode = new Element("translation_" + entry.getKey(), ContentGeneratorCst.NS_J);
            translationNode.setAttribute("language", entry.getKey(), ContentGeneratorCst.NS_JCR);
            translationNode.setAttribute("mixinTypes", "mix:title", ContentGeneratorCst.NS_JCR);
            translationNode.setAttribute("primaryType", "jnt:translation", ContentGeneratorCst.NS_JCR);
            translationNode.setAttribute("title", entry.getValue().getTitle(), ContentGeneratorCst.NS_JCR);

            if (StringUtils.isNotEmpty(description)) {
                translationNode.setAttribute("description", description, ContentGeneratorCst.NS_JCR);
            }
            pageElement.addContent(translationNode);
        }

        if (!acls.isEmpty()) {
            Element aclNode = new Element("acl", ContentGeneratorCst.NS_J);
            aclNode.setAttribute("inherit", "true", ContentGeneratorCst.NS_J);
            aclNode.setAttribute("primaryType", "jnt:acl", ContentGeneratorCst.NS_JCR);

            for (Map.Entry<String, List<String>> entry : acls.entrySet()) {
                String roles = "";
                for (String s : entry.getValue()) {
                    roles += s + " ";
                }
                Element aceNode = new Element("GRANT_"+entry.getKey().replace(":","_"));
                aceNode.setAttribute("aceType", "GRANT", ContentGeneratorCst.NS_J);
                aceNode.setAttribute("principal", entry.getKey(), ContentGeneratorCst.NS_J);
                aceNode.setAttribute("protected", "false", ContentGeneratorCst.NS_J);
                aceNode.setAttribute("roles", roles.trim(), ContentGeneratorCst.NS_J);
                aceNode.setAttribute("primaryType", "jnt:ace", ContentGeneratorCst.NS_JCR);

                aclNode.addContent(aceNode);
            }
            pageElement.addContent(aclNode);
        }

        // begin content list
        Element listNode = new Element("listA");
        listNode.setAttribute("primaryType", "jnt:contentList", ContentGeneratorCst.NS_JCR);

        LinkedList<Element> personalizableElements = new LinkedList<Element>();

        if (pageTemplate.equals(ContentGeneratorCst.PAGE_TPL_QALIST)) {

            List<String> languages = new ArrayList<String>();
            for (Map.Entry<String, ArticleBO> entry : articles.entrySet()) {
                languages.add(entry.getKey());
            }
            for (int i = 1; i <= ContentGeneratorCst.NB_NEWS_IN_QALIST; i++) {
                Element newsNode = new NewsBO(namePrefix + "-" + "news" + i , languages).getElement();
                listNode.addContent(newsNode);
                if (i <= ContentGeneratorCst.NB_NEWS_PER_PAGE_IN_QALIST) {
                    // News are split to multiple pages by Jahia at runtime, so only personalize items present on the first page.
                    personalizableElements.add(newsNode);
                }
            }
        } else if (pageTemplate.equals(ContentGeneratorCst.PAGE_TPL_DEFAULT)) {
            for (int i = 1; i <= numberBigText.intValue(); i++) {
                Element bigTextNode = new Element("bigText_" + i);
                bigTextNode.setAttribute("primaryType", "jnt:bigText", ContentGeneratorCst.NS_JCR);
                bigTextNode.setAttribute("mixinTypes", "jmix:renderable", ContentGeneratorCst.NS_JCR);
                for (Map.Entry<String, ArticleBO> entry : articles.entrySet()) {
                    Element translationNode = new Element("translation_" + entry.getKey(), ContentGeneratorCst.NS_J);
                    translationNode.setAttribute("language", entry.getKey(), ContentGeneratorCst.NS_JCR);
                    translationNode.setAttribute("primaryType", "jnt:translation", ContentGeneratorCst.NS_JCR);
                    translationNode.setAttribute("text", entry.getValue().getContent());
                    bigTextNode.addContent(translationNode);
                }
                listNode.addContent(bigTextNode);
                personalizableElements.add(bigTextNode);
            }
        }

        // for pages with external/internal file reference, we check the page name
        if (StringUtils.startsWith(namePrefix, ContentGeneratorCst.PAGE_TPL_QAEXTERNAL)) {
            for (int i = 0; i < externalFilePaths.size(); i++) {
                String externalFilePath = externalFilePaths.get(i);
                Element externalFileReference = new Element("external-file-reference-" + i);
                externalFileReference.setAttribute("node", "/mounts/" + ContentGeneratorCst.CMIS_MOUNT_POINT_NAME + "/Sites/" + cmisSite + externalFilePath, ContentGeneratorCst.NS_J);
                externalFileReference.setAttribute("primaryType", "jnt:fileReference", ContentGeneratorCst.NS_JCR);
                listNode.addContent(externalFileReference);
                personalizableElements.add(externalFileReference);
            }
        }

        if (StringUtils.startsWith(namePrefix, ContentGeneratorCst.PAGE_TPL_QAINTERNAL) && fileName != null) {

            Element randomFileNode = new Element("rand-file");
            randomFileNode.setAttribute("primaryType", "jnt:fileReference", ContentGeneratorCst.NS_JCR);

            Element fileTranslationNode = new Element("translation_en", ContentGeneratorCst.NS_J);
            fileTranslationNode.setAttribute("language", "en", ContentGeneratorCst.NS_JCR);
            fileTranslationNode.setAttribute("primaryType", "jnt:translation", ContentGeneratorCst.NS_JCR);
            fileTranslationNode.setAttribute("title", "My file", ContentGeneratorCst.NS_JCR);

            randomFileNode.addContent(fileTranslationNode);

            Element publicationNode = new Element("publication");
            publicationNode.setAttribute("primaryType", "jnt:publication", ContentGeneratorCst.NS_JCR);

            Element publicationTranslationNode = new Element("translation_en", ContentGeneratorCst.NS_J);
            publicationTranslationNode.setAttribute("author", "Jahia Content Generator");
            publicationTranslationNode.setAttribute("body", "&lt;p&gt;  Random publication&lt;/p&gt;");
            publicationTranslationNode.setAttribute("title", "Random publication", ContentGeneratorCst.NS_JCR);
            publicationTranslationNode.setAttribute("file", "/sites/" + siteKey + "/files/contributed/" + org.apache.jackrabbit.util.ISO9075.encode(fileName));
            publicationTranslationNode.setAttribute("language", "en", ContentGeneratorCst.NS_JCR);
            publicationTranslationNode.setAttribute("primaryType", "jnt:translation", ContentGeneratorCst.NS_JCR);
            publicationTranslationNode.setAttribute("source", "Jahia");

            publicationNode.addContent(publicationTranslationNode);

            listNode.addContent(publicationNode);
        }

        if (personalized) {
            if (personalizableElements.isEmpty()) {
                personalized = false;
                pageElement.setName(getName()); // Re-set the root element name: it must change according to page personalization change.
            } else {
                Element element = personalizableElements.get(ThreadLocalRandom.current().nextInt(personalizableElements.size()));
                int elementIndex = listNode.indexOf(element);
                listNode.removeContent(element);
                element = getPersonalizedElement(element);
                listNode.addContent(elementIndex, element);
            }
        }

        // end content list
        pageElement.addContent(listNode);

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
            pageElement.addContent(vanityNode);
        }

        if (visibilityEnabled) {

            Element visibilityNode = new Element("conditionalVisibility", ContentGeneratorCst.NS_J);
            visibilityNode.setAttribute("conditionalVisibility", null, ContentGeneratorCst.NS_J);
            visibilityNode.setAttribute("forceMatchAllConditions", "true", ContentGeneratorCst.NS_J);
            visibilityNode.setAttribute("primaryType", "jnt:conditionalVisibility", ContentGeneratorCst.NS_JCR);

            Element visibilityConditionNode = new Element("startEndDateCondition0", ContentGeneratorCst.NS_JNT);
            visibilityConditionNode.setAttribute("primaryType", "jnt:startEndDateCondition", ContentGeneratorCst.NS_JCR);
            visibilityConditionNode.setAttribute("start", visibilityStartDate);
            visibilityConditionNode.setAttribute("end", visibilityEndDate);

            visibilityNode.addContent(visibilityConditionNode);
            pageElement.addContent(visibilityNode);
        }

        if (null != subPages) {
            for (Iterator<PageBO> iterator = subPages.iterator(); iterator.hasNext();) {
                PageBO subPage = iterator.next();
                pageElement.addContent(subPage.getElement());
            }
        }
    }

    public String getJcrXml() {
        return getElement().getText();
    }

    private Element getPersonalizedElement(Element element) {

        Random random = ThreadLocalRandom.current();

        Element personalizationElement = new Element("experience-" + element.getName());
        personalizationElement.setAttribute("primaryType", "wemnt:personalizedContent", ContentGeneratorCst.NS_JCR);
        personalizationElement.setAttribute("active", "true", ContentGeneratorCst.NS_WEM);
        personalizationElement.setAttribute("personalizationStrategy", "priority", ContentGeneratorCst.NS_WEM);
        String[] segments = ContentGeneratorCst.SEGMENTS[random.nextInt(ContentGeneratorCst.SEGMENTS.length)];
        int nbPersonalizationVariants = minPersonalizationVariants + random.nextInt(maxPersonalizationVariants - minPersonalizationVariants + 1);
        if (nbPersonalizationVariants > segments.length) {
            nbPersonalizationVariants = segments.length;
        }
        for (int i = 0; i < nbPersonalizationVariants; i++) {

            Element variantElement = (Element) element.clone();
            variantElement.setName(variantElement.getName() + "-" + (i + 1));

            Attribute mixinTypesAttribute = variantElement.getAttribute("mixinTypes", ContentGeneratorCst.NS_JCR);
            String mixinTypes;
            if (mixinTypesAttribute == null) {
                mixinTypes = StringUtils.EMPTY;
            } else {
                mixinTypes = mixinTypesAttribute.getValue() + " ";
            }
            variantElement.setAttribute("mixinTypes", mixinTypes + "wemmix:editItem", ContentGeneratorCst.NS_JCR);

            String jsonFilter = "{\"parameterValues\":{\"subConditions\":[{\"type\":\"profileSegmentCondition\",\"parameterValues\":{\"segments\":[\"" + segments[i] + "\"]}}],\"operator\":\"and\"},\"type\":\"booleanCondition\"}";
            variantElement.setAttribute("jsonFilter", jsonFilter, ContentGeneratorCst.NS_WEM);
            personalizationElement.addContent(variantElement);
        }

        return personalizationElement;
    }
}
