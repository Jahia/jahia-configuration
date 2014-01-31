package org.jahia.utils.migration.model;

import org.jahia.commons.Version;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

/**
 * Utility class to replace all the Strings matching a regex pattern in a text file.
 */
public class ReplaceText extends MigrationOperation {

    private String pattern;
    private Pattern compiledPattern;
    private String replaceWith;
    private String messageKey;

    public void replace(InputStream inputStream, OutputStream outputStream, Version fromVersion, Version toVersion) {

    }
}
