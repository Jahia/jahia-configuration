/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.configuration.deployers.jboss;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

/**
 * Utility class for performing deployment of JDBC driver JARs for JBoss EAP 6 as a static module.
 * 
 * @author Sergiy Shyrkov
 */
final class DriverDeploymentHelper {

    private static final Map<String, String> DBMS_TYPES;
    private static final String FILE_CONTENT_ENCODING = "ISO-8859-1";

    static {
        DBMS_TYPES = new HashMap<String, String>();
        DBMS_TYPES.put("derby", "derby_embedded");
        DBMS_TYPES.put("derbyclient", "derby");
        DBMS_TYPES.put("mysql", "mysql");
        DBMS_TYPES.put("ojdbc6", "oracle");
        DBMS_TYPES.put("ojdbc7", "oracle");
        DBMS_TYPES.put("orai18n", "oracle");
        DBMS_TYPES.put("postgresql", "postgresql");
        DBMS_TYPES.put("sqljdbc4", "mssql");
        DBMS_TYPES.put("sqljdbc42", "mssql");
    }

    public static boolean deploy(File targetServerDirectory, File driverJar) throws IOException {
        String driverKey = getDriverKey(driverJar.getName());
        String driverType = getDriverType(driverKey, driverJar.getName());
        File targetDir = new File(targetServerDirectory, "/modules/org/jahia/jdbc/" + driverType + "/main");
        boolean inPlace = targetDir.equals(driverJar.getParentFile());
        if (targetDir.isDirectory()) {
            // special case for the second JAR of the Oracle driver
            // second test is for the case driver JAR is already in-place (used only in configurators to configure module.xml)
            if (!"oracle".equals(driverType) && !inPlace) {
                FileUtils.cleanDirectory(targetDir);
            }
        } else {
            if (!targetDir.mkdirs()) {
                throw new IOException("Unable to create target directory: " + targetDir);
            }
        }

        if (!inPlace) {
            FileUtils.copyFileToDirectory(driverJar, targetDir);
        }

        File moduleXml = new File(targetDir, "module.xml");
        String existingContent = moduleXml.isFile() ? FileUtils.readFileToString(moduleXml, FILE_CONTENT_ENCODING) : null;
        FileUtils.writeStringToFile(moduleXml,
                generateModuleXmlContent(existingContent, driverJar.getName(), driverType), FILE_CONTENT_ENCODING);

        return Boolean.TRUE;
    }

    private static String generateModuleXmlContent(String existingContent, String driverJarFileName, String driverType) {
        StringBuilder xml = null;
        if (existingContent != null) {
            xml = new StringBuilder(existingContent.length() + 128);
            int pos = existingContent.indexOf('>', existingContent.indexOf(".jar\""));
            xml.append(existingContent.substring(0, pos + 1));
            xml.append("\n").append("        <resource-root path=\"").append(driverJarFileName).append("\"/>\n");
            xml.append(existingContent.substring(pos + 2, existingContent.length()));
        } else {
            xml = new StringBuilder(256);
            xml.append("<?xml version=\"1.0\" ?>\n");
            xml.append("\n");
            xml.append("<module xmlns=\"urn:jboss:module:1.1\" name=\"org.jahia.jdbc.").append(driverType)
                    .append("\">\n");
            xml.append("    <resources>\n");
            xml.append("        <resource-root path=\"").append(driverJarFileName).append("\"/>\n");
            xml.append("    </resources>\n");
            xml.append("\n");
            xml.append("    <dependencies>\n");
            xml.append("        <module name=\"javax.api\"/>\n");
            xml.append("        <module name=\"javax.transaction.api\"/>\n");
            xml.append("    </dependencies>\n");
            xml.append("</module>\n");
        }

        return xml.toString();
    }

    private static String getDriverKey(String fileName) {
        int pos = fileName.indexOf('-');
        if (pos == -1) {
            throw new IllegalArgumentException("Unsupported driver JAR file: " + fileName);
        }

        return fileName.substring(0, pos);
    }

    private static String getDriverType(String driverKey, String fileName) {
        String type = DBMS_TYPES.get(driverKey);
        if (type == null) {
            throw new IllegalArgumentException("Unknown driver JAR file: " + fileName);
        }

        return type;
    }

    /**
     * Initializes an instance of this class.
     */
    private DriverDeploymentHelper() {
        super();
    }

}
