package org.jahia.utils.maven.plugin.contentgenerator.bo;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

public class PageBO {
	private Element pageElement;

	private String uniqueName;
	private Map<String, ArticleBO> articles;
	private Integer level;
	private List<PageBO> subPages;
	private PageBO parentPage;
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

	public void setIdCategory(Integer idCategory) {
		this.idCategory = idCategory;
	}

	public void setUniqueName(String uniqueName) {
		this.uniqueName = uniqueName;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	public List<PageBO> getSubPages() {
		return subPages;
	}

	public void setSubPages(List<PageBO> subPages) {
		this.subPages = subPages;
	}

	public PageBO getParentPage() {
		return parentPage;
	}

	public void setParentPage(PageBO parentPage) {
		this.parentPage = parentPage;
	}

	public Map<String, ArticleBO> getArticles() {
		return articles;
	}

	public void setArticles(Map<String, ArticleBO> articles) {
		this.articles = articles;
	}

	public Boolean getHasVanity() {
		return hasVanity;
	}

	public void setHasVanity(Boolean hasVanity) {
		this.hasVanity = hasVanity;
	}

	public String getSiteKey() {
		return siteKey;
	}

	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public Integer getNumberBigText() {
		return numberBigText;
	}

	public void setNumberBigText(Integer numberBigText) {
		this.numberBigText = numberBigText;
	}

	public Map<String, List<String>> getAcls() {
		return acls;
	}

	public void setAcls(Map<String, List<String>> acls) {
		this.acls = acls;
	}

	public Integer getIdTag() {
		return idTag;
	}

	public void setIdTag(Integer idTag) {
		this.idTag = idTag;
	}

	public void setVisibilityEnabled(Boolean visibilityEnabled) {
		this.visibilityEnabled = visibilityEnabled;
	}

	public void setVisibilityStartDate(String visibilityStartDate) {
		this.visibilityStartDate = visibilityStartDate;
	}

	public void setVisibilityEndDate(String visibilityEndDate) {
		this.visibilityEndDate = visibilityEndDate;
	}

	public PageBO(final String pUniqueName, Map<String, ArticleBO> articles, final int pLevel,
			final List<PageBO> pSubPages, Boolean pHasVanity, String pSiteKey, String pFileName,
			Integer pNumberBigText, Map<String, List<String>> acls, Integer idCategory, Integer idTag,
			Boolean visibilityEnabled, String visibilityStartDate, String visibilityEndDate) {
		this.articles = articles;
		this.level = pLevel;
		this.subPages = pSubPages;
		this.uniqueName = pUniqueName;
		this.hasVanity = pHasVanity;
		this.siteKey = pSiteKey;
		this.fileName = pFileName;
		this.numberBigText = pNumberBigText;
		this.acls = acls;
		this.idCategory = idCategory;
		this.idTag = idTag;
		this.visibilityEnabled = visibilityEnabled;
		this.visibilityStartDate = visibilityStartDate;
		this.visibilityEndDate = visibilityEndDate;
	}

	public String getHeader() {
		XMLOutputter outputter = new XMLOutputter();
		String pageString = outputter.outputString(this.getElement());
		return StringUtils.removeEnd(pageString, "</" + this.uniqueName + ">");
	}
	
	public String getFooter() {
		return "</" + this.uniqueName + ">";
	}
	
	public String toString() {
		XMLOutputter outputter = new XMLOutputter();
		return outputter.outputString(this.getElement());
	}

	public Element getElement() {
		if (this.pageElement == null) {
			this.pageElement = new Element(this.uniqueName);
			pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_JCR);
			pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_NT);
			pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_JNT);
			pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_TEST);
			pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_SV);
			pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_JMIX);
			pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_J);
			pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_MIX);

			pageElement.setAttribute("templateNode", "/sites/" + this.siteKey + "/templates/base/events",
					ContentGeneratorCst.NS_J);

			// mixinTypes value
			StringBuffer mixinTypes = new StringBuffer();
			if (this.getHasVanity()) {
				mixinTypes.append(" jmix:vanityUrlMapped ");
			}
			if (this.idCategory != null) {
				mixinTypes.append(" jmix:categorized ");
			}
			mixinTypes.append("jmix:sitemap");
			pageElement.setAttribute("mixinTypes", mixinTypes.toString(), ContentGeneratorCst.NS_JCR);

			pageElement.setAttribute("primaryType", "jnt:page", ContentGeneratorCst.NS_JCR);
			pageElement.setAttribute("priority", "0.5");

			// Categories
			if (this.idCategory != null) {
				pageElement.setAttribute("defaultCategory", "/sites/systemsite/categories/category" + idCategory,
						ContentGeneratorCst.NS_J);
			}

			// Tags
			if (this.idTag != null) {
				pageElement.setAttribute("tags", "/sites/" + siteKey + "/tags/tag", ContentGeneratorCst.NS_J);
			}

			// Translations
			for (Map.Entry<String, ArticleBO> entry : articles.entrySet()) {
				Element translation = new Element("translation_" + entry.getKey(), ContentGeneratorCst.NS_J);
				translation.setAttribute("language", entry.getKey(), ContentGeneratorCst.NS_JCR);
				translation.setAttribute("mixinTypes", "mix:title", ContentGeneratorCst.NS_JCR);
				translation.setAttribute("primaryType", "jnt:translation", ContentGeneratorCst.NS_JCR);
				translation
						.setAttribute("title", formatForXml(entry.getValue().getTitle()), ContentGeneratorCst.NS_JCR);
				pageElement.addContent(translation);
			}

			// Content (articles)
			Element list = new Element("listA");
			for (int i = 1; i <= numberBigText.intValue(); i++) {

				Element bigText = new Element("bigText_" + i);
				bigText.setAttribute("mixinTypes", "jmix:renderable", ContentGeneratorCst.NS_JCR);
				bigText.setAttribute("primaryType", "jnt:bigText", ContentGeneratorCst.NS_JCR);
				for (Map.Entry<String, ArticleBO> entry : articles.entrySet()) {
					Element translation = new Element("translation_" + entry.getKey());
					translation.setAttribute("language", entry.getKey(), ContentGeneratorCst.NS_JCR);
					translation.setAttribute("primaryType", "jnt:translation", ContentGeneratorCst.NS_JCR);
					translation.setAttribute("text", formatForXml(entry.getValue().getContent()),
							ContentGeneratorCst.NS_JCR);
					bigText.addContent(translation);
				}
				list.addContent(bigText);
			}
			pageElement.addContent(list);

			// Files
			if (this.getFileName() != null) {
				Element randomFile = new Element("random-file");
				randomFile.setAttribute("primaryType", "jnt:fileReference", ContentGeneratorCst.NS_JCR);

				for (Map.Entry<String, ArticleBO> entry : articles.entrySet()) {
					Element translation = new Element("translation_" + entry.getKey(), ContentGeneratorCst.NS_J);
					translation.setAttribute("language", entry.getKey(), ContentGeneratorCst.NS_JCR);
					translation.setAttribute("primaryType", "jnt:translation", ContentGeneratorCst.NS_JCR);
					translation.setAttribute("title", "My File - " + entry.getKey(), ContentGeneratorCst.NS_JCR);
					randomFile.addContent(translation);
				}
				list.addContent(randomFile);

				Element publication = new Element("publication");
				publication.setAttribute("primaryType", "jnt:publication", ContentGeneratorCst.NS_JCR);
				for (Map.Entry<String, ArticleBO> entry : articles.entrySet()) {
					Element translation = new Element("translation_" + entry.getKey(), ContentGeneratorCst.NS_J);
					translation.setAttribute("author", "Jahia Content Generator");
					translation.setAttribute("body", " Random publication");
					translation.setAttribute("date", "01/01/1970");
					translation.setAttribute("file",
							"/sites/" + this.getSiteKey() + "/files/contributed/" + this.getFileName());
					translation.setAttribute("language", entry.getKey(), ContentGeneratorCst.NS_JCR);
					translation.setAttribute("primaryType", "jnt:translation", ContentGeneratorCst.NS_JCR);
					translation.setAttribute("title", "Random publication - " + entry.getKey(),
							ContentGeneratorCst.NS_JCR);
					translation.setAttribute("source", "Jahia");
					publication.addContent(translation);
				}

				list.addContent(publication);
			}

			if (this.getHasVanity()) {
				Element vanity = new Element("vanityUrlMapping");
				vanity.setAttribute("primaryType", "jnt:vanityUrls", ContentGeneratorCst.NS_JCR);

				Element content = new Element("_x0025_2F");
				content.setAttribute("active", "true", ContentGeneratorCst.NS_J);
				content.setAttribute("default", "true", ContentGeneratorCst.NS_J);
				content.setAttribute("url", "/" + this.getUniqueName(), ContentGeneratorCst.NS_J);
				content.setAttribute("language", "en", ContentGeneratorCst.NS_JCR);
				content.setAttribute("primaryType", "jnt:vanityUrl", ContentGeneratorCst.NS_JCR);

				vanity.addContent(content);
				pageElement.addContent(vanity);
			}

			if (this.visibilityEnabled) {
				Element visibility = new Element("conditionalVisibility", ContentGeneratorCst.NS_J);
				visibility.setAttribute("forceMatchAllConditions", "true", ContentGeneratorCst.NS_J);
				visibility.setAttribute("primaryType", "jnt:conditionalVisibility", ContentGeneratorCst.NS_JCR);

				Element startEndDateCondition = new Element("startEndDateCondition0", ContentGeneratorCst.NS_JNT);
				startEndDateCondition.setAttribute("start", this.visibilityStartDate);
				startEndDateCondition.setAttribute("end", this.visibilityEndDate);
				startEndDateCondition.setAttribute("primaryType", "jnt:startEndDateCondition",
						ContentGeneratorCst.NS_JCR);
				visibility.addContent(startEndDateCondition);

				pageElement.addContent(visibility);
			}
			
			if (null != this.subPages) {
				for (Iterator<PageBO> iterator = subPages.iterator(); iterator.hasNext();) {
					PageBO subPage = iterator.next();
					pageElement.addContent(subPage.getElement());
				}
			}
		}
		return this.pageElement;
	}

	/**
	 * Convert & \ < > and ' into they HTML equivalent
	 * 
	 * @param s
	 *            XML string to format
	 * @return formatted XML string
	 */
	public String formatForXml(final String s) {
		String formattedString = StringUtils.replace(s, "&", "&amp;");
		formattedString = StringUtils.replace(formattedString, "\"", " &quot;");
		formattedString = StringUtils.replace(formattedString, "<", "&lt;");
		formattedString = StringUtils.replace(formattedString, ">", "&gt;");
		formattedString = StringUtils.replace(formattedString, "'", "&#39;");
		return formattedString;
	}

}
