package org.jahia.utils.maven.plugin.buildautomation;

import java.util.Map;

/**
 * Base configurator implementation.
 * User: loom
 * Date: Feb 13, 2009
 * Time: 3:29:37 PM
 */
public class AbstractConfigurator {


    protected static String getValue(Map values, String key) {
        String replacement = (String) values.get(key);
        if (replacement == null) {
            return "";
        }
        replacement = replacement.replaceAll("&", "&amp;");
        replacement = replacement.trim();
        return replacement;
    }

}
