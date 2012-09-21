package org.jahia.utils.maven.plugin.contentgenerator.bo;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jahia.utils.maven.plugin.contentgenerator.wise.bo.CollectionBO;
import org.jdom.Element;

public class UserBO {
	private String name;

	private String password;

	private String jcrPath;

    private String email;
    
    private List<CollectionBO> collections;

	public UserBO(String name, String password, String pathJcr, List<CollectionBO> collections) {
		this.name = name;
		this.password = password;
        this.email = this.name + "@example.com";
		this.jcrPath = pathJcr;
		this.collections = collections;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPathJcr() {
		return jcrPath;
	}

	public void setPathJcr(String pathJcr) {
		this.jcrPath = pathJcr;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	public List<CollectionBO> getCollections() {
		return collections;
	}

	public String getDirectoryName(int indexDir) {
		String dirname = null;
		if (this.jcrPath != null) {
			String[] directories = StringUtils.split(this.jcrPath, "/");
			dirname = directories[indexDir];
		}
		return dirname;
	}

	public Element getJcrXml() {
		Element userElement = new Element(this.name);
		userElement.setAttribute("email", this.email);
		userElement.setAttribute("emailNotificationsDisabled", Boolean.FALSE.toString());
		userElement.setAttribute("accountLocked", Boolean.FALSE.toString(), ContentGeneratorCst.NS_J);
		userElement.setAttribute("email", this.email, ContentGeneratorCst.NS_J);
		userElement.setAttribute("external", Boolean.FALSE.toString(), ContentGeneratorCst.NS_J);
		userElement.setAttribute("firstName", this.name + " firstname", ContentGeneratorCst.NS_J);
		userElement.setAttribute("lastName", this.name + " lastname", ContentGeneratorCst.NS_J);
		userElement.setAttribute("organization", "Organization", ContentGeneratorCst.NS_J);
		userElement.setAttribute("password", this.password, ContentGeneratorCst.NS_J); // W6ph5Mm5Pz8GgiULbPgzG37mj9g
		// TODO : picture
		userElement.setAttribute("picture", this.jcrPath + "/files/profile/publisher.png", ContentGeneratorCst.NS_J); //

		userElement.setAttribute("published", Boolean.TRUE.toString(), ContentGeneratorCst.NS_J);
		userElement.setAttribute("firstName", this.name + " firstname", ContentGeneratorCst.NS_J);

		userElement.setAttribute("mixinTypes", "jmix:accessControlled", ContentGeneratorCst.NS_JCR);
		userElement.setAttribute("primaryType", "jnt:user", ContentGeneratorCst.NS_JCR);

		// userElement.setAttribute("password.history.1242739225417", null);
		userElement.setAttribute("preferredLanguage", "en");

		if (collections != null) {
			Element collectionsElement = new Element("collections");
			collectionsElement.setAttribute("primaryType", "docnt:collections", ContentGeneratorCst.NS_JCR);
			collectionsElement.setAttribute("createdBy", name, ContentGeneratorCst.NS_JCR);
			
			for (Iterator<CollectionBO> iterator = collections.iterator(); iterator.hasNext();) {
				CollectionBO collection = iterator.next();
				collectionsElement.addContent(collection.getElement());
			}
			userElement.addContent(collectionsElement);
		}
		
		if (this.jcrPath == null) {
			return userElement;
		}
		
		String dir1 = getDirectoryName(1);
		String dir2 = getDirectoryName(2);
		String dir3 = getDirectoryName(3);

		Element root = new Element(dir1);
		root.setAttribute("primaryType", "jnt:usersFolder", ContentGeneratorCst.NS_JCR);

		Element subElement1 = new Element(dir2);
		subElement1.setAttribute("primaryType", "jnt:usersFolder", ContentGeneratorCst.NS_JCR);
		root.addContent(subElement1);

		Element subElement2 = new Element(dir3);
		subElement2.setAttribute("primaryType", "jnt:usersFolder", ContentGeneratorCst.NS_JCR);
		subElement1.addContent(subElement2);

		subElement2.addContent(userElement);
		return root;
	}
	
	/**
	 * Return XML code to add this user as a member of a group
	 * Example:  <user1 j:member="/users/gg/if/fh/user1" jcr:primaryType="jnt:member"/>
	 * @return Element userName
	 */
	public Element getUserMemberXml() {
		Element lightElement =  new Element(this.name);
		lightElement.setAttribute("member", jcrPath, ContentGeneratorCst.NS_J);
		lightElement.setAttribute("primaryType", "jnt:member", ContentGeneratorCst.NS_JCR);
		return lightElement;
	}
}
