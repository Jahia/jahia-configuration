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

import org.apache.commons.io.FilenameUtils;
import org.jahia.utils.osgi.parsers.cnd.JahiaCndReader;
import org.jahia.utils.osgi.parsers.cnd.NodeTypeRegistry;
import org.jahia.utils.osgi.parsers.cnd.ParseException;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.TreeSet;

/**
 * A CND (JCR content definition) file parser
 */
public class CndFileParser extends AbstractFileParser {

    public boolean canParse(String fileName) {
        String ext = FilenameUtils.getExtension(fileName).toLowerCase();
        return "cnd".equals(ext);
    }

    public boolean parse(String fileName, InputStream inputStream, String fileParent, boolean externalDependency, boolean optionalDependency, String version, ParsingContext parsingContext) throws IOException {
        getLogger().debug("Processing CND " + fileName + "...");

        try {
            JahiaCndReader jahiaCndReader = new JahiaCndReader(new InputStreamReader(inputStream), fileName, fileName, NodeTypeRegistry.getInstance());
            jahiaCndReader.setDoRegister(false);
            jahiaCndReader.parse();

            Set<String> contentTypeDefinitions = new TreeSet<String>();
            Set<String> contentTypeReferences = new TreeSet<String>();
            jahiaCndReader.getDefinitionsAndReferences(contentTypeDefinitions, contentTypeReferences);
            parsingContext.addAllContentTypeDefinitions(contentTypeDefinitions);
            parsingContext.addAllContentTypeReferences(contentTypeReferences);
        } catch (ParseException e) {
            throw new IOException(e);
        } catch (ValueFormatException e) {
            throw new IOException(e);
        } catch (RepositoryException e) {
            throw new IOException(e);
        }
        return true;
    }
}
