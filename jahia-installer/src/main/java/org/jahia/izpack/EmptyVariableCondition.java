/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2023 Jahia Solutions Group SA. All rights reserved.
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
    private static final long serialVersionUID = 5693167631780953983L;
    
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
