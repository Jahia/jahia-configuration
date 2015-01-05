package org.jahia.utils.osgi.parsers;

import org.apache.commons.io.FilenameUtils;
import org.jahia.utils.osgi.parsers.cnd.JahiaCndReader;
import org.jahia.utils.osgi.parsers.cnd.NodeTypeRegistry;
import org.jahia.utils.osgi.parsers.cnd.ParseException;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.TreeSet;

/**
 * A CND (JCR content definition) file parser
 */
public class CndFileParser extends AbstractFileParser {

    public boolean canParse(String fileName) {
        String ext = FilenameUtils.getExtension(fileName).toLowerCase();
        return "cnd".equals(ext);
    }

    public boolean parse(String fileName, InputStream inputStream, String fileParent, boolean externalDependency, boolean optionalDependency, String version, ParsingContext parsingContext) throws IOException {
        getLogger().debug("Processing CND " + fileName + "...");

        try {
            JahiaCndReader jahiaCndReader = new JahiaCndReader(new InputStreamReader(inputStream), fileName, fileName, NodeTypeRegistry.getInstance());
            jahiaCndReader.setDoRegister(false);
            jahiaCndReader.parse();

            Set<String> contentTypeDefinitions = new TreeSet<String>();
            Set<String> contentTypeReferences = new TreeSet<String>();
            jahiaCndReader.getDefinitionsAndReferences(contentTypeDefinitions, contentTypeReferences);
            parsingContext.addAllContentTypeDefinitions(contentTypeDefinitions);
            parsingContext.addAllContentTypeReferences(contentTypeReferences);
        } catch (ParseException e) {
            getLogger().error("Error while parsing CND file " + fileName, e);
        } catch (ValueFormatException e) {
            getLogger().error("Error while parsing CND file " + fileName, e);
        } catch (RepositoryException e) {
            getLogger().error("Error while parsing CND file " + fileName, e);
        }
        return true;
    }
}
