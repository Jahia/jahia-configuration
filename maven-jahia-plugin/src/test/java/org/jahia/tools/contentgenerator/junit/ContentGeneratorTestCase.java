package org.jahia.tools.contentgenerator.junit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jahia.utils.maven.plugin.contentgenerator.bo.ArticleBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.PageBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.junit.After;
import org.junit.Before;

public abstract class ContentGeneratorTestCase {
	protected ExportBO export_default;
	
	protected List<ArticleBO> articles;
	protected List<PageBO> pages;
	protected Integer total_pages = 7;
	
	protected ArticleBO articleEn;
	protected ArticleBO articleFr;

	protected static String TITLE_FR = "Titre FR";
	protected static String CONTENT_FR = "Titre FR";
	
	protected static String TITLE_EN = "Title EN";
	protected static String CONTENT_EN = "CONTENT EN";
	
	protected String SITE_KEY = "ACME";
	
	@Before
	public void setUp() throws Exception {
		createPages();
		createArticles();
		createExport();
	}

	@After
	public void tearDown() throws Exception {
	}
	
	private PageBO createPage(int pageID, List<PageBO> subPages) {
		boolean hasVanity = true;
        HashMap<String, ArticleBO> articleBOHashMap = new HashMap<String, ArticleBO>();
        articleBOHashMap.put("en", new ArticleBO(0,"Title " + pageID, "Content " + pageID));
        articleBOHashMap.put("fr", new ArticleBO(0,"Titre " + pageID, "Contenu " + pageID));
        PageBO page = new PageBO("page" + pageID, articleBOHashMap, 0, subPages, hasVanity, SITE_KEY, null, 2, new HashMap<String, List<String>>());
		return page;
	}
	
	private void createPages() {
		pages = new ArrayList();

		List<PageBO> subPages = new ArrayList<PageBO>();
		
		int pageID = 111;
		PageBO page111 = createPage(pageID, null) ;
		pageID=112;
		PageBO page112 = createPage(pageID, null);
		
		subPages.add(page111);
		subPages.add(page112);
		
		pageID=11;
		PageBO page11 = createPage(pageID, subPages);
		pageID=12;
		PageBO page12 = createPage(pageID, null);
		
		subPages = new ArrayList<PageBO>();
		subPages.add(page11);
		subPages.add(page12);
		
		pageID=1;
		PageBO page1 = createPage(pageID, subPages);

		pageID=2;
		PageBO page2 = createPage(pageID, null);
		
		pageID=3;
		PageBO page3 = createPage(pageID, null);
		
		pages.add(page1);
		pages.add(page2);
		pages.add(page3);
	}
	
	
	private void createArticles() {
		/*
		 * Articles
		 */
		articleFr = new ArticleBO(1, TITLE_FR, CONTENT_FR);
		articleEn  = new ArticleBO(2, TITLE_EN, CONTENT_EN);
	}
	
	private void createExport() {
		export_default = new ExportBO();
		export_default.setAddFilesToPage(ContentGeneratorCst.VALUE_NONE);
		export_default.setPagesHaveVanity(Boolean.FALSE);
	}
}
