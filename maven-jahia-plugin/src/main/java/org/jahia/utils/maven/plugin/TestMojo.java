package org.jahia.utils.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.net.URL;
import java.net.URLConnection;
import java.io.*;
import java.util.List;
import java.util.ArrayList;

/**
 * User: Serge Huber
 * Date: 26 d?c. 2007
 * Time: 16:43:38
 * @goal test
 * @requiresDependencyResolution runtime
 * @aggregator false
 */
public class TestMojo extends AbstractMojo {

    /**
     * Server type
     * @parameter expression="${jahia.test.url}" default-value="http://localhost:8080/cms"
     */
    protected String testURL;

    /**
     * Server type
     * @parameter expression="${test}"
     */
    protected String test;

    /**
     * Server type
     * @parameter default-value="true"
     */
    protected boolean startupWait;

    /**
     * Server type
     * @parameter default-value="60"
     */
    protected int startupTimeout;

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;


    public void execute() throws MojoExecutionException, MojoFailureException {
        if (StringUtils.isEmpty(test) || test.contains("*")) {
            executeAllTests();
        } else {
            executeTest(test);
        }
    }

    private void executeAllTests() {
        try {
            List<String> targets = new ArrayList<String>();
            String url1 = testURL + "/test" + (StringUtils.isNotEmpty(test) ? "/" + test : "");
            getLog().info("Get tests from : "+url1);
            URLConnection conn = null;

            if (startupWait) {
                getLog().info("Waiting for jahia startup");
                for (int i=startupTimeout; i>0; i--) {
                    try {
                        conn = new URL(url1).openConnection();
                        conn.connect();
                        break;
                    } catch (IOException e) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        System.out.print(".");
                    }
                }
            } else {
                conn = new URL(url1).openConnection();
            }

            InputStream is = null;
            if (conn != null) {
                is = conn.getInputStream();
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            String line;
            while ( (line = r.readLine())!= null) {
                getLog().info("Adding test "+line);
                targets.add(line);
            }
            if (is != null) {
                is.close();
            }

            long timer = System.currentTimeMillis();
            
            getLog().info("Start executing all tests (" + targets.size() + ")...");
            for (String s : targets) {
                executeTest(s);
            }
            getLog().info("...done in " + (System.currentTimeMillis() - timer) / 1000 + " s");

        } catch (IOException e) {
            getLog().error(e);
        }
    }

    private void executeTest(String test) {
        long timer = System.currentTimeMillis();
        try {
            URLConnection conn;
            InputStream is;
            String testUrl = testURL + "/test/" + test;
            getLog().info("Execute: "+testUrl);
            conn = new URL(testUrl).openConnection();
            is = conn.getInputStream();
            File out = project.getBasedir();
            out = new File(out,"target/surefire-reports");
            if (!out.exists()) {
                final boolean b = out.mkdirs();
                if(!b)
                    getLog().error("could not create directory "+out.getAbsolutePath());
            }
            FileOutputStream os = new FileOutputStream(new File(out, "TEST-"+ test + ".xml"));
            IOUtils.copy(is, os);
            is.close();
            os.close();
        } catch (IOException e) {
            getLog().error(e);
        } finally {
            getLog().info("...done " + test + " in " + (System.currentTimeMillis() - timer) + " ms");
        }
    }

}
