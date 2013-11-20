package org.jahia.utils.osgi;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * Unit test for the package utils class
 */
public class PackageUtilsTest {

    @Test
    public void testGetPackageFromClassName() {
        Set<String> packagesName = PackageUtils.getPackagesFromClass("groovyx.net.http.Method.GET");
        Assert.assertTrue("Package name is not properly extracted", packagesName.contains("groovyx.net.http"));
        Assert.assertTrue("Package name is not properly extracted", packagesName.size() == 1);
        packagesName = PackageUtils.getPackagesFromClass("javax.jcr.*");
        Assert.assertTrue("Package name is not properly extracted", packagesName.contains("javax.jcr"));
        Assert.assertTrue("Package name is not properly extracted", packagesName.size() == 1);
        packagesName = PackageUtils.getPackagesFromClass("org.jahia.test1.generic.level1.RandomClass.RandomStaticGeneric<org.jahia.test1.generic.level2.RandomClass,org.jahia.test1.generic.level2.RandomGeneric<org.jahia.test1.generic.level3.RandomClass,org.jahia.test1.generic.level3.RandomClass>>");
        Assert.assertTrue("Package name is not properly extracted", packagesName.contains("org.jahia.test1.generic.level1"));
        Assert.assertTrue("Package name is not properly extracted", packagesName.contains("org.jahia.test1.generic.level2"));
        Assert.assertTrue("Package name is not properly extracted", packagesName.contains("org.jahia.test1.generic.level3"));
        Assert.assertTrue("Package name is not properly extracted", packagesName.size() == 3);
        packagesName = PackageUtils.getPackagesFromClass("org.jahia.utils.PaginatedList<org.jahia.services.notification.Subscription>");
        Assert.assertTrue("Package name is not properly extracted", packagesName.contains("org.jahia.utils"));
        Assert.assertTrue("Package name is not properly extracted", packagesName.contains("org.jahia.services.notification"));
        Assert.assertTrue("Package name is not properly extracted", packagesName.size() == 2);
    }

}
