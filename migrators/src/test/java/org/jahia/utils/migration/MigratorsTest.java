package org.jahia.utils.migration;

import org.jahia.commons.Version;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Migration test units for migrators
 */
public class MigratorsTest {

    @Test
    public void testMigrators() {
        Migrators migrators = Migrators.getInstance();
        List<String> messages = new ArrayList<String>();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        final String projectRoot = "projects/jahia66/generic-templates";

        messages = migrateFile(projectRoot + "/src/main/webapp/META-INF/definitions.cnd", migrators, byteArrayOutputStream, true);
        displayMessages(messages);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String fileContents = new String(byteArray);
        Assert.assertTrue("New definitions.cnd file shouldn't contain custom CKEditor configuration", !fileContents.contains("richtext[ckeditor.customConfig='"));
        byteArrayOutputStream.reset();
        messages = migrateFile(projectRoot + "/src/main/webapp/genericnt_navbar/html/navbar.menu.groovy", migrators, byteArrayOutputStream, true);
        displayMessages(messages);
        byteArray = byteArrayOutputStream.toByteArray();
        fileContents = new String(byteArray);
        Assert.assertTrue("New groovy file shouldn't contain old resolveSite call", !fileContents.contains("currentNode.resolveSite.home"));

        byteArrayOutputStream.reset();
        messages = migrateFile(projectRoot + "/src/main/webapp/genericnt_navbar/html/navbar.menu.groovy", migrators, byteArrayOutputStream, false);
        displayMessages(messages);
        byteArray = byteArrayOutputStream.toByteArray();
        fileContents = new String(byteArray);
        Assert.assertTrue("New groovy file should contain old resolveSite call", fileContents.length() == 0);

        messages = migrateFile(projectRoot + "/src/main/webapp/META-INF/definitions.cnd", migrators, null, false);
        displayMessages(messages);
        Assert.assertTrue("Two warning should have been issued the the content definition file", messages.size() == 2);

        messages = migrateFile(projectRoot + "/untouched.txt", migrators, null, false);
        Assert.assertTrue("File should not be touched by migrators", messages.size() == 0);

        byteArrayOutputStream.reset();
        messages = migrateFile(projectRoot + "/src/main/webapp/META-INF/rules.drl", migrators, byteArrayOutputStream, true);
        displayMessages(messages);
        byteArray = byteArrayOutputStream.toByteArray();
        fileContents = new String(byteArray);
        Assert.assertTrue("New rule file should not contain any hash characters", !fileContents.contains("#"));
        Assert.assertTrue("New rule file should not contain reference to class org.drools.spi.KnowledgeHelper", !fileContents.contains("org.drools.spi.KnowledgeHelper"));

    }

    private List<String> migrateFile(String filePath, Migrators migrators, ByteArrayOutputStream byteArrayOutputStream, boolean performModifications) {
        List<String> messages = new ArrayList<String>();
        InputStream definitionInputStream = this.getClass().getClassLoader().getResourceAsStream(filePath);
        if (migrators.willMigrate(definitionInputStream, filePath, new Version("6.6"), new Version ("7.0"))) {
            definitionInputStream = this.getClass().getClassLoader().getResourceAsStream(filePath);
            messages.addAll(migrators.migrate(definitionInputStream, byteArrayOutputStream, filePath, new Version("6.6"), new Version("7.0"), performModifications));
        }
        return messages;
    }

    private void displayMessages(List<String> messages) {
        for (String message : messages) {
            System.out.println(message);
        }
    }
}
