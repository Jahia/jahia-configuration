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
package org.jahia.utils.maven.plugin.osgi.framework.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for cleaning up and normalizing OSGi version strings.
 * Converts fuzzy version definitions into proper OSGi semantic versions (major.minor.micro).
 *
 * <p>This implementation is based on the Maven Bundle Plugin's version cleanup logic.</p>
 */
public class VersionCleaner {

    /**
     * Pattern to match fuzzy version strings and extract major, minor, micro components.
     * Qualifiers are intentionally stripped to ensure consistent versioning.
     */
    private static final Pattern FUZZY_VERSION = Pattern.compile(
            "(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9](.*))?",
            Pattern.DOTALL
    );

    /**
     * Cleans up a version string to match OSGi semantic versioning requirements.
     * <p>
     * Converts various version formats into the standard major.minor.micro format.
     * Qualifiers and build metadata are stripped for consistency.
     * </p>
     *
     * Examples:
     * <ul>
     *   <li>"1.2.3-SNAPSHOT" → "1.2.3"</li>
     *   <li>"2.1" → "2.1.0"</li>
     *   <li>"3" → "3.0.0"</li>
     *   <li>"invalid" → "0.0.0"</li>
     * </ul>
     *
     * @param version the version string to clean up
     * @return the cleaned OSGi-compliant version string, or null if input is null
     */
    public static String cleanupVersion(String version) {
        if (version == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        Matcher matcher = FUZZY_VERSION.matcher(version);

        if (matcher.matches()) {
            String major = matcher.group(1);
            String minor = matcher.group(3);
            String micro = matcher.group(5);

            if (major != null) {
                result.append(major);

                if (minor != null) {
                    result.append(".").append(minor);

                    if (micro != null) {
                        result.append(".").append(micro);
                    } else {
                        result.append(".0");
                    }
                } else {
                    result.append(".0.0");
                }
            }
        } else {
            // If version doesn't match pattern, use default
            result.append("0.0.0");
        }

        return result.toString();
    }

    private VersionCleaner() {
        // Utility class, prevent instantiation
    }
}

