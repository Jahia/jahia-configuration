package org.jahia.utils.osgi.parsers;

import org.slf4j.Logger;

/**
 * Abstract file parser
 */
public abstract class AbstractFileParser implements FileParser, Comparable<AbstractFileParser> {

    private Logger logger;
    protected int priority = 0;

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public Logger getLogger() {
        return logger;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int compareTo(AbstractFileParser o) {
        int priorityDiff = priority - o.priority;
        if (priorityDiff != 0) {
            return priorityDiff;
        }
        return this.getClass().getName().compareTo(o.getClass().getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractFileParser that = (AbstractFileParser) o;

        if (priority != that.priority) return false;
        return getClass().getName().equals(o.getClass().getName());
    }

    @Override
    public int hashCode() {
        int result = getClass().getName().hashCode();
        result = 31 * result + priority;
        return result;
    }
}
