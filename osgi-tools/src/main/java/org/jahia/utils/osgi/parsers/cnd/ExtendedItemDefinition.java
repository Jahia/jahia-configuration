/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
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
package org.jahia.utils.osgi.parsers.cnd;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.version.OnParentVersionAction;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jahia specific {@link ItemDefinition} implementation.
 * User: toto
 * Date: 15 janv. 2008
 * Time: 17:43:58
 */
public class ExtendedItemDefinition implements ItemDefinition {

    protected ExtendedNodeType declaringNodeType;
    protected Name name;
    private boolean isProtected = false;
    private boolean autoCreated = false;
    private boolean mandatory = false;
    private boolean hidden;
    private String itemType;
    private int onParentVersion = OnParentVersionAction.VERSION;
    private int onConflict = OnConflictAction.USE_LATEST;
    protected int selector = 0;
    private Map<String,String> selectorOptions = new ConcurrentHashMap<String,String>();
    private Map<Locale, String> labels = new ConcurrentHashMap<Locale, String>(1);
    private Map<Locale, Map<String,String>> labelsByNodeType = new ConcurrentHashMap<Locale, Map<String, String>>(1);
    private Map<Locale, Map<String,String>> tooltipsByNodeType = new ConcurrentHashMap<Locale, Map<String, String>>(1);
    private boolean override = false;

    public ExtendedNodeType getDeclaringNodeType() {
        return declaringNodeType;
    }

    public void setDeclaringNodeType(ExtendedNodeType declaringNodeType) {
        this.declaringNodeType = declaringNodeType;
    }

    public String getName() {
        return name.toString();
    }

    public void setName(Name name) {
        this.name = name;
    }

    public String getLocalName() {
        return name.getLocalName();
    }

    public String getPrefix() {
        return name.getPrefix();
    }

    public boolean isProtected() {
        return isProtected;
    }

    public void setProtected(boolean isProtected) {
        this.isProtected = isProtected;
    }

    public boolean isAutoCreated() {
        return autoCreated;
    }

    public void setAutoCreated(boolean autoCreated) {
        this.autoCreated = autoCreated;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public int getOnParentVersion() {
        return onParentVersion;
    }

    public void setOnParentVersion(int onParentVersion) {
        this.onParentVersion = onParentVersion;
    }

    public int getOnConflict() {
        return onConflict;
    }

    public void setOnConflict(int onConflict) {
        this.onConflict = onConflict;
    }

    public int getSelector() {
        return selector;
    }

    public void setSelector(int selector) {
        this.selector = selector;
    }

    public Map<String,String> getSelectorOptions() {
        return Collections.unmodifiableMap(selectorOptions);
    }

    public void setSelectorOptions(Map<String,String> selectorOptions) {
        this.selectorOptions = selectorOptions;
    }

    public boolean isNode() {
        return false;
    }

    public boolean isOverride() {
        return override;
    }

    public void setOverride(boolean override) {
        this.override = override;
    }

    public String getResourceBundleKey() {
        return getResourceBundleKey(getDeclaringNodeType());
    }

    public String getResourceBundleKey(ExtendedNodeType nodeType) {
        if(nodeType==null)
            return replaceColon((getDeclaringNodeType().getName() + "." + getName()));
        else
            return replaceColon((nodeType.getName() + "." + getName()));
    }

    public String getItemType() {
        if (itemType == null) {
            String inheritedItemType = getDeclaringNodeType().getItemsType();
            if (inheritedItemType == null) {
                inheritedItemType = "content";
            }
            return inheritedItemType;
        }
        return itemType;
    }

    public String getLocalItemType() {
        return itemType;
    }

    public boolean isUnstructured() {
        return "*".equals(getName());
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public boolean isContentItem() {
        return !isHidden()&&"content".equals(getItemType());
//        declaringNodeType.isNodeType("jmix:droppableContent") || declaringNodeType.isNodeType("jnt:container")
//                 || declaringNodeType.isNodeType("jnt:content") || declaringNodeType.isNodeType("jmix:contentItem") || name.toString().equals("jcr:title") || name.toString().equals("jcr:language") || name.toString().equals("jcr:statement");
    }

    public ExtendedItemDefinition getOverridenDefinition() {
        ExtendedItemDefinition overridenItemDefintion = this;
        if (isOverride()) {
             for (ExtendedItemDefinition itemDef : declaringNodeType.getItems()) {
                 if (itemDef.getName().equals(this.getName()) && !itemDef.isOverride()) {
                     overridenItemDefintion = itemDef;
                     break;
                 }
             }
        }
        return overridenItemDefintion;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }

        final ExtendedItemDefinition other = (ExtendedItemDefinition) obj;

        return (getName() != null ? getName().equals(other.getName()) : other.getName() == null)
                && (getDeclaringNodeType().getName() != null ? getDeclaringNodeType().getName().equals(
                        other.getDeclaringNodeType().getName()) : other.getDeclaringNodeType().getName() == null);
    }

    @Override
    public int hashCode() {
        int hash = 17 * 37 + (getName() != null ? getName().hashCode() : 0);
        hash = 37 * hash + (getDeclaringNodeType().getName() != null ? getDeclaringNodeType().getName().hashCode() : 0);
        return hash;
    }

    public void clearLabels() {
        labels.clear();
        tooltipsByNodeType.clear();
        labelsByNodeType.clear();
    }

    public static String replaceColon(String name) {
        return name != null ? Patterns.COLON.matcher(name).replaceAll("_") : name;
    }

}
