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
        FileOutputStream out = new FileOutputStream(manifestFile);
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("org/jahia/utils/maven/plugin/osgi/MANIFEST.MF");
        try {
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
        mojo.inputManifestFile = manifestFile;
        mojo.execute();
    }
}
