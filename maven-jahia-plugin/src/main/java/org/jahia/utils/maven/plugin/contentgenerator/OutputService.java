/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.contentgenerator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jahia.utils.maven.plugin.contentgenerator.bo.FolderBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.PageBO;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Class used to write data into output files/directories
 *
 * @author Guillaume Lucazeau
 *
 */
public class OutputService {
    private Log logger = new SystemStreamLog();
    private String sep;

    public OutputService() {
        sep = System.getProperty("file.separator");
    }

    public void initOutputFile(File f) throws IOException {
        // if file already exist, we empty it
        FileUtils.writeStringToFile(f, "", "UTF-8");
    }

    public void appendStringToFile(File f, String s) throws IOException {
        OutputStreamWriter fwriter = new OutputStreamWriter(new FileOutputStream(f, true), "UTF-8");
        BufferedWriter fOut = new BufferedWriter(fwriter);
        fOut.write(s);
        fOut.close();
    }

    public void appendPagesToFile(File f, List<PageBO> listePages) throws IOException {
        for (Iterator<PageBO> iterator = listePages.iterator(); iterator.hasNext();) {
            PageBO page = iterator.next();
            appendPageToFile(f, page);
        }
    }

    public void appendPageToFile(File f, PageBO page) throws IOException {
        appendStringToFile(f, page.toString());
    }

    public void appendFolderToFile(File f, FolderBO folder) throws IOException {
        appendStringToFile(f, folder.toString());
    }

    public void appendPathToFile(File f, List<String> paths) throws IOException {
        for (Iterator<String> iterator = paths.iterator(); iterator.hasNext();) {
            String path = iterator.next();
            path = path + "\n";
            appendStringToFile(f, path);
        }
    }

    public File createSiteArchive(String archiveName, String outputPath, List<File> filesToArchive) {
        File newZipArchive = null;
        try {
            // Creates the ZIP file
            ZipOutputStream zipOutput = new ZipOutputStream(new FileOutputStream(outputPath + sep + archiveName));

            // Process each file or directory to add
            for (Iterator<File> iterator = filesToArchive.iterator(); iterator.hasNext();) {
                File f = iterator.next();
                zipFile(f, zipOutput);
            }
            zipOutput.close();
            newZipArchive = new File(outputPath + sep + archiveName);
        } catch (IOException e) {
            logger.error("Can not create ZIP file: ",e);
        }
        return newZipArchive;
    }

    /**
     * Zip a file, get the filename as ZIP entry name
     *
     * @param f
     * @param out
     * @throws IOException
     */
    private void zipFile(File f, ZipOutputStream out) throws IOException {
        zipFile(f, f.getName(), out);
    }

    /**
     * Zip files and directories Call itself for sub files/sub directories
     *
     * @param f
     * @param fileName
     * @param out
     * @throws IOException
     */
    // name is the name for the file
    private void zipFile(File f, String fileName, ZipOutputStream out) throws IOException {

        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File subFile : files) {
                    String childName = fileName + sep + subFile.getName();
                    zipFile(subFile, childName, out);
                }
            }
        } else {
            FileInputStream in = new FileInputStream(f);
            byte[] buf = new byte[1024];

            // Add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(fileName));

            // Transfer bytes from the file to the ZIP file
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            // Complete the entry
            out.closeEntry();
            in.close();
        }
    }

    public void writeJdomDocumentToFile(Document doc, File file) throws IOException {
        XMLOutputter out = new XMLOutputter();

        OutputStreamWriter fwriter = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        BufferedWriter fOut = new BufferedWriter(fwriter);

        Format prettyFormat = Format.getPrettyFormat();
        prettyFormat.setOmitDeclaration(true);
        out.setFormat(prettyFormat);

        out.output(doc, fOut);
        fOut.flush();
        fOut.close();
    }

    public void writeInputStreamToFile(InputStream is, File f) throws IOException {

        OutputStream out = new FileOutputStream(f);

        int read=0;
        byte[] bytes = new byte[1024];

        while((read = is.read(bytes))!= -1){
            out.write(bytes, 0, read);
        }

        is.close();
        out.flush();
        out.close();
    }

    /**
     * Remove spaces and lower-cases the string
     * @param s
     * @return String
     */
    public String formatStringForXml(String s) {
        String s2 = StringUtils.replace(s, " ", "");
        StringUtils.lowerCase(s2);
        return s2;
    }

    public void writePropertiesToFile(Properties properties, File file) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        try {
            properties.store(out, null);
        } finally {
            out.close();
        }
    }
}
