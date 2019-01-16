/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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

import aQute.bnd.version.VersionRange;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jahia.utils.osgi.PackageUtils;

import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class containing metadata information about a detected package
 */
public class PackageInfo implements Comparable<PackageInfo> {

    protected String name = null;
    protected String version = null;
    protected boolean optional = false;
    @JsonIgnore protected Set<String> sourceLocations = new TreeSet<String>();
    protected Properties otherDirectives = new Properties();
    @JsonIgnore protected ParsingContext origin = null;
    @JsonIgnore protected boolean embedded = false;

    public PackageInfo() {
    }

    public PackageInfo(PackageInfo source) {
        this.name = source.name;
        this.version = source.version;
        this.optional = source.optional;
        this.sourceLocations = new TreeSet<String>(source.getSourceLocations());
        this.otherDirectives = new Properties(source.getOtherDirectives());
        this.origin = source.origin;
        this.embedded = source.embedded;
    }

    public PackageInfo(String name, String sourceLocation, ParsingContext parsingContext) {
        this.name = name;
        this.sourceLocations.add(sourceLocation);
        this.origin = parsingContext;
    }

    public PackageInfo(String name) {
        this.name = name;
    }

    public PackageInfo(String name, String version, boolean optional, String sourceLocation, ParsingContext parsingContext) {
        this.name = name;
        this.version = version;
        this.optional = optional;
        this.sourceLocations.add(sourceLocation);
        this.origin = parsingContext;
    }

    public PackageInfo(String name, String version, boolean optional, Set<String> sourceLocations, ParsingContext parsingContext) {
        this.name = name;
        this.version = version;
        this.optional = optional;
        this.sourceLocations = sourceLocations;
        this.origin = parsingContext;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public Set<String> getSourceLocations() {
        return sourceLocations;
    }

    public Properties getOtherDirectives() {
        return otherDirectives;
    }

    public ParsingContext getOrigin() {
        return origin;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean embedded) {
        this.embedded = embedded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PackageInfo that = (PackageInfo) o;

        if (!name.equals(that.name)) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;

        return true;
    }

    public boolean equalsIgnoreVersion(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PackageInfo that = (PackageInfo) o;

        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    public int compareTo(PackageInfo o) {
        int nameCompare = name.compareTo(o.name);
        if (nameCompare != 0) {
            return nameCompare;
        }
        if (version!= null) {
            if (o.version == null) {
                return 0; // if one of the packages doesn't have a version, it always matches with a version (OSGi version matching rules)
            }
            return version.compareTo(o.version);
        }
        return nameCompare;
    }

    public String toString(boolean withVersion) {
        StringBuilder stringBuilder = new StringBuilder(name);
        if (withVersion && version != null) {
            stringBuilder.append(";version=\"");
            stringBuilder.append(version);
            stringBuilder.append("\"");
        }
        if (optional) {
            stringBuilder.append(";resolution:=optional");
        }
        if (otherDirectives.size() > 0) {
            // not yet implemented.
        }
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public void setOrigin(ParsingContext origin) {
        this.origin = origin;
    }

    public boolean matches(PackageInfo targetPackage) {
        if (!name.equals(targetPackage.getName())) {
            return false;
        }
        if ((version != null && targetPackage.getVersion() == null) ||
                (version == null && targetPackage.getVersion() != null)) {
            // only one package specifies a version, so in that case they match !
            return true;
        }
        VersionRange versionRange = null;
        VersionRange targetVersionRange = null;
        if (version.contains(",")) {
            // if a comma is present in the version, we assume a clean OSGi range and don't need to clean it up.
            versionRange = new VersionRange(version);
        } else {
            // if no range is detected, let's cleanup version in case it's needed.
            versionRange = new VersionRange(PackageUtils.cleanupVersion(version));
        }
        if (targetPackage.getVersion().contains(",")) {
            // if a comma is present in the version, we assume a clean OSGi range and don't need to clean it up.
            targetVersionRange = new VersionRange(targetPackage.getVersion());
        } else {
            // if no range is detected, let's cleanup version in case it's needed.
            targetVersionRange = new VersionRange(PackageUtils.cleanupVersion(targetPackage.getVersion()));
        }

        VersionRange versionIntersection = PackageUtils.versionRangeIntersection(versionRange, targetVersionRange);
        if (versionIntersection != null) {
            return true;
        }
        return false;
    }
}
