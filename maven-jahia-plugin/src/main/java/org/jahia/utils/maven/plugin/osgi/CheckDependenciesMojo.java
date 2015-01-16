package org.jahia.utils.maven.plugin.osgi;

import aQute.bnd.osgi.Builder;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.tika.io.IOUtils;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.glassfish.jersey.client.ClientProperties;
import org.jahia.utils.maven.plugin.support.MavenAetherHelperUtils;
import org.jahia.utils.osgi.BundleUtils;
import org.jahia.utils.osgi.ManifestValueClause;
import org.jahia.utils.osgi.PackageUtils;
import org.jahia.utils.osgi.parsers.PackageInfo;
import org.jahia.utils.osgi.parsers.ParsingContext;

import javax.net.ssl.*;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * A goal that checks the dependencies of a generated OSGi bundle JAR against the project dependencies, and reports
 * any missing packages that weren't found in any dependency export.
 *
 * @goal check-dependencies
 * @phase verify
 * @requiresDependencyResolution test
 * @requiresDependencyCollection test
 */
public class CheckDependenciesMojo extends DependenciesMojo {

    Set<PackageInfo> systemPackages = new TreeSet<PackageInfo>();

    /**
     * @component
     */
    private MavenProjectHelper mavenProjectHelper;

    /**
     * @component
     */
    private ArtifactHandlerManager artifactHandlerManager;

    /**
     * Classifier type of the bundle to be installed.  For example, "jdk14".
     * Defaults to none which means this is the project's main bundle.
     *
     * @parameter
     */
    protected String classifier;

    /**
     * @component
     */
    private ArchiverManager archiverManager;

    /**
     * The directory for the generated bundles.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File outputDirectory;

    /**
     * The directory for the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String buildDirectory;

    /**
     * @parameter default-value="true"
     */
    protected boolean failBuildOnSplitPackages = true;


    /**
     * @parameter default-value="false"
     */
    protected boolean failBuildOnMissingPackageExports = false;

    /**
     * @parameter default-value="false"
     */
    private boolean searchInDependencies = false;

    /**
     * This method will use the public REST API at search.maven.org to search for Maven dependencies that contain
     * a package using an URL such as :
     *
     * http://search.maven.org/solrsearch/select?q=fc:%22com.mchange.v2.c3p0%22&rows=20&wt=json
     *
     * @param packageName
     */
    public static List<String> findPackageInMavenCentral(String packageName) {
        List<String> artifactResults = new ArrayList<String>();
        Client client = getRestClient(PackageUtils.MAVEN_SEARCH_HOST_URL);

        WebTarget target = client.target(PackageUtils.MAVEN_SEARCH_HOST_URL).path("solrsearch/select")
                .queryParam("q", "fc:\"" + packageName + "\"")
                .queryParam("rows", "5")
                .queryParam("wt", "json");

        Invocation.Builder invocationBuilder =
                target.request(MediaType.APPLICATION_JSON_TYPE);

        Map<String, Object> searchResults = null;
        try {
            Response response = invocationBuilder.get();
            searchResults= (Map<String, Object>) response.readEntity(Map.class);
        } catch (ProcessingException pe) {
            artifactResults.add(PackageUtils.NETWORK_ERROR_PREFIX + pe.getMessage());
        }

        if (searchResults != null) {
            Map<String,Object> searchResponse = (Map<String,Object>) searchResults.get("response");
            Integer searchResultCount = (Integer) searchResponse.get("numFound");
            List<Map<String,Object>> docs = (List<Map<String,Object>>) searchResponse.get("docs");
            for (Map<String,Object> doc : docs) {
                String artifactId = (String) doc.get("id");
                artifactResults.add(artifactId);
            }
        }

        return artifactResults;
    }

    private static Map<String,Client> clients = new TreeMap<String,Client>();

    private static Client getRestClient(String targetUrl) {

        if (clients.containsKey(targetUrl)) {
            return clients.get(targetUrl);
        }

        Client client = null;
        if (targetUrl != null) {
            if (targetUrl.startsWith("https://")) {
                try {
                    // Create a trust manager that does not validate certificate chains
                    TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
                    };
                    // Create all-trusting host name verifier
                    HostnameVerifier allHostsValid = new HostnameVerifier() {
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    };
                    SSLContext sslContext = SSLContext.getInstance("SSL");
                    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                    client = ClientBuilder.newBuilder().
                            sslContext(sslContext).
                            hostnameVerifier(allHostsValid).build();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (KeyManagementException e) {
                    e.printStackTrace();
                }
            } else {
                client = ClientBuilder.newClient();

            }
        }
        if (client == null) {
            return null;
        }

        client.property(ClientProperties.CONNECT_TIMEOUT, 1000);
        client.property(ClientProperties.READ_TIMEOUT,    3000);
        /*
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(contextServerSettings.getContextServerUsername(), contextServerSettings.getContextServerPassword());
        client.register(feature);
        */
        clients.put(targetUrl, client);
        return client;
    }



    @Override
    public void execute() throws MojoExecutionException {
        setBuildDirectory(projectBuildDirectory);
        setOutputDirectory(new File(projectOutputDirectory));

        if (!"jar".equals(project.getPackaging()) && !"bundle".equals(project.getPackaging()) && !"war".equals(project.getPackaging())) {
            getLog().info("Not a JAR/WAR/Bundle project, will do nothing.");
            return;
        }
        loadSystemPackages();

        buildExclusionPatterns();

        Set<PackageInfo> explicitPackageImports = new TreeSet<PackageInfo>();

        final ParsingContext projectParsingContext = new ParsingContext(MavenAetherHelperUtils.getCoords(project.getArtifact()), 0, 0, project.getArtifactId(), project.getBasedir().getPath(), project.getVersion(), null);

        Map originalInstructions = new LinkedHashMap();
        try {
            Xpp3Dom felixBundlePluginConfiguration = (Xpp3Dom) project.getPlugin("org.apache.felix:maven-bundle-plugin")
                    .getConfiguration();
            Xpp3Dom instructionsDom = felixBundlePluginConfiguration.getChild("instructions");
            for (Xpp3Dom instructionChild : instructionsDom.getChildren()) {
                originalInstructions.put(instructionChild.getName(), instructionChild.getValue());
            }
            excludeDependencies = felixBundlePluginConfiguration.getChild("excludeDependencies").getValue();
        } catch (Exception e) {
            // no overrides
        }

        Properties properties = new Properties();
        try {
            Builder builder = getOSGiBuilder(project, originalInstructions, properties, getClasspath(project));
            resolveEmbeddedDependencies(project, builder);
        } catch (Exception e) {
            throw new MojoExecutionException("Error trying to process bundle plugin instructions", e);
        }

        List<PackageInfo> existingPackageImports = getExistingImportPackages(projectParsingContext);
        explicitPackageImports.addAll(existingPackageImports);

        parsingContextCache = new ParsingContextCache(new File(dependencyParsingCacheDirectory), null);

        long timer = System.currentTimeMillis();
        int scanned = 0;
        try {
            scanClassesBuildDirectory(projectParsingContext);

            getLog().info(
                    "Scanned classes directory in " + (System.currentTimeMillis() - timer) + " ms. Found "
                            + projectParsingContext.getLocalPackages().size() + " project packages.");

            scanned = scanDependencies(projectParsingContext);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while scanning dependencies", e);
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error while scanning project packages", e);
        }

        getLog().info(
                "Scanned " + scanned + " project dependencies in " + (System.currentTimeMillis() - timer)
                        + " ms. Currently we have " + projectParsingContext.getLocalPackages().size() + " project packages.");

        projectParsingContext.postProcess();

        if (projectParsingContext.getSplitPackages().size() > 0) {
            if (failBuildOnSplitPackages) {
                StringBuilder splitPackageList = new StringBuilder();
                for (PackageInfo packageInfo : projectParsingContext.getSplitPackages()) {
                    splitPackageList.append("  ");
                    splitPackageList.append(packageInfo.toString());
                    splitPackageList.append(" from locations:\n    ");
                    splitPackageList.append(StringUtils.join(packageInfo.getSourceLocations(), "\n    "));
                    splitPackageList.append("\n");
                }
                throw new MojoExecutionException("Detected split packages:\n" + splitPackageList.toString());
            }
        }

        String extension = project.getPackaging();
        if ("bundle".equals(extension)) {
            extension = "jar";
        }
        String artifactFilePath = project.getBuild().getDirectory() + "/" + project.getBuild().getFinalName() + "." + extension;

        File artifactFile = new File(artifactFilePath);
        if (artifactFile == null || !artifactFile.exists()) {
            throw new MojoExecutionException("No artifact generated for project, was the goal called in the proper phase (should be verify) ?");
        }

        List<PackageInfo> allPackageExports = collectAllDependenciesExports(projectParsingContext);

        Set<PackageInfo> missingPackageExports = new TreeSet<PackageInfo>();
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(artifactFile);
            Manifest manifest = jarFile.getManifest();
            if (manifest.getMainAttributes() == null) {
                throw new MojoExecutionException("Error reading OSGi bundle manifest data from artifact " + artifactFile);
            }
            String importPackageHeaderValue = manifest.getMainAttributes().getValue("Import-Package");
            Set<String> visitedPackageImports = new TreeSet<String>();
            if (importPackageHeaderValue != null) {
                List<ManifestValueClause> importPackageClauses = BundleUtils.getHeaderClauses("Import-Package", importPackageHeaderValue);
                List<ManifestValueClause> clausesToRemove = new ArrayList<ManifestValueClause>();
                boolean modifiedImportPackageClauses = false;
                for (ManifestValueClause importPackageClause : importPackageClauses) {
                    for (String importPackagePath : importPackageClause.getPaths()) {
                        String clauseVersion = importPackageClause.getAttributes().get("version");
                        String clauseResolution = importPackageClause.getDirectives().get("resolution");
                        boolean optionalClause = false;
                        if ("optional".equals(clauseResolution)) {
                            optionalClause = true;
                        } else {
                            importPackageClause.getDirectives().put("resolution", "optional");
                            modifiedImportPackageClauses = true;
                        }
                        if (visitedPackageImports.contains(importPackagePath)) {
                            getLog().warn("Duplicate import detected on package " + importPackagePath + ", will remove duplicate. To remove this warning remove the duplicate import (possibly coming from a explicit import in the maven-bundle-plugin instructions)");
                            clausesToRemove.add(importPackageClause);
                            modifiedImportPackageClauses = true;
                        }


//                        PackageInfo importPackageInfo = new PackageInfo(importPackagePath, clauseVersion, optionalClause, artifactFile.getPath(), projectParsingContext);
//                        if (!optionalClause) {
//                            if (PackageUtils.containsMatchingVersion(allPackageExports, importPackageInfo)
//                                    && !importPackageInfo.isOptional()) {
//                                // we must now check if the import is strict and if the available export is part of
//                                // an optional export, in which case we will have to change it to be optional
//                                for (PackageInfo packageExport : allPackageExports) {
//                                    if (packageExport.matches(importPackageInfo)) {
//                                        if (packageExport.getOrigin() != null) {
//                                            ParsingContext parsingContext = packageExport.getOrigin();
//                                            if (parsingContext.isOptional()) {
//                                                // JAR is optional, we should modify the import package clause to be optional too !
//                                                getLog().warn("Mandatory package import " + importPackageInfo + " provided by optional JAR " + getTrail(packageExport) + " will be forced as optional !");
//                                                importPackageClause.getDirectives().put("resolution", "optional");
//                                                modifiedImportPackageClauses = true;
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                            if (!PackageUtils.containsIgnoreVersion(allPackageExports, importPackageInfo) &&
//                                    !PackageUtils.containsIgnoreVersion(systemPackages, importPackageInfo) &&
//                                    !PackageUtils.containsIgnoreVersion(projectParsingContext.getLocalPackages(), importPackageInfo)) {
//                                missingPackageExports.add(importPackageInfo);
//                            }
//                        }
                        visitedPackageImports.add(importPackagePath);
                    }
                }
                if (modifiedImportPackageClauses) {
                    for (ManifestValueClause clauseToRemove : clausesToRemove) {
                        boolean removeSuccessful = importPackageClauses.remove(clauseToRemove);
                        if (!removeSuccessful) {
                            getLog().warn("Removal of clause " + clauseToRemove + " was not successful, duplicates may still remain in Manifest !");
                        }
                    }
                    updateBundle(manifest, importPackageClauses, artifactFile, buildDirectory);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error reading OSGi bundle manifest data from artifact " + artifactFile, e);
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    // do nothing if close fails
                }
            }
        }
        missingPackageExports.removeAll(explicitPackageImports);

        if (missingPackageExports.size() > 0) {

            List<String> missingPackageNames = new ArrayList<String>();
            for (PackageInfo missingPackageExport : missingPackageExports) {
                missingPackageNames.add(missingPackageExport.getName());
            }

            getLog().info("Search for code origin of "+missingPackageExports.size()+" missing package imports, please wait...");
            final Map<String, Map<String, Artifact>> packageResults = FindPackageUsesMojo.findPackageUses(missingPackageNames, project.getArtifacts(), project, outputDirectory, searchInDependencies, getLog());
            if (packageResults.size() == 0) {
                getLog().warn("No results found in project files, use the <searchInDependencies>true</searchInDependencies> parameter to make the plugin look in all the dependencies (which is MUCH slower !)");
            }

            StringBuilder optionaDirectivesBuilder = new StringBuilder();
            StringBuilder errorMessageBuilder = new StringBuilder();
            String separator = "";
            for (PackageInfo missingPackageExport : missingPackageExports) {
                optionaDirectivesBuilder.append(separator);
                errorMessageBuilder.append(missingPackageExport);
                List<String> artifactIDs = findPackageInMavenCentral(missingPackageExport.getName());
                if (artifactIDs.size() > 0) {
                    String artifactList = StringUtils.join(artifactIDs, ", ");
                    errorMessageBuilder.append(" (available from Maven Central artifacts ");
                    errorMessageBuilder.append(artifactList);
                    errorMessageBuilder.append(",... or more at ");
                    errorMessageBuilder.append(PackageUtils.getPackageSearchUrl(missingPackageExport.getName()));
                    errorMessageBuilder.append(" )");
                } else {
                    errorMessageBuilder.append(" (not found at Maven Central, is it part of JDK ?)");
                }
                missingPackageExport.setOptional(true);
                missingPackageExport.setVersion(null);
                optionaDirectivesBuilder.append(missingPackageExport);
                if (packageResults.containsKey(missingPackageExport.getName())) {
                    Map<String,Artifact> sourceLocations = packageResults.get(missingPackageExport.getName());
                    for (Map.Entry<String, Artifact> foundClass : sourceLocations.entrySet()) {
                        if (!foundClass.getValue().toString().equals(project.getArtifact().toString())) {
                            errorMessageBuilder.append("\n     used in class " + foundClass.getKey() + " (" + foundClass.getValue().getFile() + ")");
                        } else {
                            errorMessageBuilder.append("\n     used in class " + foundClass.getKey() + " (in project or embedded dependencies)");
                        }
                    }
                }
                errorMessageBuilder.append("\n\n");
                separator = ",\n";
            }
            getLog().warn("Couldn't find any exported packages in Maven project dependencies for the following imported packages:\n" + errorMessageBuilder.toString());
            getLog().warn("Use the following lines in the <Import-Package> maven-bundle-plugin configuration to ignore these packages :\n" + optionaDirectivesBuilder.toString());
            getLog().warn(" or add the missing dependencies to your Maven project by finding the related missing Maven dependency");
            getLog().warn("");
            getLog().warn("Bundle may not deploy successfully unless the above dependencies are either deployed, added to Maven project or marked explicitely as optional (as in the above list)");
            getLog().warn("If you prefer to keep this warning activated but not fail the build, simply add <failBuildOnMissingPackageExports>false</failBuildOnMissingPackageExports> to the check-dependencies goal of the jahia-maven-plugin");
            getLog().warn("");
            getLog().warn("You could also use mvn jahia:find-package-uses -DpackageNames=COMMA_SEPARATED_PACKAGE_LIST to find where a specific package is used in the project");
            getLog().warn("or mvn jahia:find-packages -DpackageNames=COMMA_SEPARATED_PACKAGE_LIST to find the packages inside the project");
            getLog().warn("");
            if (failBuildOnMissingPackageExports) {
                throw new MojoExecutionException("Missing package exports for imported packages (see build log for details)");
            }
        }
    }

    private void updateBundle(Manifest manifest, List<ManifestValueClause> importPackageClauses, File artifactFile, String buildDirectory) {
        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (ManifestValueClause importPackageClause : importPackageClauses) {
            sb.append(separator);
            sb.append(importPackageClause.toString());
            separator=",";
        }
        manifest.getMainAttributes().putValue("Import-Package", sb.toString());
        File outputDir = unpackBundle(artifactFile);
        if (outputDir == null) {
            getLog().error("Error unpacking artifact " + artifactFile + " aborting bundle update");
            return;
        }
        File manifestFile = new File(outputDir, "META-INF/MANIFEST.MF");
        if (manifestFile.exists()) {
            getLog().info("Overwriting existing META-INF/MANIFEST file");
        } else {
            getLog().warn("Missing META-INF/MANIFEST.MF file in bundle, how did that happen ?");
        }
        FileOutputStream manifestFileOutputStream = null;
        try {
            manifestFileOutputStream = new FileOutputStream(manifestFile);
            manifest.write(manifestFileOutputStream);
        } catch (FileNotFoundException e) {
            getLog().error("Error writing new META-INF/MANIFEST.MF file", e);
            return;
        } catch (IOException e) {
            getLog().error("Error writing new META-INF/MANIFEST.MF file", e);
            return;
        } finally {
            IOUtils.closeQuietly(manifestFileOutputStream);
        }
        packBundle(artifactFile, manifestFile, outputDir);

        Artifact mainArtifact = project.getArtifact();

        if ( "bundle".equals( mainArtifact.getType() ) )
        {
            // workaround for MNG-1682: force maven to install artifact using the "jar" handler
            mainArtifact.setArtifactHandler(artifactHandlerManager.getArtifactHandler("jar"));
        }

        if ( null == classifier || classifier.trim().length() == 0 )
        {
            mainArtifact.setFile( artifactFile );
        }
        else
        {
            mavenProjectHelper.attachArtifact(project, artifactFile, classifier);
        }

    }

    private static final String DELIM_START = "${";
    private static final String DELIM_STOP  = "}";

    /**
     * <p>
     * This method performs property variable substitution on the
     * specified value. If the specified value contains the syntax
     * <tt>${&lt;prop-name&gt;}</tt>, where <tt>&lt;prop-name&gt;</tt>
     * refers to either a configuration property or a system property,
     * then the corresponding property value is substituted for the variable
     * placeholder. Multiple variable placeholders may exist in the
     * specified value as well as nested variable placeholders, which
     * are substituted from inner most to outer most. Configuration
     * properties override system properties.
     * </p>
     * @param val The string on which to perform property substitution.
     * @param currentKey The key of the property being evaluated used to
     *        detect cycles.
     * @param cycleMap Map of variable references used to detect nested cycles.
     * @param configProps Set of configuration properties.
     * @return The value of the specified string after system property substitution.
     * @throws IllegalArgumentException If there was a syntax error in the
     *         property placeholder syntax or a recursive variable reference.
     **/
    public static String substVars(String val, String currentKey,
                                   Map cycleMap, Properties configProps)
            throws IllegalArgumentException
    {
        // If there is currently no cycle map, then create
        // one for detecting cycles for this invocation.
        if (cycleMap == null)
        {
            cycleMap = new HashMap();
        }

        // Put the current key in the cycle map.
        cycleMap.put(currentKey, currentKey);

        // Assume we have a value that is something like:
        // "leading ${foo.${bar}} middle ${baz} trailing"

        // Find the first ending '}' variable delimiter, which
        // will correspond to the first deepest nested variable
        // placeholder.
        int stopDelim = -1;
        int startDelim = -1;

        do
        {
            stopDelim = val.indexOf(DELIM_STOP, stopDelim + 1);
            // If there is no stopping delimiter, then just return
            // the value since there is no variable declared.
            if (stopDelim < 0)
            {
                return val;
            }
            // Try to find the matching start delimiter by
            // looping until we find a start delimiter that is
            // greater than the stop delimiter we have found.
            startDelim = val.indexOf(DELIM_START);
            // If there is no starting delimiter, then just return
            // the value since there is no variable declared.
            if (startDelim < 0)
            {
                return val;
            }
            while (stopDelim >= 0)
            {
                int idx = val.indexOf(DELIM_START, startDelim + DELIM_START.length());
                if ((idx < 0) || (idx > stopDelim))
                {
                    break;
                }
                else if (idx < stopDelim)
                {
                    startDelim = idx;
                }
            }
        }
        while ((startDelim > stopDelim) && (stopDelim >= 0));

        // At this point, we have found a variable placeholder so
        // we must perform a variable substitution on it.
        // Using the start and stop delimiter indices, extract
        // the first, deepest nested variable placeholder.
        String variable =
                val.substring(startDelim + DELIM_START.length(), stopDelim);

        // Verify that this is not a recursive variable reference.
        if (cycleMap.get(variable) != null)
        {
            throw new IllegalArgumentException(
                    "recursive variable reference: " + variable);
        }

        // Get the value of the deepest nested variable placeholder.
        // Try to configuration properties first.
        String substValue = (configProps != null)
                ? configProps.getProperty(variable, null)
                : null;
        if (substValue == null)
        {
            // Ignore unknown property values.
            substValue = System.getProperty(variable, "");
        }

        // Remove the found variable from the cycle map, since
        // it may appear more than once in the value and we don't
        // want such situations to appear as a recursive reference.
        cycleMap.remove(variable);

        // Append the leading characters, the substituted value of
        // the variable, and the trailing characters to get the new
        // value.
        val = val.substring(0, startDelim)
                + substValue
                + val.substring(stopDelim + DELIM_STOP.length(), val.length());

        // Now perform substitution again, since there could still
        // be substitutions to make.
        val = substVars(val, currentKey, cycleMap, configProps);

        // Return the value.
        return val;
    }

    private void loadSystemPackages() throws MojoExecutionException {
        Properties dependenciesProperties = new Properties();
        InputStream dependenciesPropertiesStream = this.getClass().getClassLoader().getResourceAsStream("org/jahia/utils/maven/plugin/osgi/dependencies.properties");
        try {
            dependenciesProperties.load(dependenciesPropertiesStream);
            dependenciesProperties.setProperty("dollar", "$");
            String propertyName = "org.osgi.framework.system.packages";
            String systemPackagesValue = dependenciesProperties.getProperty(propertyName);
            systemPackagesValue = (systemPackagesValue != null)
                    ? substVars(systemPackagesValue, propertyName, null, dependenciesProperties)
                    : null;
            List<ManifestValueClause> systemPackageClauses = BundleUtils.getHeaderClauses("Export-Package", systemPackagesValue);
            for (ManifestValueClause systemPackageClause : systemPackageClauses) {
                for (String systemPackagePath : systemPackageClause.getPaths()) {
                    String clauseVersion = systemPackageClause.getAttributes().get("version");
                    PackageInfo systemPackageInfo = new PackageInfo(systemPackagePath, clauseVersion, false, "System packages", null);
                    systemPackages.add(systemPackageInfo);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error loading system exports", e);
        }
    }

    private List<PackageInfo> collectAllDependenciesExports(ParsingContext parsingContext) {
        List<PackageInfo> allExports = new ArrayList<PackageInfo>();
        if (parsingContext.getChildren() != null) {
            for (ParsingContext childParsingContext : parsingContext.getChildren()) {
                if (childParsingContext.isExternal()) {
                    allExports.addAll(childParsingContext.getPackageExports());
                } else {
                    allExports.addAll(childParsingContext.getLocalPackages());
                }
                allExports.addAll(collectAllDependenciesExports(childParsingContext));
            }
        }
        return allExports;
    }

    private File unpackBundle( File jarFile )
    {
        File outputDir = outputDirectory;
        if ( null == outputDir ) {
            outputDir = new File( buildDirectory, "classes" );
        }

        try {
            /*
             * this directory must exist before unpacking, otherwise the plexus
             * unarchiver decides to use the current working directory instead!
             */
            if ( !outputDir.exists() ) {
                outputDir.mkdirs();
            }

            UnArchiver unArchiver = archiverManager.getUnArchiver( "jar" );
            unArchiver.setDestDirectory( outputDir );
            unArchiver.setSourceFile( jarFile );
            unArchiver.extract();
        } catch ( Exception e ) {
            getLog().error( "Problem unpacking " + jarFile + " to " + outputDir, e );
            return null;
        }
        return outputDir;
    }

    private void packBundle ( File jarFile, File manifestFile, File contentDirectory ) {
        File outputDir = outputDirectory;
        try {
            JarArchiver archiver = (JarArchiver) archiverManager.getArchiver("jar");

            archiver.setManifest(manifestFile);
            archiver.setDestFile(jarFile);

            archiver.addDirectory(contentDirectory, null, null);
            archiver.createArchive();
        } catch ( Exception e ) {
            getLog().error( "Problem packing " + jarFile + " to " + outputDir, e );
        }

    }

    private String getTrail(PackageInfo packageInfo) {
        List<ParsingContext> ancestors = new ArrayList<ParsingContext>();
        ParsingContext currentParsingContext = packageInfo.getOrigin();
        ancestors.add(currentParsingContext);
        while (currentParsingContext.getParentParsingContext() != null) {
            currentParsingContext = currentParsingContext.getParentParsingContext();
            if (currentParsingContext != null) {
                ancestors.add(currentParsingContext);
            }
        }
        Collections.reverse(ancestors);
        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (ParsingContext parsingContext : ancestors) {
            sb.append(separator);
            sb.append(parsingContext.getMavenCoords());
            separator = " -> ";
        }
        sb.append(" : ");
        sb.append(packageInfo.toString());
        return sb.toString();
    }


}
