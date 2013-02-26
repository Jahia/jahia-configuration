package org.jahia.utils.maven.plugin.osgi;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.tika.io.IOUtils;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

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
 * - String context files
 * - JCR CND content definition files for node type definition and references
 *
 * @goal dependencies
 * @requiresDependencyResolution test
 * @todo add support for groovy files, JSP tag files, more...
 */
public class DependenciesMojo extends AbstractMojo {

    public static final Pattern JSP_PAGE_IMPORT_PATTERN = Pattern.compile("<%@.*page.*import=\\\"(.*?)\\\".*%>");
    public static final Pattern JSP_TAGLIB_PATTERN = Pattern.compile("<%@.*taglib.*uri=\\\"(.*?)\\\".*%>");
    public static final Pattern CND_NAMESPACE_PATTERN = Pattern.compile("<\\s*(\\w*)\\s*=\\s*'(.*)'\\s*>");
    public static final Pattern CND_NODETYPE_DEFINITION_PATTERN = Pattern.compile("\\s+\\[([\\w:]+)\\]\\s+");
    public static final Pattern CND_ALL_NAMES_PATTERN = Pattern.compile("([a-zA-Z_]\\w+:\\w+)(?:\\s|'|\\]|\\)|,)");
    public static final Pattern CND_ALL_PROPERTY_NAMES_PATTERN = Pattern.compile("\\s+-\\s*([\\w:]+)");
    public static final Pattern CND_ALL_CHILD_NAMES_PATTERN = Pattern.compile("\\s+\\+\\s*([\\w:\\*]+)");
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
            "//jcr:mixinTypes"
    };

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    /**
     * @parameter default-value="org.jahia.modules:*,org.jahia.templates:*,org.jahia.test:*,*.jahia.modules"
     */
    protected List<String> artifactExcludes;

    /**
     * @parameter default-value="${project.basedir}/src/main/resources,${project.basedir}/src/main/import,${project.basedir}/src/main/webapp"
     */
    protected List<String> scanDirectories;

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
     * @parameter default-value="" expression="${jahia.modules.importPackage}"
     */
    protected List<String> existingImports = new ArrayList<String>();

    private Set<String> packageImports = new TreeSet<String>();
    private Set<String> taglibUris = new TreeSet<String>();
    private Map<String, Set<String>> taglibPackages = new HashMap<String, Set<String>>();
    private Map<String, Boolean> externalTaglibs = new HashMap<String, Boolean>();
    private Set<String> contentTypeDefinitions = new TreeSet<String>();
    private Set<String> contentTypeReferences = new TreeSet<String>();
    private List<Pattern> artifactExclusionPatterns = new ArrayList<Pattern>();
    private Set<String> projectPackages = new TreeSet<String>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (existingImports != null && existingImports.size() > 0) {
            getLog().info("Using " + existingImports.size() + " existing imports as import");
            for (String existingImport : existingImports) {
                if (existingImport != null) {
                    packageImports.add(existingImport.trim());
                }
            }
        }

        buildExclusionPatterns();

        try {
            scanClassesBuildDirectory();

            scanDependencies();
        } catch (IOException e) {
            throw new MojoFailureException("Error while scanning dependencies", e);
        }

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
        getLog().debug("Found project packages (potential exports) :");
        for (String projectPackage : projectPackages) {
            getLog().debug("  " + projectPackage);
        }

        // now let's remove all the project packages from the imports, we assume we will not import split packages.
        packageImports.removeAll(projectPackages);

        contentTypeReferences.removeAll(contentTypeDefinitions);

        StringBuffer generatedPackageBuffer = new StringBuffer();
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

        StringBuffer contentTypeDefinitionsBuffer = new StringBuffer();
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
        getLog().info("Provide-Capability: " + contentTypeDefinitionsBuffer.toString());
        project.getProperties().put("jahia.plugin.providedNodeTypes", contentTypeDefinitionsBuffer.toString());

        StringBuffer contentTypeReferencesBuffer = new StringBuffer();
        if (contentTypeReferences.size() > 0) {
            i = 0;
            for (String contentSuperTypeName : contentTypeReferences) {
                contentTypeReferencesBuffer.append("com.jahia.services.content; filter:=\"(nodetypes=" + contentSuperTypeName + ")\"");
                if (i < contentTypeReferences.size() - 1) {
                    contentTypeReferencesBuffer.append(",");
                }
                i++;
            }
        }
        getLog().info("Require-Capability: " + contentTypeReferencesBuffer.toString());
        project.getProperties().put("jahia.plugin.requiredNodeTypes", contentTypeReferencesBuffer.toString());

        String generatedPackageList = generatedPackageBuffer.toString();
        project.getProperties().put("jahia.plugin.projectPackageImport", generatedPackageList);
        getLog().info("Set project property jahia.plugin.projectPackageImport to package import list value: ");
        getLog().info(generatedPackageList);

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
            // getLog().debug("Processing file " + includedFile + "...");
            String entryPackage = "";
            int lastSlash = includedFile.lastIndexOf("/");
            if (lastSlash > -1) {
                entryPackage = includedFile.substring(0, lastSlash);
                entryPackage = entryPackage.replaceAll("/", ".");
                if (StringUtils.isNotEmpty(entryPackage) &&
                        !entryPackage.startsWith("META-INF") &&
                        !entryPackage.startsWith("OSGI-INF") &&
                        !entryPackage.startsWith("OSGI-OPT") &&
                        !entryPackage.startsWith("WEB-INF") &&
                        !entryPackage.startsWith("org.osgi")) {
                    projectPackages.add(entryPackage);
                }
            }
        }
    }

    private void buildExclusionPatterns() {
        if (artifactExcludes == null) {
            return;
        }
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

    private void scanDependencies() throws IOException {
        getLog().info("Scanning project dependencies...");
        String packageDirectory = "";
        for (Artifact artifact : project.getArtifacts()) {
            String exclusionMatched = null;
            for (Pattern exclusionPattern : artifactExclusionPatterns) {
                Matcher exclusionMatcher = exclusionPattern.matcher(artifact.getGroupId() + ":" + artifact.getArtifactId());
                if (exclusionMatcher.matches()) {
                    exclusionMatched = artifact.getGroupId() + ":" + artifact.getArtifactId();
                    break;
                }
            }
            if (exclusionMatched != null) {
                getLog().info("Matched exclusion " + exclusionMatched + ", ignoring artifact.");
                continue;
            }

            if (artifact.getScope().contains(Artifact.SCOPE_PROVIDED) ||
                    artifact.getScope().contains(Artifact.SCOPE_COMPILE) ||
                    artifact.getScope().contains(Artifact.SCOPE_RUNTIME)) {
                if ("war".equals(artifact.getType())) {
                    packageDirectory = "WEB-INF/classes/";
                    getLog().debug(artifact.getFile() + " is of type WAR, changing package scanning directory to WEB-INF/classes");
                } else if (!artifact.getType().equals("jar")) {
                    getLog().warn("Ignoring artifact " + artifact.getFile() + " since it is of type " + artifact.getType());
                    continue;
                }
                boolean externalDependency = false;
                if (artifact.getScope().contains(Artifact.SCOPE_PROVIDED)) {
                    externalDependency = true;
                }
                getLog().debug("Scanning " + (externalDependency ? "external" : "") + " dependency " + artifact.getFile());
                scanJar(artifact.getFile(), artifact.getBaseVersion(), externalDependency, packageDirectory);
            }
        }

    }

    private void scanJar(File jarFile, String defaultVersion, boolean externalDependency, String packageDirectory) throws IOException {
        JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile));
        JarEntry jarEntry = null;
        // getLog().debug("Processing file " + artifact.getFile() + "...");
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            if (!jarEntry.isDirectory()) {
                String entryName = jarEntry.getName();
                String entryPackage = "";
                if (entryName.startsWith(packageDirectory)) {
                    entryName = entryName.substring(packageDirectory.length());
                    int lastSlash = entryName.lastIndexOf("/");
                    if (lastSlash > -1) {
                        entryPackage = entryName.substring(0, lastSlash);
                        entryPackage = entryPackage.replaceAll("/", ".");
                        if (!externalDependency) {
                            if (StringUtils.isNotEmpty(entryPackage) &&
                                    !entryPackage.startsWith("META-INF") &&
                                    !entryPackage.startsWith("OSGI-INF") &&
                                    !entryPackage.startsWith("OSGI-OPT") &&
                                    !entryPackage.startsWith("WEB-INF") &&
                                    !entryPackage.startsWith("org.osgi")) {
                                if (!projectPackages.contains(entryPackage)) {
                                    getLog().debug(jarFile + ": found package " + entryPackage);
                                    projectPackages.add(entryPackage);
                                }
                            }
                        }
                    }
                }
                ByteArrayOutputStream entryOutputStream = new ByteArrayOutputStream();
                IOUtils.copy(jarInputStream, entryOutputStream);
                ByteArrayInputStream tempEntryInputStream = new ByteArrayInputStream(entryOutputStream.toByteArray());
                processTldFile(jarEntry.getName(), tempEntryInputStream, entryPackage, externalDependency);
                processNonTldFile(jarEntry.getName(), tempEntryInputStream, entryPackage, externalDependency);
            }
        }
        jarInputStream.close();
    }

    private void processDirectoryTlds(File directoryFile) throws IOException {
        DirectoryScanner ds = new DirectoryScanner();
        String[] excludes = {};
        ds.setExcludes(excludes);
        ds.setBasedir(directoryFile);
        ds.setCaseSensitive(true);
        ds.scan();
        String[] includedFiles = ds.getIncludedFiles();
        for (String includedFile : includedFiles) {
            // getLog().debug("Processing file " + includedFile + "...");
            File includedFileFile = new File(directoryFile, includedFile);
            String entryPackage = "";
            int lastSlash = includedFile.lastIndexOf("/");
            if (lastSlash > -1) {
                entryPackage = includedFile.substring(0, lastSlash);
                entryPackage = entryPackage.replaceAll("/", ".");
            }
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(includedFileFile);
                processTldFile(includedFile, fileInputStream, entryPackage, false);
            } finally {
                IOUtils.closeQuietly(fileInputStream);
            }
        }
    }

    private void processDirectory(File directoryFile) throws IOException {
        DirectoryScanner ds = new DirectoryScanner();
        String[] excludes = {};
        ds.setExcludes(excludes);
        ds.setBasedir(directoryFile);
        ds.setCaseSensitive(true);
        ds.scan();
        String[] includedFiles = ds.getIncludedFiles();
        for (String includedFile : includedFiles) {
            // getLog().debug("Processing file " + includedFile + "...");
            File includedFileFile = new File(directoryFile, includedFile);
            String entryPackage = "";
            int lastSlash = includedFile.lastIndexOf("/");
            if (lastSlash > -1) {
                entryPackage = includedFile.substring(0, lastSlash);
                entryPackage = entryPackage.replaceAll("/", ".");
            }
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(includedFileFile);
                processNonTldFile(includedFile, fileInputStream, entryPackage, false);
            } finally {
                IOUtils.closeQuietly(fileInputStream);
            }
        }
    }

    private void processTldFile(String fileName, InputStream inputStream, String packageName, boolean externalDependency) throws IOException {
        String childFileExtension = FileUtils.getExtension(fileName);
        if (childFileExtension == null) {
            getLog().warn("Couldn't find extension for file " + fileName + ", ignoring it...");
            return;
        }
        childFileExtension = childFileExtension.toLowerCase();
        if ("tld".equals(childFileExtension)) {
            processTld(fileName, inputStream, externalDependency);
        }
    }

    private void processNonTldFile(String fileName, InputStream inputStream, String packageName, boolean externalDependency) throws IOException {
        String childFileExtension = FileUtils.getExtension(fileName);
        if (childFileExtension == null) {
            getLog().warn("Couldn't find extension for file " + fileName + ", ignoring it...");
            return;
        }
        childFileExtension = childFileExtension.toLowerCase();
        // getLog().debug(fileName + ": File extension=" + childFileExtension);
        if ("jsp".equals(childFileExtension) ||
                "jspf".equals(childFileExtension)) {
            if (!externalDependency) {
                processJsp(fileName, inputStream, externalDependency);
            }
        } else if ("cnd".equals(childFileExtension)) {
            if (!externalDependency) {
                processCnd(fileName, inputStream, externalDependency);
            }
        } else if ("drl".equals(childFileExtension)) {
            if (!externalDependency) {
                processDrl(fileName, inputStream, externalDependency);
            }
        } else if ("xml".equals(childFileExtension)) {
            if (!externalDependency) {
                processXml(fileName, inputStream, externalDependency);
            }
        } else if ("groovy".equals(childFileExtension)) {
            if (!externalDependency) {
                processGroovy(fileName, inputStream, externalDependency);
            }
        }
    }

    private void processGroovy(String fileName, InputStream inputStream, boolean externalDependency) throws IOException {
        getLog().debug("Processing Groovy file " + fileName + "...");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            Matcher groovyImportMatcher = GROOVY_IMPORT_PATTERN.matcher(line);
            if (groovyImportMatcher.matches()) {
                String groovyImport = groovyImportMatcher.group(1);
                getLog().debug(fileName + ": found Groovy import " + groovyImport + " package=" + PackageUtils.getPackageFromClass(groovyImport));
                if (!externalDependency) {
                    addPackageImport(PackageUtils.getPackageFromClass(groovyImport));
                }
            }
        }
    }

    private void processXml(String fileName, InputStream inputStream, boolean externalDependency) throws IOException {
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
                if (!externalDependency) {
                    processJpdl(fileName, root, externalDependency);
                }
            } else if (hasNamespaceURI(root, "http://www.jcp.org/jcr/1.0")) {
                if (!externalDependency) {
                    processJCRImport(fileName, root, externalDependency);
                }
            } else if (hasNamespaceURI(root, "http://www.springframework.org/schema/beans")) {
                if (!externalDependency) {
                    processSpringContext(fileName, root, externalDependency);
                }
            }

        } catch (JDOMException e) {
            getLog().warn("Error parsing XML file " + fileName + ": " + e.getMessage() + " enable debug mode (-X) for more detailed exception");
            getLog().debug("Detailed exception", e);
        } finally {
            IOUtils.closeQuietly(childFileInputStream);
        }
    }

    private void processJCRImport(String fileName, Element root, boolean externalDependency) throws JDOMException {
        getLog().debug("Processing JCR import file " + fileName + "...");

        getRefsUsingXPathQueries(fileName, root, false, externalDependency, JCR_IMPORT_XPATH_QUERIES, "xp");
    }

    private void processSpringContext(String fileName, Element root, boolean externalDependency) throws JDOMException {
        getLog().debug("Processing Spring context file " + fileName + "...");

        getRefsUsingXPathQueries(fileName, root, true, externalDependency, SPRING_XPATH_QUERIES, "beans");
    }

    private void getRefsUsingXPathQueries(String fileName, Element root,
                                          boolean packageReferences,
                                          boolean externalDependency,
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
                    getLog().warn(fileName + ": xPath query" + xPathQuery + " return unknown XML node type " + referenceValue.getClass().getName() + "...");
                }
                if (referenceValue != null) {
                    if (!externalDependency) {
                        if (packageReferences) {
                            getLog().debug(fileName + " Found class " + referenceValue + " package=" + PackageUtils.getPackageFromClass(referenceValue));
                            addPackageImport(PackageUtils.getPackageFromClass(referenceValue));
                        } else {
                            getLog().debug(fileName + " Found content type " + referenceValue + " reference");
                            contentTypeReferences.add(referenceValue);
                        }
                    }
                }
            }
        }
    }

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
        List<Namespace> additionalNamespaces = (List<Namespace>) element.getAdditionalNamespaces();
        for (Namespace additionalNamespace : additionalNamespaces) {
            //getLog().debug("Additional namespace URI=" + additionalNamespace.getURI());
            if (additionalNamespace.getURI().equals(namespaceURI)) {
                return true;
            }
        }
        return false;
    }

    private void processJpdl(String fileName, Element root, boolean externalDependency) throws IOException, JDOMException {
        getLog().debug("Processing workflow definition file (JBPM JPDL) " + fileName + "...");
        List<Attribute> classAttributes = getAttributes(root, "//@class");
        for (Attribute classAttribute : classAttributes) {
            getLog().debug(fileName + " Found class " + classAttribute.getValue() + " package=" + PackageUtils.getPackageFromClass(classAttribute.getValue()));
            if (!externalDependency) {
                addPackageImport(PackageUtils.getPackageFromClass(classAttribute.getValue()));
            }
        }
    }

    private void processDrl(String fileName, InputStream inputStream, boolean externalDependency) throws IOException {
        getLog().debug("Processing Drools Rule file " + fileName + "...");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            Matcher ruleImportMatcher = RULE_IMPORT_PATTERN.matcher(line);
            if (ruleImportMatcher.matches()) {
                String ruleImport = ruleImportMatcher.group(1);
                getLog().debug(fileName + ": found rule import " + ruleImport + " package=" + PackageUtils.getPackageFromClass(ruleImport));
                if (!externalDependency) {
                    addPackageImport(PackageUtils.getPackageFromClass(ruleImport));
                }
            }
        }
    }

    private void processCnd(String fileName, InputStream inputStream, boolean externalDependency) throws IOException {
        getLog().debug("Processing CND " + fileName + "...");
        String cndFileContent = IOUtils.toString(inputStream);
        Set<String> namespacePrefixes =new TreeSet<String>();
        Matcher namespaceMatcher = CND_NAMESPACE_PATTERN.matcher(cndFileContent);
        while (namespaceMatcher.find()) {
            String namespacePrefix = namespaceMatcher.group(1);
            if (namespacePrefix != null) {
                namespacePrefix = namespacePrefix.trim();
            }
            if (StringUtils.isNotEmpty(namespacePrefix) &&
                !namespacePrefixes.contains(namespacePrefix)) {
                namespacePrefixes.add(namespaceMatcher.group(1));
            }
        }
        Matcher nodeTypeDefinitionMatcher = CND_NODETYPE_DEFINITION_PATTERN.matcher(cndFileContent);
        while (nodeTypeDefinitionMatcher.find()) {
            String definitionName = nodeTypeDefinitionMatcher.group(1);
            if (hasDeclaredNamespace(definitionName, namespacePrefixes)) {
                getLog().debug(fileName + ": Found valid definition name " + definitionName);
            } else {
                getLog().debug(fileName + ": Found definition name " + definitionName + " with invalid or missing namespace prefix");
            }
            contentTypeDefinitions.add(definitionName);
        }
        Set<String> allNames = new TreeSet<String>();
        Matcher allNamesMatcher = CND_ALL_NAMES_PATTERN.matcher(cndFileContent);
        while (allNamesMatcher.find()) {
            String aName = allNamesMatcher.group(1);
            getLog().debug(fileName + ": Found name " + aName);
            allNames.add(aName);
        }
        Matcher propertyNamesMatcher = CND_ALL_PROPERTY_NAMES_PATTERN.matcher(cndFileContent);
        while (propertyNamesMatcher.find()) {
            String propertyName = propertyNamesMatcher.group(1);
            getLog().debug(fileName + ": Found property name " + propertyName);
            allNames.remove(propertyName);
        }
        Matcher childNamesMatcher = CND_ALL_CHILD_NAMES_PATTERN.matcher(cndFileContent);
        while (childNamesMatcher.find()) {
            String childName = childNamesMatcher.group(1);
            getLog().debug(fileName + ": Found child name " + childName);
            allNames.remove(childName);
        }
        allNames.removeAll(contentTypeDefinitions);
        contentTypeReferences.addAll(allNames);
    }

    private boolean hasDeclaredNamespace(String nodetypeDefinition, Set<String> namespacePrefixes) {
        String[] definitionParts = nodetypeDefinition.split("\\:");
        if (definitionParts.length != 2) {
            getLog().debug("Ignoring node type definition with invalid number of parts: " + nodetypeDefinition);
            return false;
        }
        if (namespacePrefixes.contains(definitionParts[0])) {
            return true;
        }
        getLog().debug("Ignoring node type definition with invalid prefix: " + nodetypeDefinition);
        return false;
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


    private void processJsp(String fileName, InputStream inputStream, boolean externalDependency) throws IOException {
        getLog().debug("Processing JSP " + fileName + "...");
        String jspFileContent = IOUtils.toString(inputStream);
        Matcher pageImportMatcher = JSP_PAGE_IMPORT_PATTERN.matcher(jspFileContent);
        while (pageImportMatcher.find()) {
            if (!externalDependency) {
                String classImportString = pageImportMatcher.group(1);
                if (classImportString.contains(",")) {
                    getLog().debug("Multiple imports in a single JSP page import statement detected: " + classImportString);
                    String[] classImports = classImportString.split(",");
                    Set<String> packageImports = new TreeSet<String>();
                    for (String classImport :classImports) {
                        packageImports.add(PackageUtils.getPackageFromClass(classImport.trim()));
                    }
                    addAllPackageImports(packageImports);
                } else {
                    addPackageImport(PackageUtils.getPackageFromClass(classImportString));
                }
            }
        }
        Matcher taglibUriMatcher = JSP_TAGLIB_PATTERN.matcher(jspFileContent);
        while (taglibUriMatcher.find()) {
            String taglibUri = taglibUriMatcher.group(1);
            taglibUris.add(taglibUri);
            if (!taglibPackages.containsKey(taglibUri)) {
                getLog().warn("JSP " + fileName + " has a reference to taglib " + taglibUri + " that is not in the project's dependencies !");
            } else {
                if (!externalDependency) {
                    Set<String> taglibPackageSet = taglibPackages.get(taglibUri);
                    boolean externalTagLib = externalTaglibs.get(taglibUri);
                    if (externalTagLib) {
                        addAllPackageImports(taglibPackageSet);
                    }
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
        if (!packageName.startsWith("java.")) {
            packageImports.add(packageName);
        }
    }

    private void addAllPackageImports(Collection<String> packageNames) {
        for (String packageName : packageNames) {
            addPackageImport(packageName);
        }
    }

    private void dumpElementNamespaces(Element element) {
        Namespace mainNamespace = element.getNamespace();
        getLog().debug("Main namespace prefix=[" + mainNamespace.getPrefix() + "] uri=[" + mainNamespace.getURI() + "] getNamespaceURI=[" + element.getNamespaceURI() + "]");
        for (Namespace additionalNamespace : (List<Namespace>) element.getAdditionalNamespaces()) {
            getLog().debug("Additional namespace prefix=" + additionalNamespace.getPrefix() + " uri=" + additionalNamespace.getURI());
        }
    }

}
