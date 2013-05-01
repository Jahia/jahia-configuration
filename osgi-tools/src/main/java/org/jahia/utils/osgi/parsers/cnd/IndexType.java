package org.jahia.utils.osgi.parsers.cnd;

public class IndexType {
    public static final int NO = 0;
    public static final int TOKENIZED = 1;
    public static final int UNTOKENIZED = 2;

    public static final String INDEXNAME_NO = "no";
    public static final String INDEXNAME_TOKENIZED = "tokenized";
    public static final String INDEXNAME_UNTOKENIZED = "untokenized";

    public static String nameFromValue(int type) {
        switch (type) {
            case NO:
                return INDEXNAME_NO;
            case TOKENIZED:
                return INDEXNAME_TOKENIZED;
            case UNTOKENIZED:
                return INDEXNAME_UNTOKENIZED;
            default:
                throw new IllegalArgumentException("unknown index type: " + type);
        }
    }

    public static int valueFromName(String name) {
        if (name.equals(INDEXNAME_NO)) {
            return NO;
        } else if (name.equals(INDEXNAME_TOKENIZED)) {
            return TOKENIZED;
        } else if (name.equals(INDEXNAME_UNTOKENIZED)) {
            return UNTOKENIZED;
        } else {
            throw new IllegalArgumentException("unknown index type: " + name);
        }
    }

}
