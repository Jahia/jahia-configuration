package org.jahia.utils.maven.plugin.osgi.parsers.cnd;

import org.junit.Test;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Unit test for CND parser
 */
public class JahiaCndReaderTest {

    private static Logger logger = org.slf4j.LoggerFactory.getLogger(JahiaCndReaderTest.class);

    @Test
    public void testParse() throws IOException, ParseException, RepositoryException {
        InputStreamReader inputStreamReader = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("org/jahia/utils/maven/plugin/osgi/parsers/cnd/definitions.cnd"));
        final String fileName = "definitions.cnd";
        JahiaCndReader jahiaCndReader = new JahiaCndReader(inputStreamReader, fileName, fileName, NodeTypeRegistry.getInstance());
        jahiaCndReader.parse();

        jahiaCndReader.setDoRegister(false);
        jahiaCndReader.parse();
        List<ExtendedNodeType> nodeTypeList = jahiaCndReader.getNodeTypesList();
        Set<String> contentTypeDefinitions = new TreeSet<String>();
        Set<String> contentTypeReferences = new TreeSet<String>();

        jahiaCndReader.getDefinitionsAndReferences(contentTypeDefinitions, contentTypeReferences);

        getLog().info("Node type definitions:");
        for (String contentTypeDefinition : contentTypeDefinitions) {
            getLog().info("  " + contentTypeDefinition + ",");
        }

        getLog().info("External node type references:");
        for (String contentTypeReference : contentTypeReferences) {
            getLog().info("  " + contentTypeReference + ",");
        }
    }


    private Logger getLog() {
        return logger;
    }


}
