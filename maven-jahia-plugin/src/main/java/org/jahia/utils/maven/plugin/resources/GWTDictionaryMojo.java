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
package org.jahia.utils.maven.plugin.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

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

    private static String normalizeKey(String key) {
        String normalized = key.indexOf('.') != -1 ? key.replace('.', '_') : key;
        if (normalized.indexOf('-') != -1) {
            normalized = normalized.replace('-', '_');
        }
        return normalized;
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
            String value = bundle != null ? JavaScriptDictionaryMojo.getValue(bundle, key) : null;
            if (value == null) {
                value = JavaScriptDictionaryMojo.getValue(defBundle, key);
            }

            if (value != null) {
                out.append(normalizeKey(key)).append(":\"").append(JavaScriptDictionaryMojo.escape(value)).append("\"");
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

    private ResourceBundle lookupBundle(String... locales) throws IOException {
        return JavaScriptDictionaryMojo.lookupBundle(src, resourceBundle, locales);
    }
}
