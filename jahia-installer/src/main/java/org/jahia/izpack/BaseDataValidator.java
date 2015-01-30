/**
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
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

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.DataValidator;

/**
 * Base data validator.
 * 
 * @author Sergiy Shyrkov
 */
public abstract class BaseDataValidator implements DataValidator {
    
    protected static String formatMessage(String msg) {
        if (msg == null || msg.length() <= 80) {
            return msg;
        }
        
        String[] lines = msg.split("\\. ");
        StringBuilder b = new StringBuilder(msg.length());
        for (String line : lines) {
            b.append(line).append(".\n");
        }
        
        return b.toString();
    }

    protected static String getVar(AutomatedInstallData adata, String name,
            String defValue) {
        String var = adata.getVariable(name);
        return var != null ? var : defValue;
    }
    
    protected String errorMsg;

    protected String warningMsg = "";

    protected abstract boolean doValidate(AutomatedInstallData adata);

    public boolean getDefaultAnswer() {
        return false;
    }

    public String getErrorMessageId() {
        return errorMsg;
    }

    protected String getMessage(AutomatedInstallData adata, String key, String defaultValue) {
        String errorMsg = adata.langpack.getString(key);
        errorMsg = errorMsg == null || errorMsg.length() == 0
                || key.equals(errorMsg) ? defaultValue
                : errorMsg;
        return errorMsg;
    }

    public String getWarningMessageId() {
        return warningMsg;
    }

    public final Status validateData(AutomatedInstallData adata) {
        return doValidate(adata) ? Status.OK : Status.ERROR;
    }

}
