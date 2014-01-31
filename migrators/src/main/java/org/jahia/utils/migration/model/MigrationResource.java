package org.jahia.utils.migration.model;

import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A resource to migrate
 */
@XmlType
public class MigrationResource {

    private String pattern;
    private Pattern compiledPattern;
    private List<MigrationOperation> operations = new ArrayList<MigrationOperation>();

    public void setPattern(String pattern) {
        this.pattern = pattern;
        this.compiledPattern = Pattern.compile(pattern);
    }

    public Pattern getCompiledPattern() {
        return compiledPattern;
    }

    public List<MigrationOperation> getOperations() {
        return operations;
    }

    public void setOperations(List<MigrationOperation> operations) {
        this.operations = operations;
    }
}
