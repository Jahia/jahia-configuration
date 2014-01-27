package org.jahia.utils.migration;

/**
 * Main singleton for migrators access
 */
public class Migrators {

    private static final Migrators instance = new Migrators();

    public Migrators() {
    }

    public static Migrators getInstance() {
        return instance;
    }

}
