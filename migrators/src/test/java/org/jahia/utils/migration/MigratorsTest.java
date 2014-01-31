package org.jahia.utils.migration;

import org.jahia.commons.Version;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Migration test units for migrators
 */
public class MigratorsTest {

    @Test
    public void testMigrators() {
        Migrators migrators = Migrators.getInstance();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        migrateFile("projects/jahia66/definitions.cnd", migrators, byteArrayOutputStream, true);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String fileContents = new String(byteArray);
        Assert.assertTrue("New definitions.cnd file shouldn't contain custom CKEditor configuration", !fileContents.contains("richtext[ckeditor.customConfig='"));
        byteArrayOutputStream.reset();
        migrateFile("projects/jahia66/navbar.menu.groovy", migrators, byteArrayOutputStream, true);
        byteArray = byteArrayOutputStream.toByteArray();
        fileContents = new String(byteArray);
        Assert.assertTrue("New groovy file shouldn't contain old resolveSite call", !fileContents.contains("currentNode.resolveSite.home"));

        byteArrayOutputStream.reset();
        migrateFile("projects/jahia66/navbar.menu.groovy", migrators, byteArrayOutputStream, false);
        byteArray = byteArrayOutputStream.toByteArray();
        fileContents = new String(byteArray);
        Assert.assertTrue("New groovy file should contain old resolveSite call", fileContents.length() == 0);
    }

    private void migrateFile(String filePath, Migrators migrators, ByteArrayOutputStream byteArrayOutputStream, boolean performModifications) {
        InputStream definitionInputStream = this.getClass().getClassLoader().getResourceAsStream(filePath);
        migrators.migrate(definitionInputStream, byteArrayOutputStream, filePath, new Version("6.6"), new Version("7.0"), performModifications);
    }
}
