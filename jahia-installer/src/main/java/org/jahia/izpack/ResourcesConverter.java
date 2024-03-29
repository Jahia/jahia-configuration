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
package org.jahia.izpack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Converts the property files into XML language packs for IzPack installer.
 * 
 * @author Sergiy Shyrkov
 */
public class ResourcesConverter {

    /**
     * Performs conversion of the property files into XML language packs.
     * 
     * @param args
     * @throws FileNotFoundException
     */
    public static void main(String[] args) throws FileNotFoundException {
        String bundleName = args[0];
        String[] locales = args[1].split(",");
        File targetDir = new File(args[2]);
        System.out.println("Converting resource bundle " + bundleName
                + " to XML language packs...");
        for (String lc : locales) {
            Locale currentLocale = new Locale(lc.trim());
            System.out.println("...locale "
                    + currentLocale.getDisplayName(Locale.ENGLISH));
            convert(bundleName, currentLocale, targetDir);
        }
        System.out.println("...converting done.");
    }

    /**
     * Performs conversion of the property file into XML language pack.
     * 
     * @param bundleName
     *            the resource bundle name
     * @param locale
     *            locale to be used
     * @param targetFolder
     *            the target folder
     * @throws FileNotFoundException
     */
    private static void convert(String bundleName, Locale locale,
            File targetFolder) throws FileNotFoundException {
        ResourceBundle enBundle = ResourceBundle.getBundle(bundleName,
                Locale.ENGLISH);
        ResourceBundle bundle = null;
        try {
            bundle = ResourceBundle.getBundle(bundleName, locale);
        } catch (MissingResourceException e) {
            bundle = enBundle;
        }
        PrintWriter out = new PrintWriter(new File(targetFolder,
                StringUtils.substringAfterLast(bundleName, ".") + "."
                        + locale.getISO3Language() + ".xml"));
        Enumeration<String> keyEnum = enBundle.getKeys();
        List<String> keys = new LinkedList<String>();
        while (keyEnum.hasMoreElements()) {
            keys.add(keyEnum.nextElement());
        }
        Collections.sort(keys);
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<langpack>");
        for (String key : keys) {
            String value = null;
            try {
                value = bundle.getString(key);
            } catch (MissingResourceException e) {
                try {
                    value = enBundle.getString(key);
                } catch (MissingResourceException e2) {
                    value = key;
                }
            }
            out.append("    <str id=\"").append(key).append("\" txt=\"")
                    .append(StringEscapeUtils.escapeXml(value)).append("\"/>");
            out.println();
        }
        out.println("</langpack>");
        out.flush();
        out.close();
    }
}
