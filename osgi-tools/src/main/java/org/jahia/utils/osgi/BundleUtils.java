package org.jahia.utils.osgi;

import org.eclipse.osgi.util.ManifestElement;

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
        for (ManifestValueClause headerValue : headerValues) {
            if (i > 0) {
                out.print("    ");
            }
            if (headerValue.getPaths().size() == 1) {
                out.print(headerValue.getPaths().get(0));
            } else {
                out.print(headerValue.getPaths());
            }
            for (Map.Entry<String,String> attributeEntry : headerValue.getAttributes().entrySet()) {
                out.print("; ");
                out.print(attributeEntry.getKey());
                out.print("=");
                if (attributeEntry.getValue().length() > 40) {
                    out.print(attributeEntry.getValue().substring(0, 40) + "...");
                } else {
                    out.print(attributeEntry.getValue());
                }
            }
            for (Map.Entry<String,String> directiveEntry : headerValue.getDirectives().entrySet()) {
                out.print("; ");
                out.print(directiveEntry.getKey());
                out.print(":=");
                if (directiveEntry.getValue().length() > 40) {
                    out.print(directiveEntry.getValue().substring(0, 40) + "...");
                } else {
                    out.print(directiveEntry.getValue());
                }
            }
            out.println("");
            i++;
        }
        return out;
    }
}
