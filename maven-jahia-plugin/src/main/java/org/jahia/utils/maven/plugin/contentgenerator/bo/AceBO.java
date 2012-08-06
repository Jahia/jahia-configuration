package org.jahia.utils.maven.plugin.contentgenerator.bo;

import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Element;

public class AceBO {
	
	private Element aceElement;
	
	private String permission; // GRANT, DENY
	
	private String type; //u, g
	
	private String userOrGroupName;
	
	private String roles;
	
	public AceBO(String userOrGroupName, String type, String permission,  String roles) {
		this.permission = permission;
		this.type = type;
		this.userOrGroupName = userOrGroupName;
		this.roles = roles;
	}
	
  public Element getElement() {
	  String elementName = permission + "_" + type + "_" + userOrGroupName;
	  if (aceElement == null) {
		  aceElement = new Element(elementName);
		  aceElement.setAttribute("primaryType", "jnt:ace", ContentGeneratorCst.NS_JCR);
		  aceElement.setAttribute("aceType", permission, ContentGeneratorCst.NS_J);
		  aceElement.setAttribute("principal", type + ":" + userOrGroupName, ContentGeneratorCst.NS_J);
		  aceElement.setAttribute("protected", "false", ContentGeneratorCst.NS_J);
		  aceElement.setAttribute("roles", roles, ContentGeneratorCst.NS_J);
	  }
	  
	  return aceElement;
  }
}
