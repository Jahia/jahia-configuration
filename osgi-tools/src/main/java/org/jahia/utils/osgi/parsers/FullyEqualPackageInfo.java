/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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

import java.util.*;

/**
 * This class subclasses the PackageInfo class to implement more strict equals and hashcode implementations
 *
 */
public class FullyEqualPackageInfo extends PackageInfo {

    public FullyEqualPackageInfo(PackageInfo packageInfo) {
        this.name = packageInfo.name;
        this.version = packageInfo.version;
        this.optional = packageInfo.optional;
        this.sourceLocations = new TreeSet<String>(packageInfo.getSourceLocations());
        this.otherDirectives = new Properties(packageInfo.getOtherDirectives());
        this.origin = packageInfo.origin;
        this.embedded = packageInfo.embedded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PackageInfo)) return false;

        PackageInfo that = (PackageInfo) o;

        if (embedded != that.embedded) return false;
        if (optional != that.optional) return false;
        if (!name.equals(that.name)) return false;
        if (origin != null ? !origin.equals(that.origin) : that.origin != null) return false;
        if (otherDirectives != null ? !otherDirectives.equals(that.otherDirectives) : that.otherDirectives != null)
            return false;
        if (sourceLocations != null ? !sourceLocations.equals(that.sourceLocations) : that.sourceLocations != null)
            return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (optional ? 1 : 0);
        result = 31 * result + (sourceLocations != null ? sourceLocations.hashCode() : 0);
        result = 31 * result + (otherDirectives != null ? otherDirectives.hashCode() : 0);
        result = 31 * result + (origin != null ? origin.hashCode() : 0);
        result = 31 * result + (embedded ? 1 : 0);
        return result;
    }

    public static Set<FullyEqualPackageInfo> toFullyEqualPackageInfoSet(Set<PackageInfo> packageInfoSet, Set<FullyEqualPackageInfo> fullyEqualPackageInfos) {
        if (fullyEqualPackageInfos == null) {
            fullyEqualPackageInfos = new HashSet<FullyEqualPackageInfo>();
        }
        for (PackageInfo packageInfo : packageInfoSet) {
            fullyEqualPackageInfos.add(new FullyEqualPackageInfo(packageInfo));
        }
        return fullyEqualPackageInfos;
    }

}
