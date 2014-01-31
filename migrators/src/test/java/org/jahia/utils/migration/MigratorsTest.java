package org.jahia.utils.migration;

import org.jahia.commons.Version;
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
        migrateFile("projects/jahia66/definitions.cnd", migrators, byteArrayOutputStream);
        byteArrayOutputStream.reset();
        migrateFile("projects/jahia66/navbar.menu.groovy", migrators, byteArrayOutputStream);
    }

    private void migrateFile(String filePath, Migrators migrators, ByteArrayOutputStream byteArrayOutputStream) {
        InputStream definitionInputStream = this.getClass().getClassLoader().getResourceAsStream(filePath);
        migrators.migrate(definitionInputStream, byteArrayOutputStream, filePath, new Version("6.6"), new Version("7.0"), true);
    }
}
