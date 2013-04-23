package org.jahia.utils.maven.plugin.contentgenerator.bo;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class PageBO {
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
			String pSiteKey, String pFileName, Integer pNumberBigText, Map<String, List<String>> acls, Integer idCategory, Integer idTag, Boolean visibilityEnabled, String visibilityStartDate, String visibilityEndDate, String description) {
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
	}

	
	public String getHeader() {
		StringBuffer sb = new StringBuffer();
		sb.append("	<!-- generated page (level " + this.getLevel() + ") -->\n");

		sb.append("	<"
				+ this.getUniqueName()
				+ " xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" xmlns:jnt=\"http://www.jahia.org/jahia/nt/1.0\" xmlns:test=\"http://www.apache.org/jackrabbit/test\" xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" xmlns:jmix=\"http://www.jahia.org/jahia/mix/1.0\" xmlns:j=\"http://www.jahia.org/jahia/1.0\" xmlns:mix=\"http://www.jcp.org/jcr/mix/1.0\" xmlns:rep=\"internal\" changefreq=\"monthly\" j:templateNode=\"/sites/"
				+ this.getSiteKey() + "/templates/base/events\" jcr:mixinTypes=\"");
		if (this.getHasVanity()) {
			sb.append("jmix:vanityUrlMapped ");
		}

		if (this.idCategory != null) {
			sb.append(" jmix:categorized ");
		}
		
		sb.append(" jmix:sitemap\" jcr:primaryType=\"jnt:page\" priority=\"0.5\"");
		
		
		if (this.idCategory != null) {
			sb.append(" j:defaultCategory=\"/sites/systemsite/categories/category" + idCategory + "\" ");
		}
		
		if (this.idTag != null) {
			sb.append(" j:tags=\"/sites/" + siteKey + "/tags/tag" + idTag + "\"");
		}
		
		sb.append(">\n");
		// end page tag
		
        for (Map.Entry<String, ArticleBO> entry : articles.entrySet()) {
            sb.append("		<j:translation_"+entry.getKey()+" jcr:language=\""+entry.getKey()+"\" jcr:mixinTypes=\"mix:title\" jcr:primaryType=\"jnt:translation\" jcr:title=\""
				+ formatForXml(entry.getValue().getTitle()));
            
            if (StringUtils.isNotEmpty(description)) {
            	sb.append(" jcr:description=\"" + description + "\"");
            }
            sb.append("\" />\n");
        }

        if (!acls.isEmpty()) {
            sb.append("		<j:acl j:inherit=\"true\" jcr:primaryType=\"jnt:acl\">\n");
            for (Map.Entry<String, List<String>> entry : acls.entrySet()) {
                String roles = "";
                for (String s : entry.getValue()) {
                    roles += s + " ";
                }
                sb.append("		    <GRANT_"+entry.getKey().replace(":","_")+" j:aceType=\"GRANT\" j:principal=\""+entry.getKey()+"\" j:protected=\"false\" j:roles=\""+roles.trim()+"\" jcr:primaryType=\"jnt:ace\"/>\n");
            }
            sb.append("     </j:acl>\n");
        }
        sb.append("		<listA jcr:primaryType=\"jnt:contentList\">\n");
		// Big text (content)
		for (int i = 1; i <= numberBigText.intValue(); i++) {
			sb.append("			<bigText_" + i + " jcr:mixinTypes=\"jmix:renderable\" jcr:primaryType=\"jnt:bigText\">\n");
            for (Map.Entry<String, ArticleBO> entry : articles.entrySet()) {
            sb.append("				<j:translation_"+entry.getKey()+" jcr:language=\""+entry.getKey()+"\" jcr:primaryType=\"jnt:translation\" text=\""
					+ formatForXml(entry.getValue().getContent()) + " \" />");
            }
			sb.append("			</bigText_" + i + ">\n");
		}
		if (this.getFileName() != null) {
			sb.append(" 		<random-file jcr:primaryType=\"jnt:fileReference\">"
					+ " 			<j:translation_en  jcr:language=\"en\" jcr:primaryType=\"jnt:translation\" jcr:title=\"My file\" />"
					+ "			</random-file>");

			sb.append(" 		<publication jcr:primaryType=\"jnt:publication\">"
					+ "				<j:translation_en author=\"Jahia Content Generator\" body=\"&lt;p&gt;  Random publication&lt;/p&gt; \" date=\"01/01/1970\" file=\"/sites/"
					+ this.getSiteKey()
					+ "/files/contributed/"
					+ this.getFileName()
					+ "\" jcr:language=\"en\" jcr:primaryType=\"jnt:translation\" jcr:title=\"Random publication\" source=\"Jahia\" />"
					+ "			</publication>");
		}

		sb.append("		</listA>\n");

		if (this.getHasVanity()) {
			sb.append(" 	<vanityUrlMapping jcr:primaryType=\"jnt:vanityUrls\">"
					+ "			<_x0025_2F"
					+ this.getUniqueName()
					+ " j:active=\"true\" j:default=\"true\" j:url=\"/"
					+ this.getUniqueName()
					+ "\" jcr:language=\"en\" jcr:primaryType=\"jnt:vanityUrl\" />"
					+ "		</vanityUrlMapping>");
		}
		
		
		if (this.visibilityEnabled) {
		   sb.append("<j:conditionalVisibility j:forceMatchAllConditions=\"true\" jcr:primaryType=\"jnt:conditionalVisibility\">");
		   sb.append("	<jnt:startEndDateCondition0 end=\"" + this.visibilityEndDate + "\" jcr:primaryType=\"jnt:startEndDateCondition\" start=\"" + this.visibilityStartDate + "\" />");
		   sb.append("</j:conditionalVisibility>");
		  
		}
		return sb.toString();

	}

	public String getFooter() {
		return new String("		</" + this.getUniqueName() + ">\n");
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.getHeader());

		if (null != this.subPages) {
			for (Iterator<PageBO> iterator = subPages.iterator(); iterator.hasNext();) {
				PageBO subPage = iterator.next();
				sb.append(subPage.toString());
			}
		}

		sb.append(this.getFooter());

		return sb.toString();
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
