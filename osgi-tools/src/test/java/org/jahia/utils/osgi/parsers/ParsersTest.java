/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.utils.osgi.parsers;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Unit test for parsers.
 */
public class ParsersTest {

    private static final Logger logger = LoggerFactory.getLogger(ParsersTest.class);

    @Test
    public void testParsers() throws IOException {
        ParsingContext parsingContext = new ParsingContext();
        String version = "1.0";

        String tmpDirLocation = System.getProperty("java.io.tmpdir");
        File tmpDirTestLocation = new File(tmpDirLocation, "test-" + System.currentTimeMillis());
        tmpDirTestLocation.mkdirs();

        File cndDefinitionsFile = new File(tmpDirTestLocation, "definitions.cnd");
        File jspFile = new File(tmpDirTestLocation, "test.jsp");
        File ruleFile = new File(tmpDirTestLocation, "rules.drl");
        File functionsTagLibFile = new File(tmpDirTestLocation, "functions.tld");
        File jPDLWorkflowDefFile = new File(tmpDirTestLocation, "translation.jpdl.xml");

        copyClassLoaderResourceToFile("org/jahia/utils/osgi/parsers/cnd/definitions.cnd", cndDefinitionsFile);
        copyClassLoaderResourceToFile("org/jahia/utils/osgi/parsers/test.jsp", jspFile);
        copyClassLoaderResourceToFile("org/jahia/utils/osgi/parsers/rules.drl", ruleFile);
        copyClassLoaderResourceToFile("org/jahia/utils/osgi/parsers/functions.tld", functionsTagLibFile);
        copyClassLoaderResourceToFile("org/jahia/utils/osgi/parsers/translation.jpdl.xml", jPDLWorkflowDefFile);

        parseFile(cndDefinitionsFile.getName(), new FileInputStream(cndDefinitionsFile), tmpDirTestLocation.getPath(), false, false, version, logger, parsingContext);
        parseFile(jspFile.getName(), new FileInputStream(jspFile), tmpDirTestLocation.getPath(), false, false, version, logger, parsingContext);
        parseFile(ruleFile.getName(), new FileInputStream(ruleFile), tmpDirTestLocation.getPath(), false, false, version, logger, parsingContext);
        parseFile(functionsTagLibFile.getName(), new FileInputStream(functionsTagLibFile), tmpDirTestLocation.getPath(), true, false, version, logger, parsingContext);
        parseFile(jPDLWorkflowDefFile.getName(), new FileInputStream(jPDLWorkflowDefFile), tmpDirTestLocation.getPath(), true, false, version, logger, parsingContext);

        parsingContext.postProcess();

        logger.info("Parsing context contents: \n" + parsingContext);

        // now let's check that all the expected imports and exports are present.

        // from the JSP parser
        // - @elvariable type hint
        Assert.assertTrue("Missing import package", parsingContext.getPackageImports().contains(new PackageInfo("org.jahia.services.render.scripting")));
        Assert.assertTrue("Missing import package", parsingContext.getPackageImports().contains(new PackageInfo("org.jahia.test1.generic.level1")));
        Assert.assertTrue("Missing import package", parsingContext.getPackageImports().contains(new PackageInfo("org.jahia.test1.generic.level2")));
        Assert.assertTrue("Missing import package", parsingContext.getPackageImports().contains(new PackageInfo("org.jahia.test1.generic.level3")));
        Assert.assertTrue("Missing import package", parsingContext.getPackageImports().contains(new PackageInfo("org.jahia.utils")));
        Assert.assertTrue("Missing import package", parsingContext.getPackageImports().contains(new PackageInfo("org.jahia.services.notification")));
        Assert.assertFalse("bad import package", parsingContext.getPackageImports().contains(new PackageInfo("java.lang")));
        // - jsp:useBean tag
        Assert.assertTrue("Missing import package", parsingContext.getPackageImports().contains(new PackageInfo("org.jahia.configuration.modules")));
        // - from taglib usages
        Assert.assertTrue("Missing import package", parsingContext.getPackageImports().contains(new PackageInfo("org.apache.commons.lang")));
        Assert.assertTrue("Missing import package", parsingContext.getPackageImports().contains(new PackageInfo("org.jahia.taglibs.functions")));

        // from the rules DRL file
        Assert.assertTrue("Missing import package", parsingContext.getPackageImports().contains(new PackageInfo("org.jahia.services.content.rules")));

        // from the jBPM jPDL Workflow definition file
        Assert.assertTrue("Missing import package", parsingContext.getPackageImports().contains(new PackageInfo("org.jahia.services.workflow.jbpm")));

        // from the content definition file (definitions.cnd)
        Assert.assertTrue("Missing content type definition", parsingContext.getContentTypeDefinitions().contains("test:versionable"));
        Assert.assertTrue("Missing content type reference", parsingContext.getContentTypeReferences().contains("jnt:contentList"));
        Assert.assertTrue("Missing content type reference", parsingContext.getContentTypeReferences().contains("jmix:retrievableContent"));

        FileUtils.deleteDirectory(tmpDirTestLocation);

    }

    private void parseFile(String fileName, InputStream inputStream, String fileParent, boolean externalDependency, boolean optionalDependency, String version, Logger logger, ParsingContext parsingContext) throws IOException {
        try {
        Parsers.getInstance().parse(0, fileName, inputStream, fileParent, externalDependency, optionalDependency, version, logger, parsingContext);
        Parsers.getInstance().parse(1, fileName, inputStream, fileParent, externalDependency, optionalDependency, version, logger, parsingContext);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void copyClassLoaderResourceToFile(String resourcePath, File manifestFile) throws IOException {
        FileOutputStream out = new FileOutputStream(manifestFile);
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            System.out.println("Couldn't find input class loader resource " + resourcePath);
            return;
        }
        try {
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

}
