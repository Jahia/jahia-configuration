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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import pl.jalokim.propertiestojson.resolvers.primitives.PrimitiveJsonTypeResolver;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter;

/**
 * Converter for resource bundles into JSON format for usage in React/Angular.
 * 
 * @goal properties2json
 * @phase generate-resources
 * @author Sergiy Shyrkov
 */
public class Properties2JsonMojo extends AbstractMojo {
    
    protected static String toJson(File src, boolean prettyPrinting) throws FileNotFoundException, IOException {
        try (FileInputStream is = new FileInputStream(src)) {
            String json = new PropertiesToJsonConverter(new PrimitiveJsonTypeResolver[] {}).convertToJson(is);
            if (!prettyPrinting) {
                Gson gson = new GsonBuilder().serializeNulls().create();
                JsonParser jp = new JsonParser();
                JsonElement je = jp.parse(json);
                json = gson.toJson(je);
            }
            return json;
        }
    }

    /**
     * Do we need to add the processed files into project's resources.
     * 
     * @parameter default-value="true"
     */
    protected boolean addToProjectResources;

    /**
     * Should result JSON be prettified?
     * 
     * @parameter default-value="false"
     */
    protected boolean prettyPrinting;

    /**
     * The directory to output file to
     * 
     * @parameter default-value="${project.build.directory}/properties2json"
     */
    protected File dest;

    /**
     * comma- or space-separated list of patterns of files that must be excluded. No files (except default excludes) are excluded when
     * omitted.
     * 
     * @parameter
     */
    protected String excludes;

    /**
     * comma- or space-separated list of patterns of files that must be included. All files are included when omitted
     * 
     * @parameter expression="javascript/locales/*.properties"
     */
    protected String includes;

    /**
     * @parameter expression="${project}"
     * @readonly
     */
    protected MavenProject project;

    /**
     * The directory to find files in
     * 
     * @parameter default-value="${basedir}/src/main/resources"
     */
    protected File src;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (src == null || !src.exists()) {
            getLog().info("Folder " + src + " does not exist. Skipping task.");
            return;
        }

        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(src);
        ds.setIncludes(StringUtils.split(includes, ", "));
        ds.setExcludes(StringUtils.split(excludes, ", "));
        ds.scan();

        for (String f : ds.getIncludedFiles()) {
            File file = new File(src, f);
            try {
                String json = toJson(file, prettyPrinting);
                File outputFile = getOutputFile(f);
                FileUtils.writeStringToFile(outputFile, json);
                getLog().info("Converted file " + file + " into " + outputFile); 
            } catch (IOException e) {
                throw new MojoExecutionException("Error converting properties file " + file + " to JSON format", e);
            }
        }

        if (addToProjectResources) {
            Resource resource = new Resource();
            resource.setDirectory(dest.getPath());
            this.project.addResource(resource);
        }
    }

    private File getOutputFile(String f) {
        String normalizedFilePath = f.replace('\\', '/');
        return new File(new File(dest, StringUtils.substringBeforeLast(normalizedFilePath, "/")),
                StringUtils.substringAfter(
                        FilenameUtils.getBaseName(StringUtils.substringAfterLast(normalizedFilePath, "/")), "_")
                        + ".json");
    }
}
