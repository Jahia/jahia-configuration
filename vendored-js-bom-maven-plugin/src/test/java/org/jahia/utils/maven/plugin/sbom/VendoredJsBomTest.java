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
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link VendoredJsBom}.
 */
public class VendoredJsBomTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testLoadsExampleManifest() throws Exception {
        VendoredJsBom bom = VendoredJsBom.load(example());

        assertEquals(2, bom.getLibraries().size());
        ObjectNode jquery = bom.getLibraries().get(0);
        assertEquals(Collections.singletonList("src/main/resources/javascript/jquery/jquery.min.js"),
                VendoredJsBom.getFilePaths(jquery));
        assertTrue(VendoredJsBom.getFolderPaths(jquery).isEmpty());
        ObjectNode moment = bom.getLibraries().get(1);
        assertTrue(VendoredJsBom.getFilePaths(moment).isEmpty());
        assertEquals(Collections.singletonList("src/main/resources/javascript/moment/"), VendoredJsBom.getFolderPaths(moment));
    }

    @Test
    public void testOnlyFileComponentsAreFiles() throws Exception {
        VendoredJsBom bom = load("{'bomFormat':'CycloneDX','specVersion':'1.4','version':1,'components':["
                + "{'type':'library','name':'lib','components':[{'type':'file','name':'a.js'},"
                + "{'type':'library','name':'bundled-dependency'}],"
                + "'properties':[{'name':'other','value':'x'},{'name':'jahia:source-folder','value':'f1'},"
                + "{'name':'jahia:source-folder','value':'f2'}]}]}");

        ObjectNode library = bom.getLibraries().get(0);
        assertEquals(Collections.singletonList("a.js"), VendoredJsBom.getFilePaths(library));
        assertEquals(Arrays.asList("f1", "f2"), VendoredJsBom.getFolderPaths(library));
    }

    @Test
    public void testParseExtensions() {
        assertEquals(Arrays.asList(".js", ".css", ".swf"), VendoredJsBom.parseExtensions(" js, .CSS ,swf,"));
        assertEquals(Collections.singletonList(".js"), VendoredJsBom.parseExtensions(null));
        assertTrue(VendoredJsBom.hasExtension("Player.SWF", VendoredJsBom.parseExtensions("swf")));
        assertFalse(VendoredJsBom.hasExtension("lib.json", VendoredJsBom.parseExtensions("js")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsEmptyExtensions() {
        VendoredJsBom.parseExtensions(" , ");
    }

    @Test
    public void testEmptyComponentsAreAccepted() throws Exception {
        assertTrue(load("{'bomFormat':'CycloneDX','specVersion':'1.4','version':1}").getLibraries().isEmpty());
    }

    @Test
    public void testRejectsOtherFormat() throws Exception {
        assertInvalid("{'bomFormat':'SPDX','specVersion':'1.4','version':1}", "bomFormat");
    }

    @Test
    public void testRejectsOtherSpecVersion() throws Exception {
        assertInvalid("{'bomFormat':'CycloneDX','specVersion':'1.5','version':1}", "specVersion");
    }

    @Test
    public void testRejectsComponentWithoutName() throws Exception {
        assertInvalid("{'bomFormat':'CycloneDX','specVersion':'1.4','version':1,'components':[{'type':'library'}]}",
                "\"name\"");
    }

    @Test
    public void testRejectsFileComponentWithoutName() throws Exception {
        assertInvalid("{'bomFormat':'CycloneDX','specVersion':'1.4','version':1,'components':["
                + "{'type':'library','name':'lib','components':[{'type':'file'}]}]}", "its path");
    }

    @Test
    public void testRejectsNonObjectComponent() throws Exception {
        assertInvalid("{'bomFormat':'CycloneDX','specVersion':'1.4','version':1,'components':['jquery']}", "object");
    }

    private void assertInvalid(String json, String expectedMessage) throws Exception {
        try {
            load(json);
            fail("Manifest should be rejected: " + json);
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(expectedMessage));
        }
    }

    private VendoredJsBom load(String json) throws IOException {
        File file = tmpFolder.newFile();
        Files.write(file.toPath(), json.replace('\'', '"').getBytes(StandardCharsets.UTF_8));
        return VendoredJsBom.load(file);
    }

    private File example() throws URISyntaxException {
        return new File(getClass().getResource("vendored-js.cdx.json").toURI());
    }
}
