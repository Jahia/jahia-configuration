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
// $Id: DatabaseScripts.java 26186 2009-03-27 08:31:06Z bpapez $
//
//  DatabaseScripts
//
//  30.03.2001  AK  added in jahia.
//  01.04.2001  AK  change the package.
//

package org.jahia.configuration.configurators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 * desc:  This class is used by the installation and the administration
 * to get all informations required from database scripts, like msaccess.script
 * or hypersonic.script, from the jahia database script path (a jahiafiles
 * subfolder).
 *
 * Copyright:    Copyright (c) 2002
 * Company:      Jahia Ltd
 *
 * @author Alexandre Kraft
 * @version 1.0
 */
public final class DatabaseScripts {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseScripts.class);

    /**
     * Default constructor.
     * @author  Alexandre Kraft
     */
    private DatabaseScripts()
    {
        super();
    }


    /**
     * Retrieves SQL statement for schema creation, by way of the database
     * dependent configuration file.
     * @param fileObject File the database configuration file
     * @throws java.io.IOException thrown if there was an error opening or parsing
     * the files
     * @return Iterator an Iterator of String objects containing the
     * schema creation SQL statements.
     */
    public static List<String> getSchemaSQL( File fileObject )
        throws IOException {
        Properties scriptProperties = new Properties();
        try (FileInputStream scriptInputStream = new FileInputStream(fileObject.getPath())) {
            scriptProperties.load( scriptInputStream );
        }

        String scriptLocation = scriptProperties.getProperty("jahia.database.schemascriptdir");
        File parentFile = fileObject.getParentFile();
        File schemaDir = new File(parentFile, scriptLocation);

        return getSQLStatementsInDir(schemaDir, ".sql");
    }

    /**
     * Retrieves all the statement in a directory for files with a
     * specific extension (usually ".sql")
     * @param sqlDir File the directory in which to search for SQL files.
     * @param extension String extension for files, in lowercase. May be null
     * in which case all the files will be used.
     * @throws java.io.IOException
     * @return ArrayList
     */
    public static List<String> getSQLStatementsInDir (File sqlDir, final String extension)
        throws IOException {
        List<String> result = new ArrayList<>();
        File[] schemaFiles = sqlDir.listFiles((dir, name) -> {
            if (extension != null) {
                return name.toLowerCase().endsWith(extension);
            } else {
                return true;
            }
        });
        if (schemaFiles == null) {
            return result;
        }

        // sort found files in alphabetical order
        Arrays.sort(schemaFiles);

        List<File> indexFiles = new ArrayList<>();
        for (int i=0; i < schemaFiles.length; i++) {
            File sqlFile = schemaFiles[i];
            if(sqlFile.getName().endsWith("index.sql")) {
                indexFiles.add(sqlFile);
            } else {
                logger.info("Loading statements from script file: {}", sqlFile);
                List<String> curFileSQL = getScriptFileStatements(sqlFile);
                result.addAll(curFileSQL);
            }
        }
        for (int i = 0; i < indexFiles.size(); i++) {
            File indexFile = indexFiles.get(i);
            logger.info("Loading statements from index script file: {}", indexFile);
            List<String> curFileSQL = getScriptFileStatements(indexFile);
            result.addAll(curFileSQL);
        }
        return result;
    }

    /**
     * Get a Iterator containing all lines of the sql runtime from a
     * database script. This database script is getted in parameter like
     * a File object. The method use the BufferedReader object on a
     * FileReader object instanciate on the script file name.
     * @author  Alexandre Kraft
     *
     * @param   fileObject   File object of the database script file.
     * @return  Iterator containing all lines of the database script.
     */
    private static List<String> getScriptFileStatements( File fileObject )
    throws IOException
    {
        return org.jahia.commons.DatabaseScripts.getScriptStatements(new FileReader(fileObject));
    }

}