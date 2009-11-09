package org.jahia.utils.maven.plugin.configurators;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Attribute;
import org.jdom.Element;

import java.net.URL;
import java.io.File;

/**
 * Test unit for Jahia's Spring hibernate configurator
 *
 * @author loom
 *         Date: Nov 3, 2009
 *         Time: 3:29:22 PM
 */
public class SpringHibernateConfiguratorTest extends AbstractXMLConfiguratorTestCase {
    
    public void testUpdateConfiguration() throws Exception {
        URL applicationContextJahia65HibernateURL = this.getClass().getClassLoader().getResource("configurators/WEB-INF/etc/spring/applicationcontext-hibernate-jahia65.xml");
        File applicationContextHibernateJahia65File = new File(applicationContextJahia65HibernateURL.getFile());
        URL applicationContextJahia6HibernateURL = this.getClass().getClassLoader().getResource("configurators/WEB-INF/etc/spring/applicationcontext-hibernate-jahia6.xml");
        File applicationContextHibernateJahia6File = new File(applicationContextJahia6HibernateURL.getFile());
        String applicationContextHibernateFileParentPath = applicationContextHibernateJahia65File.getParentFile().getPath() + File.separator;

        SpringHibernateConfigurator websphereOracleJahia65Configurator = new SpringHibernateConfigurator(oracleDBProperties, websphereOraclePropertiesBean);
        websphereOracleJahia65Configurator.updateConfiguration(applicationContextHibernateJahia65File.toString(), applicationContextHibernateFileParentPath + "applicationcontext-hibernate-jahia65-2.xml");

        SAXBuilder saxBuilder = new SAXBuilder();
        Document jdomDocument = saxBuilder.build(applicationContextHibernateFileParentPath + "applicationcontext-hibernate-jahia65-2.xml");
        String prefix = "xp";

        assertEquals("${hibernate.dialect}", ((Element)getNode(jdomDocument, "/xp:beans/xp:bean[@id=\"sessionFactory\"]/xp:property[@name=\"hibernateProperties\"]/xp:props/xp:prop[@key=\"hibernate.dialect\"]", prefix)).getText());
        assertEquals("${nested.transaction.allowed}", ((Attribute)getNode(jdomDocument, "/xp:beans/xp:bean[@id=\"transactionManager\"]/xp:property[@name=\"nestedTransactionAllowed\"]/@value", prefix)).getValue());

        // Now let's test with Jahia 6's configuration format....

        SpringHibernateConfigurator websphereOracleJahia6Configurator = new SpringHibernateConfigurator(oracleDBProperties, websphereOraclePropertiesBean);
        websphereOracleJahia6Configurator.updateConfiguration(applicationContextHibernateJahia6File.toString(), applicationContextHibernateFileParentPath + "applicationcontext-hibernate-jahia6-2.xml");

        jdomDocument = saxBuilder.build(applicationContextHibernateFileParentPath + "applicationcontext-hibernate-jahia6-2.xml");

        assertEquals(oracleDBProperties.getProperty("jahia.database.hibernate.dialect"), ((Element)getNode(jdomDocument, "/xp:beans/xp:bean[@id=\"sessionFactory\"]/xp:property[@name=\"hibernateProperties\"]/xp:props/xp:prop[@key=\"hibernate.dialect\"]", prefix)).getText());
        String transactionIsolationLevel = oracleDBProperties.getProperty("jahia.nested_transaction_allowed");
        if ("".equals(transactionIsolationLevel)) {
            transactionIsolationLevel = "false";
        }
        assertEquals(transactionIsolationLevel, ((Attribute)getNode(jdomDocument, "/xp:beans/xp:bean[@id=\"transactionManager\"]/xp:property[@name=\"nestedTransactionAllowed\"]/@value", prefix)).getValue());
        
        SpringHibernateConfigurator tomcatMySQLJahia6Configurator = new SpringHibernateConfigurator(mysqlDBProperties, tomcatMySQLPropertiesBean);
        tomcatMySQLJahia6Configurator.updateConfiguration(applicationContextHibernateFileParentPath + "applicationcontext-hibernate-jahia6-2.xml", applicationContextHibernateFileParentPath + "applicationcontext-hibernate-jahia6-3.xml");

        jdomDocument = saxBuilder.build(applicationContextHibernateFileParentPath + "applicationcontext-hibernate-jahia6-3.xml");

        assertEquals(mysqlDBProperties.getProperty("jahia.database.hibernate.dialect"), ((Element)getNode(jdomDocument, "/xp:beans/xp:bean[@id=\"sessionFactory\"]/xp:property[@name=\"hibernateProperties\"]/xp:props/xp:prop[@key=\"hibernate.dialect\"]", prefix)).getText());
        transactionIsolationLevel = mysqlDBProperties.getProperty("jahia.nested_transaction_allowed");
        if ("".equals(transactionIsolationLevel)) {
            transactionIsolationLevel = "false";
        }
        assertEquals(transactionIsolationLevel, ((Attribute)getNode(jdomDocument, "/xp:beans/xp:bean[@id=\"transactionManager\"]/xp:property[@name=\"nestedTransactionAllowed\"]/@value", prefix)).getValue());

    }
}
