/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jahia.utils.maven.plugin.contentgenerator.PageService;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ArticleBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.PageBO;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class PageServiceTest extends ContentGeneratorTestCase {
    private static PageService pageService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        pageService = new PageService();
    }

    @After
    public void tearDown() {
        // run for one time after all test cases
    }

    @Ignore
    @Test
    public void testCreateTopPages() {
        //pageService.createTopPages(export, articles);
    }

    @Ignore
    @Test
    public void testCreateSubPages() {

    }

    //@Test
    public void testCreateNewPage() {
        String pageName = "myPage";
        Integer totalBigText = Integer.valueOf(5);
        super.export_default.setNumberOfBigTextPerPage(totalBigText);

        HashMap<String, ArticleBO> map = new HashMap<String, ArticleBO>();
        map.put("en",articleEn);
        PageBO newPage = pageService.createNewPage(super.export_default, pageName, map, 1, null);
        assertEquals(pageName, newPage.getName());
        String sPage = newPage.toString();
        assertTrue(StringUtils.contains(sPage, "<bigText_1"));
        assertTrue(StringUtils.contains(sPage, "<bigText_2"));
        assertTrue(StringUtils.contains(sPage, "<bigText_3"));
        assertTrue(StringUtils.contains(sPage, "<bigText_4"));
        assertTrue(StringUtils.contains(sPage, "<bigText_5"));
    }

    //@Test
    public void testCreateNewPageZeroBigText() {
        String pageName = "myPage";
        Integer totalBigText = Integer.valueOf(0);
        super.export_default.setNumberOfBigTextPerPage(totalBigText);

        HashMap<String, ArticleBO> map = new HashMap<String, ArticleBO>();
        map.put("en",articleEn);
        PageBO newPage = pageService.createNewPage(super.export_default, pageName, map, 1, null);
        assertEquals(pageName, newPage.getName());
        String sPage = newPage.toString();
        assertFalse(StringUtils.contains(sPage, "<bigText"));
    }

    @Test
    public void testGetPagesPath() {
        List pagesPath = pageService.getPagesPath(super.pages, null);
        assertEquals(super.total_pages.intValue(), pagesPath.size());
        assertTrue(pagesPath.contains("/page1"));
        assertTrue(pagesPath.contains("/page2"));
        assertTrue(pagesPath.contains("/page3"));
        assertTrue(pagesPath.contains("/page1/page11"));
        assertTrue(pagesPath.contains("/page1/page12"));
        assertTrue(pagesPath.contains("/page1/page11/page111"));
        assertTrue(pagesPath.contains("/page1/page11/page112"));
    }

    @Ignore
    @Test
    public void testFormatForXml() {

    }
}
