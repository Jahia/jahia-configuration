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
package com.jahia.modules.generic.rules;

import org.drools.spi.KnowledgeHelper;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.rules.AddedNodeFact;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class GenericRulesService {

    public static boolean isInWikiPage(AddedNodeFact nodeFact) throws RepositoryException {
        JCRNodeWrapper page = JCRContentUtils.getParentOfType(nodeFact.getNode(), "jnt:page");
        if (page.hasProperty("j:templateNode")) {
            Node template = page.getProperty("j:templateNode").getNode();
            if (template != null) {
                return "documentation".equals(template.getName());
            }
        }
        return false;
    }

    public void setPageContentModified(AddedNodeFact nodeFact, KnowledgeHelper drools) throws RepositoryException {
        JCRNodeWrapper content = nodeFact.getNode();
        JCRNodeWrapper page = JCRContentUtils.getParentOfType(content, "jnt:page");
        if (!page.isNodeType("genericmix:wikiPage")) {
            page.addMixin("genericmix:wikiPage");
        }
        page.setProperty("touch", true);
        page.getSession().save();
    }

}