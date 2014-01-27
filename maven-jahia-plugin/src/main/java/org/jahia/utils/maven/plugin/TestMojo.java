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
 * Execute a test servlet deployed in Jahia
 * Available tests are in a mod-test.xml file
 * @goal test
 * @requiresDependencyResolution runtime
 * @aggregator false
 */
public class TestMojo extends AbstractMojo {

    /**
     * Test URL
     * @parameter expression="${jahia.test.url}" default-value="http://localhost:8080/cms"
     */
    protected String testURL;

    /**
     * Test to execute
     * @parameter expression="${test}"
     */
    protected String test;
    
    /**
     * Test to execute
     * @parameter expression="${skipCoreTests}"
     */
    protected boolean skipCoreTests;
    
    /**
     * Server type
     * @parameter expression="${xmlTest}"
     */
    protected String xmlTest;    
    
    /**
     * Output directory for TestNG results
     * @parameter expression="${testOutputDirectory}" 
     */
    protected String testOutputDirectory;

    /**
     * Startup waiting time (seconds)
     * @parameter default-value="true"
     */
    protected boolean startupWait;

    /**
     * Startup timeout (seconds)
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
        if (!StringUtils.isEmpty(xmlTest)) {
            executeTest(xmlTest, true);     
        } else if (StringUtils.isEmpty(test) || test.contains("*")) {
            executeAllTests();
        } else {
            executeTest(test, false);
        }
    }

    private void executeAllTests() {
        try {
            List<String> targets = new ArrayList<String>();
            String url1 = testURL + "/test" + (StringUtils.isNotEmpty(test) ? "/" + test : "");
            if (skipCoreTests) {
                url1 += "?skipCoreTests=true";
            }
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
                executeTest(s, false);
            }
            getLog().info("...done in " + (System.currentTimeMillis() - timer) / 1000 + " s");

        } catch (IOException e) {
            getLog().error(e);
        }
    }

    private void executeTest(String test, boolean isXmlSuite) {
        long timer = System.currentTimeMillis();
        try {
            URLConnection conn;
            InputStream is;
            
            StringBuffer sbParameters = new StringBuffer(); 
            // dummy param to not have to test if each following parameter is the first one 
            // and then if you have to use ? or &
            sbParameters.append("?dummyParam=null");
            if (isXmlSuite) {
            	sbParameters.append("&xmlTest=" + test);
            }
            if (StringUtils.isNotEmpty(testOutputDirectory)) {
            	sbParameters.append("&testOutputDirectory=" + testOutputDirectory);
            }
            if (skipCoreTests) {
                sbParameters.append("&skipCoreTests=true");
            }
            
            String testUrl = testURL + "/test/" + test + sbParameters.toString();
            getLog().info("Execute: "+testUrl);
            conn = new URL(testUrl).openConnection();
            is = conn.getInputStream();
            
            File out;
            if (testOutputDirectory == null) {
                out = project.getBasedir();
                out = new File(out,"target/surefire-reports");
            } else {
            	out = new File(testOutputDirectory);
            }
            
            if (!out.exists()) {
                final boolean b = out.mkdirs();
                if(!b)
                    getLog().error("could not create directory "+out.getAbsolutePath());
            }
            
            if (!isXmlSuite) {
            	FileOutputStream os = new FileOutputStream(new File(out, "TEST-"+ test + ".xml"));
                IOUtils.copy(is, os);
                is.close();
                os.close();
            }
        } catch (IOException e) {
            getLog().error(e);
        } finally {
            getLog().info("...done " + test + " in " + (System.currentTimeMillis() - timer) + " ms");
        }
    }

}
