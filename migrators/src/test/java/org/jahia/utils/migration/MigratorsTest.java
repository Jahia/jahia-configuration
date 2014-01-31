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
        messages = migrateFile("projects/jahia66/definitions.cnd", migrators, byteArrayOutputStream, true);
        displayMessages(messages);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String fileContents = new String(byteArray);
        Assert.assertTrue("New definitions.cnd file shouldn't contain custom CKEditor configuration", !fileContents.contains("richtext[ckeditor.customConfig='"));
        byteArrayOutputStream.reset();
        messages = migrateFile("projects/jahia66/navbar.menu.groovy", migrators, byteArrayOutputStream, true);
        displayMessages(messages);
        byteArray = byteArrayOutputStream.toByteArray();
        fileContents = new String(byteArray);
        Assert.assertTrue("New groovy file shouldn't contain old resolveSite call", !fileContents.contains("currentNode.resolveSite.home"));

        byteArrayOutputStream.reset();
        messages = migrateFile("projects/jahia66/navbar.menu.groovy", migrators, byteArrayOutputStream, false);
        displayMessages(messages);
        byteArray = byteArrayOutputStream.toByteArray();
        fileContents = new String(byteArray);
        Assert.assertTrue("New groovy file should contain old resolveSite call", fileContents.length() == 0);

        messages = migrateFile("projects/jahia66/definitions.cnd", migrators, null, false);
        displayMessages(messages);
        Assert.assertTrue("Two warning should have been issued the the content definition file", messages.size() == 2);

    }

    private List<String> migrateFile(String filePath, Migrators migrators, ByteArrayOutputStream byteArrayOutputStream, boolean performModifications) {
        List<String> messages = new ArrayList<String>();
        InputStream definitionInputStream = this.getClass().getClassLoader().getResourceAsStream(filePath);
        messages.addAll(migrators.migrate(definitionInputStream, byteArrayOutputStream, filePath, new Version("6.6"), new Version("7.0"), performModifications));
        return messages;
    }

    private void displayMessages(List<String> messages) {
        for (String message : messages) {
            System.out.println(message);
        }
    }
}
