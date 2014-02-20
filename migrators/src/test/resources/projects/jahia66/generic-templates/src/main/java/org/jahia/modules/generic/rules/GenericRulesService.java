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