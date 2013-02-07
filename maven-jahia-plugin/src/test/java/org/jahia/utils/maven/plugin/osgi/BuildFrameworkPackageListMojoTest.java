package org.jahia.utils.maven.plugin.osgi;

import junit.framework.Assert;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.poi.util.IOUtils;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.osgi.util.ManifestElement;
import org.junit.Test;
import org.osgi.framework.BundleException;

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
    public void testPackageListBuilding() throws IOException, MojoFailureException, MojoExecutionException, BundleException {
        BuildFrameworkPackageListMojo mojo = new BuildFrameworkPackageListMojo();
        String tmpDirLocation = System.getProperty("java.io.tmpdir");
        File tmpDirFile = new File(tmpDirLocation);
        File manifestFile = new File(tmpDirFile, "MANIFEST.MF");
        File propertiesInputFile = new File(tmpDirFile, "felix-framework.properties");
        File propertiesOutputFile = new File(tmpDirFile, "felix-framework-generated.properties");
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
        packageExcludes.add("groovy.grape.*");
        packageExcludes.add("org.jahia.taglibs.*");
        packageExcludes.add("org.apache.taglibs.*");
        packageExcludes.add("javax.servlet.jsp.*");
        mojo.packageExcludes = packageExcludes;
        mojo.execute();
        manifestFile.delete();
        propertiesInputFile.delete();
        Properties properties = new Properties();
        properties.load(new FileReader(propertiesOutputFile));
        String systemPackagePropValue = properties.getProperty(mojo.propertyFilePropertyName);
        Assert.assertTrue("Couldn't find system package list property value ", systemPackagePropValue != null);
        Assert.assertTrue("System package list should not end with comma", systemPackagePropValue.charAt(systemPackagePropValue.length() - 1) != ',');
        ManifestElement[] manifestElements = ManifestElement.parseHeader("Export-Package", systemPackagePropValue);
        for (ManifestElement manifestElement : manifestElements) {
            String[] packageNames = manifestElement.getValueComponents();
            String version = manifestElement.getAttribute("version");
            for (String packageName : packageNames) {
                Assert.assertTrue("Package should have been excluded", !packageName.contains("groovy.grape"));
                Assert.assertTrue("Package should have been excluded", !packageName.contains("javax.servlet.jsp"));
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
    }

    private void copyClassLoaderResourceToFile(String resourcePath, File manifestFile) throws IOException {
        FileOutputStream out = new FileOutputStream(manifestFile);
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            System.out.println("Couldn't find input class loader resource " + resourcePath);
            return;
        }
        try {
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }
}
