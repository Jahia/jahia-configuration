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

    @Test
    public void testFindPackageInMavenCentral() {

        List<String> artifacts = PackageUtils.findPackageInMavenCentral("com.mchange.v2.c3p0");

        if (artifacts.size() == 1 && artifacts.get(0).startsWith(PackageUtils.NETWORK_ERROR_PREFIX)) {
            System.out.println(artifacts.get(0) + ", won't run remaining tests...");
            return;
        }

        Assert.assertTrue("Package lookup result should not be empty !", artifacts.size()>0);
    }
}
