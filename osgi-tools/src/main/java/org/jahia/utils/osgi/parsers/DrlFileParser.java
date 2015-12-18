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
 *     This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
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
package org.jahia.utils.osgi.parsers;

import org.apache.commons.io.FilenameUtils;
import org.jahia.utils.osgi.PackageUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Drools rule definition file parser
 */
public class DrlFileParser extends AbstractFileParser {

    public static final Pattern RULE_IMPORT_PATTERN = Pattern.compile("^\\s*import\\s*([\\w.\\*]*)\\s*$");

    public boolean canParse(String fileName) {
        String ext = FilenameUtils.getExtension(fileName).toLowerCase();
        return "drl".equals(ext);
    }

    public boolean parse(String fileName, InputStream inputStream, String fileParent, boolean externalDependency, boolean optionalDependency, String version, ParsingContext parsingContext) throws IOException {
        getLogger().debug("Processing Drools Rule file " + fileName + "...");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            Matcher ruleImportMatcher = RULE_IMPORT_PATTERN.matcher(line);
            if (ruleImportMatcher.matches()) {
                String ruleImport = ruleImportMatcher.group(1);
                getLogger().debug(fileParent + " / " + fileName + ": found rule import " + ruleImport + " package=" + PackageUtils.getPackagesFromClass(ruleImport, optionalDependency, version, fileName, parsingContext).toString());
                parsingContext.addAllPackageImports(PackageUtils.getPackagesFromClass(ruleImport, optionalDependency, version, fileParent + "/" + fileName, parsingContext));
            }
        }
        return true;
    }
}
