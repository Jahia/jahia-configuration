package org.jahia.utils.osgi;

/**
 * A tokenizer for an OSGi Manifest Header value according to section 3.2.4 "Common header syntax" of the OSGi
 * core specification : http://www.osgi.org/download/r5/osgi.core-5.0.0.pdf
 */
public class ManifestValueTokenizer {

    enum TokenizeState { IN_STRING, POSSIBLE_DELIMITER };
    private char[] stringToTokenize;
    private int stringLength;
    private int charPos = 0;
    private char matchedDelimiter = 0;

    public ManifestValueTokenizer(String stringToTokenize) {
        this.stringToTokenize = stringToTokenize.toCharArray();
        this.stringLength = stringToTokenize.length();
    }

    public int eatWhiteSpace() {
        if (charPos >= stringLength) {
            return charPos;
        }
        char c = stringToTokenize[charPos];
        while ((c == ' ') || (c == '\t') || (c == '\n') || (c == '\r')) {
            charPos++;
            if (charPos >= stringLength) {
                return charPos;
            }
            c = stringToTokenize[charPos];
        }
        return charPos;
    }

    public String getNextToken(String delimiters) {
        eatWhiteSpace();
        StringBuffer nextToken = new StringBuffer();
        if (charPos >= stringLength) {
            return nextToken.toString();
        }
        while (charPos < stringLength) {
            char currentChar = stringToTokenize[charPos];
            if (stringToTokenize[charPos] == '\"') {
                nextToken.append(currentChar);
                charPos++;
                while (charPos < stringLength) {
                    currentChar = stringToTokenize[charPos];
                    if (currentChar == '\\') {
                        charPos++;
                        if (charPos >= stringLength) {
                            // illegal escape at the end of the string, this should not happen.
                            return nextToken.toString();
                        } else {
                            currentChar = stringToTokenize[charPos];
                        }
                    }
                    if (currentChar == '\"') {
                        // we found the end of the string, we simply ignore this character.
                        break;
                    } else {
                        nextToken.append(currentChar);
                    }
                    charPos++;
                }
            }
            if (delimiters.indexOf(currentChar) > -1) {
                matchedDelimiter = currentChar;
                charPos++;
                return nextToken.toString();
            }
            nextToken.append(currentChar);
            charPos++;
        }
        return nextToken.toString();
    }

    public char getMatchedDelimiter() {
        return matchedDelimiter;
    }

    public char peekCurrentChar() {
        if (charPos < stringLength) {
            return stringToTokenize[charPos];
        } else {
            return 0;
        }
    }

    public char getNextChar() {
        charPos++;
        if (charPos < stringLength) {
            return stringToTokenize[charPos];
        } else {
            return 0;
        }
    }
}
