package org.jahia.utils.maven.plugin.contentgenerator;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jahia.configuration.configurators.JahiaGlobalConfigurator;
import org.jahia.utils.maven.plugin.contentgenerator.bo.GroupBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.UserBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jdom.Document;
import org.jdom.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UserGroupService {

    private Log logger = new SystemStreamLog();

    private static String sep = System.getProperty("file.separator");
    ;

    public Document createUsersRepository(List<UserBO> users) {
        String jcrDate = ContentGeneratorService.getInstance().getDateForJcrImport(null);

        Document doc = new Document();
        Element contentNode = new Element("content");
        doc.setRootElement(contentNode);

        Element usersNode = new Element("users");
        contentNode.addContent(usersNode);

        UserBO rootUser = new UserBO("root", JahiaGlobalConfigurator.encryptPassword("root"), null);
        Element rootUserNode = rootUser.getJcrXml();
        usersNode.addContent(rootUserNode);

        for (Iterator<UserBO> iterator = users.iterator(); iterator.hasNext(); ) {
            UserBO userBO = iterator.next();
            usersNode.addContent(userBO.getJcrXml());
        }

        return doc;
    }

    public File createFileTreeForUsers(List<UserBO> users, File tempDirectory) throws IOException {
        ClassLoader cl = this.getClass().getClassLoader();
        OutputService os = new OutputService();

        File f = new File(tempDirectory + sep + "content" + sep + "users");
        FileUtils.forceMkdir(f);

        File dirUser;
        for (Iterator<UserBO> iterator = users.iterator(); iterator.hasNext(); ) {
            UserBO userBO = iterator.next();
            logger.debug("Creates directories tree for user " + userBO.getName());
            dirUser = new File(f + sep + userBO.getDirectoryName(1) + sep + userBO.getDirectoryName(2) + sep
                    + userBO.getDirectoryName(3) + sep + userBO.getName() + sep + "files" + sep + "profiles" + sep
                    + "publisher.png");
            FileUtils.forceMkdir(dirUser);

            File thumbnail = new File(dirUser + sep + "publisher.png");
            os.writeInputStreamToFile(cl.getResourceAsStream("publisher.png"), thumbnail);
        }
        return f;
    }

    public Element generateJcrGroups(String siteKey, List<GroupBO> groups) {
        logger.info("Users and groups generated, creation of JCR document...");
        Element groupsNode = new Element("groups");

        // site-administrators node
        Element siteAdminNode = new Element("site-administrators");
        siteAdminNode.setAttribute("mixinTypes", "jmix:systemNode", ContentGeneratorCst.NS_JCR);
        siteAdminNode.setAttribute("primaryType", "jnt:group", ContentGeneratorCst.NS_JCR);
        groupsNode.addContent(siteAdminNode);

        Element jmembersSiteAdmin = new Element("members", ContentGeneratorCst.NS_J);
        jmembersSiteAdmin.setAttribute("primaryType", "jnt:member", ContentGeneratorCst.NS_JCR);
        siteAdminNode.addContent(jmembersSiteAdmin);

        Element rootUser = new Element("root");
        rootUser.setAttribute("member", "/users/root", ContentGeneratorCst.NS_J);
        rootUser.setAttribute("primaryType", "jnt:member", ContentGeneratorCst.NS_JCR);
        jmembersSiteAdmin.addContent(rootUser);

        // site-privileged node
        Element sitePrivilegedNode = new Element("site-privileged");
        sitePrivilegedNode.setAttribute("mixinTypes", "systemNode", ContentGeneratorCst.NS_JMIX);
        sitePrivilegedNode.setAttribute("primaryType", "jnt:group", ContentGeneratorCst.NS_JCR);
        sitePrivilegedNode.setAttribute("hidden", "false", ContentGeneratorCst.NS_J);
        groupsNode.addContent(sitePrivilegedNode);

        Element jmembersSitePrivileged = new Element("members", ContentGeneratorCst.NS_J);
        jmembersSitePrivileged.setAttribute("primaryType", "jnt:member", ContentGeneratorCst.NS_JCR);
        sitePrivilegedNode.addContent(jmembersSitePrivileged);

        Element siteAdminGroup = new Element("site-administrators");
        siteAdminGroup.setAttribute("member", "/sites/" + siteKey + "/groups/site-administrators",
                ContentGeneratorCst.NS_J);
        siteAdminGroup.setAttribute("primaryType", "jnt:member", ContentGeneratorCst.NS_JCR);
        siteAdminGroup.setAttribute("hidden", "false", ContentGeneratorCst.NS_J);
        jmembersSitePrivileged.setContent(siteAdminGroup);

        for (Iterator<GroupBO> iterator = groups.iterator(); iterator.hasNext(); ) {
            GroupBO group = iterator.next();

            Element groupNode = group.getJcrXml();
            groupsNode.addContent(groupNode);
        }
        return groupsNode;
    }

    public List<UserBO> generateUsers(Integer nbUsers) {
        logger.info(nbUsers + " users are going to be generated");

        List<UserBO> users = new ArrayList<UserBO>();
        for (int userid = 1; userid < (nbUsers+1); userid++) {

            String username = "user" + userid;
            String pathJcr = getPathForUsername(username);
            UserBO user = new UserBO(username, JahiaGlobalConfigurator.encryptPassword(username), pathJcr);
            users.add(user);
        }
        return users;
    }

    public List<GroupBO> generateGroups(Integer nbGroups, Integer nbUsersPerGroup, List<UserBO> users) {
        logger.info(nbGroups + " groups are going to be generated");

        List<GroupBO> groups = new ArrayList<GroupBO>();

        int cptGroups = 1;
        int cptUsers = 0;

        while (cptGroups <= nbGroups) {
            List<UserBO> usersForGroup = new ArrayList<UserBO>();
            int total = cptUsers + nbUsersPerGroup;
            for (; cptUsers < total; cptUsers++) {
                usersForGroup.add(users.get(cptUsers % users.size()));
            }

            GroupBO group = new GroupBO("group" + cptGroups, usersForGroup);
            groups.add(group);
            cptGroups++;
        }
        return groups;
    }

    public String getPathForUsername(String username) {

        StringBuilder builder = new StringBuilder();

        int userNameHashcode = Math.abs(username.hashCode());
        String firstFolder = getFolderName(userNameHashcode).toLowerCase();
        userNameHashcode = Math.round(userNameHashcode / 100);
        String secondFolder = getFolderName(userNameHashcode).toLowerCase();
        userNameHashcode = Math.round(userNameHashcode / 100);
        String thirdFolder = getFolderName(userNameHashcode).toLowerCase();
        return builder.append("/users").append("/").append(firstFolder).append("/").append(secondFolder).append("/")
                .append(thirdFolder).append("/").append(username).toString();
    }

    private String getFolderName(int userNameHashcode) {
        int i = (userNameHashcode % 100);
        return Character.toString((char) ('a' + Math.round(i / 10))) + Character.toString((char) ('a' + (i % 10)));
    }

    public Integer getNbUsersPerGroup(Integer nbUsers, Integer nbGroups) {
        Integer nbUsersPerGroup = nbUsers / nbGroups;
        return nbUsersPerGroup;
    }

    public Integer getNbUsersRemaining(Integer nbUsers, Integer nbGroups) {
        Integer nbUsersRemaining = nbUsers % nbGroups;
        return nbUsersRemaining;
    }

}