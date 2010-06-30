package org.jahia.configuration.configurators;

import java.util.Map;

/**
 * Base configurator implementation. A configurator is responsible for configuring a sub-system of Jahia. It should
 * work with all versions of Jahia and applications servers, or declare that it isn't compatible with them.
 *
 * TODO add supported versions and supported servers structure so that configurators can declared their support.
 * 
 * User: loom
 * Date: Feb 13, 2009
 * Time: 3:29:37 PM
 */
public abstract class AbstractConfigurator {

    protected Map dbProperties;
    protected JahiaConfigInterface jahiaConfigInterface;

    public AbstractConfigurator(Map dbProps, JahiaConfigInterface jahiaConfigInterface) {
        this.dbProperties = dbProps;
        this.jahiaConfigInterface = jahiaConfigInterface;
    }

    public abstract void updateConfiguration(String sourceFileName, String destFileName) throws Exception;

    protected static String getValue(Map values, String key) {
        String replacement = (String) values.get(key);
        if (replacement == null) {
            return "";
        }
        replacement = replacement.trim();
        return replacement;
    }

    protected String getDBProperty(String key) {
        return getValue(dbProperties, key);
    }

}
