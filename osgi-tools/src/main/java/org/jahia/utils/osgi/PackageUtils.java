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
package org.jahia.utils.osgi;

import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import org.jahia.utils.osgi.parsers.PackageInfo;
import org.jahia.utils.osgi.parsers.ParsingContext;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Library of utilities to handle package names
 */
public class PackageUtils {

    /**
     * The left endpoint is open and is excluded from the range.
     * <p/>
     * The value of {@code LEFT_OPEN} is {@code '('}.
     */
    public static final char LEFT_OPEN = '(';
    /**
     * The left endpoint is closed and is included in the range.
     * <p/>
     * The value of {@code LEFT_CLOSED} is {@code '['}.
     */
    public static final char LEFT_CLOSED = '[';
    /**
     * The right endpoint is open and is excluded from the range.
     * <p/>
     * The value of {@code RIGHT_OPEN} is {@code ')'}.
     */
    public static final char RIGHT_OPEN = ')';
    /**
     * The right endpoint is closed and is included in the range.
     * <p/>
     * The value of {@code RIGHT_CLOSED} is {@code ']'}.
     */
    public static final char RIGHT_CLOSED = ']';
    public static final String NETWORK_ERROR_PREFIX = "NETWORK ERROR: ";
    public static final String MAVEN_SEARCH_HOST_URL = "http://search.maven.org";

    public static Set<PackageInfo> getPackagesFromClass(final String fqnClassName, boolean optionalDependency, String version, String sourceLocation, ParsingContext parsingContext) {
        Set<PackageInfo> packages = new HashSet<PackageInfo>();
        // Split for generics
        String[] classNames = fqnClassName.split("<|>|,");
        for (String className : classNames) {
            // remove all blank space in class name
            className = className.replaceAll("\\s+", "");
            String[] classNameParts = className.split("\\.");
            if (classNameParts.length > 1 &&
                    classNameParts[classNameParts.length - 2].length() > 0 &&
                    Character.isUpperCase(classNameParts[classNameParts.length - 2].charAt(0))) {
                // we found a static import, we will return all parts except the last two.
                StringBuilder packageNameBuilder = new StringBuilder();
                for (int i = 0; i < classNameParts.length - 2; i++) {
                    packageNameBuilder.append(classNameParts[i]);
                    if (i < classNameParts.length - 3) {
                        packageNameBuilder.append(".");
                    }
                }
                if (packageNameBuilder.length() > 0) {
                    packages.add(new PackageInfo(packageNameBuilder.toString(), version, optionalDependency, sourceLocation, parsingContext));
                    continue;
                } else {
                    continue;
                }
            }
            int lastDot = className.lastIndexOf(".");
            if (lastDot < 0) {
                continue;
            }
            packages.add(new PackageInfo(className.substring(0, lastDot), version, optionalDependency, sourceLocation, parsingContext));
        }
        return packages;
    }

    public static boolean containsIgnoreVersion(Collection<PackageInfo> packages, PackageInfo targetPackage) {
        for (PackageInfo packageInfo : packages) {
            if (packageInfo.getName().equals(targetPackage.getName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsMatchingVersion(Collection<PackageInfo> packages, PackageInfo targetPackage) {
        for (PackageInfo packageInfo : packages) {
            if (packageInfo.matches(targetPackage)) {
                return true;
            }
        }
        return false;
    }

    public static int removeMatchingVersions(Collection<PackageInfo> packages, PackageInfo targetPackage) {
        int matchCount = 0;
        List<PackageInfo> packagesToRemove = new ArrayList<PackageInfo>();
        for (PackageInfo packageInfo : packages) {
            if (packageInfo.matches(targetPackage)) {
                packagesToRemove.add(packageInfo);
            }
        }
        for (PackageInfo packageToRemove : packagesToRemove) {
            if (packages.remove(packageToRemove)) {
                matchCount++;
            }
        }
        return matchCount;
    }

    /**
     * Returns the intersection of this version range with the specified version
     * ranges.
     *
     * @return A version range representing the intersection of this version
     * range and the specified version ranges. If no version ranges are
     * specified, then this version range is returned.
     */
    public static VersionRange versionRangeIntersection(final VersionRange versionRange1, final VersionRange versionRange2) {
        if (versionRange1 == null || versionRange2 == null) {
            return null;
        }

        if (!versionRange1.isRange() && !versionRange2.isRange()) {
            if (versionRange1.getLow().equals(versionRange2.getLow())) {
                return new VersionRange(versionRange1.toString());
            } else {
                return null;
            }
        }

        VersionRange lowVersionRange = versionRange1;
        VersionRange highVersionRange = versionRange2;
        int comparison = lowVersionRange.getLow().compareTo(versionRange2.getLow());
        if (comparison > 0) {
            lowVersionRange = versionRange2;
            highVersionRange = versionRange1;
        }

        VersionRange newVersionRange = null;
        // prime with data from this version range

        boolean includesLow = lowVersionRange.includeLow();
        boolean includesHigh = lowVersionRange.includeHigh();
        Version endpointLow = lowVersionRange.getLow();
        Version endpointHigh = lowVersionRange.getHigh();

        if (highVersionRange.isRange()) {
            comparison = endpointLow.compareTo(highVersionRange.getLow());
            if (comparison == 0) {
                includesLow = includesLow && highVersionRange.includeLow();
            } else {
                if (comparison < 0) { // move endpointLeft to the right
                    endpointLow = highVersionRange.getLow();
                    includesLow = highVersionRange.includeLow();
                } else {

                }
            }
        }
        if (!highVersionRange.getHigh().equals(highVersionRange.getLow())) {
            if (endpointHigh.equals(lowVersionRange.getLow())) {
                endpointHigh = highVersionRange.getHigh();
                includesHigh = highVersionRange.includeHigh();
                newVersionRange = getVersionRange(includesLow, includesHigh, endpointLow, endpointHigh);
                if (!newVersionRange.includes(lowVersionRange.getLow())) {
                    return null;
                }
            } else {
                comparison = endpointHigh.compareTo(highVersionRange.getHigh());
                if (comparison == 0) {
                    includesHigh = includesHigh && highVersionRange.includeHigh();
                } else {
                    if (comparison > 0) { // move endpointRight to the left
                        endpointHigh = highVersionRange.getHigh();
                        includesHigh = highVersionRange.includeHigh();
                    } else {
                        comparison = endpointHigh.compareTo(highVersionRange.getLow());
                        if (comparison == 0) {
                            if (includesHigh && highVersionRange.includeLow()) {
                                // don't change anything, values are alright correct
                            } else {
                                // both ranges share border but at least one of them excludes it -> no intersection
                                return null;
                            }
                        } else if (comparison < 0) {
                            // ranges do not overlap !
                            return null;
                        }
                    }
                }
            }
        } else {
            newVersionRange = getVersionRange(includesLow, includesHigh, endpointLow, endpointHigh);
            if (!newVersionRange.includes(highVersionRange.getLow())) {
                return null;
            } else {
                return new VersionRange(highVersionRange.toString());
            }
        }

        if (newVersionRange == null) {
            newVersionRange = getVersionRange(includesLow, includesHigh, endpointLow, endpointHigh);
        }
        return newVersionRange;
    }

    public static VersionRange getVersionRange(boolean includesLow, boolean includesHigh, Version endpointLow, Version endpointHigh) {
        if (endpointLow.equals(endpointHigh)) {
            return new VersionRange(endpointLow.toString());
        } else {
            return new VersionRange((includesLow ? LEFT_CLOSED : LEFT_OPEN) + endpointLow.toString() + "," + endpointHigh.toString() + (includesHigh ? RIGHT_CLOSED : RIGHT_OPEN));
        }
    }

    /**
     * Clean up version parameters. Other builders use more fuzzy definitions of
     * the version syntax. This method cleans up such a version to match an OSGi
     * version.
     *
     * @param VERSION_STRING
     * @return
     */
    static final Pattern FUZZY_VERSION = Pattern.compile("(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9](.*))?",
            Pattern.DOTALL);


    static public String cleanupVersion(String version) {
        StringBuffer result = new StringBuffer();
        Matcher m = FUZZY_VERSION.matcher(version);
        if (m.matches()) {
            String major = m.group(1);
            String minor = m.group(3);
            String micro = m.group(5);
            String qualifier = m.group(7);

            if (major != null) {
                result.append(major);
                if (minor != null) {
                    result.append(".");
                    result.append(minor);
                    if (micro != null) {
                        result.append(".");
                        result.append(micro);
                        if (qualifier != null) {
                            result.append(".");
                            cleanupModifier(result, qualifier);
                        }
                    } else if (qualifier != null) {
                        result.append(".0.");
                        cleanupModifier(result, qualifier);
                    } else {
                        result.append(".0");
                    }
                } else if (qualifier != null) {
                    result.append(".0.0.");
                    cleanupModifier(result, qualifier);
                } else {
                    result.append(".0.0");
                }
            }
        } else {
            result.append("0.0.0.");
            cleanupModifier(result, version);
        }
        return result.toString();
    }


    static void cleanupModifier(StringBuffer result, String modifier) {
        for (int i = 0; i < modifier.length(); i++) {
            char c = modifier.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'
                    || c == '-')
                result.append(c);
            else
                result.append('_');
        }
    }

    public static String getPackageSearchUrl(String packageName) {
        return "http://search.maven.org/#search|ga|1|fc:\""+packageName+"\"";
    }

}
