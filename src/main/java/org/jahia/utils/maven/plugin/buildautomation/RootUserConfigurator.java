/**
 *
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2009 Jahia Limited. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have recieved a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license"
 *
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Limited. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */
package org.jahia.utils.maven.plugin.buildautomation;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.FileUtils;

import java.util.Map;
import java.io.*;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : rincevent
 * @since : JAHIA 6.1
 *        Created : 9 juil. 2009
 */
public class RootUserConfigurator {
    public static void updateConfiguration(String sourceFileName, String destFileName, String rootPassword ) throws IOException {
        File sourceConfigFile = new File(sourceFileName);
        File destConfigFile = new File(destFileName);
        if (sourceConfigFile.exists()) {
            // let's load the file's content in memory, assuming it won't be
            // too big.
            String fileContent = FileUtils.readFileToString(sourceConfigFile, "UTF-8");

                fileContent = StringUtils.replace(fileContent, "ROOT_NAME_PLACEHOLDER", "root");

                fileContent = StringUtils.replace(fileContent, "@ROOT_PASSWORD@", rootPassword);
                StringBuffer userProperties = new StringBuffer();
                fileContent = StringUtils.replace(fileContent, "root_user_properties=\"\"", "");
                // we have finished replacing values, let's save the modified
                // file.
                FileUtils.writeStringToFile(destConfigFile, fileContent, "UTF-8");           
        }
    }
}
