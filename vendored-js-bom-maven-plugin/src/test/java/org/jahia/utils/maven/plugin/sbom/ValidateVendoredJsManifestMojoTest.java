/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2026 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.sbom;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ValidateVendoredJsManifestMojo}.
 */
public class ValidateVendoredJsManifestMojoTest {

    private static final String MANIFEST = "{'bomFormat':'CycloneDX','specVersion':'1.4','version':1,'components':["
            + "{'type':'library','name':'jquery','components':[{'type':'file','name':'src/main/resources/javascript/jquery.min.js'}]},"
            + "{'type':'library','name':'moment','properties':[{'name':'jahia:source-folder','value':'src/main/resources/javascript/moment'}]}]}";

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    private File basedir;

    @Before
    public void setUp() throws Exception {
        basedir = tmpFolder.newFolder("project");
        writeFile("src/main/resources/javascript/jquery.min.js", "/* jquery */");
        writeFile("src/main/resources/javascript/moment/moment.js", "/* moment */");
        writeFile("src/main/resources/javascript/moment/locale/fr.js", "/* fr */");
    }

    private ValidateVendoredJsManifestMojo createMojo(String excludePatterns) throws Exception {
        return createMojo(excludePatterns, "js");
    }

    private ValidateVendoredJsManifestMojo createMojo(String excludePatterns, String fileExtensions) throws Exception {
        ValidateVendoredJsManifestMojo mojo = new ValidateVendoredJsManifestMojo();
        setField(mojo, "basedir", basedir);
        setField(mojo, "manifestFile", "vendored-js.cdx.json");
        setField(mojo, "vendoredDirectories", "src/main/resources/javascript,src/main/resources/css");
        setField(mojo, "fileExtensions", fileExtensions);
        setField(mojo, "excludePatterns", excludePatterns);
        setField(mojo, "skipIfNoManifest", true);
        return mojo;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testSkipsWhenManifestMissing() throws Exception {
        writeFile("src/main/resources/javascript/unlisted.js", "/* unlisted */");

        createMojo("").execute();
    }

    @Test
    public void testFilesAndFoldersCoverAllJsFiles() throws Exception {
        writeManifest(MANIFEST);

        createMojo("").execute();
    }

    @Test
    public void testUnreferencedFileFails() throws Exception {
        writeManifest(MANIFEST);
        writeFile("src/main/resources/javascript/unlisted.js", "/* unlisted */");

        try {
            createMojo("").execute();
            fail("Unlisted .js file must fail validation");
        } catch (MojoFailureException e) {
            assertTrue(e.getMessage().contains("vendored-js.cdx.json"));
        }
    }

    @Test
    public void testExcludedFileIsIgnored() throws Exception {
        writeManifest(MANIFEST);
        writeFile("src/main/resources/javascript/jquery.jahia.js", "/* jahia's own code */");

        createMojo("jquery.jahia.js").execute();
    }

    @Test
    public void testOnlyConfiguredExtensionsAreChecked() throws Exception {
        writeManifest(MANIFEST);
        writeFile("src/main/resources/css/unlisted.css", "/* unlisted */");
        writeFile("src/main/resources/javascript/player.swf", "FWS");

        createMojo("").execute();
    }

    @Test
    public void testOtherExtensionsAreCheckedWhenConfigured() throws Exception {
        writeManifest(MANIFEST);
        writeFile("src/main/resources/css/unlisted.css", "/* unlisted */");

        try {
            createMojo("", "js, .CSS").execute();
            fail("Unlisted .css file must fail validation when css is configured");
        } catch (MojoFailureException e) {
            assertTrue(e.getMessage().contains("vendored-js.cdx.json"));
        }
    }

    @Test
    public void testFolderCoversConfiguredExtensions() throws Exception {
        writeManifest(MANIFEST);
        writeFile("src/main/resources/javascript/moment/moment.css", "/* moment */");
        writeFile("src/main/resources/javascript/moment/player.SWF", "FWS");

        createMojo("", "js,css,swf").execute();
    }

    private void writeManifest(String json) throws IOException {
        writeFile("vendored-js.cdx.json", json.replace('\'', '"'));
    }

    private void writeFile(String path, String content) throws IOException {
        File file = new File(basedir, path);
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }
}
