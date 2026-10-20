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
 *     Enterprises Distribution - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.utils.maven.plugin.sbom;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link VendoredJsManifestParser}.
 */
public class VendoredJsManifestParserTest {

    private File getTestResource(String name) {
        URL resource = getClass().getResource(name);
        assertNotNull("Test resource not found: " + name, resource);
        return new File(resource.getFile());
    }

    @Test
    public void testParseBasicManifest() throws Exception {
        File manifest = getTestResource("vendored-js-test.yaml");
        VendoredJsManifest result = VendoredJsManifestParser.parse(manifest);

        assertNotNull(result);
        List<VendoredJsComponent> components = result.getComponents();
        assertNotNull(components);
        assertEquals(2, components.size());
    }

    @Test
    public void testParseComponentFields() throws Exception {
        File manifest = getTestResource("vendored-js-test.yaml");
        VendoredJsManifest result = VendoredJsManifestParser.parse(manifest);

        VendoredJsComponent jquery = result.getComponents().get(0);
        assertEquals("jquery", jquery.getName());
        assertEquals("3.6.0", jquery.getVersion());
        assertEquals("library", jquery.getType());
        assertEquals("pkg:npm/jquery@3.6.0", jquery.getPurl());
        assertEquals("jQuery Foundation", jquery.getSupplier());
        assertEquals("Copyright JS Foundation and other contributors", jquery.getCopyright());
        assertEquals("Minified jQuery library", jquery.getNotes());
        assertFalse(jquery.isModified());
    }

    @Test
    public void testParseLicenses() throws Exception {
        File manifest = getTestResource("vendored-js-test.yaml");
        VendoredJsManifest result = VendoredJsManifestParser.parse(manifest);

        VendoredJsComponent jquery = result.getComponents().get(0);
        List<String> licenses = jquery.getLicenses();
        assertNotNull(licenses);
        assertEquals(1, licenses.size());
        assertEquals("MIT", licenses.get(0));
    }

    @Test
    public void testParseFiles() throws Exception {
        File manifest = getTestResource("vendored-js-test.yaml");
        VendoredJsManifest result = VendoredJsManifestParser.parse(manifest);

        VendoredJsComponent jquery = result.getComponents().get(0);
        List<String> files = jquery.getFiles();
        assertNotNull(files);
        assertEquals(1, files.size());
        assertEquals("src/main/resources/javascript/jquery/jquery.min.js", files.get(0));
    }

    @Test
    public void testParseDirectoryReference() throws Exception {
        File manifest = getTestResource("vendored-js-test.yaml");
        VendoredJsManifest result = VendoredJsManifestParser.parse(manifest);

        VendoredJsComponent moment = result.getComponents().get(1);
        List<String> files = moment.getFiles();
        assertNotNull(files);
        assertEquals(1, files.size());
        assertEquals("src/main/resources/javascript/moment/", files.get(0));
        assertTrue(moment.isModified());
    }

    @Test
    public void testParseEmptyManifest() throws Exception {
        File manifest = getTestResource("vendored-js-empty.yaml");
        VendoredJsManifest result = VendoredJsManifestParser.parse(manifest);

        assertNotNull(result);
        assertNotNull(result.getComponents());
        assertTrue(result.getComponents().isEmpty());
    }
}
