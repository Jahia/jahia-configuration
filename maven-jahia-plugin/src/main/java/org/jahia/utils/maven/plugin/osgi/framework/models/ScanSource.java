/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2025 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.osgi.framework.models;

/**
 * Enumeration of methods used to discover packages during JAR scanning.
 * This helps track and report how packages were identified in the codebase.
 */
public enum ScanSource {

    /**
     * Package was explicitly declared in the OSGi Export-Package manifest header.
     */
    EXPORT_PACKAGE_HEADER("Export-Package header"),

    /**
     * Package was found as a named entry in the JAR manifest with version information.
     */
    MANIFEST_ENTRY("Manifest entry"),

    /**
     * Package was discovered by scanning the internal structure of the JAR file.
     */
    JAR_SCAN("JAR internal scan"),

    /**
     * Package inherited its version from a parent package defined in the manifest.
     */
    PARENT_PACKAGE_INHERITANCE("Inherited from parent package in manifest");

    private final String description;

    ScanSource(String description) {
        this.description = description;
    }

    /**
     * Gets a human-readable description of this scan source.
     *
     * @return the description string
     */
    public String getDescription() {
        return description;
    }
}

