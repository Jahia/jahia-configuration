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
package org.jahia.utils.maven.plugin.osgi;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.poi.util.IOUtils;
import org.eclipse.osgi.util.ManifestElement;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * Test unit for the BuildFrameworkPackageListMojo
 */
public class BuildFrameworkPackageListMojoTest {

    @Test
    public void testPackageListBuilding() throws IOException, MojoFailureException, MojoExecutionException, Exception {
        BuildFrameworkPackageListMojo mojo = new BuildFrameworkPackageListMojo();
        String tmpDirLocation = System.getProperty("java.io.tmpdir");
        File tmpDirTestLocation = new File(tmpDirLocation, "test-" + System.currentTimeMillis());
        tmpDirTestLocation.mkdirs();
        File manifestFile = new File(tmpDirTestLocation, "MANIFEST.MF");
        File propertiesInputFile = new File(tmpDirTestLocation, "felix-framework.properties");
        File propertiesOutputFile = new File(tmpDirTestLocation, "felix-framework-generated.properties");
        copyClassLoaderResourceToFile("org/jahia/utils/maven/plugin/osgi/MANIFEST.MF", manifestFile);
        copyClassLoaderResourceToFile("org/jahia/utils/maven/plugin/osgi/felix-framework.properties", propertiesInputFile);
        mojo.inputManifestFile = manifestFile;
        mojo.propertiesInputFile = propertiesInputFile;
        mojo.propertiesOutputFile = propertiesOutputFile;
        List<String> manualPackageList = new ArrayList<String>();
        manualPackageList.add("javax.servlet;version=\"3.0\"");
        mojo.manualPackageList = manualPackageList;
        List<String> artifactExcludes = new ArrayList<String>();
        artifactExcludes.add("org.jahia.modules:*");
        artifactExcludes.add("org.jahia.templates:*");
        artifactExcludes.add("org.jahia.test:*");
        artifactExcludes.add("*.jahia.modules");
        mojo.artifactExcludes = artifactExcludes;
        List<String> packageExcludes = new ArrayList<String>();
        packageExcludes.add("groovy.grape*");
        packageExcludes.add("org.jahia.taglibs.*");
        packageExcludes.add("org.apache.taglibs.*");
        packageExcludes.add("javax.servlet.jsp*");
        mojo.packageExcludes = packageExcludes;
        mojo.execute();
        manifestFile.delete();
        propertiesInputFile.delete();
        Properties properties = new Properties();
        FileReader reader = new FileReader(propertiesOutputFile);
        try {
            properties.load(reader);
        } finally {
            IOUtils.closeQuietly(reader);
        }
        String systemPackagePropValue = properties.getProperty(mojo.propertyFilePropertyName);
        Assert.assertTrue("Couldn't find system package list property value ", systemPackagePropValue != null);
        Assert.assertTrue("System package list should not end with comma", systemPackagePropValue.charAt(systemPackagePropValue.length() - 1) != ',');
        ManifestElement[] manifestElements = ManifestElement.parseHeader("Export-Package", systemPackagePropValue);
        for (ManifestElement manifestElement : manifestElements) {
            String[] packageNames = manifestElement.getValueComponents();
            manifestElement.getAttribute("version");
            for (String packageName : packageNames) {
                Assert.assertTrue("Package should have been excluded", !packageName.contains("groovy.grape"));
                Assert.assertTrue("Package should have been excluded", !packageName.contains("javax.servlet.jsp"));
                Assert.assertTrue("Package should have been excluded", !packageName.contains("org.jahia.taglibs*"));
                // System.out.println(packageName + " version=" + version);
            }
        }
        Enumeration<?> propertyNames = properties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String propertyName = (String) propertyNames.nextElement();
            if (propertyName.contains(";version")) {
                Assert.assertTrue("Found property with ;version in it, probably a missing comma from another multi-valued property: " + propertyName, false);
            }
        }
        FileUtils.deleteDirectory(tmpDirTestLocation);
    }

    private void copyClassLoaderResourceToFile(String resourcePath, File manifestFile) throws IOException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            System.out.println("Couldn't find input class loader resource " + resourcePath);
            return;
        }
        FileOutputStream out = new FileOutputStream(manifestFile);
        try {
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }
}
