package org.jahia.utils.maven.plugin.osgi.parsers.cnd;

import java.util.List;

import javax.jcr.Value;

/**
 * Defines a property value initializer.
 * User: toto
 * Date: Apr 3, 2008
 * Time: 4:09:58 PM
 */
public interface ValueInitializer {

    public Value[] getValues(ExtendedPropertyDefinition declaringPropertyDefinition, List<String> params);

}
