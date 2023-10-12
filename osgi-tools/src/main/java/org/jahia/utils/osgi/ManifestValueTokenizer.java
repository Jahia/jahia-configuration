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
