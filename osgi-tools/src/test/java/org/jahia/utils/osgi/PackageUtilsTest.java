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
import junit.framework.Assert;
import org.jahia.utils.osgi.parsers.PackageInfo;
import org.jahia.utils.osgi.parsers.ParsingContext;
import org.junit.Test;

import java.util.List;
import java.util.Set;

/**
 * Unit test for the package utils class
 */
public class PackageUtilsTest {

    @Test
    public void testGetPackageFromClassName() {
        String fileName = this.getClass().getName();
        String version = "1.0";

        ParsingContext parsingContext = new ParsingContext("org.jahia.server:jahia-maven-plugin", 0, 0, "PackageUtilsTest", "", "1.0", null);

        Set<PackageInfo> packagesName = PackageUtils.getPackagesFromClass("groovyx.net.http.Method.GET", false, version, fileName, parsingContext);
        Assert.assertTrue("Package name is not properly extracted", packagesName.contains(new PackageInfo("groovyx.net.http", version, false, fileName, parsingContext)));
        Assert.assertTrue("Package name is not properly extracted", packagesName.size() == 1);
        packagesName = PackageUtils.getPackagesFromClass("javax.jcr.*", false, version, fileName, parsingContext);
        Assert.assertTrue("Package name is not properly extracted", packagesName.contains(new PackageInfo("javax.jcr", version, false, fileName, parsingContext)));
        Assert.assertTrue("Package name is not properly extracted", packagesName.size() == 1);
        packagesName = PackageUtils.getPackagesFromClass("org.jahia.test1.generic.level1.RandomClass.RandomStaticGeneric<org.jahia.test1.generic.level2.RandomClass,org.jahia.test1.generic.level2.RandomGeneric<org.jahia.test1.generic.level3.RandomClass,org.jahia.test1.generic.level3.RandomClass>>", false, version , fileName, parsingContext);
        Assert.assertTrue("Package name is not properly extracted", packagesName.contains(new PackageInfo("org.jahia.test1.generic.level1", version, false, fileName, parsingContext)));
        Assert.assertTrue("Package name is not properly extracted", packagesName.contains(new PackageInfo("org.jahia.test1.generic.level2", version, false, fileName, parsingContext)));
        Assert.assertTrue("Package name is not properly extracted", packagesName.contains(new PackageInfo("org.jahia.test1.generic.level3", version, false, fileName, parsingContext)));
        Assert.assertTrue("Package name is not properly extracted", packagesName.size() == 3);
        packagesName = PackageUtils.getPackagesFromClass("org.jahia.utils.PaginatedList<org.jahia.services.notification.Subscription>", false, version, fileName, parsingContext);
        Assert.assertTrue("Package name is not properly extracted", packagesName.contains(new PackageInfo("org.jahia.utils", version, false, fileName, parsingContext)));
        Assert.assertTrue("Package name is not properly extracted", packagesName.contains(new PackageInfo("org.jahia.services.notification", version, false, fileName, parsingContext)));
        Assert.assertTrue("Package name is not properly extracted", packagesName.size() == 2);
    }

    @Test
    public void testVersionRangeIntersection() {
        VersionRange versionRangeIntersection = PackageUtils.versionRangeIntersection(new VersionRange("1.0.0.Final"), new VersionRange("3.0"));
        Assert.assertNull("Intersection should be null but isn't", versionRangeIntersection);
        versionRangeIntersection = PackageUtils.versionRangeIntersection(new VersionRange("1.0.0.Final"), new VersionRange("[3.0.0,4.0.0)"));
        Assert.assertNull("Intersection should be null but isn't", versionRangeIntersection);
        versionRangeIntersection = PackageUtils.versionRangeIntersection(new VersionRange("5.0.0.2"), new VersionRange("[3.0.0,4.0.0)"));
        Assert.assertNull("Intersection should be null but isn't", versionRangeIntersection);
        versionRangeIntersection = PackageUtils.versionRangeIntersection(new VersionRange("[3.0.0,4.0.0)"), new VersionRange("1.0.0.Final"));
        Assert.assertNull("Intersection should be null but isn't", versionRangeIntersection);
        versionRangeIntersection = PackageUtils.versionRangeIntersection(new VersionRange("[1.0,2.0)"), new VersionRange("[3.0.0,4.0.0)"));
        Assert.assertNull("Intersection should be null but isn't", versionRangeIntersection);
        versionRangeIntersection = PackageUtils.versionRangeIntersection(new VersionRange("[3.0.0,4.0.0)"), new VersionRange("[1.0,2.0)"));
        Assert.assertNull("Intersection should be null but isn't", versionRangeIntersection);
        versionRangeIntersection = PackageUtils.versionRangeIntersection(new VersionRange("[3.0.0,4.0.0)"), new VersionRange("[4.0,5.0)"));
        Assert.assertNull("Intersection should be null but isn't", versionRangeIntersection);

        versionRangeIntersection = PackageUtils.versionRangeIntersection(new VersionRange("3.5"), new VersionRange("[3.0.0,4.0.0)"));
        Assert.assertTrue("Intersection doesn't have expected value", !versionRangeIntersection.isRange() &&
                versionRangeIntersection.getLow().equals(new Version("3.5")));
        versionRangeIntersection = PackageUtils.versionRangeIntersection(new VersionRange("[3.0.0,4.0.0)"), new VersionRange("3.5"));
        Assert.assertTrue("Intersection doesn't have expected value", !versionRangeIntersection.isRange() &&
                versionRangeIntersection.getLow().equals(new Version("3.5")));
        versionRangeIntersection = PackageUtils.versionRangeIntersection(new VersionRange("[3.0.0,4.0.0]"), new VersionRange("[4.0,5.0)"));
        Assert.assertTrue("Intersection doesn't have expected value", !versionRangeIntersection.isRange() &&
                versionRangeIntersection.getLow().equals(new Version("4.0")));
        versionRangeIntersection = PackageUtils.versionRangeIntersection(new VersionRange("[3.0.0,4.0.0)"), new VersionRange("[3.5.0,5.0)"));
        Assert.assertTrue("Intersection doesn't have expected value", versionRangeIntersection.isRange() &&
                        versionRangeIntersection.getLow().equals(new Version("3.5")) &&
                        versionRangeIntersection.includeLow() &&
                        versionRangeIntersection.getHigh().equals(new Version("4.0")) &&
                        !versionRangeIntersection.includeHigh()
        );
        versionRangeIntersection = PackageUtils.versionRangeIntersection(new VersionRange("[1.0.0,4.0.0)"), new VersionRange("[2.0,3.0)"));
        Assert.assertTrue("Intersection doesn't have expected value", versionRangeIntersection.isRange() &&
                        versionRangeIntersection.getLow().equals(new Version("2.0")) &&
                        versionRangeIntersection.includeLow() &&
                        versionRangeIntersection.getHigh().equals(new Version("3.0")) &&
                        !versionRangeIntersection.includeHigh()
        );
        versionRangeIntersection = PackageUtils.versionRangeIntersection(new VersionRange("[2.0,3.0)"), new VersionRange("[1.0.0,4.0.0)"));
        Assert.assertTrue("Intersection doesn't have expected value", versionRangeIntersection.isRange() &&
                        versionRangeIntersection.getLow().equals(new Version("2.0")) &&
                        versionRangeIntersection.includeLow() &&
                        versionRangeIntersection.getHigh().equals(new Version("3.0")) &&
                        !versionRangeIntersection.includeHigh()
        );
    }

//    @Test
//    public void testFindPackageInMavenCentral() {
//
//        List<String> artifacts = CheckDependenciesMojo.findPackageInMavenCentral("com.mchange.v2.c3p0");
//
//        if (artifacts.size() == 1 && artifacts.get(0).startsWith(PackageUtils.NETWORK_ERROR_PREFIX)) {
//            System.out.println(artifacts.get(0) + ", won't run remaining tests...");
//            return;
//        }
//
//        Assert.assertTrue("Package lookup result should not be empty !", artifacts.size()>0);
//    }
}
