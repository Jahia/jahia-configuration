package org.jahia.utils.osgi.parsers;

import org.codehaus.plexus.util.FileUtils;
import org.jahia.utils.osgi.parsers.cnd.JahiaCndReader;
import org.jahia.utils.osgi.parsers.cnd.NodeTypeRegistry;
import org.jahia.utils.osgi.parsers.cnd.ParseException;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A CND (JCR content definition) file parser
 */
public class CndFileParser extends AbstractFileParser {

    public boolean canParse(String fileName) {
        String ext = FileUtils.getExtension(fileName).toLowerCase();
        return "cnd".equals(ext);
    }

    public boolean parse(String fileName, InputStream inputStream, ParsingContext parsingContext, boolean externalDependency) throws IOException {
        getLogger().debug("Processing CND " + fileName + "...");

        try {
            JahiaCndReader jahiaCndReader = new JahiaCndReader(new InputStreamReader(inputStream), fileName, fileName, NodeTypeRegistry.getInstance());
            jahiaCndReader.setDoRegister(false);
            jahiaCndReader.parse();

            jahiaCndReader.getDefinitionsAndReferences(parsingContext.getContentTypeDefinitions(), parsingContext.getContentTypeReferences());
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
