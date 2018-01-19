/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.izpack;

/**
 * Utility class for detecting OS specific parameters.
 * 
 * @author Sergiy Shyrkov
 */
public final class OsUtils {

    /**
     * Contains <code>64</code> for 64 bit platform, <code>32</code> for 32 bit platforms and "unknown" otherwise.
     */
    public static final String OS_ARCH_DATA_MODEL = detectDataModel();

    /**
     * Is <code>true</code> for 32 bit platforms.
     */
    public static final boolean IS_OS_ARCH_DATA_MODEL_32 = OS_ARCH_DATA_MODEL.equals("32");

    /**
     * Is <code>true</code> for 64bit platforms.
     */
    public static final boolean IS_OS_ARCH_DATA_MODEL_64 = OS_ARCH_DATA_MODEL.equals("64");

    private static final String detectDataModel() {
        String dm = System.getProperty("sun.arch.data.model");
        if (dm != null) {
            return dm;
        }
        String osArch = System.getProperty("os.arch");
        if (osArch == null) {
            return "unknown";
        }

        osArch = osArch.toLowerCase();

        return osArch.equals("amd64") || osArch.equals("x86_64") || osArch.startsWith("ia64") ? "64" : "32";
    }

    public static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    public static boolean isLinux() {
        return OS_NAME.startsWith("linux");
    }

    public static boolean isMac() {
        return OS_NAME.startsWith("mac");
    }

    public static boolean isWindows() {
        return OS_NAME.startsWith("windows");
    }
}
