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
package org.jahia.utils.osgi;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A clause in a OSGi bundle manifest header value. Clauses are separated by commas in the OSGi bundle manifest header values.
 */
public class ManifestValueClause {
    List<String> paths;
    Map<String,String> attributes;
    Map<String,String> directives;

    public static final Pattern EXTENDED_PATTERN = Pattern.compile("[A-Za-z0-9-_\\.]+");

    public ManifestValueClause(List<String> paths, Map<String, String> attributes, Map<String, String> directives) {
        this.paths = paths;
        this.attributes = attributes;
        this.directives = directives;
    }

    public List<String> getPaths() {
        return paths;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Map<String, String> getDirectives() {
        return directives;
    }

    private boolean mustQuote(String value) {
        Matcher extendedMatcher = EXTENDED_PATTERN.matcher(value);
        if (extendedMatcher.matches()) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (int i=0; i < paths.size(); i++) {
            sb.append(paths.get(i));
            if (i < paths.size()-1) {
                sb.append(";");
            }
        }
        if (paths.size() > 0 && attributes.size() > 0) {
            sb.append(";");
        }
        int i=0;
        for (Map.Entry<String,String> attributeEntry : attributes.entrySet()) {
            sb.append(attributeEntry.getKey());
            sb.append("=");
            boolean needQuotes = mustQuote(attributeEntry.getValue());
            if (needQuotes) {
                sb.append("\"");
            }
            sb.append(attributeEntry.getValue());
            if (needQuotes) {
                sb.append("\"");
            }
            if (i < attributes.size() -1) {
                sb.append(";");
            }
            i++;
        }
        if ((paths.size() > 0 || attributes.size() > 0) && directives.size() > 0) {
            sb.append(";");
        }
        i=0;
        for (Map.Entry<String,String> directiveEntry : directives.entrySet()) {
            sb.append(directiveEntry.getKey());
            sb.append(":=");
            boolean needQuotes = mustQuote(directiveEntry.getValue());
            if (needQuotes) {
                sb.append("\"");
            }
            sb.append(directiveEntry.getValue());
            if (needQuotes) {
                sb.append("\"");
            }
            if (i < directives.size() -1) {
                sb.append(";");
            }
            i++;
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ManifestValueClause)) return false;

        ManifestValueClause that = (ManifestValueClause) o;

        if (paths != null ? !paths.equals(that.paths) : that.paths != null) {
            return false;
        }
        if (directives != null ? !directives.equals(that.directives) : that.directives != null) {
            return false;
        }
        if (attributes != null ? !attributes.equals(that.attributes) : that.attributes != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = paths != null ? paths.hashCode() : 0;
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        result = 31 * result + (directives != null ? directives.hashCode() : 0);
        return result;
    }
}
