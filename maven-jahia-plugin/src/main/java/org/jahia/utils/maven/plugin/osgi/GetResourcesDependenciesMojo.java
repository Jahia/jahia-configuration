package org.jahia.utils.maven.plugin.osgi;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
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
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A maven goal to scan the project resources for package dependencies, useful for building OSGi Import-Package
 * Manifest header.
 *
 * This goal is currently capable of scanning:
 * - TLD files in dependencies and the project
 * - JSP for page import and Taglib references (tag files are not supported yet)
 * - Drools rule definition imports
 * - JBPM Workflow definition files
 *
 * @goal get-resource-dependencies
 * @requiresDependencyResolution test
 *
 * @todo add support for CND definition files, groovy files, JSP tag files
 */
public class GetResourcesDependenciesMojo extends AbstractMojo {

    public static final Pattern JSP_PAGE_IMPORT_PATTERN = Pattern.compile("<%@.*page.*import=\\\"(.*?)\\\".*%>");
    public static final Pattern JSP_TAGLIB_PATTERN = Pattern.compile("<%@.*taglib.*uri=\\\"(.*?)\\\".*%>");
    public static final Pattern CND_NODETYPE_PATTERN = Pattern.compile("\\[([\\w:]+)\\]\\s*(?:>\\s*([\\w:]+)(?:\\s*,\\s*([\\w:]+))*)?");
    public static final Pattern RULE_IMPORT_PATTERN = Pattern.compile("^\\s*import\\s*([\\w.\\*]*)\\s*$");

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

    private Set<String> packageImports = new TreeSet<String>();
    private Set<String> taglibUris = new TreeSet<String>();
    private Map<String, Set<String>> taglibPackages = new HashMap<String, Set<String>>();
    private Map<String, Boolean> externalTaglibs = new HashMap<String, Boolean>();
    private Set<String> contentTypeNames = new TreeSet<String>();
    private Set<String> contentSuperTypeNames = new TreeSet<String>();
    private List<Pattern> artifactExclusionPatterns = new ArrayList<Pattern>();
    private Set<String> projectPackages = new TreeSet<String>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        buildExclusionPatterns();

        try {
            scanClassesBuildDirectory();

            scanDependencies();
        } catch (IOException e) {
            throw new MojoFailureException("Error while scanning dependencies", e);
        }

        List<Resource> resources = project.getResources();

        for (Resource resource : resources) {
            File resourceDirectory = new File(resource.getDirectory());
            if (!resourceDirectory.exists()) {
                getLog().warn("Couldn't find directory " + resourceDirectory + ", skipping !");
                continue;
            }
            try {
                getLog().info("Scanning resource directory " + resourceDirectory + "...");
                processDirectoryTlds(resourceDirectory);
                processDirectory(resourceDirectory);
            } catch (IOException e) {
                throw new MojoFailureException("Error processing resource directory " + resourceDirectory, e);
            }
        }
        getLog().info("Found project packages (potential exports) :");
        for (String projectPackage : projectPackages) {
            getLog().info("  " + projectPackage);
        }

        // now let's remove all the project packages from the imports, we assume we will not import split packages.
        packageImports.removeAll(projectPackages);

        StringBuffer generatedPackageBuffer = new StringBuffer();
        for (String packageImport : packageImports) {
            generatedPackageBuffer.append(packageImport);
            generatedPackageBuffer.append(",\n");
        }
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
        getLog().info("Found content type names:");
        for (String contentTypeName : contentTypeNames) {
            getLog().info("  " + contentTypeName);
        }
        getLog().info("Found content supertype names:");
        for (String contentSuperTypeName : contentSuperTypeNames) {
            getLog().info("  " + contentSuperTypeName);
        }
        String generatedPackageList = generatedPackageBuffer.toString();
        generatedPackageList = generatedPackageList.substring(0, generatedPackageList.length() - ",\n".length()); // remove the last comma
        project.getProperties().put("jahia.plugin.projectPackageImport", generatedPackageList);
        getLog().info("Set project property jahia.plugin.projectPackageImport to package import list value: ");
        getLog().info(generatedPackageList);
    }

    private void scanClassesBuildDirectory() throws IOException {
        File outputDirectoryFile = new File(project.getBuild().getOutputDirectory());
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
            }

            if (artifact.getScope().contains(Artifact.SCOPE_PROVIDED) ||
                    artifact.getScope().contains(Artifact.SCOPE_COMPILE) ||
                    artifact.getScope().contains(Artifact.SCOPE_RUNTIME)) {
                if (!artifact.getType().equals("jar")) {
                    getLog().warn("Ignoring artifact " + artifact.getFile() + " since it is of type " + artifact.getType());
                    continue;
                }
                boolean externalDependency = false;
                if (artifact.getScope().contains(Artifact.SCOPE_PROVIDED)) {
                    externalDependency = true;
                }
                getLog().debug("Scanning "+(externalDependency ? "external" : "")+" dependency " + artifact.getFile());
                scanJar(artifact.getFile(), artifact.getBaseVersion(), externalDependency);
            }
        }

    }

    private void scanJar(File jarFile, String defaultVersion, boolean externalDependency) throws IOException {
        JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile));
        JarEntry jarEntry = null;
        // getLog().debug("Processing file " + artifact.getFile() + "...");
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            if (!jarEntry.isDirectory()) {
                String entryName = jarEntry.getName();
                String entryPackage = "";
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
                            projectPackages.add(entryPackage);
                        }
                    }
                    ByteArrayOutputStream entryOutputStream = new ByteArrayOutputStream();
                    IOUtils.copy(jarInputStream, entryOutputStream);
                    ByteArrayInputStream tempEntryInputStream = new ByteArrayInputStream(entryOutputStream.toByteArray());
                    processTldFile(jarEntry.getName(), tempEntryInputStream, entryPackage, externalDependency);
                    processNonTldFile(jarEntry.getName(), tempEntryInputStream, entryPackage, externalDependency);
                }
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
                FileInputStream fileInputStream = null;
                try {
                    fileInputStream = new FileInputStream(includedFileFile);
                    processTldFile(includedFile, fileInputStream, entryPackage, false);
                } finally {
                    IOUtils.closeQuietly(fileInputStream);
                }
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
                FileInputStream fileInputStream = null;
                try {
                    fileInputStream = new FileInputStream(includedFileFile);
                    processNonTldFile(includedFile, fileInputStream, entryPackage, false);
                } finally {
                    IOUtils.closeQuietly(fileInputStream);
                }
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
        } else if (fileName.toLowerCase().endsWith(".jpdl.xml")) {
            if (!externalDependency) {
                processJpdl(fileName, inputStream, externalDependency);
            }
        }
    }

    private void processJpdl(String fileName, InputStream inputStream, boolean externalDependency) throws IOException {
        getLog().debug("Processing workflow definition file (JBPM JPDL) " + fileName + "...");
        SAXBuilder saxBuilder = new SAXBuilder();
        FileInputStream childFileInputStream = null;
        try {
            InputStreamReader fileReader = new InputStreamReader(inputStream);
            org.jdom.Document jdomDocument = saxBuilder.build(fileReader);
            Element root = jdomDocument.getRootElement();

            List<Attribute> classAttributes = getAttributes(root, "//@class");
            for (Attribute classAttribute : classAttributes) {
                getLog().debug(fileName + " Found class " + classAttribute.getValue() + " package=" + getPackageFromClass(classAttribute.getValue()));
                if (!externalDependency) {
                    addPackageImport(getPackageFromClass(classAttribute.getValue()));
                }
            }
        } catch (JDOMException e) {
            getLog().warn("Error parsing TLD file " + fileName, e);
        } finally {
            IOUtils.closeQuietly(childFileInputStream);
        }
    }

    private void processDrl(String fileName, InputStream inputStream, boolean externalDependency) throws IOException {
        getLog().debug("Processing Drools Rule file " + fileName + "...");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        while ((line=bufferedReader.readLine()) != null) {
            Matcher ruleImportMatcher = RULE_IMPORT_PATTERN.matcher(line);
            if (ruleImportMatcher.matches()) {
                String ruleImport = ruleImportMatcher.group(1);
                getLog().debug(fileName + ": found rule import " + ruleImport) ;
                if (!externalDependency) {
                    addPackageImport(getPackageFromClass(ruleImport));
                }
            }
        }
    }

    private void processCnd(String fileName, InputStream inputStream, boolean externalDependency) throws IOException {
        /*
        getLog().debug("Processing CND " + fileName + "...");
        String cndFileContent = IOUtils.toString(inputStream);
        Matcher cndNodeTypeDefinitionMatcher = CND_NODETYPE_PATTERN.matcher(cndFileContent);
        while (cndNodeTypeDefinitionMatcher.find()) {
            getLog().debug(fileName + ": found node type " + cndNodeTypeDefinitionMatcher.group(1) + " groupCount=" + cndNodeTypeDefinitionMatcher.groupCount()) ;
            contentTypeNames.add(cndNodeTypeDefinitionMatcher.group(1));
            if (cndNodeTypeDefinitionMatcher.groupCount() > 1) {
                for (int i=2; i < cndNodeTypeDefinitionMatcher.groupCount(); i++) {
                    if (cndNodeTypeDefinitionMatcher.group(i) != null) {
                        getLog().debug(fileName + ":   with super type " + cndNodeTypeDefinitionMatcher.group(i)) ;
                        contentSuperTypeNames.add(cndNodeTypeDefinitionMatcher.group(i));
                    }
                }
            }
        }
        */
    }

    private void processTld(String fileName, InputStream inputStream, boolean externalDependency) throws IOException {
        getLog().debug("Processing TLD " + fileName + "...");
        SAXBuilder saxBuilder = new SAXBuilder();
        FileInputStream childFileInputStream = null;
        try {
            InputStreamReader fileReader = new InputStreamReader(inputStream);
            org.jdom.Document jdomDocument = saxBuilder.build(fileReader);
            Element root = jdomDocument.getRootElement();

            Element uriElement = getElement(root, "/xp:taglib/xp:uri");
            String uri = uriElement.getTextTrim();
            getLog().debug("Taglib URI="+uri);
            Set<String> taglibPackageSet = taglibPackages.get(uri);
            if (taglibPackageSet == null) {
                taglibPackageSet = new TreeSet<String>();
            }

            List<Element> tagClassElements = getElements(root, "//xp:tag/xp:tag-class");
            for (Element tagClassElement : tagClassElements) {
                getLog().debug(fileName + " Found tag class " + tagClassElement.getTextTrim() + " package=" + getPackageFromClass(tagClassElement.getTextTrim()));
                taglibPackageSet.add(getPackageFromClass(tagClassElement.getTextTrim()));
            }
            List<Element> functionClassElements = getElements(root, "//xp:function/xp:function-class");
            for (Element functionClassElement : functionClassElements) {
                getLog().debug(fileName + " Found function class " + functionClassElement.getTextTrim() + " package=" + getPackageFromClass(functionClassElement.getTextTrim()));
                taglibPackageSet.add(getPackageFromClass(functionClassElement.getTextTrim()));
            }
            taglibPackages.put(uri, taglibPackageSet);
            externalTaglibs.put(uri, externalDependency);
        } catch (JDOMException e) {
            getLog().warn("Error parsing TLD file " + fileName, e);
        } finally {
            IOUtils.closeQuietly(childFileInputStream);
        }
    }

    private String getPackageFromClass(String className) {
        int lastDot = className.lastIndexOf(".");
        if (lastDot < 0) {
            return null;
        }
        return className.substring(0, lastDot);
    }

    private void processJsp(String fileName, InputStream inputStream, boolean externalDependency) throws IOException {
        getLog().debug("Processing JSP " + fileName + "...");
        String jspFileContent = IOUtils.toString(inputStream);
        Matcher pageImportMatcher = JSP_PAGE_IMPORT_PATTERN.matcher(jspFileContent);
        while (pageImportMatcher.find()) {
            if (!externalDependency) {
                String classImport = pageImportMatcher.group(1);
                int lastDot = classImport.lastIndexOf(".");
                if (lastDot > 0) {
                    String packageName = classImport.substring(0, lastDot);
                    addPackageImport(packageName);
                }
            }
        }
        Matcher taglibUriMatcher = JSP_TAGLIB_PATTERN.matcher(jspFileContent);
        while (taglibUriMatcher.find()) {
            String taglibUri = taglibUriMatcher.group(1);
            taglibUris.add(taglibUri);
            if (!taglibPackages.containsKey(taglibUri)) {
                getLog().warn("JSP " + fileName + " has a reference to taglib "+taglibUri+" that is not in the project's dependencies !");
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
     * @param scopeElement the scope in which to execute the XPath query
     * @param xPathExpression the XPath query to select the element we wish to retrieve. In the case where multiple
     * elements match, only the first one will be returned.
     * @return the first element that matches the XPath expression, or null if no element matches.
     * @throws JDOMException raised if there was a problem navigating the JDOM structure.
     */
    public Element getElement(Element scopeElement, String xPathExpression) throws JDOMException {
        XPath xPath = XPath.newInstance(xPathExpression);
        String namespaceURI = scopeElement.getDocument().getRootElement().getNamespaceURI();
        if ((namespaceURI != null) && (!"".equals(namespaceURI))) {
            xPath.addNamespace("xp", namespaceURI);
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
        for (Object obj : xPath.selectNodes(scopeElement)) {
            if (obj instanceof Attribute) {
                elems.add((Attribute) obj);
            }
        }

        return elems;
    }

    private void addPackageImport(String packageName) {
        if (!packageName.startsWith("java.")) {
            packageImports.add(packageName);
        }
    }

    private void addAllPackageImports(Collection<String> packageNames) {
        for (String packageName : packageNames) {
            addPackageImport(packageName);
        }
    }

}
