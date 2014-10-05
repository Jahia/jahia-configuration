/**
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
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

package org.jahia.utils.maven.plugin.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Converts resource bundles into a JavaScript dictionary for use in GWT UI.<br>
 * 
 * @goal gwt-dictionary
 * @phase process-resources
 * @author Sergiy Shyrkov
 */
public class GWTDictionaryMojo extends AbstractMojo {

    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() * 2);
        int sz = value.length();
        for (int i = 0; i < sz; i++) {
            char ch = value.charAt(i);

            // handle unicode
            if (ch > 0xfff) {
                out.append("\\u" + hex(ch));
            } else if (ch > 0xff) {
                out.append("\\u0" + hex(ch));
            } else if (ch > 0x7f) {
                out.append("\\u00" + hex(ch));
            } else if (ch < 32) {
                switch (ch) {
                    case '\b':
                        out.append('\\');
                        out.append('b');
                        break;
                    case '\n':
                        out.append('\\');
                        out.append('n');
                        break;
                    case '\t':
                        out.append('\\');
                        out.append('t');
                        break;
                    case '\f':
                        out.append('\\');
                        out.append('f');
                        break;
                    case '\r':
                        out.append('\\');
                        out.append('r');
                        break;
                    default:
                        if (ch > 0xf) {
                            out.append("\\u00" + hex(ch));
                        } else {
                            out.append("\\u000" + hex(ch));
                        }
                        break;
                }
            } else {
                switch (ch) {
                    case '\'':
                        out.append('\\');
                        out.append('\'');
                        break;
                    case '"':
                        out.append('\\');
                        out.append('"');
                        break;
                    case '\\':
                        out.append('\\');
                        out.append('\\');
                        break;
                    case '/':
                        out.append('\\');
                        out.append('/');
                        break;
                    default:
                        out.append(ch);
                        break;
                }
            }
        }

        return out.toString();
    }

    private static String hex(char ch) {
        return Integer.toHexString(ch).toUpperCase(Locale.ENGLISH);
    }

    /**
     * The directory to output files into
     * 
     * @parameter default-value="${project.build.directory}/${project.build.finalName}/gwt/resources/i18n"
     */
    protected File dest;

    /**
     * The name of the GWT dictionary to use in generated JavaScript output files
     * 
     * @parameter default-value="jahia_gwt_messages"
     */
    protected String dictionaryName;

    /**
     * Comma- or space-separated list of language codes for resource bundle to process
     * 
     * @parameter default-value="de,en,es,fr,it,pt"
     */
    protected String languages;

    /**
     * Should the output files be pretty-printed (line-breaks, whitespace etc.)
     * 
     * @parameter default-value="false"
     */
    protected boolean prettyPrint;

    /**
     * The name of the resource bundle to be converted
     * 
     * @parameter default-value="JahiaInternalResources"
     */
    protected String resourceBundle;

    /**
     * The directory to find files in (default is basedir)
     * 
     * @parameter default-value="${basedir}/src/main/resources"
     */
    protected File src;

    /**
     * Output file names (without extension)
     * 
     * @parameter default-value="messages"
     */
    protected String targetFileName;

    /**
     * Performs conversion of the property file into JavaScript file.
     * 
     * @param locale
     *            locale to be used
     * @throws IOException
     *             in case of an error
     */
    private void convert(String locale) throws IOException {
        ResourceBundle defBundle = lookupBundle("", "en");
        if (defBundle == null) {
            throw new FileNotFoundException("ERROR : Couldn't find bundle with name " + resourceBundle
                    + ".properties nor " + resourceBundle + "_en.properties in " + src + " folder, skipping...");
        }

        ResourceBundle bundle = locale != null ? lookupBundle(locale) : null;

        if (!dest.exists() && !dest.mkdirs()) {
            throw new IOException("Unable to create folder " + dest);
        }
        File target = new File(dest, targetFileName + (locale != null ? "_" + locale : "") + ".js");
        getLog().info("Creating " + target + " ...");
        PrintWriter out = new PrintWriter(target);
        Enumeration<String> keyEnum = defBundle.getKeys();
        List<String> keys = new LinkedList<String>();
        while (keyEnum.hasMoreElements()) {
            keys.add(keyEnum.nextElement());
        }
        Collections.sort(keys);
        out.print("var ");
        out.print(dictionaryName);
        out.print("={");
        if (prettyPrint) {
            out.println();
        }
        for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
            String key = iterator.next();
            String value = bundle != null ? getValue(bundle, key) : null;
            if (value == null) {
                value = getValue(defBundle, key);
            }

            if (value != null) {
                out.append(key.replace('.', '_')).append(":\"").append(escape(value)).append("\"");
                if (iterator.hasNext()) {
                    out.append(",");
                }
                if (prettyPrint) {
                    out.println();
                }
            }
        }
        out.print("};");
        if (prettyPrint) {
            out.println();
        }

        out.flush();
        out.close();
        getLog().info("done");
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Converting resource bundle " + resourceBundle + " into JavaScript files...");
        try {
            getLog().info("...no locale (default file)");
            convert(null);
            for (String lc : StringUtils.split(languages, " ,")) {
                String l = lc.trim();
                getLog().info("...locale " + l);
                convert(l);
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        getLog().info("...conversion done.");
    }

    private String getValue(ResourceBundle bundle, String key) {
        String value = null;
        try {
            value = bundle.getString(key);
        } catch (MissingResourceException e) {
            // ignore
        }
        return value;
    }

    private ResourceBundle lookupBundle(String... locales) throws IOException {
        ResourceBundle rb = null;
        for (String locale : locales) {
            File f = new File(src, resourceBundle + "_" + locale + ".properties");
            if (f.exists()) {
                InputStream is = null;
                try {
                    is = FileUtils.openInputStream(f);
                    rb = new PropertyResourceBundle(is);
                    break;
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        }
        return rb;
    }
}
