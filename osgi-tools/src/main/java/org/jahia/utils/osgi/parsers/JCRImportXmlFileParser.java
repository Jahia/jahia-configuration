package org.jahia.utils.osgi.parsers;

import org.jdom2.Element;
import org.jdom2.JDOMException;

/**
 * JCR Import file parser
 */
public class JCRImportXmlFileParser extends AbstractXmlFileParser {

    private final static String[] JCR_IMPORT_XPATH_QUERIES = {
            "//@jcr:primaryType",
            "//@jcr:mixinTypes"
    };

    @Override
    public boolean canParse(String fileName, Element rootElement) {
        return hasNamespaceURI(rootElement, "http://www.jcp.org/jcr/1.0");
    }

    @Override
    public void parse(String fileName, Element rootElement, ParsingContext parsingContext, boolean externalDependency) throws JDOMException {
        getLogger().debug("Processing JCR import file " + fileName + "...");

        getRefsUsingXPathQueries(fileName, rootElement, false, false, JCR_IMPORT_XPATH_QUERIES, "xp", parsingContext);
    }
}
