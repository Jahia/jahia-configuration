package org.jahia.utils.osgi;

import java.util.List;
import java.util.Map;

/**
 * A clause in a OSGi bundle manifest header value. Clauses are separated by commas in the OSGi bundle manifest header values.
 */
public class ManifestValueClause {
    List<String> paths;
    Map<String,String> attributes;
    Map<String,String> directives;

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
            sb.append(attributeEntry.getValue());
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
            sb.append(directiveEntry.getValue());
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
