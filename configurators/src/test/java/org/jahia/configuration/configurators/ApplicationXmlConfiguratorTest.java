package org.jahia.configuration.configurators;

import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.net.URL;

/**
 * Test unit for application.xml configurator
 */
public class ApplicationXmlConfiguratorTest extends AbstractXMLConfiguratorTestCase {
    @Override
    public void testUpdateConfiguration() throws Exception {
        FileSystemManager fsManager = VFS.getManager();
        URL applicationXmlUrl = this.getClass().getClassLoader().getResource("configurators/META-INF/application.xml");
        File applicationXmlFile = new File(applicationXmlUrl.getFile());
        String applicationXmlParentPath = applicationXmlFile.getParentFile().getPath() + File.separator;

        ApplicationXmlConfigurator applicationXmlConfiguratorWebsphere = new ApplicationXmlConfigurator(oracleDBProperties, websphereOracleConfigBean);
        applicationXmlConfiguratorWebsphere.updateConfiguration(new VFSConfigFile(fsManager, applicationXmlUrl.toExternalForm()), applicationXmlParentPath + "application-modified.xml");

        // The following tests are NOT exhaustive
        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Document jdomDocument = saxBuilder.build(applicationXmlParentPath + "application-modified.xml");
        String prefix = "";

        assertAllTextEquals(jdomDocument, "//application/module[@id=\"jahia-war\"]/web/web-uri/text()", prefix, "ROOT.war");
        assertAllTextEquals(jdomDocument, "//application/module[@id=\"jahia-war\"]/web/context-root/text()", prefix, "/");
        assertAllTextEquals(jdomDocument, "//application/module[@id=\"portlet-testsuite\"]/web/web-uri/text()", prefix, "websphere-testsuite.war");
        assertAllTextEquals(jdomDocument, "//application/module[@id=\"portlet-testsuite\"]/web/context-root/text()", prefix, "testsuite");

    }
}
