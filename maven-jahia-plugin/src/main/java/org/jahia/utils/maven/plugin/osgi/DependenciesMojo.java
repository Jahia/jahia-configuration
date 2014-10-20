package org.jahia.utils.maven.plugin.osgi;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jahia.utils.maven.plugin.SLF4JLoggerToMojoLogBridge;
import org.jahia.utils.osgi.PropertyFileUtils;
import org.jahia.utils.osgi.parsers.Parsers;
import org.jahia.utils.osgi.parsers.ParsingContext;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A maven goal to scan the project for package dependencies, useful for building OSGi Import-Package
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

    private static final Set<String> SUPPORTED_FILE_EXTENSIONS_TO_SCAN = new HashSet<String>(Arrays.asList("jsp",
            "jspf", "tag", "tagf", "cnd", "drl", "xml", "groovy"));
    
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
     * @parameter
     */
    protected List<String> scanDirectories = new ArrayList<String>();

    /**
     * @parameter
     */
    protected List<String> excludeFromDirectoryScan = new ArrayList<String>();

    /**
     * @parameter
     */
    protected List<String> excludedJarEntries;

    protected List<Pattern> excludedJarEntryPatterns;

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

    private List<Pattern> artifactExclusionPatterns = new ArrayList<Pattern>();
    private Set<String> artifactsToSkip = new TreeSet<String>();
    private Logger logger = new SLF4JLoggerToMojoLogBridge(getLog());

    @Override
    public void setLog(Log log) {
        super.setLog(log);
        logger = new SLF4JLoggerToMojoLogBridge(log);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (project.getGroupId().equals("org.jahia.modules") && project.getArtifactId().equals("jahia-modules")
                || !"jar".equals(project.getPackaging()) && !"bundle".equals(project.getPackaging()) && !"war".equals(project.getPackaging())) {
            return;
        }
        if (excludeFromDirectoryScan == null || excludeFromDirectoryScan.size() == 0) {
            excludeFromDirectoryScan.add("**/legacyMappings/*");
        }
        long startTime = System.currentTimeMillis();
        ParsingContext parsingContext = new ParsingContext();

        if (existingImports != null) {
            parsingContext.addAllPackageImports(Arrays.asList(StringUtils.split(existingImports, ", \n\r")));
        }

        buildExclusionPatterns(parsingContext);
        
        readArtifactsToSkip(parsingContext);
        
        long timer = System.currentTimeMillis();
        
        try {
            scanClassesBuildDirectory(parsingContext);
            
            getLog().info(
                    "Scanned classes directory in " + (System.currentTimeMillis() - timer) + " ms. Found "
                            + parsingContext.getProjectPackages().size() + " project packages.");

            timer = System.currentTimeMillis();
            
            int scanned = scanDependencies(parsingContext);
            
            getLog().info(
                    "Scanned " + scanned + " project dependencies in " + (System.currentTimeMillis() - timer)
                            + " ms. Currently we have " + parsingContext.getProjectPackages().size() + " project packages.");
        } catch (IOException e) {
            throw new MojoFailureException("Error while scanning dependencies", e);
        }
        if (scanDirectories.isEmpty()) {
            scanDirectories.add(project.getBasedir() + "/src/main/resources");
            scanDirectories.add(project.getBasedir() + "/src/main/import");
            scanDirectories.add(project.getBasedir() + "/src/main/webapp");
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
                processDirectoryTlds(scanDirectoryFile, parsingContext);
                processDirectory(scanDirectoryFile, parsingContext);
            } catch (IOException e) {
                throw new MojoFailureException("Error processing resource directory " + scanDirectoryFile, e);
            }
        }
        getLog().info(
                "Scanned resource directories in " + (System.currentTimeMillis() - timer)
                        + " ms. Currently we have " + parsingContext.getProjectPackages().size() + " project packages.");
        if (getLog().isDebugEnabled()) {
            getLog().debug("Found project packages (potential exports) :");
            for (String projectPackage : parsingContext.getProjectPackages()) {
                getLog().debug("  " + projectPackage);
            }
        }

        parsingContext.postProcess();

        if (parsingContext.getUnresolvedTaglibUris().size() > 0 ) {
            for (Map.Entry<String,Set<String>> unresolvedUrisForJsp : parsingContext.getUnresolvedTaglibUris().entrySet()) {
                for (String unresolvedUriForJsp : unresolvedUrisForJsp.getValue()) {
                    getLogger().warn("JSP " + unresolvedUrisForJsp.getKey() + " has a reference to taglib " + unresolvedUriForJsp + " that is not in the project's dependencies !");
                }
            }
        }

        StringBuilder generatedPackageBuffer = new StringBuilder(256);
        int i = 0;
        Map<String, String> importOverrides = getPackageImportOverrides();
        if (importOverrides != null) {
            getLog().info(
                    "Considering provided Import-Package-Override: " + StringUtils.join(importOverrides.values(), ", "));
        }
        for (String packageImport : parsingContext.getPackageImports()) {
            if (importOverrides != null && importOverrides.containsKey(packageImport)) {
                generatedPackageBuffer.append(importOverrides.get(packageImport));
            } else {
                generatedPackageBuffer.append(packageImport);
            }
            if (i < parsingContext.getPackageImports().size() - 1) {
                generatedPackageBuffer.append(",\n");
            }
            i++;
        }
        getLog().info("Generated " + parsingContext.getPackageImports().size() + " package imports for project.");
        getLog().info("Found referenced tag library URIs (from JSPs) :");
        for (String taglibUri : parsingContext.getTaglibUris()) {
            boolean foundInDependencies = parsingContext.getTaglibPackages().containsKey(taglibUri);
            String foundMessage = "";
            if (!foundInDependencies) {
                foundMessage = "NOT FOUND";
            }
            if (foundInDependencies) {
                boolean externalTagLib = parsingContext.getExternalTaglibs().get(taglibUri);
                if (externalTagLib) {
                    foundMessage += " provided";
                }
            }
            getLog().info("  " + taglibUri + " " + foundMessage);
        }

        StringBuilder contentTypeDefinitionsBuffer = new StringBuilder(256);
        if (parsingContext.getContentTypeDefinitions().size() > 0) {
            contentTypeDefinitionsBuffer.append("com.jahia.services.content; nodetypes:List<String>=\"");
            i = 0;
            for (String contentTypeName : parsingContext.getContentTypeDefinitions()) {
                contentTypeDefinitionsBuffer.append(contentTypeName);
                if (i < parsingContext.getContentTypeDefinitions().size() - 1) {
                    contentTypeDefinitionsBuffer.append(",");
                }
                i++;
            }
            contentTypeDefinitionsBuffer.append("\"");
        }
        if (contentDefinitionCapabilitiesActivated) {
            getLog().info("Found " + parsingContext.getContentTypeDefinitions().size() + " new content node type definitions in project.");
            getLog().debug("Provide-Capability: " + contentTypeDefinitionsBuffer.toString());
            project.getProperties().put("jahia.plugin.providedNodeTypes", contentTypeDefinitionsBuffer.toString());
        } else {
            // we set an empty property so that Maven will not fail the build with a non-existing property
            project.getProperties().put("jahia.plugin.providedNodeTypes", "");
        }

        StringBuffer contentTypeReferencesBuffer = new StringBuffer();
        if (parsingContext.getContentTypeReferences().size() > 0) {
            i = 0;
            for (String contentTypeReference : parsingContext.getContentTypeReferences()) {
                contentTypeReferencesBuffer.append("com.jahia.services.content; filter:=\"(nodetypes=" + contentTypeReference + ")\"");
                if (i < parsingContext.getContentTypeReferences().size() - 1) {
                    contentTypeReferencesBuffer.append(",");
                }
                i++;
            }
        }

        if (contentDefinitionCapabilitiesActivated) {
            getLog().info("Found " + parsingContext.getContentTypeReferences().size() + " content node type definitions referenced in project.");
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
                        new SLF4JLoggerToMojoLogBridge(getLog()));
            } catch (IOException e) {
                getLog().warn("Error saving extra system capabilities to file " + propertiesOutputFile);
            }
        }
        getLog().info("Took " + (System.currentTimeMillis() - startTime) + " ms for the dependencies analysis");
    }

    private Map<String, String> getPackageImportOverrides() {
        Map<String, String> overrides = null;
        String importPackageOverride = null;
        try {
            importPackageOverride = ((Xpp3Dom) project.getPlugin("org.apache.felix:maven-bundle-plugin")
                    .getConfiguration()).getChild("instructions").getChild("Import-Package-Override").getValue();
        } catch (Exception e) {
            // no overrides
        }
        if (StringUtils.isNotEmpty(importPackageOverride)) {
            overrides = new HashMap<String, String>();
            for (String token : StringUtils.split(importPackageOverride, ",\n\r")) {
                token = token.trim();
                if (token.length() > 0) {
                    overrides.put(token.contains(";") ? StringUtils.substringBefore(token, ";").trim() : token, token);
                }
            }
        }
        return overrides != null && !overrides.isEmpty() ? overrides : null;
    }

    private void readArtifactsToSkip(ParsingContext parsingContext) {
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

    private void processProjectPackageEntry(String entry, String fileSeparator, ParsingContext parsingContext) {
        int lastSlash = entry.lastIndexOf(fileSeparator);
        if (lastSlash == -1) {
            return;
        }
        String entryPackage = StringUtils.replace(entry.substring(0, lastSlash), fileSeparator, ".");
        if (StringUtils.isNotEmpty(entryPackage) && !parsingContext.getProjectPackages().contains(entryPackage)
                && !entryPackage.startsWith("META-INF") && !entryPackage.startsWith("OSGI-INF")
                && !entryPackage.startsWith("OSGI-OPT") && !entryPackage.startsWith("WEB-INF")
                && !entryPackage.startsWith("org.osgi")) {
            parsingContext.getProjectPackages().add(entryPackage);
        }
    }
    

    private void scanClassesBuildDirectory(ParsingContext parsingContext) throws IOException {
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
            processProjectPackageEntry(includedFile, File.separator, parsingContext);
        }
    }

    private void buildExclusionPatterns(ParsingContext parsingContext) {
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
        
        if (excludedJarEntries != null && excludedJarEntries.size() > 0) {
            excludedJarEntryPatterns = new LinkedList<Pattern>();
            for (String p : excludedJarEntries) {
                excludedJarEntryPatterns.add(Pattern.compile(p.trim()));
            }
        }
    }

    private int scanDependencies(ParsingContext parsingContext) throws IOException {
        getLog().info("Scanning project dependencies...");
        int scanned = 0;
        for (Artifact artifact : project.getArtifacts()) {
            if (!DEPENDENCIES_SCAN_PACKAGING.contains(artifact.getType())
                    || !DEPENDENCIES_SCAN_SCOPES.contains(artifact.getScope()) || isExcludedFromScan(artifact)) {
                continue;
            }

            boolean externalDependency = Artifact.SCOPE_PROVIDED.equals(artifact.getScope());

            long timer = System.currentTimeMillis();

            int scannedInJar = scanJar(artifact.getFile(), externalDependency,
                    "war".equals(artifact.getType()) ? "WEB-INF/classes/" : "", parsingContext);

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

    private int scanJar(File jarFile, boolean externalDependency, String packageDirectory, ParsingContext parsingContext) throws IOException {
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
                    processProjectPackageEntry(entryName, "/", parsingContext);
                }

                if (!"tld".equals(ext) && (!SUPPORTED_FILE_EXTENSIONS_TO_SCAN.contains(ext) || externalDependency)
                        || entryName.endsWith("/pom.xml")) {
                    continue;
                }
                
                if (excludeJarEntry(entryName)) {
                    continue;
                }
                ByteArrayOutputStream entryOutputStream = new ByteArrayOutputStream();
                IOUtils.copy(jarInputStream, entryOutputStream);
                if ("tld".equals(ext)) {
                    getLog().debug("\tscanning entry: " + jarEntry.getName());
                    Parsers.getInstance().parse(0, jarEntry.getName(), new ByteArrayInputStream(entryOutputStream.toByteArray()), parsingContext,
                            externalDependency, getLogger());
                    scanned++;
                }
                if (!externalDependency && SUPPORTED_FILE_EXTENSIONS_TO_SCAN.contains(ext)) {
                    getLog().debug("\tscanning entry: " + jarEntry.getName());
                    if (processNonTldFile(jarEntry.getName(), new ByteArrayInputStream(entryOutputStream.toByteArray()), parsingContext)) {
                        scanned++;
                    }
                }
            }
        } finally {
            jarInputStream.close();
        }

        if (parsingContext.getAdditionalFilesToParse().size() > 0) {
            getLog().debug("Processing additional files to parse...");
            JarFile jar = new JarFile(jarFile);
            for (String fileToParse : parsingContext.getAdditionalFilesToParse()) {
                JarEntry jarEntry = jar.getJarEntry(fileToParse);
                if (jarEntry != null) {
                    InputStream jarEntryInputStream = jar.getInputStream(jarEntry);
                    ByteArrayOutputStream entryOutputStream = new ByteArrayOutputStream();
                    IOUtils.copy(jarEntryInputStream, entryOutputStream);
                    if (processNonTldFile(jarEntry.getName(), new ByteArrayInputStream(entryOutputStream.toByteArray()), parsingContext)) {
                        scanned++;
                    }
                    IOUtils.closeQuietly(jarEntryInputStream);
                } else {
                    getLog().warn("Couldn't find additional file to parse " + fileToParse + " in JAR " + jarFile);
                }
            }
            parsingContext.getAdditionalFilesToParse().clear();
        }
        
        return scanned;
    }

    private boolean excludeJarEntry(String entryName) {
        if (excludedJarEntryPatterns != null) {
            for (Pattern p : excludedJarEntryPatterns) {
                if (p.matcher(entryName).matches()) {
                    getLog().info("Matched JAR entry exclusion pattern for entry " + entryName);
                    return true;
                }
            }
        }
        return false;
    }

    private void processDirectoryTlds(File directoryFile, ParsingContext parsingContext) throws IOException {
        DirectoryScanner ds = new DirectoryScanner();
        String[] excludes = excludeFromDirectoryScan.toArray(new String[excludeFromDirectoryScan.size()]);
        ds.setExcludes(excludes);
        ds.setIncludes(new String[]{"**/*.tld"});
        ds.setBasedir(directoryFile);
        ds.setCaseSensitive(true);
        ds.scan();
        String[] includedFiles = ds.getIncludedFiles();
        for (String includedFile : includedFiles) {
            Parsers.getInstance().parse(0, includedFile, new BufferedInputStream(new FileInputStream(new File(directoryFile, includedFile))), parsingContext, false, getLogger());
        }
    }

    private void processDirectory(File directoryFile, ParsingContext parsingContext) throws IOException {
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
                processNonTldFile(includedFile, fileInputStream, parsingContext);
            } finally {
                IOUtils.closeQuietly(fileInputStream);
            }
        }
    }

    private boolean processNonTldFile(String fileName, InputStream inputStream, ParsingContext parsingContext) throws IOException {
        String ext = FileUtils.getExtension(fileName).toLowerCase();

        if (!SUPPORTED_FILE_EXTENSIONS_TO_SCAN.contains(ext)) {
            return false;
        }

        return Parsers.getInstance().parse(1, fileName, inputStream, parsingContext, false, getLogger());

    }

    private Logger getLogger() {
        return logger;
    }

}
