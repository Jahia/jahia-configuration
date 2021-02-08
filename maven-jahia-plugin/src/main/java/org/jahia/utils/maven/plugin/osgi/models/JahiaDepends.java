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
package org.jahia.utils.maven.plugin.osgi.models;

import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import org.apache.commons.lang.StringUtils;

import static org.jahia.utils.maven.plugin.osgi.utils.Constants.*;

/**
 * Parser for jahia-depends value
 */
public class JahiaDepends {

    private String moduleName = "";
    private VersionRange range = null;
    private String parsedString = null;


    public JahiaDepends(String dependency) {
        this.parsedString = dependency;
        String[] deps = dependency.split("=");
        this.moduleName = StringUtils.isNotBlank(deps[0]) ? deps[0].trim() : "";

        if (deps.length > 1 && StringUtils.isNotBlank(deps[1])) {
            range = new VersionRange(deps[1]);
        }
    }

    public boolean hasVersion() {
        return StringUtils.isNotEmpty(getMinVersion())
                && StringUtils.isNotEmpty(getMaxVersion());
    }


    public String getModuleName() {
        return moduleName;
    }

    public String getMinVersion() {
        return (range != null && range.getLow() != null) ? range.getLow().toString() : "";
    }

    public String getMaxVersion() {
        return (range != null && range.getHigh() != null) ? range.getHigh().toString() : "";
    }

    public VersionRange getVersionRange() {
        return range;
    }

    public boolean inRange(String version) {
        Version v = new Version(toOsgiVersion(version));
        return range == null || range.includes(v);
    }

    public String toFilterString() {
        String verFilter = (range != null) ? range.toFilter() : "";
        verFilter = verFilter.replace("version", OSGI_CAPABILITY_MODULE_DEPENDENCIES_VERSION_KEY);
        String filter = String.format("(%s=%s)", OSGI_CAPABILITY_MODULE_DEPENDENCIES_KEY, moduleName);
        if (!verFilter.isEmpty()) {
            filter = verFilter.startsWith("(&") ?
                    verFilter.replace("&", "&" + filter) :
                    String.format("(&%s%s)", filter, verFilter);
        }
        return filter;
    }

    public static JahiaDepends parse(String dependency) {
        return new JahiaDepends(dependency);
    }

    /** Workaround to convert maven project version to OSGI-compatible version */
    public static String toOsgiVersion(String version) {
        return org.apache.felix.utils.version.VersionCleaner.clean(version);
    }

    /** @return if clause starts with VersionRange.LEFT_OPEN or VersionRange.LEFT_CLOSED */
    public static boolean isOpenClause(String clause) {
        return StringUtils.isNotBlank(clause) && (
                clause.trim().startsWith("[") ||
                        clause.trim().startsWith("(") );
    }

    @Override public String toString() {
        return parsedString;
    }
}
