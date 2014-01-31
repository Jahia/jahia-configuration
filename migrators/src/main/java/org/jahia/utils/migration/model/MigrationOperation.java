package org.jahia.utils.migration.model;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Super class for all migration operations
 */
public abstract class MigrationOperation {

    private MigrationResource migrationResource;

    public abstract List<String> execute(InputStream inputStream, OutputStream outputStream, String filePath, boolean performModification);

}
