package org.jahia.tools.contentgenerator.junit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jahia.utils.maven.plugin.contentgenerator.ContentGeneratorService;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ArticleBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.PageBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.junit.After;
import org.junit.Before;

public abstract class ContentGeneratorTestCase {
	protected ContentGeneratorService contentGeneratorService;
	
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
		contentGeneratorService = ContentGeneratorService.getInstance();
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
        PageBO page = new PageBO("page" + pageID, articleBOHashMap, 0, subPages, hasVanity, SITE_KEY, null, 2, new HashMap<String, List<String>>(), 1, 1, Boolean.FALSE, null, null, null, ContentGeneratorCst.PAGE_TPL_DEFAULT, null, null);
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
		Integer nbPagesTopLevel = Integer.valueOf(1);
		Integer nbLevels = Integer.valueOf(2);
		Integer nbPagesPerLevel = Integer.valueOf(3);
		
		export_default = new ExportBO();
		export_default.setPagesHaveVanity(Boolean.FALSE);
		export_default.setNumberOfCategories(2);
		
		Integer totalPages = contentGeneratorService.getTotalNumberOfPagesNeeded(nbPagesTopLevel, nbLevels, nbPagesPerLevel);
		export_default.setTotalPages(totalPages);
		export_default.setNumberOfTags(2);
	}
}
