package org.jahia.utils.migration.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * Root element for all migrations
 */
@XmlRootElement(name="migrations")
@XmlType(name="migrations")
public class Migrations {

    private List<Migration> migrations = new ArrayList<Migration>();

    public List<Migration> getMigrations() {
        return migrations;
    }

    @XmlElement(name="migration")
    public void setMigrations(List<Migration> migrations) {
        this.migrations = migrations;
    }
}
