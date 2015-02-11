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
