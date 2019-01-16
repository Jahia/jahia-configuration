/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.osgi.parsers.cnd;

import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 *
 * User: toto
 * Date: 14 mars 2008
 * Time: 18:43:40
 *
 */
public class Name {
    private String localName;
    private String prefix;
    private String uri;

    private String preComputedToString;
    private int preComputedHashCode;

    public Name(String localName, String prefix, String uri) {
        this.localName = localName;
        this.prefix = prefix;
        this.uri = uri;
    }

    public Name(String qualifiedName, Map<String,String> namespaceMapping) {
        if (qualifiedName.startsWith("{")) {
            int endUri = qualifiedName.indexOf("}");
            if (endUri != -1 && qualifiedName.length() > endUri) {
                uri = StringUtils.substringBetween(qualifiedName, "{", "}");
                for (Map.Entry<String, String> entry : namespaceMapping.entrySet()) {
                    if (entry.getValue().equals(uri)) {
                        prefix = entry.getKey();
                        break;
                    }
                }
                localName = qualifiedName.substring(endUri + 1);
            } else {
                localName = qualifiedName;
                prefix = "";
                uri = namespaceMapping.get("");
            }
        }
        if (localName == null) {
            String s[] = Patterns.COLON.split(qualifiedName);
            if (s.length == 2) {
                prefix = s[0];
                localName = s[1];
                uri = namespaceMapping.get(prefix);
            } else {
                prefix = "";
                localName = s[0];
                uri = namespaceMapping.get("");
            }
        }
    }

    public String getLocalName() {
        return localName;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getUri() {
        return uri;
    }

    public String toString() {
        if (preComputedToString != null) {
            return preComputedToString;
        }
        if (prefix.equals("")) {
            preComputedToString = localName;
            return preComputedToString;
        } else {
            preComputedToString = prefix + ":" + localName;
            return preComputedToString;
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Name name = (Name) o;

        if (localName != null ? !localName.equals(name.localName) : name.localName != null) return false;
        if (uri != null ? !uri.equals(name.uri) : name.uri != null) return false;

        return true;
    }

    public int hashCode() {
        if (preComputedHashCode != 0) {
            return preComputedHashCode;
        }
        int result;
        result = (localName != null ? localName.hashCode() : 0);
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        preComputedHashCode = result;
        return preComputedHashCode;
    }
}
