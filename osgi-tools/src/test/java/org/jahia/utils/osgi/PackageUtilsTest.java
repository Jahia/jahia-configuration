package org.jahia.utils.osgi;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Unit test for the package utils class
 */
public class PackageUtilsTest {

    @Test
    public void testGetPackageFromClassName() {
        String packageName = PackageUtils.getPackageFromClass("groovyx.net.http.Method.GET");
        Assert.assertEquals("Package name is not properly extracted", "groovyx.net.http", packageName);
        packageName = PackageUtils.getPackageFromClass("javax.jcr.*");
        Assert.assertEquals("Package name is not properly extracted", "javax.jcr", packageName);
    }

}
