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
package org.jahia.utils.osgi;

import org.eclipse.osgi.util.ManifestElement;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * A unit test class for the ManifestValueParser implementation
 */
public class ManifestValueParserTest {

    @Test
    public void testManifestValueParser() throws IOException {
        InputStream manifest1InputStream = this.getClass().getClassLoader().getResourceAsStream("org/jahia/utils/osgi/manifests/MANIFEST1.MF");
        System.out.println("Parsing org/jahia/utils/osgi/manifests/MANIFEST1.MF ...");
        parseManifest(manifest1InputStream);

        System.out.println("");

        InputStream manifest2InputStream = this.getClass().getClassLoader().getResourceAsStream("org/jahia/utils/osgi/manifests/MANIFEST2.MF");
        System.out.println("Parsing org/jahia/utils/osgi/manifests/MANIFEST2.MF ...");
        parseManifest(manifest2InputStream);
    }

    private void parseManifest(InputStream manifest1InputStream) throws IOException {
        Manifest manifest1 = new Manifest(manifest1InputStream);
        Attributes mainAttributes1 = manifest1.getMainAttributes();
        Map<String,Object> sortedMainAttributes = new TreeMap<String,Object>();
        for (Map.Entry<Object, Object> attributeEntry : mainAttributes1.entrySet()) {
            Attributes.Name attributeName = (Attributes.Name) attributeEntry.getKey();
            sortedMainAttributes.put(attributeName.toString(), attributeEntry.getValue());
        }
        for (Map.Entry<String, Object> attributeEntry : sortedMainAttributes.entrySet()) {
            String attributeValue = attributeEntry.getValue().toString();
            ManifestValueParser manifestValueParser = new ManifestValueParser(attributeEntry.getKey(), attributeValue, true);
            List<ManifestValueClause> manifestValueClauses = manifestValueParser.getManifestValueClauses();
            List<ManifestValueClause> eclipseManifestValueClauses = getEclipseHeaderCauses(attributeEntry.getKey(), attributeValue);
            System.out.println(attributeEntry.getKey() + ": " + printHeaderValues(manifestValueClauses));
            assertEquals("Header " + attributeEntry.getKey() + " clauses do not match between our own parser and Eclipse's", eclipseManifestValueClauses, manifestValueClauses);
        }
        Map<String,Map<String,Object>> sortedEntriesAttributes = new TreeMap<String,Map<String,Object>>();
        Map<String,Attributes> entries1 = manifest1.getEntries();
        for (Map.Entry<String,Attributes> entry : entries1.entrySet()) {
            System.out.println("");
            System.out.println("Name: " + entry.getKey());
            for (Map.Entry<Object, Object> attributeEntry : entry.getValue().entrySet()) {
                Attributes.Name attributeName = (Attributes.Name) attributeEntry.getKey();
                String attributeValue = (String) attributeEntry.getValue();
                ManifestValueParser manifestValueParser = new ManifestValueParser(attributeName.toString(), attributeValue, true);
                List<ManifestValueClause> manifestValueClauses = manifestValueParser.getManifestValueClauses();
                List<ManifestValueClause> eclipseManifestValueClauses = getEclipseHeaderCauses(attributeName.toString(), attributeValue);
                System.out.println(attributeName.toString() + ": " + printHeaderValues(manifestValueClauses));
                assertEquals("Header " + attributeName.toString() + " clauses do not match between our own parser and Eclipse's", eclipseManifestValueClauses, manifestValueClauses);
            }
        }
    }

    private String printHeaderValues(List<ManifestValueClause> headerValues) {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i < headerValues.size(); i++) {
            builder.append(headerValues.get(i));
            if (i < headerValues.size() -1 ) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    public List<ManifestValueClause> getEclipseHeaderCauses(String headerName, String headerValue) throws IOException {
        List<ManifestValueClause> headerClauses = new ArrayList<ManifestValueClause>();
        try {
            ManifestElement[] manifestElements = ManifestElement.parseHeader(headerName, headerValue);
            for (ManifestElement manifestElement : manifestElements) {
                Map<String,String> attributes = new HashMap<String,String>();
                Enumeration<String> attributeKeyEnum = manifestElement.getKeys();
                if (attributeKeyEnum != null) {
                    while (attributeKeyEnum.hasMoreElements()) {
                        String attributeKeyName = attributeKeyEnum.nextElement();
                        attributes.put(attributeKeyName, manifestElement.getAttribute(attributeKeyName));
                    }
                }
                Map<String,String> directives = new HashMap<String,String>();
                Enumeration<String> directiveKeyEnum = manifestElement.getDirectiveKeys();
                if (directiveKeyEnum != null) {
                    while (directiveKeyEnum.hasMoreElements()) {
                        String directiveKeyName = directiveKeyEnum.nextElement();
                        directives.put(directiveKeyName, manifestElement.getDirective(directiveKeyName));
                    }
                }
                headerClauses.add(new ManifestValueClause(Arrays.asList(manifestElement.getValueComponents()), attributes, directives));
            }
        } catch (Exception e) {
            throw new IOException("Error processing bundle headers", e);
        }
        return headerClauses;
    }

}
