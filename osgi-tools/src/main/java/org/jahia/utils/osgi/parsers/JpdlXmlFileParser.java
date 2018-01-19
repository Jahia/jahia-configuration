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
package org.jahia.utils.osgi.parsers;

import org.jahia.utils.osgi.PackageUtils;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.util.List;

/**
 * XML file parser for JPDL file types
 */
public class JpdlXmlFileParser extends AbstractXmlFileParser {

    public boolean canParse(String fileName) {
        return fileName.toLowerCase().endsWith(".jpdl.xml");
    }

    @Override
    public boolean canParse(String fileName, Element rootElement) {
        return true;
    }

    @Override
    public void parse(String fileName, Element rootElement, String fileParent, boolean externalDependency, boolean optionalDependency, String version, ParsingContext parsingContext) throws JDOMException {
        getLogger().debug("Processing workflow definition file (JBPM JPDL) " + fileParent + " / " +fileName + "...");
        List<Attribute> classAttributes = getAttributes(rootElement, "//@class");
        for (Attribute classAttribute : classAttributes) {
            getLogger().debug(fileName + " Found class " + classAttribute.getValue() + " package=" + PackageUtils.getPackagesFromClass(classAttribute.getValue(), optionalDependency, version, fileName, parsingContext).toString());
            parsingContext.addAllPackageImports(PackageUtils.getPackagesFromClass(classAttribute.getValue(), optionalDependency, version, fileParent + "/" +fileName, parsingContext));
        }
    }
}
