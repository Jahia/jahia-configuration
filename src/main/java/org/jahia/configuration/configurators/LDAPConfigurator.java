package org.jahia.configuration.configurators;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.archiver.AbstractArchiver;
import org.codehaus.plexus.archiver.dir.DirectoryArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.Map;

/**
 * LDAP configurator that generate a module WAR configured to the ldap properties
 * User: loom
 * Date: 19.04.11
 * Time: 15:54
 * To change this template use File | Settings | File Templates.
 */
public class LDAPConfigurator extends AbstractXMLConfigurator {

    public LDAPConfigurator(Map dbProps, JahiaConfigInterface jahiaConfigInterface) {
        super(dbProps, jahiaConfigInterface);
    }

    @Override
    public void updateConfiguration(String sourceFileName, String destFileName) throws Exception {

        if (!"true".equals(jahiaConfigInterface.getLDAPActivated())) {
            return;
        }

        File tempDirectory = FileUtils.getTempDirectory();
        File ldapConfigDirectory = new File(tempDirectory, "ldap-config");
        ldapConfigDirectory.mkdir();
        File metainfDir = new File(ldapConfigDirectory, "META-INF");
        metainfDir.mkdir();
        File springDir = new File(metainfDir, "spring");
        springDir.mkdir();

        SAXBuilder saxBuilder = new SAXBuilder();
        InputStream skeletonContextStream = this.getClass().getClassLoader().getResourceAsStream("ldap/META-INF/spring/applicationcontext-ldap-config.xml");
        Document jdomDocument = saxBuilder.build(skeletonContextStream);

        // configure ldap file
        // setup user LDAP
        setLDAPAttribute(jdomDocument, "jahiaUserLDAPProvider", "url", jahiaConfigInterface.getLDAPConnectionURL());
        setLDAPAttribute(jdomDocument, "jahiaUserLDAPProvider", "public.bind.dn", jahiaConfigInterface.getLDAPPublicBindDN());
        setLDAPAttribute(jdomDocument, "jahiaUserLDAPProvider", "public.bind.password", jahiaConfigInterface.getLDAPPublicBindPassword());
        setLDAPAttribute(jdomDocument, "jahiaUserLDAPProvider", "uid.search.attribute", jahiaConfigInterface.getLDAPUserUIDSearchAttribute());
        setLDAPAttribute(jdomDocument, "jahiaUserLDAPProvider", "uid.search.name", jahiaConfigInterface.getLDAPUserUIDSearchName());
        // setup group LDAP
        setLDAPAttribute(jdomDocument, "jahiaGroupLDAPProvider", "url", jahiaConfigInterface.getLDAPConnectionURL());
        setLDAPAttribute(jdomDocument, "jahiaGroupLDAPProvider", "public.bind.dn", jahiaConfigInterface.getLDAPPublicBindDN());
        setLDAPAttribute(jdomDocument, "jahiaGroupLDAPProvider", "public.bind.password", jahiaConfigInterface.getLDAPPublicBindPassword());
        setLDAPAttribute(jdomDocument, "jahiaGroupLDAPProvider", "search.attribute", jahiaConfigInterface.getLDAPGroupSearchAttribute());
        setLDAPAttribute(jdomDocument, "jahiaGroupLDAPProvider", "search.name", jahiaConfigInterface.getLDAPGroupSearchName());

        Format customFormat = Format.getPrettyFormat();
        customFormat.setLineSeparator(System.getProperty("line.separator"));
        XMLOutputter xmlOutputter = new XMLOutputter(customFormat);
        File destFile = new File(springDir, "applicationcontext-ldap-config.xml");
        xmlOutputter.output(jdomDocument, new FileWriter(destFile));

        // copy MANIFEST file.
        InputStream manifestStream = this.getClass().getClassLoader().getResourceAsStream("ldap/META-INF/MANIFEST.MF");
        File targetManifestFile = new File(metainfDir, "MANIFEST.MF");
        FileUtils.copyInputStreamToFile(manifestStream, targetManifestFile);

        boolean verbose = true;
        JarArchiver archiver = new JarArchiver();
        if (verbose) {
            archiver.enableLogging(new ConsoleLogger(Logger.LEVEL_DEBUG,
                    "console"));
        }

        // let's generate the WAR file
        File targetFile = new File(tempDirectory, "ldap-config.war");
        archiver.setManifest(targetManifestFile);
        archiver.setDestFile(targetFile);
        String excludes = null;

        archiver.addDirectory(ldapConfigDirectory, null,
                excludes != null ? excludes.split(",") : null);
        archiver.createArchive();

        // copy the WAR file to the deployment directory
        FileUtils.copyFileToDirectory(targetFile, new File(destFileName));

        FileUtils.deleteDirectory(ldapConfigDirectory);
        targetFile.delete();

    }

    void setLDAPAttribute(Document document, String providerId, String propertyName, String propertyValue) throws JDOMException {
        setElementAttribute(document.getRootElement(),
                "/xp:beans/xp:bean[@id=\""+ providerId +"\"]/xp:property[@name=\"ldapProperties\"]/xp:map/xp:entry[@key=\""+propertyName+"\"]",
                "value",
                propertyValue);

    }
}
