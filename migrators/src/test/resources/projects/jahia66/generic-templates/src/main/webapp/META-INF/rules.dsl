[condition][]- its path matches {pattern}=node.path matches "{pattern}"
[condition][]- it is in a wiki page=eval(com.jahia.modules.generic.rules.GenericRulesService.isInWikiPage(node))
[consequence][]Set {node} modification on parent page=genericRulesService.setPageContentModified(node, drools);