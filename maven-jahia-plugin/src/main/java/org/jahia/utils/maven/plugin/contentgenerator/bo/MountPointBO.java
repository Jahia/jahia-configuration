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
package org.jahia.utils.maven.plugin.contentgenerator.bo;

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

public class MountPointBO {
	private Element mountsElement;
	
	private String mountPointType;
	
	private String mountPointRepositoryId;
	
	private String mountPointUrl;
	
	private String mountPointUser;
	
	private String mountPointPassword;
	
	private String mountPointName = ContentGeneratorCst.MOUNT_POINT_NAME;
	
	public String getMountPointName() {
		return this.mountPointName;
	}
	
	public MountPointBO(String mountPointType, String mountPointRepositoryId, String mountPointUrl, String mountPointUser, String mountPointPassword) {
		this.mountPointType = mountPointType;		
		this.mountPointRepositoryId = mountPointRepositoryId;		
		this.mountPointUrl = mountPointUrl;
		this.mountPointUser = mountPointUser;		
		this.mountPointPassword = mountPointPassword;
	}
	
	public Element getElement() {
		if (mountsElement == null) {
			
			mountsElement = new Element("mounts");
			mountsElement.setAttribute("primaryType", "jnt:mounts", ContentGeneratorCst.NS_JCR);
			mountsElement.setAttribute("nodename", "jnt:mounts", ContentGeneratorCst.NS_J);
			
			Element mountPointElement = new Element(this.mountPointName);
			
			if (this.mountPointType.equals(ContentGeneratorCst.MOUNT_POINT_CMIS)) {
				mountPointElement.setAttribute("primaryType", "cmis:cmisMountPoint", ContentGeneratorCst.NS_JCR);
				
//				mountPointElement.setAttribute("path", "/", ContentGeneratorCst.NS_CMIS);
//				mountPointElement.setAttribute("objectTypeId", "cmis:folder", ContentGeneratorCst.NS_CMIS);
//				mountPointElement.setAttribute("objectId", "workspace://SpacesStore/" + mountPointUUID, ContentGeneratorCst.NS_CMIS);
//				mountPointElement.setAttribute("baseTypeId", "cmis:folder", ContentGeneratorCst.NS_CMIS);
				
				mountPointElement.setAttribute("user", mountPointUser);
				mountPointElement.setAttribute("password", mountPointPassword);
				mountPointElement.setAttribute("url", mountPointUrl);
				mountPointElement.setAttribute("repositoryId", mountPointRepositoryId);
			}
			
			mountsElement.addContent(mountPointElement);
		}
		return mountsElement;
	}
}
