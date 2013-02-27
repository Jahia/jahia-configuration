package org.jahia.utils.maven.plugin.osgi.parsers.cnd;

import org.apache.jackrabbit.commons.iterator.RangeIteratorAdapter;

import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeType;
import java.util.Iterator;

/**
 *
 * User: toto
 * Date: Sep 21, 2009
 * Time: 5:32:24 PM
 *
 */
public class NodeTypeIteratorImpl extends RangeIteratorAdapter implements NodeTypeIterator {

    public NodeTypeIteratorImpl(Iterator iterator, long size) {
        super(iterator, size);
    }

    public NodeType nextNodeType() {
        return (NodeType) next();
    }

}
