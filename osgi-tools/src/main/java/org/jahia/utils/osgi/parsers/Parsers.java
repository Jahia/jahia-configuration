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

    public boolean parse(int phaseID,
                         String fileName,
                         InputStream inputStream,
                         ParsingContext parsingContext,
                         boolean externalDependency,
                         Logger logger) throws IOException {
        SortedSet<FileParser> phaseParsers = getPhaseParsers(phaseID);
        if (phaseParsers == null || phaseParsers.size() == 0) {
            return false;
        }
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        bufferedInputStream.mark(4*1024*1024);
        for (FileParser fileParser : phaseParsers) {
            fileParser.setLogger(logger);
            if (fileParser.canParse(fileName)) {
                if (fileParser.parse(fileName, bufferedInputStream, parsingContext, externalDependency)) {
                    return true;
                }
                bufferedInputStream.reset();
            }
        }
        return false;
    }

}
