/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
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

import org.jahia.utils.maven.plugin.contentgenerator.PageService;
import org.jahia.utils.maven.plugin.contentgenerator.UserGroupService;
import org.junit.Before;
import org.junit.Test;

public class UserGroupServiceTest extends ContentGeneratorTestCase{

	private static UserGroupService userService;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		userService = new UserGroupService();
	}

	@Test
	public void testGetNbUsersPerGroup() {
		Integer nbGroups = new Integer(10);
		Integer nbUsers = new Integer(550);
		
		Integer nbUsersPerGroup = userService.getNbUsersPerGroup(nbUsers, nbGroups);
		assertEquals(Integer.valueOf(55), nbUsersPerGroup);
		
		nbGroups = new Integer(10);
		nbUsers = new Integer(558);
		
		nbUsersPerGroup = userService.getNbUsersPerGroup(nbUsers, nbGroups);
		assertEquals(Integer.valueOf(55), nbUsersPerGroup);
	}

	@Test
	public void testGetNbUsersRemaining() {
		Integer nbGroups = new Integer(10);
		Integer nbUsers = new Integer(550);
		
		Integer nbUsersLastGroup = userService.getNbUsersRemaining(nbUsers, nbGroups);
		assertEquals(Integer.valueOf(0), nbUsersLastGroup);
		
		nbGroups = new Integer(10);
		nbUsers = new Integer(558);
		
		nbUsersLastGroup = userService.getNbUsersRemaining(nbUsers, nbGroups);
		assertEquals(Integer.valueOf(8), nbUsersLastGroup);
	}
}
