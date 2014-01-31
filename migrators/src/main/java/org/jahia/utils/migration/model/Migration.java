package org.jahia.utils.migration.model;

import org.jahia.commons.Version;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Base migration element
 */
@XmlType(name = "migration")
public class Migration {

    private String from;
    private String to;

    private Version fromVersion;
    private Version toVersion;

    private Set<MigrationResource> migrationResources = new LinkedHashSet<MigrationResource>();

    @XmlAttribute(name="from")
    public void setFrom(String from) {
        this.from = from;
        this.fromVersion = new Version(from);
    }

    @XmlAttribute(name="to")
    public void setTo(String to) {
        this.to = to;
        this.toVersion = new Version(to);
    }

    public Set<MigrationResource> getMigrationResources() {
        return migrationResources;
    }

    @XmlElementWrapper(name="resources")
    @XmlElement(name="resource")
    public void setMigrationResources(Set<MigrationResource> migrationResources) {
        this.migrationResources = migrationResources;
    }

    public Version getFromVersion() {
        return fromVersion;
    }

    public Version getToVersion() {
        return toVersion;
    }
}
