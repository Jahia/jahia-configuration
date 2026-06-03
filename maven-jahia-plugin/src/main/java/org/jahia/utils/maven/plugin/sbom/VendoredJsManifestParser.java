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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Parser for vendored-js.yaml manifest files.
 */
public class VendoredJsManifestParser {

    private VendoredJsManifestParser() {
        // Utility class, no instantiation needed
    }
    
    public static VendoredJsManifest parse(File manifestFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> data = mapper.readValue(new FileInputStream(manifestFile), new TypeReference<Map<String, Object>>() {});

        VendoredJsManifest manifest = new VendoredJsManifest();
        List<VendoredJsComponent> components = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> componentsList = (List<Map<String, Object>>) data.get("components");
        if (componentsList != null) {
            for (Map<String, Object> componentMap : componentsList) {
                VendoredJsComponent component = new VendoredJsComponent();
                component.setType(asString(componentMap.get("type")));
                component.setName(asString(componentMap.get("name")));
                component.setVersion(asString(componentMap.get("version")));
                component.setPurl(asString(componentMap.get("purl")));
                component.setSupplier(asString(componentMap.get("supplier")));
                component.setCopyright(asString(componentMap.get("copyright")));
                component.setNotes(asString(componentMap.get("notes")));

                Object modifiedObj = componentMap.get("modified");
                if (modifiedObj != null) {
                    component.setModified(Boolean.parseBoolean(modifiedObj.toString()));
                }

                List<String> licenses = asStringList(componentMap.get("licenses"));
                if (!licenses.isEmpty()) {
                    component.setLicenses(licenses);
                }

                List<String> files = asStringList(componentMap.get("files"));
                if (!files.isEmpty()) {
                    component.setFiles(files);
                }

                components.add(component);
            }
        }

        manifest.setComponents(components);
        return manifest;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static List<String> asStringList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }

        List<?> rawList = (List<?>) value;
        List<String> result = new ArrayList<>(rawList.size());
        for (Object item : rawList) {
            if (item != null) {
                result.add(item.toString());
            }
        }

        return result;
    }
}
