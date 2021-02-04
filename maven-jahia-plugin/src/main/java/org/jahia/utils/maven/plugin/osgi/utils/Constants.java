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

/**
 * Short description of the class
 *
 * @author gflores
 */
public final class Constants {

    private Constants() {}

    public static final String OSGI_CAPABILITY_MODULE_DEPENDENCIES_KEY = "moduleIdentifier";

    public static final String OSGI_CAPABILITY_MODULE_DEPENDENCIES_VERSION_KEY = "moduleVersion";

    public static final String OSGI_CAPABILITY_MODULE_DEPENDENCIES = "com.jahia.modules.dependencies";

    public static final String REQUIRE_CAPABILITY_PROJECT_PROP_KEY = "jahia.plugin.requiredModulesCapabilities";

    public static final String PROVIDE_CAPABILITY_PROJECT_PROP_KEY = "jahia.plugin.providedModulesCapabilities";

}
