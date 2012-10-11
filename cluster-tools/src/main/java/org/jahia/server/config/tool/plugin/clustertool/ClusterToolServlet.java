package org.jahia.server.config.tool.plugin.clustertool;

import org.jahia.configuration.cluster.tool.ClusterTool;
import org.jahia.configuration.cluster.tool.LinkedListLogger;
import org.jahia.server.config.tool.framework.BundleServlet;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Main servlet for cluster tool REST API
 */
public class ClusterToolServlet extends BundleServlet {

    private ClusterTool clusterTool;
    private LinkedListLogger linkedListLogger = new LinkedListLogger(ClusterToolServlet.class.getName());
    private ConfigurationAdmin configurationAdmin = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            clusterTool = new ClusterTool("", "", linkedListLogger);
        } catch (Exception e) {
            throw new ServletException("Error during initialization of cluster tool", e);
        }
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("messages", req.getLocale());
        if ("/".equals(req.getPathInfo()) || (req.getPathInfo() == null)) {
            resp.sendRedirect("../clusterAssets/index.html");
        } else if (req.getPathInfo().startsWith("/operationList")) {
            resp.setContentType("application/json");
            PrintWriter out = resp.getWriter();

            JSONWriter json = new JSONWriter(out);
            try {
                json.array();
                for (String operation : clusterTool.getOperations().keySet()) {
                    json.object();
                    json.key("operationName").value(operation);
                    if (resourceBundle.getString("operation." + operation + ".label") != null) {
                        json.key("operationDisplayName").value(resourceBundle.getString("operation." + operation + ".label"));
                    }
                    if (resourceBundle.getString("operation." + operation + ".description") != null) {
                        json.key("operationDescription").value(resourceBundle.getString("operation." + operation + ".description"));
                    }
                    json.endObject();
                }
                json.endArray();
            } catch (JSONException e) {
                throw new ServletException("Error retrieving operation list", e);
            }
        } else if (req.getPathInfo().startsWith("/validConfiguration")) {
            if (!clusterTool.getClusterConfigBean().isValid()) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Configuration is not valid");
                return;
            }
            PrintWriter printWriter = resp.getWriter();
            printWriter.print("Configuration valid.");
        } else if (req.getPathInfo().startsWith("/configuration")) {
            resp.setContentType("application/json");
            PrintWriter out = resp.getWriter();

            Configuration configuration = configurationAdmin.getConfiguration("org.jahia.server.config.tool.ClusterToolConfigurator");

            JSONWriter json = new JSONWriter(out);
            try {
                json.array();
                Dictionary properties = configuration.getProperties();
                Enumeration propKeyEnum = properties.keys();
                while (propKeyEnum.hasMoreElements()) {
                    String propKey = (String) propKeyEnum.nextElement();
                    String propValue = (String) properties.get(propKey);
                    json.object();
                    json.key(propKey).value(propValue);
                    json.endObject();
                }
                json.endArray();
            } catch (JSONException e) {
                throw new ServletException("Error retrieving operation list", e);
            }

        } else if (req.getPathInfo().startsWith("/logs")) {
            LinkedList<String> logEntries = linkedListLogger.getLogEntries();
            resp.setContentType("application/json");
            PrintWriter out = resp.getWriter();

            JSONWriter json = new JSONWriter(out);
            try {
                json.array();
                for (String logEntry : logEntries) {
                    json.value(logEntry);
                }
                json.endArray();
                linkedListLogger.flushLogEntries();
            } catch (JSONException e) {
                throw new ServletException("Error retrieving operation list", e);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo().startsWith("/operation/")) {
            String command = req.getPathInfo().substring("/operation/".length());
            if (clusterTool.getOperations().keySet().contains(command)) {
                clusterTool.setCommand(command);
                try {
                    clusterTool.run();
                } catch (Exception e) {
                    throw new ServletException("Error during execution of cluster tool", e);
                }
            }
        } else if ("/configuration".equals(req.getPathInfo())) {
            Configuration configuration = configurationAdmin.getConfiguration("org.jahia.server.config.tool.ClusterToolConfigurator");
            Dictionary properties = configuration.getProperties();
            Enumeration parameterNames = req.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String parameterName = (String) parameterNames.nextElement();
                String[] parameterValues = req.getParameterValues(parameterName);
                StringBuffer propValueBuf = new StringBuffer();
                for (int i=0; i < parameterValues.length; i++) {
                    propValueBuf.append(parameterValues);
                    if (i < parameterValues.length-1) {
                        propValueBuf.append(",");
                    }
                }
                properties.put(parameterName, propValueBuf.toString());
            }
            configuration.update(properties);
        }
    }
}
