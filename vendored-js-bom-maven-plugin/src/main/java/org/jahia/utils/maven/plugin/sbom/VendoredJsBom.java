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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Hand-written, partial CycloneDX 1.4 BOM describing vendored JavaScript libraries (vendored-js.cdx.json).
 * <p>
 * Each top-level component is a vendored library. Its files are declared as nested components of type
 * {@code file} whose {@code name} is the path relative to the project basedir. A whole folder is declared
 * with a {@value #SOURCE_FOLDER_PROPERTY} property on the library; it covers every {@code .js} file below it.
 * Everything else is plain CycloneDX and is passed through untouched by the generator.
 */
public final class VendoredJsBom {

    public static final String SPEC_VERSION = "1.4";
    public static final String SOURCE_FOLDER_PROPERTY = "jahia:source-folder";
    public static final String FILE_TYPE = "file";

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final File source;
    private final ObjectNode root;

    private VendoredJsBom(File source, ObjectNode root) {
        this.source = source;
        this.root = root;
    }

    /**
     * Reads the manifest and checks the structure both goals rely on. Full schema validation is not done here.
     */
    public static VendoredJsBom load(File file) throws IOException {
        JsonNode tree = MAPPER.readTree(file);
        if (tree == null || !tree.isObject()) {
            throw invalid(file, "the document must be a JSON object");
        }
        VendoredJsBom bom = new VendoredJsBom(file, (ObjectNode) tree);
        bom.checkStructure();
        return bom;
    }

    private void checkStructure() throws IOException {
        if (!"CycloneDX".equals(root.path("bomFormat").asText(null))) {
            throw invalid(source, "\"bomFormat\" must be \"CycloneDX\"");
        }
        if (!SPEC_VERSION.equals(root.path("specVersion").asText(null))) {
            throw invalid(source, "\"specVersion\" must be \"" + SPEC_VERSION + "\"");
        }
        JsonNode components = root.get("components");
        if (components != null && !components.isArray()) {
            throw invalid(source, "\"components\" must be an array");
        }
        if (components != null && getLibraries().size() != components.size()) {
            throw invalid(source, "every entry of \"components\" must be an object");
        }
        for (ObjectNode library : getLibraries()) {
            String name = library.path("name").asText("");
            if (name.trim().isEmpty() || library.path("type").asText("").trim().isEmpty()) {
                throw invalid(source, "every component needs a \"type\" and a \"name\"");
            }
            JsonNode children = library.get("components");
            if (children != null && !children.isArray()) {
                throw invalid(source, "\"components\" of " + name + " must be an array");
            }
            for (ObjectNode file : getFileComponents(library)) {
                if (file.path("name").asText("").trim().isEmpty()) {
                    throw invalid(source, "every file component of " + name + " needs a \"name\" (its path)");
                }
            }
        }
    }

    private static IOException invalid(File file, String reason) {
        return new IOException("Invalid vendored JS manifest " + file + ": " + reason);
    }

    public ObjectNode getRoot() {
        return root;
    }

    /**
     * @return the top-level components, i.e. the vendored libraries
     */
    public List<ObjectNode> getLibraries() {
        return objects(root.get("components"));
    }

    /**
     * @return the nested components of type {@code file} declared on the library
     */
    public static List<ObjectNode> getFileComponents(ObjectNode library) {
        List<ObjectNode> files = new ArrayList<>();
        for (ObjectNode child : objects(library.get("components"))) {
            if (FILE_TYPE.equals(child.path("type").asText(null))) {
                files.add(child);
            }
        }
        return files;
    }

    public static List<String> getFilePaths(ObjectNode library) {
        List<String> paths = new ArrayList<>();
        for (ObjectNode file : getFileComponents(library)) {
            paths.add(file.path("name").asText());
        }
        return paths;
    }

    /**
     * @return the values of the {@value #SOURCE_FOLDER_PROPERTY} properties declared on the library
     */
    public static List<String> getFolderPaths(ObjectNode library) {
        List<String> paths = new ArrayList<>();
        for (ObjectNode property : objects(library.get("properties"))) {
            String value = property.path("value").asText("");
            if (SOURCE_FOLDER_PROPERTY.equals(property.path("name").asText(null)) && !value.trim().isEmpty()) {
                paths.add(value);
            }
        }
        return paths;
    }

    public static boolean hasProperty(ObjectNode component, String name) {
        for (ObjectNode property : objects(component.get("properties"))) {
            if (name.equals(property.path("name").asText(null))) {
                return true;
            }
        }
        return false;
    }

    public void write(File file) throws IOException {
        MAPPER.writeValue(file, root);
    }

    static ObjectNode newObject() {
        return MAPPER.createObjectNode();
    }

    static ObjectNode getOrCreateObject(ObjectNode parent, String field) {
        JsonNode node = parent.get(field);
        if (node instanceof ObjectNode) {
            return (ObjectNode) node;
        }
        return parent.putObject(field);
    }

    static ArrayNode getOrCreateArray(ObjectNode parent, String field) {
        JsonNode node = parent.get(field);
        if (node instanceof ArrayNode) {
            return (ArrayNode) node;
        }
        return parent.putArray(field);
    }

    private static List<ObjectNode> objects(JsonNode array) {
        if (array == null || !array.isArray()) {
            return Collections.emptyList();
        }
        List<ObjectNode> result = new ArrayList<>();
        for (JsonNode node : array) {
            if (node instanceof ObjectNode) {
                result.add((ObjectNode) node);
            }
        }
        return result;
    }
}
