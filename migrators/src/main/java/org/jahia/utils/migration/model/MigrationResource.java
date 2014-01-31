package org.jahia.utils.migration.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A resource to migrate
 */
@XmlType(name="resource")
public class MigrationResource {

    private String antPattern;
    private Pattern compiledPattern;
    private List<MigrationOperation> operations = new ArrayList<MigrationOperation>();

    @XmlAttribute(name="pattern")
    public void setAntPattern(String antPattern) {
        this.antPattern = antPattern;
        String pattern = antPattern;
        pattern = pattern.replaceAll("\\.", "\\\\.");
        pattern = pattern.replaceAll("\\?", ".");
        pattern = pattern.replaceAll("\\*", ".*");
        pattern = pattern.replaceAll("\\.\\*\\.\\*/", ".*/");
        this.compiledPattern = Pattern.compile(pattern);
    }

    public Pattern getCompiledPattern() {
        return compiledPattern;
    }

    public List<MigrationOperation> getOperations() {
        return operations;
    }

    @XmlElementRefs( {
            @XmlElementRef(name = "replace", type = ReplaceText.class),
            @XmlElementRef(name = "move",   type = Move.class) })
    public void setOperations(List<MigrationOperation> operations) {
        this.operations = operations;
    }
}
