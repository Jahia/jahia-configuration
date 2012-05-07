package org.jahia.izpack;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.panels.PasswordGroup;
import com.izforge.izpack.panels.ProcessingClient;
import com.izforge.izpack.panels.StringInputProcessingClient;
import com.izforge.izpack.panels.Validator;
import com.izforge.izpack.util.VariableSubstitutor;

public class ToolInstalledValidator implements Validator {

    public boolean validate(ProcessingClient pdata) {
        Map<String, String> params = getParams(pdata);
        boolean valid = false;
        try {
            if (pdata.getText() == null || pdata.getText().trim().length() == 0
                    || params == null
                    || !Boolean.valueOf(params.get("validate"))) {
                // no need to validate
                return true;
            }
            File toolDir = new File(pdata.getText());
            valid = toolDir.exists() && toolDir.isDirectory()
                    && toolDir.list().length > 0;

        } catch (Exception e) {
            System.out.println("validate() Failed: " + e);
        }

        return valid;
    }
    
    private Map<String, String> getParams(ProcessingClient client)
    {
        Map<String, String> returnValue = null;
        StringInputProcessingClient context = null;
        InstallData idata = getIdata(client);
        VariableSubstitutor vs = new VariableSubstitutor(idata.getVariables());
        try
        {
            context = (StringInputProcessingClient) client;
            if (context.hasParams())
            {
                Map<String, String> params = context.getValidatorParams();
                returnValue = new HashMap<String, String>();
                Iterator<String> keys = params.keySet().iterator();
                while (keys.hasNext())
                {
                    String key = keys.next();
                    // Feed parameter values through vs
                    String value = vs.substitute(params.get(key), null);
                    // System.out.println("Adding local parameter: "+key+"="+value);
                    returnValue.put(key, value);
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("getParams() Failed: " + e);
        }
        return returnValue;
    }

    private InstallData getIdata(ProcessingClient client)
    {
        StringInputProcessingClient context = null;
        InstallData idata = null;
        try
        {
            context = (StringInputProcessingClient) client;
            idata = (InstallData)context.getInstallData();
        }
        catch (Exception e)
        {
            System.out.println("getIdata() Failed: " + e);
        }
        return idata;
    }
    

}
