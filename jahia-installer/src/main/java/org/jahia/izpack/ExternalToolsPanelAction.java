/**
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2011 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Solutions Group SA. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */

package org.jahia.izpack;

import java.io.File;
import java.io.FilenameFilter;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.PanelAction;
import com.izforge.izpack.installer.PanelActionConfiguration;
import com.izforge.izpack.util.AbstractUIHandler;

/**
 * Panel action that is responsible for pre-filling values of auto-detected external tools.
 * 
 * @author Benjamin Papez
 */
public class ExternalToolsPanelAction implements PanelAction {

    private static final String OS_NAME = System.getProperty("os.name")
            .toLowerCase();

    public void executeAction(AutomatedInstallData adata,
            AbstractUIHandler handler) {

        String officePath = adata.getVariable("dmConfig.officePath");
        if (officePath == null || officePath.length() == 0) {
            File officeHome = getDefaultOfficeHome();
            if (officeHome != null) {
                adata.setVariable("dmConfig.officePath",
                        officeHome.getAbsolutePath());
            }
        }
        String imagemagickPath = adata.getVariable("dmConfig.imagemagickPath");
        if (imagemagickPath == null || imagemagickPath.length() == 0) {
            File imagemagickHome = getDefaultImagemagickHome();
            if (imagemagickHome != null) {
                adata.setVariable("dmConfig.imagemagickPath",
                        imagemagickHome.getAbsolutePath());
            }
        }
        String ffmpegPath = adata.getVariable("dmConfig.ffmpegPath");
        if (ffmpegPath == null || ffmpegPath.length() == 0) {
            File ffmpegExecutable = getDefaultFFMpegExecutable();
            if (ffmpegExecutable != null) {
                adata.setVariable("dmConfig.ffmpegPath",
                        ffmpegExecutable.getAbsolutePath());
            }
        }
        String swftoolsPath = adata.getVariable("dmConfig.swftoolsPath");
        if (swftoolsPath == null || swftoolsPath.length() == 0) {
            File swftoolsExecutable = getDefaultPDF2SWFExecutable();
            if (swftoolsExecutable != null) {
                adata.setVariable("dmConfig.swftoolsPath",
                        swftoolsExecutable.getAbsolutePath());
            }
        }

    }

    public void initialize(PanelActionConfiguration configuration) {
        // do nothing
    }

    public static File getDefaultOfficeHome() {
        if (System.getProperty("office.home") != null) {
            return new File(System.getProperty("office.home"));
        }
        if (isWindows()) {
            return findToolHome(new String[] { "program/soffice.bin" },
                    new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name
                                    .matches("(LibreOffice|OpenOffice.org) [3-9].*");
                        }
                    }, false, System.getenv("ProgramFiles(x86)"), System
                            .getenv("ProgramFiles"));
        } else if (isMac()) {
            return findToolHome(new String[] { "MacOS/soffice.bin" }, null,
                    false, "/Applications/OpenOffice.org.app/Contents",
                    "/Applications/LibreOffice.app/Contents");
        } else {
            // Linux or other *nix variants
            return findToolHome(new String[] { "program/soffice.bin" }, null,
                    false, "/opt/openoffice.org3", "/opt/libreoffice",
                    "/usr/lib/openoffice", "/usr/lib/libreoffice");
        }
    }

    public static File getDefaultImagemagickHome() {
        if (System.getProperty("imagemagick.home") != null) {
            return new File(System.getProperty("imagemagick.home"));
        }
        if (isWindows()) {
            return findToolHome(
                    new String[] { "convert.exe" },
                    new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.toLowerCase().startsWith("imagemagick");
                        }
                    }, false, System.getenv("ProgramFiles(x86)"),
                    System.getenv("ProgramFiles"));
        } else if (isMac()) {
            return findToolHome(new String[] { "convert", "bin/convert" },
                    new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.toLowerCase().startsWith("imagemagick")
                                    || name.equals("bin");
                        }
                    }, false, "/Applications", "/opt/local");
        } else {
            // Linux or other *nix variants
            return findToolHome(new String[] { "convert" }, null, false,
                    "/usr/bin", "/usr/local/bin", "/usr/bin/X11");
        }
    }

    public static File getDefaultFFMpegExecutable() {
        if (System.getProperty("ffmpeg.executable") != null) {
            return new File(System.getProperty("ffmpeg.executable"));
        }
        if (isWindows()) {
            return findToolHome(
                    new String[] { "ffmpeg.exe", "bin/ffmpeg.exe" },
                    new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.toLowerCase().startsWith("imagemagick")
                                    || name.toLowerCase().startsWith("ffmpeg");
                        }
                    }, true, System.getenv("ProgramFiles(x86)"),
                    System.getenv("ProgramFiles"));
        } else if (isMac()) {
            return findToolHome(new String[] { "ffmpeg", "bin/ffmpeg" },
                    new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.toLowerCase().startsWith("imagemagick")
                                    || name.equals("bin");
                        }
                    }, true, "/Applications", "/opt/local");
        } else {
            // Linux or other *nix variants
            return findToolHome(new String[] { "ffmpeg" }, null, true,
                    "/usr/bin", "/usr/local/bin", "/usr/bin/X11");
        }
    }

    public static File getDefaultPDF2SWFExecutable() {
        if (System.getProperty("pdf2swf.executable") != null) {
            return new File(System.getProperty("pdf2swf.executable"));
        }
        if (isWindows()) {
            return findToolHome(new String[] { "pdf2swf.exe" },
                    new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.toLowerCase().startsWith("swftools");
                        }
                    }, true, System.getenv("ProgramFiles(x86)"),
                    System.getenv("ProgramFiles"));
        } else if (isMac()) {
            return findToolHome(new String[] { "pdf2swf" }, null, true,
                    "/opt/local/bin");
        } else {
            // Linux or other *nix variants
            return findToolHome(new String[] { "pdf2swf" }, null, true,
                    "/usr/bin", "/usr/local/bin", "/usr/bin/X11");
        }
    }

    private static File findToolHome(String[] executableNames,
            FilenameFilter filter, boolean returnExecutable,
            String... knownPaths) {
        for (String path : knownPaths) {
            if (path != null) {
                File home = new File(path);
                File[] possibleHomes = filter != null ? home.listFiles(filter)
                        : new File[] { home };
                for (File possibleHome : possibleHomes) {
                    for (String executableName : executableNames) {
                        File executable = getToolExecutable(executableName,
                                possibleHome);
                        if (executable.isFile()) {
                            return returnExecutable ? executable : possibleHome;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static File getToolExecutable(String executableName, File officeHome) {
        return new File(officeHome, executableName);
    }

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
