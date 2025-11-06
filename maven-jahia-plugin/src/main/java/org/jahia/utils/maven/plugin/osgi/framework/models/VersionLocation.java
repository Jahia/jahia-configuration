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
 * Represents a specific location where a package version was found.
 * Tracks the version information and how many times it appeared in that location.
 */
public class VersionLocation {

    private final String location;
    private final String version;
    private final String specificationVersion;
    private long counter;

    /**
     * Creates a new VersionLocation.
     *
     * @param location the file path or identifier where the version was found
     * @param version the package version
     * @param specificationVersion the specification version from the manifest (may be null)
     */
    public VersionLocation(String location, String version, String specificationVersion) {
        this.location = location;
        this.version = version;
        this.specificationVersion = specificationVersion;
        this.counter = 0;
    }

    /**
     * Increments the counter indicating how many times this version was found at this location.
     */
    public void incrementCounter() {
        counter++;
    }

    public String getLocation() {
        return location;
    }

    public String getVersion() {
        return version;
    }

    public String getSpecificationVersion() {
        return specificationVersion;
    }

    public long getCounter() {
        return counter;
    }
}

