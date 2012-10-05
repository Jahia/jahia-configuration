package org.jahia.server.config.tool.plugin.clustertool;

import org.jahia.configuration.cluster.tool.ClusterTool;
import org.jahia.server.config.tool.framework.BundleServlet;
import org.json.JSONException;
import org.json.JSONWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Main servlet for cluster tool REST API
 */
public class ClusterToolServlet extends BundleServlet {

    private ClusterTool clusterTool;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            clusterTool = new ClusterTool("", "");
        } catch (Exception e) {
            throw new ServletException("Error during initialization of cluster tool", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
                    json.endObject();
                }
                json.endArray();
            } catch (JSONException e) {
                throw new ServletException("Error retrieving operation list", e);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo().startsWith("/operation")) {
            // todo we should validate the parameter against the list of commands instead of using it directly.
            clusterTool.setCommand(req.getParameter("command"));
            try {
                clusterTool.run();
            } catch (Exception e) {
                throw new ServletException("Error during execution of cluster tool", e);
            }
        }
    }
}
