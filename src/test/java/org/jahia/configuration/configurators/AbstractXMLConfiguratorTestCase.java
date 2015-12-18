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
 *     This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
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
package org.jahia.configuration.configurators;

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
