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

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link BomSerializer}.
 */
public class BomSerializerTest {

    @Test
    public void testSerializesToValidJson() throws Exception {
        BomModel bom = createMinimalBom();

        StringWriter writer = new StringWriter();
        BomSerializer.serializeToJson(bom, writer);

        String json = writer.toString();
        assertNotNull(json);
        assertFalse(json.trim().isEmpty());

        // Validate it is parseable JSON
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        assertNotNull(root);
    }

    @Test
    public void testSerializedJsonContainsSpecVersion() throws Exception {
        BomModel bom = createMinimalBom();

        StringWriter writer = new StringWriter();
        BomSerializer.serializeToJson(bom, writer);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(writer.toString());

        assertTrue("JSON must contain specVersion", root.has("specVersion"));
        assertEquals("1.4", root.get("specVersion").asText());
    }

    @Test
    public void testSerializedJsonContainsComponents() throws Exception {
        BomModel bom = createMinimalBom();
        ComponentModel component = new ComponentModel();
        component.setType("library");
        component.setName("test-lib");
        component.setVersion("1.0.0");
        bom.setComponents(Collections.singletonList(component));

        StringWriter writer = new StringWriter();
        BomSerializer.serializeToJson(bom, writer);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(writer.toString());

        assertTrue("JSON must contain components array", root.has("components"));
        assertTrue(root.get("components").isArray());
        assertEquals(1, root.get("components").size());
        assertEquals("test-lib", root.get("components").get(0).get("name").asText());
    }

    @Test
    public void testComponentLicenseSerializationStructure() throws Exception {
        BomModel bom = createMinimalBom();
        ComponentModel component = new ComponentModel();
        component.setType("library");
        component.setName("licensed-lib");
        component.setVersion("1.0.0");
        component.setLicenses(Arrays.asList(new ComponentModel.LicenseChoice("MIT")));
        bom.setComponents(Collections.singletonList(component));

        StringWriter writer = new StringWriter();
        BomSerializer.serializeToJson(bom, writer);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(writer.toString());

        JsonNode licenses = root.get("components").get(0).get("licenses");
        assertNotNull("Component must have licenses array", licenses);
        assertTrue(licenses.isArray());
        assertEquals(1, licenses.size());
        JsonNode licenseEntry = licenses.get(0).get("license");
        assertNotNull("License entry must have 'license' object", licenseEntry);
        assertEquals("MIT", licenseEntry.get("id").asText());
    }

    private BomModel createMinimalBom() {
        BomModel bom = new BomModel();
        bom.setBomVersion("1");
        bom.setSpecVersion("1.4");
        bom.setVersion(1);
        bom.setComponents(Collections.emptyList());
        return bom;
    }
}
