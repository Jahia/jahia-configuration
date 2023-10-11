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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang.StringUtils;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom2.Attribute;
import org.jdom2.Element;

/*
 * Generic class for either PageBO or FolderBO to inherit
 */
abstract public class ContentBO {

    protected Element element;
    protected String namePrefix;
    protected List<ContentBO> subContents;
    protected Map<String, ArticleBO> articles;
    protected String siteKey;
    protected String fileName;
    protected Integer numberBigText;
    protected Map<String, List<String>> acls;
    protected Integer idCategory;
    protected Integer idTag;
    protected Boolean visibilityEnabled;
    protected String visibilityStartDate;
    protected String visibilityEndDate;
    protected String description;
    protected String cmisSite;
    protected List<String> externalFilePaths;
    protected boolean personalized;
    protected int minPersonalizationVariants;
    protected int maxPersonalizationVariants;

    public ContentBO(final String namePrefix, Map<String, ArticleBO> articles, List<ContentBO> subContents, String siteKey, String fileName,
            Integer numberBigText, Map<String, List<String>> acls, Integer idCategory, Integer idTag, Boolean visibilityEnabled,
            String visibilityStartDate, String visibilityEndDate, String description,
            String cmisSite, List<String> externalFilePaths, boolean personalized, int minPersonalizationVariants, int maxPersonalizationVariants) {

        this.subContents = subContents;
        this.articles = articles;
        this.namePrefix = namePrefix;
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
        this.cmisSite = cmisSite;
        this.externalFilePaths = externalFilePaths;
        this.personalized = personalized;
        this.minPersonalizationVariants = minPersonalizationVariants;
        this.maxPersonalizationVariants = maxPersonalizationVariants;
        buildElement();
    }

    public String getName() {
        if (personalized) {
            return namePrefix + "-personalized";
        } else {
            return namePrefix;

        }
    }

    public Element getElement() {
        return element;
    }

    private void buildElement() {

        element = new Element(getName());

        if (null != subContents) {
            for (Iterator<ContentBO> iterator = subContents.iterator(); iterator.hasNext();) {
                ContentBO subContent = iterator.next();
                element.addContent(subContent.getElement());
            }
        }

        if (idCategory != null) {
            element.setAttribute("jcategorized", StringUtils.EMPTY, ContentGeneratorCst.NS_JMIX);
            element.setAttribute("defaultCategory", "/sites/systemsite/categories/category" + idCategory, ContentGeneratorCst.NS_J);
        }

        if (idTag != null) {
            element.setAttribute("tags", "/sites/" + siteKey + "/tags/tag" + idTag, ContentGeneratorCst.NS_J);
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
            element.addContent(translationNode);
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
            element.addContent(aclNode);
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
            element.addContent(visibilityNode);
        }

    }

    protected void buildPersonalizedElements(Element element) {
        LinkedList<Element> personalizableElements = new LinkedList<>();

        if (StringUtils.startsWith(namePrefix, ContentGeneratorCst.PAGE_TPL_QALIST)) {

            List<String> languages = new ArrayList<>();
            for (Map.Entry<String, ArticleBO> entry : articles.entrySet()) {
                languages.add(entry.getKey());
            }
            for (int i = 1; i <= ContentGeneratorCst.NB_NEWS_IN_QALIST; i++) {
                Element newsNode = new NewsBO(namePrefix + "-" + "news" + i, languages).getElement();
                element.addContent(newsNode);
                if (i <= ContentGeneratorCst.NB_NEWS_PER_PAGE_IN_QALIST) {
                    // News are split to multiple pages by Jahia at runtime, so only personalize items present on the first page.
                    personalizableElements.add(newsNode);
                }
            }
        } else if (StringUtils.startsWith(namePrefix, ContentGeneratorCst.PAGE_TPL_DEFAULT)) {
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
                element.addContent(bigTextNode);
                personalizableElements.add(bigTextNode);
            }
        }

        // for pages with external/internal file reference, we check the page name
        else if (StringUtils.startsWith(namePrefix, ContentGeneratorCst.PAGE_TPL_QAEXTERNAL)) {
            for (int i = 0; i < externalFilePaths.size(); i++) {
                String externalFilePath = externalFilePaths.get(i);
                Element externalFileReference = new Element("external-file-reference-" + i);
                externalFileReference.setAttribute("node",
                        "/mounts/" + ContentGeneratorCst.CMIS_MOUNT_POINT_NAME + "/Sites/" + cmisSite + externalFilePath,
                        ContentGeneratorCst.NS_J);
                externalFileReference.setAttribute("primaryType", "jnt:fileReference", ContentGeneratorCst.NS_JCR);
                personalizableElements.add(externalFileReference);
                element.addContent(externalFileReference);
            }
        }

        else if (StringUtils.startsWith(namePrefix, ContentGeneratorCst.PAGE_TPL_QAINTERNAL) && fileName != null) {

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
            publicationTranslationNode.setAttribute("file",
                    "/sites/" + siteKey + "/files/contributed/" + org.apache.jackrabbit.util.ISO9075.encode(fileName));
            publicationTranslationNode.setAttribute("language", "en", ContentGeneratorCst.NS_JCR);
            publicationTranslationNode.setAttribute("primaryType", "jnt:translation", ContentGeneratorCst.NS_JCR);
            publicationTranslationNode.setAttribute("source", "Jahia");

            publicationNode.addContent(publicationTranslationNode);

            element.addContent(publicationNode);
        }

        if (personalized) {
            if (personalizableElements.isEmpty()) {
                personalized = false;
                element.setName(getName()); // Re-set the root element name: it must change according to page personalization change.
            } else {
                Element persoElement = personalizableElements.get(ThreadLocalRandom.current().nextInt(personalizableElements.size()));
                int elementIndex = element.indexOf(persoElement);
                element.removeContent(persoElement);
                persoElement = getPersonalizedElement(persoElement);
                element.addContent(elementIndex, persoElement);
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

    public List<ContentBO> getSubContents() {
        return subContents;
    }
}
