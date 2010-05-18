package org.jahia.utils.maven.plugin.configurators;

import junit.framework.TestCase;
import org.jdom.*;
import org.jdom.xpath.XPath;

import java.util.List;

/**
 * Base class for all XML configurator test cases, provides helper methods to validate XML once it has been modified.
 *
 * @author loom
 *         Date: Oct 26, 2009
 *         Time: 1:01:49 PM
 */
public abstract class AbstractXMLConfiguratorTestCase extends AbstractConfiguratorTestCase {

    public Object getNode(Document jdomDocument, String xPathExpression, String prefix) throws JDOMException {
        Element rootElement = jdomDocument.getRootElement();
        String namespaceURI = rootElement.getNamespaceURI();
        XPath objectXPath = XPath.newInstance(xPathExpression);
        objectXPath.addNamespace(prefix, namespaceURI);
        return objectXPath.selectSingleNode(jdomDocument);
    }

    public void checkOnlyOneElement(Document jdomDocument, String xPathExpression, String prefix) throws JDOMException {
        Element rootElement = jdomDocument.getRootElement();
        String namespaceURI = rootElement.getNamespaceURI();
        XPath contextParamXPath = XPath.newInstance(xPathExpression);
        contextParamXPath.addNamespace(prefix, namespaceURI);
        List contextParamList = contextParamXPath.selectNodes(jdomDocument);
        assertEquals(1, contextParamList.size());
    }

    public void assertAllTextEquals(Document jdomDocument, String xPathExpression, String prefix, String value) throws JDOMException {
        Element rootElement = jdomDocument.getRootElement();
        String namespaceURI = rootElement.getNamespaceURI();
        XPath contextParamXPath = XPath.newInstance(xPathExpression);
        contextParamXPath.addNamespace(prefix, namespaceURI);
        List resultList = contextParamXPath.selectNodes(jdomDocument);
        for (Object currentObject : resultList) {
            if (currentObject instanceof Attribute) {
                assertEquals(value, ((Attribute) currentObject).getValue());
            } else if (currentObject instanceof Element) {
                assertEquals(value, ((Element) currentObject).getText());
            } else if (currentObject instanceof Text) {
                assertEquals(value, ((Text) currentObject).getValue());
            } else if (currentObject instanceof Comment) {
                assertEquals(value, ((Comment) currentObject).getText());
            } else if (currentObject instanceof CDATA) {
                assertEquals(value, ((CDATA) currentObject).getText());
            } else if (currentObject instanceof ProcessingInstruction) {
                assertEquals(value, ((ProcessingInstruction) currentObject).getValue());
            } else {
                // default fall-back comparison, should rarely be useful.
                assertEquals(value, currentObject.toString());
            }
        }
    }

}
