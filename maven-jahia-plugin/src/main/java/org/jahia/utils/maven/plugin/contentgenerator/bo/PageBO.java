package org.jahia.utils.maven.plugin.contentgenerator.bo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

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
	private String description;
	private String pageTemplate;
	private String cmisSite;
	private List externalFilePaths;

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

	public PageBO(final String pUniqueName, Map<String, ArticleBO> articles, final int pLevel, final List<PageBO> pSubPages, Boolean pHasVanity,
			String pSiteKey, String pFileName, Integer pNumberBigText, Map<String, List<String>> acls, Integer idCategory, Integer idTag, Boolean visibilityEnabled, String visibilityStartDate, String visibilityEndDate, String description, String pageTemplate, String cmisSite, List externalFilePaths) {
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
        this.description = description;
        this.pageTemplate = pageTemplate;
        this.cmisSite = cmisSite;
        this.externalFilePaths = externalFilePaths;
	}

	public Element getElement() {
		if (pageElement == null) {
			pageElement = new Element(this.getUniqueName());
			pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_JCR);
			pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_NT);
			pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_JNT);
			pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_TEST);
			pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_SV);
			pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_JMIX);
			pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_J);
			pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_SV);
			pageElement.addNamespaceDeclaration(ContentGeneratorCst.NS_REP);
			
			//xmlns:rep=\"internal\"
					
			pageElement.setAttribute("changefreq", "monthly");
			pageElement.setAttribute("templateNode", "/sites/" + this.getSiteKey() + "/templates/base/" + pageTemplate, ContentGeneratorCst.NS_J);
			pageElement.setAttribute("primaryType", "jnt:page", ContentGeneratorCst.NS_JCR);
			pageElement.setAttribute("priority", "0.5");
			
			String mixinTypes = "jmix:sitemap";
			if (this.getHasVanity()) {
				mixinTypes = mixinTypes + " jmix:vanityUrlMapped";
			}
			pageElement.setAttribute("mixinTypes", mixinTypes, ContentGeneratorCst.NS_JCR);
			
			if (this.idCategory != null) {
				pageElement.setAttribute("jcategorized", null, ContentGeneratorCst.NS_JMIX);
				pageElement.setAttribute("defaultCategory", "/sites/systemsite/categories/category" + idCategory, ContentGeneratorCst.NS_J);
			}
			
			if (this.idTag != null) {
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
			if (this.pageTemplate.equals(ContentGeneratorCst.PAGE_TPL_QALIST)) {
				List<String> languages = new ArrayList<String>();
				for (Map.Entry<String, ArticleBO> entry : articles.entrySet()) {
					languages.add(entry.getKey());
				}
				
				for (int i = 1; i <= ContentGeneratorCst.NB_NEWS_IN_QALIST; i++) {
					listNode.addContent(new NewsBO(this.uniqueName + "-" + "news" + i , languages).getElement());
				}
			} else if (this.pageTemplate.equals(ContentGeneratorCst.PAGE_TPL_DEFAULT)) {
				// Big text (content)
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
				}
			}
			
			// for pages with external/internal file reference, we check the page name
			if (StringUtils.startsWith(this.uniqueName, ContentGeneratorCst.PAGE_TPL_QAEXTERNAL)) {
				int i = 0;
				for (Iterator iterator = externalFilePaths.iterator(); iterator.hasNext();) {
					String externalFilePath = (String) iterator.next();

					Element externalFileReference = new Element("external-file-reference-" + i);
					externalFileReference.setAttribute("node", "/mounts/" + ContentGeneratorCst.MOUNT_POINT_NAME + "/Sites/" + this.cmisSite + externalFilePath, ContentGeneratorCst.NS_J);
					externalFileReference.setAttribute("primaryType", "jnt:fileReference", ContentGeneratorCst.NS_JCR);
					listNode.addContent(externalFileReference);
					i++;
				}
			} 
			
			if (StringUtils.startsWith(this.uniqueName, ContentGeneratorCst.PAGE_TPL_QAINTERNAL)) {
				if (this.getFileName() != null) {
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
					publicationTranslationNode.setAttribute("file", "/sites/" + this.getSiteKey() + "/files/contributed/" + org.apache.jackrabbit.util.ISO9075.encode(this.getFileName()));
					publicationTranslationNode.setAttribute("language", "en", ContentGeneratorCst.NS_JCR);
					publicationTranslationNode.setAttribute("primaryType", "jnt:translation", ContentGeneratorCst.NS_JCR);
					publicationTranslationNode.setAttribute("source", "Jahia");
					
					publicationNode.addContent(publicationTranslationNode);
					
					listNode.addContent(publicationNode);
				}
			}
	
			// end content list
			pageElement.addContent(listNode);
				
			if (this.getHasVanity()) {
				Element vanityNode = new Element("vanityUrlMapping");
				vanityNode.setAttribute("primaryType", "jnt:vanityUrls", ContentGeneratorCst.NS_JCR); 
				
				Element vanitySubNode = new Element(this.getUniqueName());
				vanitySubNode.setAttribute("active", "true", ContentGeneratorCst.NS_J);
				vanitySubNode.setAttribute("default", "true", ContentGeneratorCst.NS_J);
				vanitySubNode.setAttribute("url", "/" + this.getUniqueName(), ContentGeneratorCst.NS_J);
				vanitySubNode.setAttribute("language", "en", ContentGeneratorCst.NS_JCR);
				vanitySubNode.setAttribute("primaryType", "jnt:vanityUrl", ContentGeneratorCst.NS_JCR); 
				
				vanityNode.addContent(vanitySubNode);
				pageElement.addContent(vanityNode);
			}
			
			if (this.visibilityEnabled) {
				Element visibilityNode = new Element("conditionalVisibility", ContentGeneratorCst.NS_J);
				visibilityNode.setAttribute("conditionalVisibility", null, ContentGeneratorCst.NS_J);
				visibilityNode.setAttribute("forceMatchAllConditions", "true", ContentGeneratorCst.NS_J);
				visibilityNode.setAttribute("primaryType", "jnt:conditionalVisibility", ContentGeneratorCst.NS_JCR); 
				
				Element visibilityConditionNode = new Element("startEndDateCondition0", ContentGeneratorCst.NS_JNT);
				visibilityConditionNode.setAttribute("primaryType", "jnt:startEndDateCondition", ContentGeneratorCst.NS_JCR);
				visibilityConditionNode.setAttribute("start", this.visibilityStartDate);
				visibilityConditionNode.setAttribute("end", this.visibilityEndDate);
				
				visibilityNode.addContent(visibilityConditionNode);
				pageElement.addContent(visibilityNode);
			}		
			
			if (null != this.subPages) {
				for (Iterator<PageBO> iterator = subPages.iterator(); iterator.hasNext();) {
					PageBO subPage = iterator.next();
					pageElement.addContent(subPage.getElement());
				}
			}
		}
		return pageElement;
	}
	
	public String getJcrXml() {
		return getElement().getText();
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
