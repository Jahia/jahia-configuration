package org.jahia.utils.maven.plugin.osgi.parsers;

import org.jdom.Element;
import org.jdom.JDOMException;

/**
 * Spring application context file parser
 */
public class SpringContextXmlFileParser extends AbstractXmlFileParser {

    private final static String[] SPRING_XPATH_QUERIES = {
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
            "//osgi:reference/osgi:interfaces/beans:value",
            "//context:component-scan/@base-package",
    };

    @Override
    public boolean canParse(String fileName, Element rootElement) {
        return hasNamespaceURI(rootElement, "http://www.springframework.org/schema/beans");
    }

    @Override
    public void parse(String fileName, Element rootElement, ParsingContext parsingContext, boolean externalDependency) throws JDOMException {
        getLogger().debug("Processing Spring context file " + fileName + "...");

        getRefsUsingXPathQueries(fileName, rootElement, true, SPRING_XPATH_QUERIES, "beans", parsingContext);
    }
}
