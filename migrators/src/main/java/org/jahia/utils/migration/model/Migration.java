/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
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
