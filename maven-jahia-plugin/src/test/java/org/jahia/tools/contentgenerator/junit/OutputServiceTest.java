/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.tools.contentgenerator.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jahia.utils.maven.plugin.contentgenerator.OutputService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OutputServiceTest extends ContentGeneratorTestCase {

	private String sep;
	private File testDir;
	private String testFilename = "test.txt";
	private File testFile;
	private OutputService os;

	@Before
	public void setUp() {
		sep = System.getProperty("file.separator");
		String sTempDir = System.getProperty("java.io.tmpdir");
		File tempDir = new File(sTempDir + sep + "cg-test");
		tempDir.mkdir();
		
		os = new OutputService();
		
		testFile = new File(testDir + sep + testFilename);
	}
	
	@After
	public void tearDown() {
		testFile.delete();
	}
	
	@Test
	public void testInitOutputFile() {
		try {			
			os.initOutputFile(testFile);
			assertTrue(testFile.exists());
			String content = FileUtils.readFileToString(testFile);
                        assertTrue(content == null || content.length() == 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testInitOutputFileAlreadyExists() {
		String s = "aaa\\nbbb\\nccc\\nddd\\neee\\nfff\\nggg\\nhhh";
		try {
			FileUtils.writeStringToFile(testFile, s);
			assertEquals(s, FileUtils.readFileToString(testFile));
			
			os.initOutputFile(testFile);
			assertTrue(testFile.exists());
                        String content = FileUtils.readFileToString(testFile);
                        assertTrue(content == null || content.length() == 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testAppendStringToFile() {
		String s1 = "abcdef\\n";
		String s2 = "123456";
		
		try {
			testFile.createNewFile();

			FileUtils.writeStringToFile(testFile, s1);
			
			os.appendStringToFile(testFile, s2);
			
			String s3 = s1 + s2;
			assertEquals(s3, FileUtils.readFileToString(testFile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
