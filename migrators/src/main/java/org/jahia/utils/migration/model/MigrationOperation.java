package org.jahia.utils.migration.model;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Super class for all migration operations
 */
public abstract class MigrationOperation {

    private MigrationResource migrationResource;

    /**
     * Test if the migration operation will execute on the specified input stream
     * @param inputStream
     * @param filePath
     * @return
     */
    public abstract boolean willMigrate(InputStream inputStream, String filePath);

    /**
     * Execute the migration operation on the specified input stream
     * @param inputStream
     * @param outputStream
     * @param filePath
     * @param performModification set to true if the modification should be generated in the
     *                            output stream, false will not output any modifications.
     * @return
     */
    public abstract List<String> execute(InputStream inputStream, OutputStream outputStream, String filePath, boolean performModification);

}
