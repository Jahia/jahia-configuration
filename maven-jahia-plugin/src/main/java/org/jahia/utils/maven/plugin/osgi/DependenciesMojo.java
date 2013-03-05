package org.jahia.utils.maven.plugin.osgi;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.jahia.utils.maven.plugin.osgi.parsers.cnd.JahiaCndReader;
import org.jahia.utils.maven.plugin.osgi.parsers.cnd.NodeTypeRegistry;
import org.jahia.utils.maven.plugin.osgi.parsers.cnd.ParseException;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A maven goal to scan the project  for package dependencies, useful for building OSGi Import-Package
 * Manifest header.
 * <p/>
 * This goal is currently capable of scanning:
 * - TLD files in dependencies and the project
 * - JSP for page import and Taglib references (tag files are not supported yet)
 * - Drools rule definition imports
 * - JBPM Workflow definition files
 * - Spring context files
 * - JCR CND content definition files for node type definition and references
 * - Groovy files
 *
 * @goal dependencies
 * @requiresDependencyResolution test
 * @todo add support for JSP tag files, more...
 */
public class DependenciesMojo extends AbstractMojo {

    public static final Pattern JSP_PAGE_IMPORT_PATTERN = Pattern.compile("<%@.*page.*import=\\\"(.*?)\\\".*%>");
    public static final Pattern JSP_TAGLIB_PATTERN = Pattern.compile("<%@.*taglib.*uri=\\\"(.*?)\\\".*%>");
    public static final Pattern RULE_IMPORT_PATTERN = Pattern.compile("^\\s*import\\s*([\\w.\\*]*)\\s*$");
    public static final Pattern GROOVY_IMPORT_PATTERN = Pattern.compile("^\\s*import\\s*(?:static)?\\s*([\\w\\.\\*]*)\\s*(?:as\\s*(\\w*)\\s*)?");

    public static final Pattern XPATH_PREFIX_PATTERN = Pattern.compile("(\\w+):[\\w-]+");

    private final static String[] SPRING_XPATH_QUERIES = {
            "//beans:bean/@class",
            "//aop:declare-parents/@implement-interface",
            "//aop:declare-parents/@default-impl",
            "//context:load-time-weaver/@weaver-class",
            "//context:component-scan/@name-generator",
            "//context:component-scan/@scope-resolver",
            "//jee:jndi-lookup/@expected-type",
            "//jee:jndi-lookup/@proxy-interface",
            "//jee:remote-slsb/@home-interface",
            "//jee:remote-slsb/@business-interface",
            "//jee:local-slsb/@business-interface",
            "//jms:listener-container/@container-class",
            "//lang:jruby/@script-interfaces",
            "//lang:bsh/@script-interfaces",
            "//oxm:class-to-be-bound/@name",
            "//oxm:jibx-marshaller/@target-class",
            "//osgi:reference/@interface",
            "//osgi:service/@interface",
            "//util:list/@list-class",
            "//util:map/@map-class",
            "//util:set/@set-class",
            "//webflow:flow-builder/@class",
            "//webflow:attribute/@type",
            "//osgi:service/osgi:interfaces/beans:value",
            "//osgi:reference/osgi:interfaces/beans:value",
            "//context:component-scan/@base-package",
    };

    private final static String[] JCR_IMPORT_XPATH_QUERIES = {
            "//@jcr:primaryType",
            "//@jcr:mixinTypes"
    };
    
    private static final Set<String> SUPPORTED_FILE_EXTENSIONS_TO_SCAN = new HashSet<String>(Arrays.asList("jsp",
            "jspf", "cnd", "drl", "xml", "groovy"));
    
    private static final Set<String> DEPENDENCIES_SCAN_PACKAGING = new HashSet<String>(Arrays.asList("jar", "war"));

    private static final Set<String> DEPENDENCIES_SCAN_SCOPES = new HashSet<String>(Arrays.asList(
            Artifact.SCOPE_PROVIDED, Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME));
    
    private Set<String> dependenciesThatCanBeSkipped = new TreeSet<String>(); 

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    /**
     * @parameter
     */
    protected List<String> artifactExcludes = new ArrayList<String>();

    /**
     * @parameter default-value="${project.basedir}/src/main/resources,${project.basedir}/src/main/import,${project.basedir}/src/main/webapp"
     */
    protected List<String> scanDirectories;

    /**
     * @parameter
     */
    protected List<String> excludeFromDirectoryScan = new ArrayList<String>();

    /**
     * @parameter default-value="${project.build.outputDirectory}"
     */
    protected String projectOutputDirectory;

    /**
     * @parameter
     */
    protected File propertiesInputFile;

    /**
     * @parameter
     */
    protected File propertiesOutputFile;

    /**
     * @parameter default-value="org.osgi.framework.system.capabilities.extra"
     */
    protected String systemExtraCapabilitiesPropertyName = "org.osgi.framework.system.capabilities.extra";

    /**
     * @parameter default-value="true"
     */
    protected boolean contentDefinitionCapabilitiesActivated = true;

    /**
     * @parameter default-value="" expression="${jahia.modules.importPackage}"
     */
    protected String existingImports = "";

    private Set<String> packageImports = new TreeSet<String>();
    private Set<String> taglibUris = new TreeSet<String>();
    private Map<String, Set<String>> taglibPackages = new HashMap<String, Set<String>>();
    private Map<String, Boolean> externalTaglibs = new HashMap<String, Boolean>();
    private Set<String> contentTypeDefinitions = new TreeSet<String>();
    private Set<String> contentTypeReferences = new TreeSet<String>();
    private List<Pattern> artifactExclusionPatterns = new ArrayList<Pattern>();
    private Set<String> artifactsToSkip = new TreeSet<String>();
    private Set<String> projectPackages = new TreeSet<String>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (project.getGroupId().equals("org.jahia.modules") && project.getArtifactId().equals("jahia-modules")) {
            return;
        }
        long startTime = System.currentTimeMillis();

        if (existingImports != null) {
            addAllPackageImports(Arrays.asList(StringUtils.split(existingImports, ", \n\r")));
        }

        buildExclusionPatterns();
        
        readArtifactsToSkip();
        
        long timer = System.currentTimeMillis();
        
        try {
            scanClassesBuildDirectory();
            
            getLog().info(
                    "Scanned classes directory in " + (System.currentTimeMillis() - timer) + " ms. Found "
                            + projectPackages.size() + " project packages.");

            timer = System.currentTimeMillis();
            
            int scanned = scanDependencies();
            
            getLog().info(
                    "Scanned " + scanned + " project dependencies in " + (System.currentTimeMillis() - timer)
                            + " ms. Currently we have " + projectPackages.size() + " project packages.");
        } catch (IOException e) {
            throw new MojoFailureException("Error while scanning dependencies", e);
        }

        timer = System.currentTimeMillis();
        for (String scanDirectory : scanDirectories) {
            File scanDirectoryFile = new File(scanDirectory);
            if (!scanDirectoryFile.exists()) {
                getLog().debug("Couldn't find directory " + scanDirectoryFile + ", skipping !");
                continue;
            }
            try {
                getLog().info("Scanning resource directory " + scanDirectoryFile + "...");
                processDirectoryTlds(scanDirectoryFile);
                processDirectory(scanDirectoryFile);
            } catch (IOException e) {
                throw new MojoFailureException("Error processing resource directory " + scanDirectoryFile, e);
            }
        }
        getLog().info(
                "Scanned resource directories in " + (System.currentTimeMillis() - timer)
                        + " ms. Currently we have " + projectPackages.size() + " project packages.");
        if (getLog().isDebugEnabled()) {
            getLog().debug("Found project packages (potential exports) :");
            for (String projectPackage : projectPackages) {
                getLog().debug("  " + projectPackage);
            }
        }

        // now let's remove all the project packages from the imports, we assume we will not import split packages.
        packageImports.removeAll(projectPackages);

        contentTypeReferences.removeAll(contentTypeDefinitions);

        StringBuilder generatedPackageBuffer = new StringBuilder(256);
        int i = 0;
        for (String packageImport : packageImports) {
            generatedPackageBuffer.append(packageImport);
            if (i < packageImports.size() - 1) {
                generatedPackageBuffer.append(",\n");
            }
            i++;
        }
        getLog().info("Generated " + packageImports.size() + " package imports for project.");
        getLog().info("Found referenced tag library URIs (from JSPs) :");
        for (String taglibUri : taglibUris) {
            boolean foundInDependencies = taglibPackages.containsKey(taglibUri);
            String foundMessage = "";
            if (!foundInDependencies) {
                foundMessage = "NOT FOUND";
            }
            if (foundInDependencies) {
                boolean externalTagLib = externalTaglibs.get(taglibUri);
                if (externalTagLib) {
                    foundMessage += " provided";
                }
            }
            getLog().info("  " + taglibUri + " " + foundMessage);
        }

        StringBuilder contentTypeDefinitionsBuffer = new StringBuilder(256);
        if (contentTypeDefinitions.size() > 0) {
            contentTypeDefinitionsBuffer.append("com.jahia.services.content; nodetypes:List<String>=\"");
            i = 0;
            for (String contentTypeName : contentTypeDefinitions) {
                contentTypeDefinitionsBuffer.append(contentTypeName);
                if (i < contentTypeDefinitions.size() - 1) {
                    contentTypeDefinitionsBuffer.append(",");
                }
                i++;
            }
            contentTypeDefinitionsBuffer.append("\"");
        }
        if (contentDefinitionCapabilitiesActivated) {
            getLog().info("Found " + contentTypeDefinitions.size() + " new content node type definitions in project.");
            getLog().debug("Provide-Capability: " + contentTypeDefinitionsBuffer.toString());
            project.getProperties().put("jahia.plugin.providedNodeTypes", contentTypeDefinitionsBuffer.toString());
        } else {
            // we set an empty property so that Maven will not fail the build with a non-existing property
            project.getProperties().put("jahia.plugin.providedNodeTypes", "");
        }

        StringBuffer contentTypeReferencesBuffer = new StringBuffer();
        if (contentTypeReferences.size() > 0) {
            i = 0;
            for (String contentTypeReference : contentTypeReferences) {
                contentTypeReferencesBuffer.append("com.jahia.services.content; filter:=\"(nodetypes=" + contentTypeReference + ")\"");
                if (i < contentTypeReferences.size() - 1) {
                    contentTypeReferencesBuffer.append(",");
                }
                i++;
            }
        }

        if (contentDefinitionCapabilitiesActivated) {
            getLog().info("Found " + contentTypeReferences.size() + " content node type definitions referenced in project.");
            getLog().debug("Require-Capability: " + contentTypeReferencesBuffer.toString());
            project.getProperties().put("jahia.plugin.requiredNodeTypes", contentTypeReferencesBuffer.toString());
        } else {
            // we set an empty property so that Maven will not fail the build with a non-existing property
            project.getProperties().put("jahia.plugin.requiredNodeTypes", "");
        }

        String generatedPackageList = generatedPackageBuffer.toString();
        project.getProperties().put("jahia.plugin.projectPackageImport", generatedPackageList);
        getLog().debug("Set project property jahia.plugin.projectPackageImport to package import list value: ");
        getLog().debug(generatedPackageList);

        if (propertiesOutputFile != null) {
            String[] extraCapabilitiesPropertyValue = new String[] {
                    contentTypeDefinitionsBuffer.toString()
            };
            try {
                PropertyFileUtils.updatePropertyFile(
                        propertiesInputFile,
                        propertiesOutputFile,
                        systemExtraCapabilitiesPropertyName,
                        extraCapabilitiesPropertyValue,
                        getLog());
            } catch (IOException e) {
                getLog().warn("Error saving extra system capabilities to file " + propertiesOutputFile);
            }
        }
        getLog().info("Took " + (System.currentTimeMillis() - startTime) + " ms for the dependencies analysis");
    }

    private void readArtifactsToSkip() {
        InputStream is = this.getClass().getResourceAsStream("dependenciesToSkip.txt");
        try {
            for (String dependency : IOUtils.readLines(is)) {
                String d = dependency.trim();
                if (d.length() > 0) {
                    artifactsToSkip.add(d);
                }
            }
        } catch (IOException e) {
            getLog().error("Unable to read dependenciesToSkip.txt", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
        getLog().info(
                artifactsToSkip.size() + " artifacts will be skipped (as configured in the dependenciesToSkip.txt)");
    }

    private void processProjectPackageEntry(String entry, String fileSeparator) {
        int lastSlash = entry.lastIndexOf(fileSeparator);
        if (lastSlash == -1) {
            return;
        }
        String entryPackage = StringUtils.replace(entry.substring(0, lastSlash), fileSeparator, ".");
        if (StringUtils.isNotEmpty(entryPackage) && !projectPackages.contains(entryPackage)
                && !entryPackage.startsWith("META-INF") && !entryPackage.startsWith("OSGI-INF")
                && !entryPackage.startsWith("OSGI-OPT") && !entryPackage.startsWith("WEB-INF")
                && !entryPackage.startsWith("org.osgi")) {
            projectPackages.add(entryPackage);
        }
    }
    

    private void scanClassesBuildDirectory() throws IOException {
        File outputDirectoryFile = new File(projectOutputDirectory);
        if (!outputDirectoryFile.exists()) {
            getLog().warn("Couldn't scan project output directory " + outputDirectoryFile + " because it doesn't exist !");
            return;
        }
        getLog().info("Scanning project build directory " + outputDirectoryFile.getCanonicalPath());
        DirectoryScanner ds = new DirectoryScanner();
        String[] excludes = {"META-INF/**", "OSGI-INF/**", "OSGI-OPT/**", "WEB-INF/**"};
        ds.setExcludes(excludes);
        ds.setBasedir(outputDirectoryFile);
        ds.setCaseSensitive(true);
        ds.scan();
        String[] includedFiles = ds.getIncludedFiles();
        for (String includedFile : includedFiles) {
            processProjectPackageEntry(includedFile, File.separator);
        }
    }

    private void buildExclusionPatterns() {
        if (artifactExcludes != null) {
            for (String artifactExclude : artifactExcludes) {
                int colonPos = artifactExclude.indexOf(":");
                String groupPattern = ".*";
                String artifactPattern = null;
                if (colonPos > -1) {
                    groupPattern = artifactExclude.substring(0, colonPos);
                    artifactPattern = artifactExclude.substring(colonPos + 1);
                } else {
                    artifactPattern = artifactExclude;
                }
                groupPattern = groupPattern.replaceAll("\\.", "\\\\.");
                groupPattern = groupPattern.replaceAll("\\*", ".*");
                artifactPattern = artifactPattern.replaceAll("\\.", "\\\\.");
                artifactPattern = artifactPattern.replaceAll("\\*", ".*");
                artifactExclusionPatterns.add(Pattern.compile(groupPattern + ":" + artifactPattern));
            }
        }
        if (artifactExclusionPatterns.size() > 0) {
            getLog().info(
                    "Configured " + artifactExclusionPatterns.size()
                            + " artifact exclusions for scanning project dependencies: "
                            + artifactExclusionPatterns.toArray());
        } else {
            getLog().info("No artifact exclusions specified. Will scan all related dependencies of the project.");
        }
    }

    private int scanDependencies() throws IOException {
        getLog().info("Scanning project dependencies...");
        int scanned = 0;
        for (Artifact artifact : project.getArtifacts()) {
            if (!DEPENDENCIES_SCAN_PACKAGING.contains(artifact.getType())
                    || !DEPENDENCIES_SCAN_SCOPES.contains(artifact.getScope()) || isExcludedFromScan(artifact)) {
                continue;
            }

            boolean externalDependency = Artifact.SCOPE_PROVIDED.equals(artifact.getScope());

            long timer = System.currentTimeMillis();

            int scannedInJar = scanJar(artifact.getFile(), artifact.getBaseVersion(), externalDependency,
                    "war".equals(artifact.getType()) ? "WEB-INF/classes/" : "");

            long took = System.currentTimeMillis() - timer;
            if (getLog().isInfoEnabled() && (scannedInJar > 0)) {
                getLog().info(
                        "Processed " + scannedInJar + ((scanned == 1) ? " entry" : " entries") + " in "
                                + (externalDependency ? "external " : "") + "dependency " + artifact + " in " + took
                                + " ms");
            }
            if (scannedInJar == 0 && !externalDependency) {
                dependenciesThatCanBeSkipped.add(artifact.toString());
            }

            scanned++;
        }
        
        if (dependenciesThatCanBeSkipped.size() > 0) {
            File fld = new File(projectOutputDirectory).getParentFile();
            fld.mkdirs();
            File dependenciesFile = new File(fld, "dependenciesToSkip.txt");
            getLog().info(
                    dependenciesThatCanBeSkipped.size() + " dependencies could have been skipped"
                            + " as they do not contain TLDs or other processable resources." + " See "
                            + dependenciesFile + " for details.");
            FileOutputStream os = new FileOutputStream(dependenciesFile);
            try {
                IOUtils.writeLines(dependenciesThatCanBeSkipped, "\n", os);
            } finally {
                IOUtils.closeQuietly(os);
            }
        }

        return scanned;
    }

    private boolean isExcludedFromScan(Artifact artifact) {
        String id = StringUtils.substringBeforeLast(artifact.toString(), ":");
        if (artifactsToSkip.contains(id) || id.contains(":") && artifactsToSkip.contains(StringUtils.substringBeforeLast(id, ":"))) {
            return true;
        }
        for (Pattern exclusionPattern : artifactExclusionPatterns) {
            id = artifact.getGroupId() + ":" + artifact.getArtifactId();
            Matcher exclusionMatcher = exclusionPattern.matcher(id);
            if (exclusionMatcher.matches()) {
                getLog().info("Ignoring artifact as the exclusion matched for " + id);
                return true;
            }
        }
        return false;
    }

    private int scanJar(File jarFile, String defaultVersion, boolean externalDependency, String packageDirectory) throws IOException {
        int scanned = 0;
        JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile));
        try {
            JarEntry jarEntry = null;
            // getLog().debug("Processing file " + artifact.getFile() + "...");
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                if (jarEntry.isDirectory()) {
                    continue;
                }
                String entryName = jarEntry.getName();
                String ext = FileUtils.getExtension(entryName).toLowerCase();

                if (!externalDependency && entryName.startsWith(packageDirectory)) {
                    entryName = entryName.substring(packageDirectory.length());
                    processProjectPackageEntry(entryName, "/");
                }

                if (!"tld".equals(ext) && (!SUPPORTED_FILE_EXTENSIONS_TO_SCAN.contains(ext) || externalDependency)
                        || entryName.endsWith("/pom.xml")) {
                    continue;
                }
                ByteArrayOutputStream entryOutputStream = new ByteArrayOutputStream();
                IOUtils.copy(jarInputStream, entryOutputStream);
                if ("tld".equals(ext)) {
                    getLog().info("\tscanning entry: " + jarEntry.getName());
                    processTld(jarEntry.getName(), new ByteArrayInputStream(entryOutputStream.toByteArray()),
                            externalDependency);
                    scanned++;
                }
                if (!externalDependency && SUPPORTED_FILE_EXTENSIONS_TO_SCAN.contains(ext)) {
                    getLog().info("\tscanning entry: " + jarEntry.getName());
                    if (processNonTldFile(jarEntry.getName(), new ByteArrayInputStream(entryOutputStream.toByteArray()))) {
                        scanned++;
                    }
                }
            }
        } finally {
            jarInputStream.close();
        }
        
        return scanned;
    }

    private void processDirectoryTlds(File directoryFile) throws IOException {
        DirectoryScanner ds = new DirectoryScanner();
        String[] excludes = excludeFromDirectoryScan.toArray(new String[excludeFromDirectoryScan.size()]);
        ds.setExcludes(excludes);
        ds.setIncludes(new String[] {"**/*.tld"});
        ds.setBasedir(directoryFile);
        ds.setCaseSensitive(true);
        ds.scan();
        String[] includedFiles = ds.getIncludedFiles();
        for (String includedFile : includedFiles) {
            processTld(includedFile, new BufferedInputStream(new FileInputStream(new File(directoryFile, includedFile))), false);
        }
    }

    private void processDirectory(File directoryFile) throws IOException {
        DirectoryScanner ds = new DirectoryScanner();
        String[] excludes = excludeFromDirectoryScan.toArray(new String[excludeFromDirectoryScan.size()]);
        ds.setExcludes(excludes);
        ds.setBasedir(directoryFile);
        ds.setCaseSensitive(true);
        ds.scan();
        String[] includedFiles = ds.getIncludedFiles();
        for (String includedFile : includedFiles) {
            String ext = FileUtils.getExtension(includedFile).toLowerCase();
            if (!SUPPORTED_FILE_EXTENSIONS_TO_SCAN.contains(ext)) {
                continue;
            }

            InputStream fileInputStream = null;
            try {
                fileInputStream = new BufferedInputStream(new FileInputStream(new File(directoryFile, includedFile)));
                processNonTldFile(includedFile, fileInputStream);
            } finally {
                IOUtils.closeQuietly(fileInputStream);
            }
        }
    }

    private boolean processNonTldFile(String fileName, InputStream inputStream) throws IOException {
        String ext = FileUtils.getExtension(fileName).toLowerCase();

        if (!SUPPORTED_FILE_EXTENSIONS_TO_SCAN.contains(ext)) {
            return false;
        }

        if ("jsp".equals(ext) || "jspf".equals(ext)) {
            processJsp(fileName, inputStream);
            return true;
        } else if ("cnd".equals(ext)) {
            processCnd(fileName, inputStream);
            return true;
        } else if ("drl".equals(ext)) {
            processDrl(fileName, inputStream);
            return true;
        } else if ("xml".equals(ext)) {
            return processXml(fileName, inputStream);
        } else if ("groovy".equals(ext)) {
            processGroovy(fileName, inputStream);
            return true;
        }
        
        return false;
    }

    private void processGroovy(String fileName, InputStream inputStream) throws IOException {
        getLog().debug("Processing Groovy file " + fileName + "...");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            Matcher groovyImportMatcher = GROOVY_IMPORT_PATTERN.matcher(line);
            if (groovyImportMatcher.matches()) {
                String groovyImport = groovyImportMatcher.group(1);
                getLog().debug(fileName + ": found Groovy import " + groovyImport + " package=" + PackageUtils.getPackageFromClass(groovyImport));
                addPackageImport(PackageUtils.getPackageFromClass(groovyImport));
            }
        }
    }

    private boolean processXml(String fileName, InputStream inputStream) throws IOException {
        boolean processed = true;
        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setValidation(false);
        saxBuilder.setFeature("http://xml.org/sax/features/validation", false);
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        FileInputStream childFileInputStream = null;
        try {
            InputStreamReader fileReader = new InputStreamReader(inputStream);
            org.jdom.Document jdomDocument = saxBuilder.build(fileReader);
            Element root = jdomDocument.getRootElement();
            // getLog().debug("Parsed XML file" + fileName + " successfully.");

            if (fileName.toLowerCase().endsWith(".jpdl.xml")) {
                processJpdl(fileName, root);
            } else if (hasNamespaceURI(root, "http://www.jcp.org/jcr/1.0")) {
                processJCRImport(fileName, root);
            } else if (hasNamespaceURI(root, "http://www.springframework.org/schema/beans")) {
                processSpringContext(fileName, root);
            } else {
                processed = false;
            }
        } catch (JDOMException e) {
            getLog().warn("Error parsing XML file " + fileName + ": " + e.getMessage() + " enable debug mode (-X) for more detailed exception");
            getLog().debug("Detailed exception", e);
        } finally {
            IOUtils.closeQuietly(childFileInputStream);
        }
        return processed;
    }

    private void processJCRImport(String fileName, Element root) throws JDOMException {
        getLog().debug("Processing JCR import file " + fileName + "...");

        getRefsUsingXPathQueries(fileName, root, false, JCR_IMPORT_XPATH_QUERIES, "xp");
    }

    private void processSpringContext(String fileName, Element root) throws JDOMException {
        getLog().debug("Processing Spring context file " + fileName + "...");

        getRefsUsingXPathQueries(fileName, root, true, SPRING_XPATH_QUERIES, "beans");
    }

    private void getRefsUsingXPathQueries(String fileName, Element root,
                                          boolean packageReferences,
                                          String[] xPathQueries, String defaultNamespacePrefix) throws JDOMException {
        for (String xPathQuery : xPathQueries) {
            Set<String> missingPrefixes = getMissingQueryPrefixes(root, xPathQuery);
            if (missingPrefixes.size() > 0) {
                getLog().debug(fileName + ": xPath query " + xPathQuery + " cannot be executed on this file since it has prefixes not declared in the file: " + missingPrefixes);
                continue;
            }
            List<Object> classObjects = getNodes(root, xPathQuery, defaultNamespacePrefix);
            for (Object classObject : classObjects) {
                String referenceValue = null;
                if (classObject instanceof Attribute) {
                    referenceValue = ((Attribute) classObject).getValue();
                } else if (classObject instanceof Element) {
                    referenceValue = ((Element) classObject).getTextTrim();
                } else {
                    getLog().warn(fileName + ": xPath query" + xPathQuery + " return unknown XML node type " + classObject + "...");
                }
                if (referenceValue != null) {
                        if (packageReferences) {
                            getLog().debug(fileName + " Found class " + referenceValue + " package=" + PackageUtils.getPackageFromClass(referenceValue));
                            addPackageImport(PackageUtils.getPackageFromClass(referenceValue));
                        } else {
                            if (referenceValue.contains(" ")) {
                                getLog().debug(fileName + "Found multi-valued reference: " + referenceValue);
                                String[] referenceValueArray = referenceValue.split(" ");
                                for (String reference : referenceValueArray) {
                                    getLog().debug(fileName + " Found content type " + referenceValue + " reference");
                                    contentTypeReferences.add(reference);
                                }
                            } else if (referenceValue.contains(",")) {
                                getLog().debug(fileName + "Found multi-valued reference: " + referenceValue);
                                String[] referenceValueArray = referenceValue.split(",");
                                for (String reference : referenceValueArray) {
                                    getLog().debug(fileName + " Found content type " + referenceValue + " reference");
                                    contentTypeReferences.add(reference);
                                }
                            } else {
                                getLog().debug(fileName + " Found content type " + referenceValue + " reference");
                                contentTypeReferences.add(referenceValue);
                            }
                        }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> getMissingQueryPrefixes(Element root, String xPathQuery) {
        Set<String> xPathQueryPrefixes = getPrefixesInXPath(xPathQuery);
        Set<String> elementPrefixes = new HashSet<String>();
        for (Namespace additionalNamespaces : (List<Namespace>) root.getAdditionalNamespaces()) {
            elementPrefixes.add(additionalNamespaces.getPrefix());
        }
        elementPrefixes.add("beans");
        Set<String> missingPrefixes = new TreeSet<String>();
        for (String xPathQueryPrefix : xPathQueryPrefixes) {
            if (!elementPrefixes.contains(xPathQueryPrefix)) {
                missingPrefixes.add(xPathQueryPrefix);
            }
        }
        return missingPrefixes;
    }

    private Set<String> getPrefixesInXPath(String xPathQuery) {
        Set<String> prefixes = new TreeSet<String>();
        Matcher xPathPrefixMatcher = XPATH_PREFIX_PATTERN.matcher(xPathQuery);
        while (xPathPrefixMatcher.find()) {
            prefixes.add(xPathPrefixMatcher.group(1));
        }
        return prefixes;
    }

    private boolean hasNamespaceURI(Element element, String namespaceURI) {
        //getLog().debug("Main namespace URI=" + element.getNamespace().getURI());
        if (element.getNamespace().getURI().equals(namespaceURI)) {
            return true;
        }
        @SuppressWarnings("unchecked")
        List<Namespace> additionalNamespaces = (List<Namespace>) element.getAdditionalNamespaces();
        for (Namespace additionalNamespace : additionalNamespaces) {
            //getLog().debug("Additional namespace URI=" + additionalNamespace.getURI());
            if (additionalNamespace.getURI().equals(namespaceURI)) {
                return true;
            }
        }
        return false;
    }

    private void processJpdl(String fileName, Element root) throws IOException, JDOMException {
        getLog().debug("Processing workflow definition file (JBPM JPDL) " + fileName + "...");
        List<Attribute> classAttributes = getAttributes(root, "//@class");
        for (Attribute classAttribute : classAttributes) {
            getLog().debug(fileName + " Found class " + classAttribute.getValue() + " package=" + PackageUtils.getPackageFromClass(classAttribute.getValue()));
            addPackageImport(PackageUtils.getPackageFromClass(classAttribute.getValue()));
        }
    }

    private void processDrl(String fileName, InputStream inputStream) throws IOException {
        getLog().debug("Processing Drools Rule file " + fileName + "...");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            Matcher ruleImportMatcher = RULE_IMPORT_PATTERN.matcher(line);
            if (ruleImportMatcher.matches()) {
                String ruleImport = ruleImportMatcher.group(1);
                getLog().debug(fileName + ": found rule import " + ruleImport + " package=" + PackageUtils.getPackageFromClass(ruleImport));
                addPackageImport(PackageUtils.getPackageFromClass(ruleImport));
            }
        }
    }

    private void processCnd(String fileName, InputStream inputStream) throws IOException {
        getLog().debug("Processing CND " + fileName + "...");

        try {
            JahiaCndReader jahiaCndReader = new JahiaCndReader(new InputStreamReader(inputStream), fileName, fileName, NodeTypeRegistry.getInstance());
            jahiaCndReader.setDoRegister(false);
            jahiaCndReader.parse();

            jahiaCndReader.getDefinitionsAndReferences(contentTypeDefinitions, contentTypeReferences);
        } catch (ParseException e) {
            getLog().error("Error while parsing CND file " + fileName, e);
        } catch (ValueFormatException e) {
            getLog().error("Error while parsing CND file " + fileName, e);
        } catch (RepositoryException e) {
            getLog().error("Error while parsing CND file " + fileName, e);
        }
    }

    private void processTld(String fileName, InputStream inputStream, boolean externalDependency) throws IOException {
        getLog().debug("Processing TLD " + fileName + "...");
        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setValidation(false);
        saxBuilder.setFeature("http://xml.org/sax/features/validation", false);
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        FileInputStream childFileInputStream = null;
        try {
            InputStreamReader fileReader = new InputStreamReader(inputStream);
            org.jdom.Document jdomDocument = saxBuilder.build(fileReader);
            Element root = jdomDocument.getRootElement();
            dumpElementNamespaces(root);
            boolean hasDefaultNamespace = !StringUtils.isEmpty(root.getNamespaceURI());
            if (hasDefaultNamespace) {
                getLog().debug("Using default namespace XPath queries");
            }

            Element uriElement = null;
            if (hasDefaultNamespace) {
                uriElement = getElement(root, "/xp:taglib/xp:uri");
            } else {
                uriElement = getElement(root, "/taglib/uri");
            }
            if (uriElement == null) {
                getLog().warn("Couldn't find /taglib/uri tag in " + fileName + ", aborting TLD parsing !");
                return;
            }
            String uri = uriElement.getTextTrim();
            getLog().debug("Taglib URI=" + uri);
            Set<String> taglibPackageSet = taglibPackages.get(uri);
            if (taglibPackageSet == null) {
                taglibPackageSet = new TreeSet<String>();
            }

            List<Element> tagClassElements = null;
            if (hasDefaultNamespace) {
                tagClassElements = getElements(root, "//xp:tag/xp:tag-class");
            } else {
                tagClassElements = getElements(root, "//tag/tag-class");
            }
            for (Element tagClassElement : tagClassElements) {
                getLog().debug(fileName + " Found tag class " + tagClassElement.getTextTrim() + " package=" + PackageUtils.getPackageFromClass(tagClassElement.getTextTrim()));
                taglibPackageSet.add(PackageUtils.getPackageFromClass(tagClassElement.getTextTrim()));
            }
            List<Element> functionClassElements = null;
            if (hasDefaultNamespace) {
                functionClassElements = getElements(root, "//xp:function/xp:function-class");
            } else {
                functionClassElements = getElements(root, "//function/function-class");
            }
            for (Element functionClassElement : functionClassElements) {
                getLog().debug(fileName + " Found function class " + functionClassElement.getTextTrim() + " package=" + PackageUtils.getPackageFromClass(functionClassElement.getTextTrim()));
                taglibPackageSet.add(PackageUtils.getPackageFromClass(functionClassElement.getTextTrim()));
            }
            taglibPackages.put(uri, taglibPackageSet);
            externalTaglibs.put(uri, externalDependency);
        } catch (JDOMException e) {
            getLog().warn("Error parsing TLD file " + fileName, e);
        } finally {
            IOUtils.closeQuietly(childFileInputStream);
        }
    }


    private void processJsp(String fileName, InputStream inputStream) throws IOException {
        getLog().debug("Processing JSP " + fileName + "...");
        String jspFileContent = IOUtils.toString(inputStream);
        Matcher pageImportMatcher = JSP_PAGE_IMPORT_PATTERN.matcher(jspFileContent);
        while (pageImportMatcher.find()) {
            String classImportString = pageImportMatcher.group(1);
            if (classImportString.contains(",")) {
                getLog().debug("Multiple imports in a single JSP page import statement detected: " + classImportString);
                String[] classImports = StringUtils.split(classImportString, ",");
                Set<String> packageImports = new TreeSet<String>();
                for (String classImport : classImports) {
                    packageImports.add(PackageUtils.getPackageFromClass(classImport.trim()));
                }
                addAllPackageImports(packageImports);
            } else {
                addPackageImport(PackageUtils.getPackageFromClass(classImportString));
            }
        }
        Matcher taglibUriMatcher = JSP_TAGLIB_PATTERN.matcher(jspFileContent);
        while (taglibUriMatcher.find()) {
            String taglibUri = taglibUriMatcher.group(1);
            taglibUris.add(taglibUri);
            if (!taglibPackages.containsKey(taglibUri)) {
                getLog().warn("JSP " + fileName + " has a reference to taglib " + taglibUri + " that is not in the project's dependencies !");
            } else {
                Set<String> taglibPackageSet = taglibPackages.get(taglibUri);
                boolean externalTagLib = externalTaglibs.get(taglibUri);
                if (externalTagLib) {
                    addAllPackageImports(taglibPackageSet);
                }
            }
        }
    }

    /**
     * Utility method to retrieve an XML element using an XPath expression. Note that this method is
     * namespace aware and will require you to use the "xp" prefix in your XPath queries. For example, an XPath query
     * for a Spring XML configuration will look like this :
     * /xp:beans/xp:bean[@id="FileListSync"]/xp:property[@name="syncUrl"]
     * Currently there is no way to rename the prefix.
     *
     * @param scopeElement    the scope in which to execute the XPath query
     * @param xPathExpression the XPath query to select the element we wish to retrieve. In the case where multiple
     *                        elements match, only the first one will be returned.
     * @return the first element that matches the XPath expression, or null if no element matches.
     * @throws JDOMException raised if there was a problem navigating the JDOM structure.
     */
    @SuppressWarnings("unchecked")
    public Element getElement(Element scopeElement, String xPathExpression) throws JDOMException {
        XPath xPath = XPath.newInstance(xPathExpression);
        String namespaceURI = scopeElement.getDocument().getRootElement().getNamespaceURI();
        if ((namespaceURI != null) && (!"".equals(namespaceURI))) {
            xPath.addNamespace("xp", namespaceURI);
        }
        for (Namespace additionalNamespace : (List<Namespace>) scopeElement.getDocument().getRootElement().getAdditionalNamespaces()) {
            xPath.addNamespace(additionalNamespace);
        }
        return (Element) xPath.selectSingleNode(scopeElement);
    }

    @SuppressWarnings("unchecked")
    public List<Element> getElements(Element scopeElement, String xPathExpression) throws JDOMException {
        List<Element> elems = new LinkedList<Element>();
        XPath xPath = XPath.newInstance(xPathExpression);
        String namespaceURI = scopeElement.getDocument().getRootElement().getNamespaceURI();
        if ((namespaceURI != null) && (!"".equals(namespaceURI))) {
            xPath.addNamespace("xp", namespaceURI);
        }
        for (Namespace additionalNamespace : (List<Namespace>) scopeElement.getDocument().getRootElement().getAdditionalNamespaces()) {
            xPath.addNamespace(additionalNamespace);
        }
        for (Object obj : xPath.selectNodes(scopeElement)) {
            if (obj instanceof Element) {
                elems.add((Element) obj);
            }
        }

        return elems;
    }

    @SuppressWarnings("unchecked")
    public List<Attribute> getAttributes(Element scopeElement, String xPathExpression) throws JDOMException {
        List<Attribute> elems = new LinkedList<Attribute>();
        XPath xPath = XPath.newInstance(xPathExpression);
        String namespaceURI = scopeElement.getDocument().getRootElement().getNamespaceURI();
        if ((namespaceURI != null) && (!"".equals(namespaceURI))) {
            xPath.addNamespace("xp", namespaceURI);
        }
        for (Namespace additionalNamespace : (List<Namespace>) scopeElement.getDocument().getRootElement().getAdditionalNamespaces()) {
            xPath.addNamespace(additionalNamespace);
        }
        for (Object obj : xPath.selectNodes(scopeElement)) {
            if (obj instanceof Attribute) {
                elems.add((Attribute) obj);
            }
        }

        return elems;
    }

    @SuppressWarnings("unchecked")
    public List<Object> getNodes(Element scopeElement, String xPathExpression, String defaultPrefix) throws JDOMException {
        List<Object> nodes = new LinkedList<Object>();
        XPath xPath = XPath.newInstance(xPathExpression);
        String namespaceURI = scopeElement.getDocument().getRootElement().getNamespaceURI();
        if ((namespaceURI != null) && (!"".equals(namespaceURI))) {
            xPath.addNamespace(defaultPrefix, namespaceURI);
        }
        for (Namespace additionalNamespace : (List<Namespace>) scopeElement.getDocument().getRootElement().getAdditionalNamespaces()) {
            xPath.addNamespace(additionalNamespace);
        }
        for (Object obj : xPath.selectNodes(scopeElement)) {
            nodes.add(obj);
        }
        return nodes;
    }

    private void addPackageImport(String packageName) {
        if (StringUtils.isEmpty(packageName)) {
            return;
        }
        if (!packageName.startsWith("java.") &&
            !packageImports.contains(packageName) &&
            !packageImports.contains(packageName + ";resolution:=optional")) {
            packageImports.add(packageName);
        }
    }

    private void addAllPackageImports(Collection<String> packageNames) {
        for (String packageName : packageNames) {
            addPackageImport(packageName);
        }
    }

    @SuppressWarnings("unchecked")
    private void dumpElementNamespaces(Element element) {
        Namespace mainNamespace = element.getNamespace();
        getLog().debug("Main namespace prefix=[" + mainNamespace.getPrefix() + "] uri=[" + mainNamespace.getURI() + "] getNamespaceURI=[" + element.getNamespaceURI() + "]");
        for (Namespace additionalNamespace : (List<Namespace>) element.getAdditionalNamespaces()) {
            getLog().debug("Additional namespace prefix=" + additionalNamespace.getPrefix() + " uri=" + additionalNamespace.getURI());
        }
    }

}
