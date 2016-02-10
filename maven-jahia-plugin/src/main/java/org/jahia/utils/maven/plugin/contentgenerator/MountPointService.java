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

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MountPointService {
	private Log logger = new SystemStreamLog();
	private String sep;

	public MountPointService() {
		sep = System.getProperty("file.separator");
	}

	public File createAndPopulateRepositoryFile(File tempOutputDir, File mountPointFile)
			throws IOException {
		File repositoryFile = new File(tempOutputDir, "repository.xml");

		FileOutputStream output = new FileOutputStream(repositoryFile, true);
		IOUtils.write(getHeader(), output);

		// there is an XML files for attachments only if we requested some
		if (mountPointFile != null) {
			IOUtils.copy(new FileInputStream(mountPointFile), output);
		}
		
		IOUtils.write(getFooter(), output);

		return repositoryFile;
	}

    public String getHeader() {
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n");
        sb.append("<content xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" xmlns:j=\"http://www.jahia.org/jahia/1.0\">\n");
        sb.append("<mounts jcr:primaryType=\"jnt:mounts\">\n");
        return sb.toString();
    }

    public String getFooter() {
        StringBuffer sb = new StringBuffer();
        sb.append("</mounts>\n");
        sb.append("</content>\n");
        return sb.toString();
    }

}
