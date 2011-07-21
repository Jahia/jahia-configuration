/**
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2011 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Solutions Group SA. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */

package org.jahia.izpack;

import com.izforge.izpack.adaptator.IXMLElement;
import com.izforge.izpack.adaptator.impl.XMLElementImpl;
import com.izforge.izpack.rules.Condition;
import com.izforge.izpack.util.Debug;

/**
 * This condition checks if a certain variable has an empty value. If it is not
 * in the current list of variables it will evaluate to false.
 * 
 * @author Sergiy Shyrkov
 */
public class EmptyVariableCondition extends Condition {
    private String variable;

    public EmptyVariableCondition() {
        this.variable = "default.variable";
    }

    public String getVariable() {
        return variable;
    }

    @Override
    public boolean isTrue() {
        String value = this.installdata.getVariable(this.variable);
        return value != null && value.length() == 0;
    }

    @Override
    public void makeXMLData(IXMLElement conditionRoot) {
        XMLElementImpl variableEl = new XMLElementImpl("variable",
                conditionRoot);
        variableEl.setContent(this.variable);
        conditionRoot.addChild(variableEl);
    }

    @Override
    public void readFromXML(IXMLElement xmlcondition) {
        if (xmlcondition != null) {
            IXMLElement variableElement = xmlcondition
                    .getFirstChildNamed("variable");
            if (variableElement != null) {
                this.variable = variableElement.getContent();
            } else {
                Debug.error("VariableExistenceCondition needs a variable element in its spec.");
            }
        }
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

}
