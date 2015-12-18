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
package org.jahia.utils.osgi.parsers;

import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Collection of parsers, organized by phases
 */
public class Parsers {

    public static final String DEFAULT_PARSERS_PROPERTY_FILE_PATH = "org/jahia/utils/osgi/default-parsers.properties";
    List<SortedSet<FileParser>> parsersByPhase;

    private final static Parsers instance = new Parsers();

    public Parsers() {
        InputStream defaultParsersStream = this.getClass().getClassLoader().getResourceAsStream(DEFAULT_PARSERS_PROPERTY_FILE_PATH);
        if (defaultParsersStream != null) {
            Properties defaultParsersProperties = new Properties();
            try {
                defaultParsersProperties.load(defaultParsersStream);
                int i=0;
                String defaultParsersStr = defaultParsersProperties.getProperty("phase." + Integer.toString(i) + ".parsers");
                while (defaultParsersStr != null) {
                    String[] defaultParsers = defaultParsersStr.split(",");
                    for (String defaultParser : defaultParsers) {
                        try {
                            Class defaultParserClass = Class.forName(defaultParser);
                            FileParser defaultFileParser = (FileParser) defaultParserClass.newInstance();
                            addParserToPhase(i, defaultFileParser);
                        } catch (ClassNotFoundException e) {
                            System.out.println("Error instantiating default parser " + defaultParser);
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        } catch (InstantiationException e) {
                            System.out.println("Error instantiating default parser " + defaultParser);
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        } catch (IllegalAccessException e) {
                            System.out.println("Error instantiating default parser " + defaultParser);
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    }
                    i++;
                    defaultParsersStr = defaultParsersProperties.getProperty("phase." + Integer.toString(i) + ".parsers");
                }
            } catch (IOException e) {
                System.out.println("Error loading default parsers property file from " + DEFAULT_PARSERS_PROPERTY_FILE_PATH);
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } else {
            System.err.println("Warning : default parsers configuration file " + DEFAULT_PARSERS_PROPERTY_FILE_PATH + " not found, parsers are not initialized !");
        }
    }

    public static Parsers getInstance() {
        return instance;
    }

    public boolean addParserToPhase(int phaseID, FileParser fileParser) {
        SortedSet<FileParser> phaseParsers = getPhaseParsers(phaseID);
        if (!phaseParsers.contains(fileParser)) {
            phaseParsers.add(fileParser);
        }
        return true;
    }

    private SortedSet<FileParser> getPhaseParsers(int phaseID) {
        if (parsersByPhase == null) {
            parsersByPhase = new ArrayList();
        }
        while (phaseID > (parsersByPhase.size()-1) && phaseID < 100) {
            parsersByPhase.add(new TreeSet());
        }
        return parsersByPhase.get(phaseID);
    }

    public boolean canParseForPhase(int phaseID, String fileName) {
        SortedSet<FileParser> phaseParsers = getPhaseParsers(phaseID);
        if (phaseParsers == null || phaseParsers.size() == 0) {
            return false;
        }
        for (FileParser fileParser : phaseParsers) {
            if (fileParser.canParse(fileName)) {
                return true;
            }
        }
        return false;
    }

    public boolean parse(int phaseID,
                         String fileName,
                         InputStream inputStream,
                         String fileParent,
                         boolean externalDependency, boolean optionalDependency, String version, Logger logger, ParsingContext parsingContext) throws IOException {
        SortedSet<FileParser> phaseParsers = getPhaseParsers(phaseID);
        if (phaseParsers == null || phaseParsers.size() == 0) {
            return false;
        }
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        bufferedInputStream.mark(4*1024*1024);
        for (FileParser fileParser : phaseParsers) {
            fileParser.setLogger(logger);
            if (fileParser.canParse(fileName)) {
                if (fileParser.parse(fileName, bufferedInputStream, fileParent, externalDependency, optionalDependency , version, parsingContext)) {
                    return true;
                }
                bufferedInputStream.reset();
            }
        }
        return false;
    }

}
