package org.jahia.utils.migration.model;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A migration operation to move a directory or a file
 */
@XmlRootElement(name="move")
@XmlType
public class Move extends MigrationOperation {
    @Override
    public void execute(InputStream inputStream, OutputStream outputStream, String filePath, boolean performModification) {

    }
}
