/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2021 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.utils.maven.plugin.osgi.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.project.MavenProject;
import org.junit.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.jahia.utils.maven.plugin.osgi.utils.Constants.*;

/**
 * Unit test for the {@link CapabilityUtils}.
 */
public class CapabilityUtilsTest {

    protected MavenProject project;
    protected Set<String> skipRequireDependencies = new HashSet<>();

    // corresponds to DependenciesMojo.jahiaDependsCapabilitiesPrefix
    private static final String prefix = ",";

    @Before
    public void setUp() throws Exception {
        setUpProject();
        skipRequireDependencies.clear();
    }

    public void setUpProject() {
        project = new MavenProject();
        project.setArtifactId("my-artifact");
        project.setName("my artifact name");
        project.setVersion("0.1.0.0-SNAPSHOT");
    }


    /** Helper methods */

    public String getProjectRequiresProp() {
        return project.getProperties().getProperty(REQUIRE_CAPABILITY_PROJECT_PROP_KEY);
    }

    public String getProjectProvidesProp() {
        return project.getProperties().getProperty(PROVIDE_CAPABILITY_PROJECT_PROP_KEY);
    }

    public String getExpectedProvides() {
        String version = project.getVersion();
        return String.format(",com.jahia.modules.dependencies;moduleIdentifier=\"%s\";moduleVersion:Version=%s,"
                        + "com.jahia.modules.dependencies;moduleIdentifier=\"%s\";moduleVersion:Version=%s",
                project.getArtifactId(), version, project.getName(), version);
    }

    public String getExpectedRequire(String moduleName, String minVersion, String minQualifier,
            String maxVersion, String maxQualifier) {
        StringBuilder builder = new StringBuilder("com.jahia.modules.dependencies;filter:=\"");
        if (minVersion == null && maxVersion == null) {
            builder.append(String.format("(moduleIdentifier=%s)", moduleName));
        } else {
            builder.append(String.format("(&(moduleIdentifier=%s)", moduleName));
            if (minVersion != null) {
                if (">=".equals(minQualifier)) {
                    builder.append(String.format("(moduleVersion%s%s)", minQualifier, minVersion));
                } else {
                    builder.append(String.format("(!(moduleVersion%s%s))", "<=", minVersion));
                }
            }
            if (maxVersion != null) {
                if ("<=".equals(maxQualifier)) {
                    builder.append(String.format("(moduleVersion%s%s)", maxQualifier, maxVersion));
                } else {
                    builder.append(String.format("(!(moduleVersion%s%s))", ">=", maxVersion));
                }
            }
            builder.append(')');
        }
        return builder.append("\"").toString();
    }

    public String getExpectedRequire(String... args) {
        return getExpectedRequire(args[0], args[1], args[2], args[3], args[4]);
    }


    /**Tests */

    @Test
    public void testBuildDependencies() {
        try {
            String jahiaDependsValue = "module-name";
            CapabilityUtils.buildJahiaDependencies(project, jahiaDependsValue, skipRequireDependencies, prefix);
            assertEquals(getExpectedProvides(), getProjectProvidesProp());

            String reqFmt = ",com.jahia.modules.dependencies;filter:=\"(moduleIdentifier=%s)\"";
            assertEquals(String.format(reqFmt, jahiaDependsValue), getProjectRequiresProp());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testBuildDependenciesBlankRequire() {
        try {
            String jahiaDependsValue = "";
            CapabilityUtils.buildJahiaDependencies(project, jahiaDependsValue, skipRequireDependencies, prefix);

            assertEquals(getExpectedProvides(), getProjectProvidesProp());
            assertEquals("Empty requires capability", "", getProjectRequiresProp());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testBuildSkipRequireDependencies() {
        try {
            String[] requireModules = new String[] { "module-name2", "module-name" };
            String jahiaDependsValue = StringUtils.join(requireModules, ',');
            skipRequireDependencies.add("module-name");
            CapabilityUtils.buildJahiaDependencies(project, jahiaDependsValue, skipRequireDependencies, prefix);

            assertEquals(getExpectedProvides(), getProjectProvidesProp());
            String reqFmt = ",com.jahia.modules.dependencies;filter:=\"(moduleIdentifier=%s)\"";
            assertEquals(String.format(reqFmt, requireModules[0]), getProjectRequiresProp());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testBuildSkipAllRequireDependencies() {
        try {
            String[] requireModules = new String[] { "module-name2", "module-name" };
            String jahiaDependsValue = StringUtils.join(requireModules, ',');
            skipRequireDependencies.add("module-name");
            skipRequireDependencies.add("module-name2");
            CapabilityUtils.buildJahiaDependencies(project, jahiaDependsValue, skipRequireDependencies, prefix);

            assertEquals(getExpectedProvides(), getProjectProvidesProp());
            assertEquals("skip all requires", "", getProjectRequiresProp());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testBuildSkipRequireWithVersionDependencies() {
        try {
            String[] requireModules = new String[] { "module-name2=[2.4,3]", "module-name" };
            String jahiaDependsValue = StringUtils.join(requireModules, ',');
            skipRequireDependencies.add("module-name");
            skipRequireDependencies.add("module-name2");
            CapabilityUtils.buildJahiaDependencies(project, jahiaDependsValue, skipRequireDependencies, prefix);

            assertEquals(getExpectedProvides(), getProjectProvidesProp());
            assertEquals("skip all requires", "", getProjectRequiresProp());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testBuildRequireWithVersionDependencies() {
        try {
            String moduleName = "module-name2";
            String minVersion = "1.0";
            String minVersionExpected = "1.0.0"; // padded with 0
            String maxVersion = "3.4";
            String maxVersionExpected = "3.4.0"; // padded with 0
            String jahiaDependsValue = String.format("%s=[%s,%s]", moduleName, minVersion, maxVersion);
            CapabilityUtils.buildJahiaDependencies(project, jahiaDependsValue, skipRequireDependencies, prefix);

            assertEquals(getExpectedProvides(), getProjectProvidesProp());
            String expected = "," + getExpectedRequire(moduleName, minVersionExpected, ">=", maxVersionExpected, "<=");
            assertEquals(expected, getProjectRequiresProp());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testBuildRequireWithMinVersion() {
        try {
            String moduleName = "module-name2";
            String minVersion = "1.5";
            String minVersionExpected = "1.5.0"; // padding to 0

            String jahiaDependsValue = String.format("%s=%s", moduleName, minVersion);
            CapabilityUtils.buildJahiaDependencies(project, jahiaDependsValue, skipRequireDependencies, prefix);
            assertEquals(getExpectedProvides(), getProjectProvidesProp());
            String expected = "," + getExpectedRequire(moduleName, minVersionExpected, ">=", minVersionExpected, "<=");
            assertEquals(expected, getProjectRequiresProp());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }


    @Test
    public void testBuildRequireWithVersionMixedDependencies() {
        try {
            String jahiaDependsValue = "module-name1=3.2,module-name2, module-name3 = [0,1.4), "
                    + "module with a space  ,module-name5=(2,3.4.23]";
            CapabilityUtils.buildJahiaDependencies(project, jahiaDependsValue, skipRequireDependencies, prefix);
            assertEquals(getExpectedProvides(), getProjectProvidesProp());

            String[][] requireModules = new String[][] {
                    new String[] { "module-name1", "3.2.0", ">=", "3.2.0", "<=" },
                    new String[] { "module-name2", null, null, null, null },
                    new String[] { "module-name3", "0.0.0", ">=", "1.4.0", "<" },
                    new String[] { "module with a space", null, null, null, null },
                    new String[] { "module-name5", "2.0.0", ">", "3.4.23", "<=" },
            };
            String expected = Arrays.stream(requireModules)
                    .map(this::getExpectedRequire).collect(Collectors.joining(","));
            assertEquals("," + expected, getProjectRequiresProp());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }

    }

    @Test
    public void testParseJahiaDepends() {
        try {
            String jahiaDepends;
            jahiaDepends = "module1";
            assertEquals(jahiaDepends,  CapabilityUtils.replaceDependsDelimiter(jahiaDepends));
            jahiaDepends = "module1=1.4";
            assertEquals(jahiaDepends,  CapabilityUtils.replaceDependsDelimiter(jahiaDepends));
            jahiaDepends = "module1 = [ 1.4,  2]  ";
            assertEquals(jahiaDepends,  CapabilityUtils.replaceDependsDelimiter(jahiaDepends));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testParseJahiaDependsMixed() {
        try {
            String jahiaDepends = "module-name1=2.5,module-name2=(2.5.4, 3], module-name3=[0,1.4), "
                    + "module with a space ,module-name5=[2,3.4.23]";
            String actual = CapabilityUtils.replaceDependsDelimiter(jahiaDepends);
            assertEquals("module-name1=2.5;module-name2=(2.5.4, 3]; module-name3=[0,1.4); "
                    + "module with a space ;module-name5=[2,3.4.23]", actual);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testParseJahiaDependsFail() {
        try {
            String jahiaDepends = "module-name1=(,],module-name2=[2.5.4, ], module-name3=[,1.4], module with a space =,module-name5=[2,3.4"
                    + ".23]";
            String actual = CapabilityUtils.replaceDependsDelimiter(jahiaDepends);
            fail("Should fail for 'module with a space = ' and 'module-name1=(,]'");
        } catch (Exception e) {
            // expected exception
        }
    }

}
