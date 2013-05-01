package org.jahia.utils.osgi.parsers.cnd;

import javax.jcr.Value;
import java.util.List;

/**
 * Defines a property value initializer.
 * User: toto
 * Date: Apr 3, 2008
 * Time: 4:09:58 PM
 */
public interface ValueInitializer {

    public Value[] getValues(ExtendedPropertyDefinition declaringPropertyDefinition, List<String> params);

}
