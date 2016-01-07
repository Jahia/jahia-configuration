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
package org.jahia.utils.osgi.parsers;

import org.jdom2.Element;
import org.jdom2.JDOMException;

/**
 * Spring application context file parser
 */
public class SpringContextXmlFileParser extends AbstractXmlFileParser {

    private final static String[] SPRING_XPATH_CLASSNAME_QUERIES = {
            "//beans:bean/@class",
            "//aop:declare-parents/@implement-interface",
            "//aop:declare-parents/@default-impl",
            "//context:load-time-weaver/@weaver-class",
            "//context:component-scan/@name-generator",
            "//context:component-scan/@scope-resolver",
            "//jee:jndi-lookup/@expected-type",
            "//jee:jndi-lookup/@proxy-interface",
            "//jee:remote-slsb/@home-interface",
            "//jee:remote-slsb/@business-interface",
            "//jee:local-slsb/@business-interface",
            "//jms:listener-container/@container-class",
            "//lang:jruby/@script-interfaces",
            "//lang:bsh/@script-interfaces",
            "//oxm:class-to-be-bound/@name",
            "//oxm:jibx-marshaller/@target-class",
            "//osgi:reference/@interface",
            "//osgi:service/@interface",
            "//util:list/@list-class",
            "//util:map/@map-class",
            "//util:set/@set-class",
            "//webflow:flow-builder/@class",
            "//webflow:attribute/@type",
            "//osgi:service/osgi:interfaces/beans:value",
            "//osgi:reference/osgi:interfaces/beans:value"
    };

    private final static String[] SPRING_XPATH_PACKAGE_QUERIES = {
            "//context:component-scan/@base-package",
    };

    @Override
    public boolean canParse(String fileName, Element rootElement) {
        return hasNamespaceURI(rootElement, "http://www.springframework.org/schema/beans");
    }

    @Override
    public void parse(String fileName, Element rootElement, String fileParent, boolean externalDependency, boolean optionalDependency, String version, ParsingContext parsingContext) throws JDOMException {
        getLogger().debug("Processing Spring context file " + fileName + "...");

        getRefsUsingXPathQueries(fileName, rootElement, true, false, SPRING_XPATH_CLASSNAME_QUERIES, "beans", fileParent, version, optionalDependency, parsingContext);
        getRefsUsingXPathQueries(fileName, rootElement, false, true, SPRING_XPATH_PACKAGE_QUERIES, "beans", fileParent, version, optionalDependency, parsingContext);
    }
}
