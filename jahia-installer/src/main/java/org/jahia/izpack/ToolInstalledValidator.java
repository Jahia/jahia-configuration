package org.jahia.izpack;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.izforge.izpack.installer.AutomatedInstallData;
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
            boolean shouldBeDir = Boolean.valueOf(params.get("directory"));
            File tool = new File(pdata.getText());
            valid = tool.exists() && (shouldBeDir ? tool.isDirectory()
                    && tool.list().length > 0 : tool.isFile());
            String fileName = params.get("fileName");
            if (fileName != null && fileName.length() > 0) {
                // additionally validate the required file name
                valid = valid && validateFileName(tool, shouldBeDir, fileName);
            }

        } catch (Exception e) {
            System.out.println("validate() Failed: " + e);
        }

        return valid;
    }
    
    private boolean validateFileName(File tool, boolean shouldBeDir, String fileName) {
        if (shouldBeDir) {
            String[] names = fileName.split(",");
            for (String name : names) {
                name = name.trim();
                File file = new File(tool, name);
                if (file.exists() && file.isFile()) {
                    return true;
                }
                if (ExternalToolsPanelAction.isWindows()) {
                    file =  new File(tool, name + ".exe");
                    if (file.exists() && file.isFile()) {
                        return true;
                    }
                }
            }
        } else {
            String toolName = tool.getName();
            String[] names = fileName.split(",");
            for (String name : names) {
                name = name.trim();
                if (toolName.equals(name) || ExternalToolsPanelAction.isWindows() && toolName.equals(name + ".exe")) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<String, String> getParams(ProcessingClient client)
    {
        Map<String, String> returnValue = null;
        StringInputProcessingClient context = null;
        AutomatedInstallData idata = getIdata(client);
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

    private AutomatedInstallData getIdata(ProcessingClient client)
    {
        StringInputProcessingClient context = null;
        AutomatedInstallData idata = null;
        try
        {
            context = (StringInputProcessingClient) client;
            idata = (AutomatedInstallData) context.getInstallData();
        }
        catch (Exception e)
        {
            System.out.println("getIdata() Failed: " + e);
        }
        return idata;
    }
    

}
