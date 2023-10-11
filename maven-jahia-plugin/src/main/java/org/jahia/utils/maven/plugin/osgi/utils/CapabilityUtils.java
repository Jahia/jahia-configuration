/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2023 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.osgi.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.jahia.utils.maven.plugin.osgi.models.JahiaDepends;

import java.util.*;
import java.util.stream.Collectors;

import static org.jahia.utils.maven.plugin.osgi.utils.Constants.*;

/**
 * Utility class for handling and parsing of jahia-depends config
 * into OSGi Requires-Capability and Provide-Capability clauses in the Manifest file
 */
public class CapabilityUtils {

    static final String DELIMITER = "#"; // Anything except comma or semicolon

    public static void buildJahiaDependencies(MavenProject project, String jahiaDependsValue,
            Set<String> skipRequireDependencies, String prefix) throws MojoExecutionException {
        Properties projectProp = project.getProperties();
        projectProp.put(REQUIRE_CAPABILITY_PROJECT_PROP_KEY, "");
        projectProp.put(PROVIDE_CAPABILITY_PROJECT_PROP_KEY, "");

        if (StringUtils.isNotBlank(jahiaDependsValue)) {
            jahiaDependsValue = replaceDependsDelimiter(jahiaDependsValue);
            String requireCapabilities = Arrays.stream(jahiaDependsValue.split(DELIMITER))
                    .map(moduleId -> buildRequireCapabilities(moduleId, skipRequireDependencies))
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.joining(","));
            if (!requireCapabilities.isEmpty()) {
                projectProp.put(REQUIRE_CAPABILITY_PROJECT_PROP_KEY, prefix + requireCapabilities);
            }
        }

        /* Build provide capabilities */
        String version = JahiaDepends.toOsgiVersion(project.getVersion());
        String provideArtifactId = buildProvideCapabilities(project.getArtifactId(), version);
        String provideProjectName = buildProvideCapabilities(project.getName(), version);
        StringBuilder provideCapabilities = new StringBuilder(prefix).append(provideArtifactId);
        if (StringUtils.isNotEmpty(provideProjectName)) {
            provideCapabilities.append(',').append(provideProjectName);
        }
        projectProp.put(PROVIDE_CAPABILITY_PROJECT_PROP_KEY, provideCapabilities.toString());
    }

    public static String buildRequireCapabilities(String dependency, Set<String> skipRequireDependencies) {
        if (StringUtils.isBlank(dependency)) return "";

        JahiaDepends depends = new JahiaDepends(dependency);
        if (skipRequireDependencies.contains(depends.getModuleName())) return "";

        StringBuilder sb = new StringBuilder(OSGI_CAPABILITY_MODULE_DEPENDENCIES)
                .append(";filter:=\"").append(depends.toFilterString()).append("\"");
        if (depends.isOptional()) {
            sb.append(";resolution:=\"optional\"");
        }
        return sb.toString();
    }

    public static String buildProvideCapabilities(String dependency, String version) {
        if (StringUtils.isBlank(dependency)) return "";

        // e.g. com.jahia.modules.dependencies;moduleIdentifier="<id>";module-version:Version=<version>
        String prefix = String.format("%s;%s=\"", OSGI_CAPABILITY_MODULE_DEPENDENCIES,
                OSGI_CAPABILITY_MODULE_DEPENDENCIES_KEY);
        StringBuilder strBuilder = new StringBuilder(prefix).append(dependency).append('\"');
        if (StringUtils.isNotEmpty(version)) {
            strBuilder.append(';').append(OSGI_CAPABILITY_MODULE_DEPENDENCIES_VERSION_KEY)
                    .append(":Version=").append(version.trim());
        }
        return strBuilder.toString();
    }

    /**
     *
     * @return Replace dependency separator with DEPENDENCY_DELIMITER instead of comma
     * so it doesn't conflict with version range separator
     *
     * e.g. 'module1=[1.2,2),module2,module3=4'
     * to 'module1=[1.2,2);module2;module3=4'
     */
    public static String replaceDependsDelimiter(String dependsValue) {
        List<String> result = new ArrayList<>();
        StringTokenizer tokens = new StringTokenizer(dependsValue, ",");
        String nextToken = null;
        while (nextToken != null || tokens.hasMoreTokens()) {
            String token = (nextToken != null) ? nextToken : tokens.nextToken();
            nextToken = (tokens.hasMoreTokens()) ? tokens.nextToken() : null;
            if (!token.contains("=")) {
                result.add(token);
                continue;
            }

            String dependency = token;
            String[] deps = token.split("=");
            if (JahiaDepends.isOpenClause(deps[1])) {
                String nextTokenStr = (nextToken != null) ? nextToken : "";
                dependency = String.format("%s,%s", token, nextTokenStr);
                nextToken = null;
            }
            JahiaDepends.parse(dependency); // try to parse; will throw error if invalid format
            result.add(dependency);
        }
        return String.join(DELIMITER, result);
    }

}
