package org.jahia.utils.osgi;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * A small utility class to perform some analysis on an OSGi JAR bundle
 */
public class BundleUtils {

    public static final String DUPLICATE_MARKER = " - DUPLICATE #";

    /**
     * Retrieves the headers values for a single OSGi JAR bundle manifest entry. This may contain multiple
     * components for a single entry, as well as attributes (such as version) or directives.
     *
     * @param headerName the name of the Manifest header we want to parse
     * @param headerValue the value of the Manifest header we want to parse
     * @return a list of ManifestValueElement instances that contain value components, attributes and
     * directives.
     * @throws IOException
     */
    public static List<ManifestValueClause> getHeaderClauses(String headerName, String headerValue) throws IOException {
        ManifestValueParser manifestValueParser = new ManifestValueParser(headerName, headerValue, true);
        return manifestValueParser.getManifestValueClauses();
    }

    /**
     * Dumps all the manifest headers of an OSGi JAR bundle into the print writer passed as a parameter
     * @param jarInputStream
     * @param out
     * @return
     * @throws IOException
     */
    public static PrintWriter dumpManifestHeaders(JarInputStream jarInputStream, PrintWriter out) throws IOException {
        Manifest jarManifest = jarInputStream.getManifest();

        Attributes mainAttributes = jarManifest.getMainAttributes();
        Map<String, String> sortedMainAttributes = getSortedAttributes(mainAttributes);
        for (Map.Entry<String,String> attributeEntry : sortedMainAttributes.entrySet()) {
            List<ManifestValueClause> headerValues = BundleUtils.getHeaderClauses(attributeEntry.getKey(), attributeEntry.getValue());
            dumpHeaderValues(attributeEntry.getKey(), headerValues, out);
        }

        for (Map.Entry<String, Attributes> entryAttributes : jarManifest.getEntries().entrySet()) {
            out.println("");
            out.println("Entry: " + entryAttributes.getKey());
            Map<String, String> sortedEntryAttributes = getSortedAttributes(entryAttributes.getValue());
            for (Map.Entry<String,String> attributeEntry : sortedEntryAttributes.entrySet()) {
                List<ManifestValueClause> headerValues = BundleUtils.getHeaderClauses(attributeEntry.getKey(), attributeEntry.getValue());
                dumpHeaderValues(attributeEntry.getKey(), headerValues, out);
            }
        }
        return out;
    }

    private static Map<String, String> getSortedAttributes(Attributes mainAttributes) {
        Map<String,String> sortedMainAttributes = new TreeMap<String,String>();
        for (Map.Entry<Object,Object> attributeEntry : mainAttributes.entrySet()) {
            Attributes.Name attributeName = (Attributes.Name) attributeEntry.getKey();
            sortedMainAttributes.put(attributeName.toString(), (String) attributeEntry.getValue());
        }
        return sortedMainAttributes;
    }

    /**
     * Dumps the values of a single OSGi Jar bundle header into the specified print writer
     * @param headerName
     * @param headerValues
     * @param out
     * @return
     */
    public static PrintWriter dumpHeaderValues(String headerName, List<ManifestValueClause> headerValues, PrintWriter out) {
        out.print(headerName + ": ");
        int i=0;
        Set<String> values = new TreeSet<String>();
        for (ManifestValueClause headerValue : headerValues) {
            StringBuffer sb = new StringBuffer();
            if (headerValue.getPaths().size() == 1) {
                sb.append(headerValue.getPaths().get(0));
            } else {
                sb.append(headerValue.getPaths());
            }
            for (Map.Entry<String,String> attributeEntry : headerValue.getAttributes().entrySet()) {
                sb.append(";");
                sb.append(attributeEntry.getKey());
                sb.append("=\"");
                if (attributeEntry.getValue().length() > 40) {
                    sb.append(attributeEntry.getValue().substring(0, 40) + "...");
                } else {
                    sb.append(attributeEntry.getValue());
                }
                sb.append("\"");
            }
            for (Map.Entry<String,String> directiveEntry : headerValue.getDirectives().entrySet()) {
                sb.append(";");
                sb.append(directiveEntry.getKey());
                sb.append(":=");
                if (directiveEntry.getValue().length() > 40) {
                    sb.append(directiveEntry.getValue().substring(0, 40) + "...");
                } else {
                    sb.append(directiveEntry.getValue());
                }
            }
            final String value = sb.toString();
            if (!values.contains(value)) {
                values.add(value);
            } else {
                int duplicateCount = 1;
                while (values.contains(value + DUPLICATE_MARKER + duplicateCount)) {
                    duplicateCount++;
                }
                values.add(value + DUPLICATE_MARKER + duplicateCount);
            }
            i++;
        }
        String prefix = "";
        for (String value : values) {
            out.print(prefix);
            out.print(value);
            prefix = ",\n    ";
        }
        out.println("");
        return out;
    }
}
