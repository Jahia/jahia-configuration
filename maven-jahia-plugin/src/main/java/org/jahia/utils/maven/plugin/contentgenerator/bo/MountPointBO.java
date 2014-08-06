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
