package org.jahia.utils.maven.plugin.osgi;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.poi.util.IOUtils;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

import java.io.*;

/**
 * Test unit for the BuildFrameworkPackageListMojo
 */
public class BuildFrameworkPackageListMojoTest {

    @Test
    public void testPackageListBuilding() throws IOException, MojoFailureException, MojoExecutionException {
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
        mojo.execute();
        manifestFile.delete();
        propertiesInputFile.delete();
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
