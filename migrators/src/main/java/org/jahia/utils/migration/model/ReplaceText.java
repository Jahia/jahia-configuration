package org.jahia.utils.migration.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.*;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to replace all the Strings matching a regex pattern in a text file.
 */
@XmlRootElement(name="replace")
@XmlType(name="replace")
public class ReplaceText extends MigrationOperation {

    private String pattern;
    private Pattern compiledPattern;
    private String replaceWith;
    private String warningMessageKey;
    private String performMessageKey;
    private ResourceBundle resourceBundle = ResourceBundle.getBundle("messages");

    @XmlAttribute(name="pattern")
    public void setPattern(String pattern) {
        this.pattern = pattern;
        this.compiledPattern = Pattern.compile(pattern);
    }

    @XmlAttribute(name="with")
    public void setReplaceWith(String replaceWith) {
        this.replaceWith = replaceWith;
    }

    @XmlAttribute(name="warningMessageKey")
    public void setWarningMessageKey(String warningMessageKey) {
        this.warningMessageKey = warningMessageKey;
    }

    @XmlAttribute(name="performMessageKey")
    public void setPerformMessageKey(String performMessageKey) {
        this.performMessageKey = performMessageKey;
    }

    public void execute(InputStream inputStream, OutputStream outputStream, String filePath, boolean performModification) {
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
        String line = null;
        int lineCount = 0;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                lineCount++;
                Matcher lineMatcher = compiledPattern.matcher(line);
                if (lineMatcher.find()) {
                    String newLine = line.replaceAll(pattern, replaceWith);
                    if (performModification) {
                        displayMessage(performMessageKey, filePath, lineCount, line, newLine);
                        bufferedWriter.write(newLine);
                    } else {
                        displayMessage(warningMessageKey, filePath, lineCount, line, newLine);
                    }
                } else {
                    if (performModification) {
                        bufferedWriter.write(line);
                    } else {
                        // do nothing in this case
                    }
                }
            }
        } catch (IOException ioe) {
            System.out.println("Error in file " + filePath + " at line " + lineCount + ":" + line);
            ioe.printStackTrace();
        }
    }

    private void displayMessage(String messageKey, String filePath, int lineCount, String line, String newLine) {
        String alternateMessage = "Replacing in " + filePath + " at line " + lineCount + "\n  Current line=" + line + "\n  New line=" + newLine;
        if (messageKey == null) {
            System.out.println("Null messageKey passed.\n" + alternateMessage);
            return;
        }
        if (resourceBundle != null) {
            if (resourceBundle.containsKey(messageKey)) {
                String message = resourceBundle.getString(messageKey);
                System.out.println(MessageFormat.format(message, filePath, lineCount, line, newLine));
            } else {
                System.out.println("Couldn't find resource key " + messageKey + " in resource bundle : messages\n" + alternateMessage);
            }
        } else {
            System.out.println("Couldn't find resource bundle : messages\n" + alternateMessage);
        }
    }
}
