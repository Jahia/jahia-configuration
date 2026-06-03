/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2026 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.utils.maven.plugin.sbom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CycloneDX 1.4 Component model.
 */
public class ComponentModel {
    /**
     * CycloneDX OrganizationalEntity — serializes as {"name": "..."}.
     */
    public static class OrganizationalEntity {
        private String name;

        public OrganizationalEntity(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private String type;
    private String name;
    private String version;
    private String purl;
    /**
     * CycloneDX LicenseChoice — serializes as {"license": {"id": "..."}}
     * or {"expression": "..."} for compound expressions.
     */
    public static class LicenseChoice {
        private LicenseEntry license;

        public LicenseChoice(String spdxId) {
            this.license = new LicenseEntry(spdxId);
        }

        public LicenseEntry getLicense() {
            return license;
        }

        public void setLicense(LicenseEntry license) {
            this.license = license;
        }
    }

    public static class LicenseEntry {
        private String id;

        public LicenseEntry(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    private OrganizationalEntity supplier;
    private String copyright;
    private List<LicenseChoice> licenses;
    private List<Map<String, String>> hashes;
    private List<Property> properties;
    private String description;

    /**
     * CycloneDX Property — serializes as {"name": "...", "value": "..."}.
     */
    public static class Property {
        private String name;
        private String value;

        public Property(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPurl() {
        return purl;
    }

    public void setPurl(String purl) {
        this.purl = purl;
    }

    public OrganizationalEntity getSupplier() {
        return supplier;
    }

    public void setSupplier(OrganizationalEntity supplier) {
        this.supplier = supplier;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public List<LicenseChoice> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<LicenseChoice> licenses) {
        this.licenses = licenses;
    }

    public List<Map<String, String>> getHashes() {
        return hashes;
    }

    public void setHashes(List<Map<String, String>> hashes) {
        this.hashes = hashes;
    }

    public void addHash(Map<String, String> hash) {
        if (hashes == null) {
            hashes = new ArrayList<>();
        }
        hashes.add(hash);
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
