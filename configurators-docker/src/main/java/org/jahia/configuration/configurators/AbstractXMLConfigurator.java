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
package org.jahia.configuration.configurators;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPath;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.io.*;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;

/**
 * Abstract configurator that includes helper methods to ease XML DOM manipulation.
 *
 * @author loom
 * Date: Oct 27, 2009
 * Time: 3:28:04 PM
 */
public abstract class AbstractXMLConfigurator extends AbstractConfigurator {
    public AbstractXMLConfigurator(Map dbProps, JahiaConfigInterface jahiaConfigInterface) {
        super(dbProps, jahiaConfigInterface);
    }

    /**
     * This method makes it easier to insert an XML DOM element immediately after the element that has been retrieved
     * by the XPath expression. If you are using this method with a document that uses namespaces, you will need to use
     * the "xp" prefix in your XPath expression, as in the following example :
     * /xp:beans/xp:bean[@id="FileListSync"]/xp:property[@name="syncUrl"]
     * Note that it is important that the XPath expression point to an element, not an attribute or other DOM nodes. No
     * sanity checks will be done by the method but an casting exception will be raised in that case.
     *
     * @param jdomDocument    the document used to scope the XPath execution
     * @param xPathExpression the xpath expression, including the "xp" prefix if namespaces are used
     * @param element         the element to be inserted immediately after the element retrieved by the XPath.
     * @throws JDOMException in case there was an error executing the XPath expression or manipulating the DOM.
     */
    public void insertElementAfter(Document jdomDocument, String xPathExpression, Element element) throws JDOMException {
        Element rootElement = jdomDocument.getRootElement();
        String namespaceURI = rootElement.getNamespaceURI();
        XPath previousElementXPath = XPath.newInstance(xPathExpression);
        if ((namespaceURI != null) && (!"".equals(namespaceURI))) {
            previousElementXPath.addNamespace("xp", namespaceURI);
        }
        Element previousElement = (Element) (previousElementXPath.selectSingleNode(jdomDocument));
        int previousElementIndex = previousElement.getParent().indexOf(previousElement);
        previousElement.getParent().getContent().add(previousElementIndex + 1, element);
    }

    /**
     * Utility method to set an attribute on all elements matching the xPathExpression. Note that this method is
     * namespace aware and will require you to use the "xp" prefix in your XPath queries. For example, an XPath query
     * for a Spring XML configuration will look like this :
     * /xp:beans/xp:bean[@id="FileListSync"]/xp:property[@name="syncUrl"]
     * Currently there is no way to rename the prefix.
     *
     * @param scopeElement    the scope in which to execute the XPath query
     * @param xPathExpression the XPath query to select all the elements on which we want to set an attribute
     * @param attributeName   the name of the attribute to set on ALL the elements matching the XPath expression
     * @param attributeValue  the value of the attribute to set
     * @throws JDOMException raised if there was a problem manipulating the JDOM structure.
     */
    public void setElementAttribute(Element scopeElement, String xPathExpression, String attributeName, String attributeValue) throws JDOMException {
        XPath xPath = XPath.newInstance(xPathExpression);
        String namespaceURI = scopeElement.getDocument().getRootElement().getNamespaceURI();
        if ((namespaceURI != null) && (!"".equals(namespaceURI))) {
            xPath.addNamespace("xp", namespaceURI);
        }
        List<Element> elementList = (List<Element>) xPath.selectNodes(scopeElement);
        for (Element element : elementList) {
            element.setAttribute(attributeName, attributeValue);
        }
    }

    /**
     * Utility method to retrieve an XML element using an XPath expression. Note that this method is
     * namespace aware and will require you to use the "xp" prefix in your XPath queries. For example, an XPath query
     * for a Spring XML configuration will look like this :
     * /xp:beans/xp:bean[@id="FileListSync"]/xp:property[@name="syncUrl"]
     * Currently there is no way to rename the prefix.
     *
     * @param scopeElement    the scope in which to execute the XPath query
     * @param xPathExpression the XPath query to select the element we wish to retrieve. In the case where multiple
     *                        elements match, only the first one will be returned.
     * @return the first element that matches the XPath expression, or null if no element matches.
     * @throws JDOMException raised if there was a problem navigating the JDOM structure.
     */
    public Element getElement(Element scopeElement, String xPathExpression) throws JDOMException {
        return getElement(scopeElement, xPathExpression, null);
    }

    public Element getElement(Element scopeElement, String xPathExpression, Namespace namespace) throws JDOMException {
        XPathFactory instance = XPathFactory.instance();
        XPathExpression<Element> xPath;
        if (namespace != null) {
            xPath = instance.compile(xPathExpression, Filters.element(), null, namespace);
        } else {
            xPath = instance.compile(xPathExpression, Filters.element());
        }
        return xPath.evaluateFirst(scopeElement);
    }

    public List<Element> getElements(Element scopeElement, String xPathExpression) throws JDOMException {
        String namespaceURI = scopeElement.getDocument().getRootElement().getNamespaceURI();
        XPathFactory instance = XPathFactory.instance();
        Namespace namespace = null;
        if ((namespaceURI != null) && (!"".equals(namespaceURI))) {
            namespace = Namespace.getNamespace(namespaceURI);
        }
        XPathExpression<Element> xPath;
        if (namespace != null) {
            xPath = instance.compile(xPathExpression, Filters.element(), null, namespace);
        } else {
            xPath = instance.compile(xPathExpression, Filters.element());
        }

        return new LinkedList<>(xPath.evaluate(scopeElement));
    }

    /**
     * Removes matching element from the document.
     *
     * @param scopeElement    the root element to start search from
     * @param xPathExpression the XPath search expression
     * @throws JDOMException in case of an JDOM navigation error
     */
    protected void removeElementIfExists(Element scopeElement,
                                         String xPathExpression) throws JDOMException {
        Element el = getElement(scopeElement, xPathExpression);
        if (el != null) {
            el.getParent().removeContent(el);
        }
    }

    /**
     * Removes matching elements from the document.
     *
     * @param scopeElement    the root element to start search from
     * @param xPathExpression the XPath search expression
     * @throws JDOMException in case of an JDOM navigation error
     */
    protected void removeAllElements(Element scopeElement, String xPathExpression) throws JDOMException {
        for (Element el : getElements(scopeElement, xPathExpression)) {
            el.getParent().removeContent(el);
        }
    }

    /**
     * Write a JDom document to a file.
     *
     * <p>Characters will be encoded using UTF-8
     *
     * @param document    the document to write
     * @param destination the file to write to
     * @throws IOException if an error occurs while writing the document
     */
    protected final void write(Document document, File destination) throws IOException {
        String lineSeparator = System.getProperty("line.separator");
        Format format = Format.getPrettyFormat().setLineSeparator(lineSeparator);

        write(document, destination, format);
    }

    /**
     * Write a JDom document to a file.
     *
     * <P>Characters will be encoded using {@link Format#getEncoding()}
     *
     * @param document    the document to write
     * @param destination the file to write to
     * @param format      the format to apply
     * @throws IOException if an error occurs while writing the document
     */
    protected final void write(Document document, File destination, Format format) throws IOException {
        XMLOutputter outputter = new XMLOutputter(format);
        try (OutputStream out = new FileOutputStream(destination)) {
            outputter.output(document, out);
        }
    }

}
