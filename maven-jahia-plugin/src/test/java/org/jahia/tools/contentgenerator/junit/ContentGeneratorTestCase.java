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
package org.jahia.tools.contentgenerator.junit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jahia.utils.maven.plugin.contentgenerator.ContentGeneratorService;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ArticleBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ContentBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.PageBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.junit.Before;

public abstract class ContentGeneratorTestCase {

    protected ContentGeneratorService contentGeneratorService;
    protected ExportBO export_default;
    protected List<ArticleBO> articles;
    protected List<ContentBO> pages;
    protected Integer total_pages = 7;
    protected ArticleBO articleEn;
    protected ArticleBO articleFr;
    protected String SITE_KEY = "ACME";

    protected static String TITLE_FR = "Titre FR";
    protected static String CONTENT_FR = "Titre FR";

    protected static String TITLE_EN = "Title EN";
    protected static String CONTENT_EN = "CONTENT EN";

    @Before
    public void setUp() throws Exception {
        contentGeneratorService = ContentGeneratorService.getInstance();
        createPages();
        createArticles();
        createExport();
    }

    private PageBO createPage(int pageID, List<ContentBO> subPages) {
        boolean hasVanity = true;
        HashMap<String, ArticleBO> articleBOHashMap = new HashMap<>();
        articleBOHashMap.put("en", new ArticleBO(0,"Title " + pageID, "Content " + pageID));
        articleBOHashMap.put("fr", new ArticleBO(0,"Titre " + pageID, "Contenu " + pageID));
        PageBO page = new PageBO("page" + pageID, articleBOHashMap, subPages, hasVanity, SITE_KEY, null, 2,
                new HashMap<String, List<String>>(), 1, 1, Boolean.FALSE, null, null, null, ContentGeneratorCst.PAGE_TPL_DEFAULT, null,
                null, false, 2, 3);
        return page;
    }

    private void createPages() {

        pages = new ArrayList<>();

        List<ContentBO> subPages = new ArrayList<>();

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

        subPages = new ArrayList<>();
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
