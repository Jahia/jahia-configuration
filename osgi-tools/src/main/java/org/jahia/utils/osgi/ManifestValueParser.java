package org.jahia.utils.osgi;

import java.util.*;

/**
 * A parser for OSGi Bundle Manifest Header values.
 */
public class ManifestValueParser {

    private String headerName;
    private String headerValue;
    private List<ManifestValueClause> manifestValueClauses;
    private boolean removeQuotes = false;

    public ManifestValueParser(String headerName, String headerValue, boolean removeQuotes) {
        this.headerName = headerName;
        this.headerValue = headerValue;
        this.removeQuotes = removeQuotes;
        parse();
    }

    public List<ManifestValueClause> getManifestValueClauses() {
        return manifestValueClauses;
    }

    private void parse() {
        manifestValueClauses = new ArrayList<ManifestValueClause>();
        parseClauses(headerValue);
    }

    private void parseClauses(String clauses) {
        String token = "";
        ManifestValueTokenizer clausesTokenizer = new ManifestValueTokenizer(clauses);
        while ((token = clausesTokenizer.getNextToken(",")).length() > 0) {
            manifestValueClauses.add(parseClause(token));
        }
    }

    private ManifestValueClause parseClause(String clause) {
        ManifestValueTokenizer clauseTokenizer = new ManifestValueTokenizer(clause);
        String token = "";
        Set<String> paths = new LinkedHashSet<String>();
        Map<String,String> directives = new LinkedHashMap<String,String>();
        Map<String,String> attributes = new LinkedHashMap<String,String>();
        while ((token = clauseTokenizer.getNextToken(";")).length() > 0) {
            parsePathOrParameter(token, paths, directives, attributes);
        }
        return new ManifestValueClause(new ArrayList(paths), attributes, directives);
    }

    private void parsePathOrParameter(String pathOrParameter, Set<String> paths, Map<String,String> directives, Map<String,String> attributes) {
        ManifestValueTokenizer pathOrParameterTokenizer = new ManifestValueTokenizer(pathOrParameter);
        String token = "";
        while ((token = pathOrParameterTokenizer.getNextToken(":=")).length() > 0) {
            char matchedDelimiter = pathOrParameterTokenizer.getMatchedDelimiter();
            if (matchedDelimiter == ':') {
                if (pathOrParameterTokenizer.peekCurrentChar() == '=') {
                    // matched := directive operator
                    pathOrParameterTokenizer.getNextChar();
                    String directiveValue = pathOrParameterTokenizer.getNextToken(":");
                    directives.put(token, removeQuotesFromValue(directiveValue));
                } else {
                    // could be in case of a typed attribute such as country:List<String>="nl,be,fr,uk"
                    String attributeTypeValue = pathOrParameterTokenizer.getNextToken("=");
                    String attributeValue = pathOrParameterTokenizer.getNextToken("=");
                    if (attributeValue.length() > 0) {
                        attributes.put(token + ":" + attributeTypeValue, removeQuotesFromValue(attributeValue));
                    } else {
                        if (attributeTypeValue.length() > 0) {
                            paths.add(token + ":" + attributeTypeValue);
                        } else {
                            paths.add(token);
                        }
                    }
                }
            } else if (matchedDelimiter == '=') {
                String attributeValue = pathOrParameterTokenizer.getNextToken("=");
                if (attributeValue.length() > 0) {
                    attributes.put(token, removeQuotesFromValue(attributeValue));
                } else {
                    paths.add(token + "=");
                }
            } else {
                paths.add(token);
            }
        }
    }

    private String removeQuotesFromValue(String potentiallyQuotedString) {
        if (removeQuotes && potentiallyQuotedString.startsWith("\"") && potentiallyQuotedString.endsWith("\"")) {
            return potentiallyQuotedString.substring(1, potentiallyQuotedString.length()-1);
        }
        return potentiallyQuotedString;
    }

}
