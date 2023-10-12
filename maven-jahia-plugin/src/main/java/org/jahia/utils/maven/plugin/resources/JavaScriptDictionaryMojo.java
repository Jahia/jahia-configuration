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
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Converts resource bundles into a JavaScript dictionary for use in UI.<br>
 * 
 * @goal javascript-dictionary
 * @phase process-resources
 * @author Sergiy Shyrkov
 */
public class JavaScriptDictionaryMojo extends AbstractMojo {

    protected static String escape(String value) {
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

    protected static String getValue(ResourceBundle bundle, String key) {
        String value = null;
        try {
            value = bundle.getString(key);
        } catch (MissingResourceException e) {
            // ignore
        }
        return value;
    }

    protected static String hex(char ch) {
        return Integer.toHexString(ch).toUpperCase(Locale.ENGLISH);
    }

    protected static ResourceBundle lookupBundle(File src, String resourceBundle, String... locales) throws IOException {
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

    /**
     * Do we need to add the processed files into project's resources.
     * 
     * @parameter default-value="true"
     */
    protected boolean addToProjectResources;

    protected File base;

    protected File dest;

    /**
     * The name of the dictionary variable to use in generated JavaScript output files
     * 
     * @parameter default-value="i18n"
     */
    protected String dictionaryName;

    /**
     * Comma- or space-separated list of language codes for resource bundle to process
     * 
     * @parameter default-value="de,en,fr"
     */
    protected String languages;

    /**
     * Should the output files be pretty-printed (line-breaks, whitespace etc.)
     * 
     * @parameter default-value="false"
     */
    protected boolean prettyPrint;

    /**
     * @parameter expression="${project}"
     * @readonly
     */
    protected MavenProject project;

    /**
     * Do we need to quote the keys.
     * 
     * @parameter default-value="true"
     */
    protected boolean quoteKeys;

    /**
     * The name of the resource bundle to be converted
     * 
     * @parameter
     */
    protected String resourceBundle;

    /**
     * The directory to find files in (default is basedir)
     * 
     * @parameter default-value="${basedir}/src/main/resources/resources"
     */
    protected File src;

    /**
     * The sub-directory under destination directory to output files into
     * 
     * @parameter default-value="javascript/i18n"
     */
    protected String targetDirName;

    /**
     * Output file names (without extension)
     * 
     * @parameter
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
    protected void convert(String locale) throws IOException {
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
                if (quoteKeys) {
                    out.append("\"");
                }
                out.append(processKey(key));
                if (quoteKeys) {
                    out.append("\"");
                }
                out.append(":\"").append(processValue(value)).append("\"");
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
        init();
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

        if (addToProjectResources && dest.isDirectory()) {
            Resource resource = new Resource();
            resource.setDirectory(base.getPath());
            project.addResource(resource);
        }
    }

    protected void init() {
        base = new File(project.getBuild().getDirectory(), "i18n2js");
        dest = new File(base, targetDirName != null && targetDirName.length() > 0 ? targetDirName : "javascript");

        if (resourceBundle == null) {
            // let's guess
            if (new File(src, project.getArtifactId() + "_en.properties").isFile()) {
                resourceBundle = project.getArtifactId();
            }
        }
        if (resourceBundle == null) {
            throw new IllegalArgumentException("No resource bundle file for conversion specified");
        }

        if (targetFileName == null) {
            targetFileName = project.getArtifactId() + "-i18n";
        }
    }

    protected ResourceBundle lookupBundle(String... locales) throws IOException {
        return lookupBundle(src, resourceBundle, locales);
    }

    protected String processKey(String key) {
        return key;
    }

    protected String processValue(String value) {
        return escape(value);
    }
}
