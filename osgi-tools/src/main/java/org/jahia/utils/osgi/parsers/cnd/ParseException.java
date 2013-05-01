package org.jahia.utils.osgi.parsers.cnd;

/**
 * ParseException
 */
public class ParseException extends Exception {

    /**
     * the line number where the error occurred
     */
    private final int lineNumber;

    /**
     * the column number where the error occurred
     */
    private final int colNumber;

    /**
     * the systemid of the source that produced the error
     */
    private final String filename;


    /**
     * Constructs a new instance of this class with <code>null</code> as its
     * detail message.
     */
    public ParseException(int lineNumber, int colNumber, String filename) {
        super();
        this.lineNumber = lineNumber;
        this.colNumber = colNumber;
        this.filename = filename;
    }

    /**
     * Constructs a new instance of this class with the specified detail
     * message.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public ParseException(String message, int lineNumber, int colNumber, String filename) {
        super(message);
        this.lineNumber = lineNumber;
        this.colNumber = colNumber;
        this.filename = filename;
    }

    /**
     * Constructs a new instance of this class with the specified detail
     * message and root cause.
     *
     * @param message   the detail message. The detail message is saved for
     *                  later retrieval by the {@link #getMessage()} method.
     * @param rootCause root failure cause
     */
    public ParseException(String message, Throwable rootCause, int lineNumber, int colNumber, String filename) {
        super(message, rootCause);
        this.lineNumber = lineNumber;
        this.colNumber = colNumber;
        this.filename = filename;
    }

    /**
     * Constructs a new instance of this class with the specified root cause.
     *
     * @param rootCause root failure cause
     */
    public ParseException(Throwable rootCause, int lineNumber, int colNumber, String filename) {
        super(rootCause);
        this.lineNumber = lineNumber;
        this.colNumber = colNumber;
        this.filename = filename;
    }

    /**
     * {@inheritDoc}
     */
    public String getMessage() {
        StringBuffer b = new StringBuffer(super.getMessage());
        String delim = " (";
        if (filename != null && !filename.equals("")) {
            b.append(delim);
            b.append(filename);
            delim = ", ";
        }
        if (lineNumber >= 0) {
            b.append(delim);
            b.append("line ");
            b.append(lineNumber);
            delim = ", ";
        }
        if (colNumber >= 0) {
            b.append(delim);
            b.append("col ");
            b.append(colNumber);
            delim = ", ";
        }
        if (delim.equals(", ")) {
            b.append(")");
        }
        return b.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return super.toString(); // + " (" + systemId + ", line " + lineNumber +", col " + colNumber +")";
    }

}
