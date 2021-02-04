/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2021 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.utils.maven.plugin.osgi.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.utils.version.VersionRange;
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

    static final String DELIMITER = ";";

    public static void buildJahiaDependencies(MavenProject project, String jahiaDependsValue,
            Set<String> skipRequireDependencies, String prefix) throws MojoExecutionException {
        Properties projectProp = project.getProperties();
        projectProp.put(REQUIRE_CAPABILITY_PROJECT_PROP_KEY, "");
        projectProp.put(PROVIDE_CAPABILITY_PROJECT_PROP_KEY, "");

        if (StringUtils.isNotBlank(jahiaDependsValue)) {
            jahiaDependsValue = replaceDependsDelimiter(jahiaDependsValue);
            String requireCapabilities = Arrays.stream(jahiaDependsValue.split(DELIMITER))
                    .map(moduleId -> buildRequireCapabilities(moduleId, skipRequireDependencies))
                    .filter(dep -> StringUtils.isNotEmpty(dep))
                    .collect(Collectors.joining(","));
            if (!requireCapabilities.isEmpty()) {
                projectProp.put(REQUIRE_CAPABILITY_PROJECT_PROP_KEY, prefix + requireCapabilities);
            }
        }

        /* Build provide capabilities */
        String version = toOsgiVersion(project.getVersion());
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

        StringBuilder strBuilder = new StringBuilder(OSGI_CAPABILITY_MODULE_DEPENDENCIES).append(";filter:=\"(");
        String nameFilter = String.format("%s=%s", OSGI_CAPABILITY_MODULE_DEPENDENCIES_KEY, depends.getModuleName());
        if (!depends.hasVersion()) {
            // e.g. com.jahia.modules.dependencies;filter:="(moduleIdentifier=<moduleName>)"
            strBuilder.append(nameFilter);
        } else {
            // e.g. com.jahia.modules.dependencies;filter:="(&(moduleIdentifier=<moduleName>)(moduleVersion>=<minVersion>)
            // (moduleVersion<=<maxVersion>))"
            strBuilder.append(String.format("&(%s)", nameFilter));
            if (depends.hasMinVersion()) {
                strBuilder.append('(').append(OSGI_CAPABILITY_MODULE_DEPENDENCIES_VERSION_KEY)
                        .append(">=").append(depends.getMinVersion()).append(')');
            }
            if (depends.hasMaxVersion()) {
                strBuilder.append('(').append(OSGI_CAPABILITY_MODULE_DEPENDENCIES_VERSION_KEY)
                        .append("<=").append(depends.getMaxVersion()).append(')');
            }
        }
        return strBuilder.append(")\"").toString();
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
     * @return intermediary string for easy parsing of module and version ranges
     *
     * e.g. 'module1=[1.2,2],module2,module3=[,4.0]'
     * to 'module1=[1.2,2];module2;module3=[,4.0]'
     */
    public static String replaceDependsDelimiter(String dependsValue) throws MojoExecutionException {
        List<String> result = new ArrayList<>();
        StringTokenizer tokens = new StringTokenizer(dependsValue, ",");
        String nextToken = null;
        while (nextToken != null || tokens.hasMoreTokens()) {
            String token = (nextToken != null) ? nextToken : tokens.nextToken().trim();
            nextToken = (tokens.hasMoreTokens()) ? tokens.nextToken().trim() : null;
            String[] dep = token.split("=", 2);

            if (dep.length > 1) {
                if (JahiaDepends.isMinVersion(dep[1]) && JahiaDepends.isMaxVersion(nextToken)) {
                    result.add(String.format("%s=%s,%s", dep[0].trim(), dep[1].trim(), nextToken));
                    nextToken = null;
                } else {
                    throw new MojoExecutionException("Error while parsing Jahia-depends version clause: " + token);
                }
            } else {
                result.add(token);
            }
        }
        return String.join(DELIMITER, result);
    }

    /** Workaround to convert maven project version to OSGI-compatible version */
    public static String toOsgiVersion(String version) {
        VersionRange range = VersionRange.parseVersionRange(String.format("(,%s)", version));
        return range.getCeiling().toString();
    }

}
