package org.jahia.utils.maven.plugin.osgi;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jahia.utils.maven.plugin.AetherAwareMojo;
import org.jahia.utils.maven.plugin.SLF4JLoggerToMojoLogBridge;
import org.jahia.utils.maven.plugin.support.ArtifactProcessor;
import org.jahia.utils.maven.plugin.support.MavenAetherHelperUtils;
import org.jahia.utils.osgi.BundleUtils;
import org.jahia.utils.osgi.ManifestValueClause;
import org.jahia.utils.osgi.PropertyFileUtils;
import org.jahia.utils.osgi.parsers.PackageInfo;
import org.jahia.utils.osgi.parsers.Parsers;
import org.jahia.utils.osgi.parsers.ParsingContext;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;
import java.util.jar.*;
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
 * @requiresDependencyCollection test
 * @todo add support for JSP tag files, more...
 */
public class DependenciesMojo extends AetherAwareMojo {

    protected static final Set<String> SUPPORTED_FILE_EXTENSIONS_TO_SCAN = new HashSet<String>(Arrays.asList("jsp",
            "jspf", "tag", "tagf", "cnd", "drl", "xml", "groovy"));
    
    protected static final Set<String> DEPENDENCIES_SCAN_PACKAGING = new HashSet<String>(Arrays.asList("jar", "war"));

    protected static final Set<String> DEPENDENCIES_SCAN_SCOPES = new HashSet<String>(Arrays.asList(
            Artifact.SCOPE_PROVIDED, Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME));

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

    /**
     * @parameter expression="${user.home}/.m2/dependency-cache";
     */
    protected String dependencyParsingCacheDirectory = null;

    protected List<Pattern> artifactExclusionPatterns = new ArrayList<Pattern>();
    protected Logger logger = new SLF4JLoggerToMojoLogBridge(getLog());
    protected ParsingContextCache parsingContextCache;

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
        ParsingContext projectParsingContext = new ParsingContext(MavenAetherHelperUtils.getCoords(project.getArtifact()), 0, 0, project.getArtifactId(), project.getBasedir().getPath(), project.getVersion(), null);

        parsingContextCache = new ParsingContextCache(new File(dependencyParsingCacheDirectory), null);

        if (existingImports != null) {
            String[] existingImportsArray = StringUtils.split(existingImports, ", \n\r");
            for (String existingImport : existingImportsArray) {
                projectParsingContext.addPackageImport(new PackageInfo(existingImport, null, false, "Maven plugin configuration", projectParsingContext));
            }
        }

        buildExclusionPatterns();

        long timer = System.currentTimeMillis();
        
        try {
            scanClassesBuildDirectory(projectParsingContext);
            
            getLog().info(
                    "Scanned classes directory in " + (System.currentTimeMillis() - timer) + " ms. Found "
                            + projectParsingContext.getLocalPackages().size() + " project packages.");

            timer = System.currentTimeMillis();
            
            int scanned = scanDependencies(projectParsingContext);
            
            getLog().info(
                    "Scanned " + scanned + " project dependencies in " + (System.currentTimeMillis() - timer)
                            + " ms. Currently we have " + projectParsingContext.getLocalPackages().size() + " project packages.");
        } catch (IOException e) {
            throw new MojoFailureException("Error while scanning dependencies", e);
        } catch (DependencyResolutionRequiredException e) {
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
                processDirectoryTlds(scanDirectoryFile, project.getVersion(), projectParsingContext);
                processDirectory(scanDirectoryFile, false, project.getVersion(), projectParsingContext);
            } catch (IOException e) {
                throw new MojoFailureException("Error processing resource directory " + scanDirectoryFile, e);
            }
        }
        getLog().info(
                "Scanned resource directories in " + (System.currentTimeMillis() - timer)
                        + " ms. Currently we have " + projectParsingContext.getLocalPackages().size() + " project packages.");
        if (getLog().isDebugEnabled()) {
            getLog().debug("Found project packages (potential exports) :");
            for (PackageInfo projectPackage : projectParsingContext.getLocalPackages()) {
                getLog().debug("  " + projectPackage);
            }
        }

        projectParsingContext.postProcess();

        if (projectParsingContext.getUnresolvedTaglibUris().size() > 0 ) {
            for (Map.Entry<String,Set<String>> unresolvedUrisForJsp : projectParsingContext.getUnresolvedTaglibUris().entrySet()) {
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
        Set<String> uniquePackageImports = new TreeSet<String>();
        for (PackageInfo packageImport : projectParsingContext.getPackageImports()) {
            String packageImportName = null;
            if (importOverrides != null && importOverrides.containsKey(packageImport.getName())) {
                packageImportName = importOverrides.get(packageImport.getName());
            } else {
                packageImportName = packageImport.toString(false);
            }
            if (uniquePackageImports.contains(packageImport.getName())) {
                continue;
            }
            generatedPackageBuffer.append(packageImportName);
            if (i < projectParsingContext.getPackageImports().size() - 1) {
                generatedPackageBuffer.append(",\n");
            }
            uniquePackageImports.add(packageImport.getName());
            i++;
        }
        getLog().info("Generated " + projectParsingContext.getPackageImports().size() + " package imports for project.");
        getLog().info("Found referenced tag library URIs (from JSPs) :");
        for (String taglibUri : projectParsingContext.getTaglibUris()) {
            boolean foundInDependencies = projectParsingContext.getTaglibPackages().containsKey(taglibUri);
            String foundMessage = "";
            if (!foundInDependencies) {
                foundMessage = "NOT FOUND";
            }
            if (foundInDependencies) {
                boolean externalTagLib = projectParsingContext.getExternalTaglibs().get(taglibUri);
                if (externalTagLib) {
                    foundMessage += " provided";
                }
            }
            getLog().info("  " + taglibUri + " " + foundMessage);
        }

        StringBuilder contentTypeDefinitionsBuffer = new StringBuilder(256);
        if (projectParsingContext.getContentTypeDefinitions().size() > 0) {
            contentTypeDefinitionsBuffer.append("com.jahia.services.content; nodetypes:List<String>=\"");
            i = 0;
            for (String contentTypeName : projectParsingContext.getContentTypeDefinitions()) {
                contentTypeDefinitionsBuffer.append(contentTypeName);
                if (i < projectParsingContext.getContentTypeDefinitions().size() - 1) {
                    contentTypeDefinitionsBuffer.append(",");
                }
                i++;
            }
            contentTypeDefinitionsBuffer.append("\"");
        }
        if (contentDefinitionCapabilitiesActivated) {
            getLog().info("Found " + projectParsingContext.getContentTypeDefinitions().size() + " new content node type definitions in project.");
            getLog().debug("Provide-Capability: " + contentTypeDefinitionsBuffer.toString());
            project.getProperties().put("jahia.plugin.providedNodeTypes", contentTypeDefinitionsBuffer.toString());
        } else {
            // we set an empty property so that Maven will not fail the build with a non-existing property
            project.getProperties().put("jahia.plugin.providedNodeTypes", "");
        }

        StringBuilder contentTypeReferencesBuffer = new StringBuilder();
        if (projectParsingContext.getContentTypeReferences().size() > 0) {
            i = 0;
            for (String contentTypeReference : projectParsingContext.getContentTypeReferences()) {
                contentTypeReferencesBuffer.append("com.jahia.services.content; filter:=\"(nodetypes=").append(contentTypeReference).append(")\"");
                if (i < projectParsingContext.getContentTypeReferences().size() - 1) {
                    contentTypeReferencesBuffer.append(",");
                }
                i++;
            }
        }

        if (contentDefinitionCapabilitiesActivated) {
            getLog().info("Found " + projectParsingContext.getContentTypeReferences().size() + " content node type definitions referenced in project.");
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

    protected void scanClassesBuildDirectory(ParsingContext parsingContext) throws IOException, DependencyResolutionRequiredException, MojoExecutionException, MojoFailureException {
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
            processLocalPackageEntry(includedFile, File.separator, outputDirectoryFile.getCanonicalPath(), project.getVersion(), false, parsingContext);
        }
    }

    protected void buildExclusionPatterns() {
        if (artifactExcludes != null) {
            for (String artifactExclude : artifactExcludes) {
                int colonPos = artifactExclude.indexOf(":");
                String groupPattern = ".*";
                String artifactPattern;
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
                            + Arrays.toString(artifactExclusionPatterns.toArray()));
        } else {
            getLog().info("No artifact exclusions specified. Will scan all related dependencies of the project.");
        }

        excludedJarEntryPatterns = new LinkedList<Pattern>();
        excludedJarEntryPatterns.add(Pattern.compile(".*/legacyDefinitions/.*\\.cnd"));
        if (excludedJarEntries != null && excludedJarEntries.size() > 0) {
            for (String p : excludedJarEntries) {
                excludedJarEntryPatterns.add(Pattern.compile(p.trim()));
            }
        }
    }

    protected int scanDependencies(final ParsingContext projectParsingContext) throws IOException, MojoExecutionException {
        getLog().info("Scanning project embedded dependencies...");
        int scanned = 0;

        for (Artifact artifact : project.getDependencyArtifacts()) {
            if (artifact.isOptional()) {
                getLog().info("Scanning optional dependency " + artifact + "...");
            }

            if (!DEPENDENCIES_SCAN_PACKAGING.contains(artifact.getType())
                    || !DEPENDENCIES_SCAN_SCOPES.contains(artifact.getScope()) || isExcludedFromScan(artifact)) {
                continue;
            }

            if (Artifact.SCOPE_PROVIDED.equals(artifact.getScope())) {
                continue;
            }

            final int scannedCopy = scanned;
            final DependenciesMojo dependenciesMojo = this;
            getAetherHelper().processArtifactAndDependencies(artifact, artifact.isOptional(), new ArtifactProcessor() {

                @Override
                public ParsingContext enterArtifact(Artifact artifact, boolean optional, boolean external, ParsingContext parentParsingContext, String logPrefix, int depth) throws MojoExecutionException {
                    try {
                        return dependenciesMojo.startProcessingArtifact(projectParsingContext, scannedCopy, artifact, external, optional, parentParsingContext, logPrefix, depth);
                    } catch (IOException e) {
                        throw new MojoExecutionException("Error processing artifact " + artifact, e);
                    }
                }

                @Override
                public boolean exitArtifact(Artifact artifact, boolean optional, boolean external, String logPrefix, ParsingContext parsingContext, int depth) throws MojoExecutionException {
                    return dependenciesMojo.endProcessingArtifact(projectParsingContext, artifact, external, logPrefix, parsingContext, depth);
                }
            }, artifact.getArtifactHandler(), projectParsingContext);

            scanned++;
        }

        projectParsingContext.removeLocalPackagesFromImports();

        getLog().info("Scanning project external dependencies...");

        for (Artifact artifact : project.getDependencyArtifacts()) {
            if (artifact.isOptional()) {
                getLog().info("Scanning optional dependency " + artifact + "...");
            }

            if (!DEPENDENCIES_SCAN_PACKAGING.contains(artifact.getType())
                    || !DEPENDENCIES_SCAN_SCOPES.contains(artifact.getScope()) || isExcludedFromScan(artifact)) {
                continue;
            }

            if (!Artifact.SCOPE_PROVIDED.equals(artifact.getScope())) {
                continue;
            }

            final int scannedCopy = scanned;
            final DependenciesMojo dependenciesMojo = this;
            getAetherHelper().processArtifactAndDependencies(artifact, artifact.isOptional(), new ArtifactProcessor() {

                @Override
                public ParsingContext enterArtifact(Artifact artifact, boolean optional, boolean external, ParsingContext parentParsingContext, String logPrefix, int depth) throws MojoExecutionException {
                    try {
                        return dependenciesMojo.startProcessingArtifact(projectParsingContext, scannedCopy, artifact, external, optional, parentParsingContext, logPrefix, depth);
                    } catch (IOException e) {
                        throw new MojoExecutionException("Error processing artifact " + artifact, e);
                    }
                }
                @Override
                public boolean exitArtifact(Artifact artifact, boolean optional, boolean external, String logPrefix, ParsingContext parsingContext, int depth) throws MojoExecutionException {
                    return dependenciesMojo.endProcessingArtifact(projectParsingContext, artifact, external, logPrefix, parsingContext, depth);
                }

            }, artifact.getArtifactHandler(), projectParsingContext);


            scanned++;
        }

        return scanned;
    }

    protected ParsingContext startProcessingArtifact(ParsingContext projectParsingContext, int scanned, Artifact artifact, boolean externalDependency, boolean optional, ParsingContext parentParsingContext, String logPrefix, int depth) throws MojoExecutionException, IOException {
        ParsingContext parsingContext = parsingContextCache.get(artifact);
        if (parsingContext == null) {
            parsingContext = new ParsingContext(MavenAetherHelperUtils.getCoords(artifact),
                    artifact.getFile().lastModified(), artifact.getFile().length(), artifact.getFile().getName(), artifact.getFile().getPath(), artifact.getVersion(), parentParsingContext);

            long timer = System.currentTimeMillis();

            int scannedInJar = scanJar(artifact.getFile(), externalDependency,
                    "war".equals(artifact.getType()) ? "WEB-INF/classes/" : "", artifact.getVersion(), artifact.isOptional(), parsingContext, logPrefix);

            long took = System.currentTimeMillis() - timer;
            if (getLog().isInfoEnabled() && (scannedInJar > 0)) {
                getLog().info(logPrefix +
                        "Processed " + scannedInJar + ((scanned == 1) ? " entry" : " entries") + " in "
                                + (externalDependency ? "external" : "") + "dependency " + artifact + " in " + took
                                + " ms");
            }
        }
        if (parentParsingContext != null) {
            parentParsingContext.addChildJarParsingContext(parsingContext);
        }
        if (optional) {
            parsingContext.setOptional(true);
        }
        if (depth == 1 && externalDependency) {
            parsingContext.setExternal(true);
        }
        return parsingContext;
    }

    protected boolean endProcessingArtifact(ParsingContext projectParsingContext, Artifact artifact, boolean externalDependency, String logPrefix, ParsingContext parsingContext, int depth) {
        if (artifact == null) {
            getLog().warn(logPrefix + ": Artifact is null, will not put parsed JAR context "+ parsingContext +" in cache !");
            return false;
        }
        if (!artifact.getFile().getPath().equals(parsingContext.getFilePath())) {
            getLog().warn(logPrefix + ": Artifact file path ("+artifact.getFile().getPath()+") and jarParsingContext file path ("+ parsingContext.getFilePath()+") do not match, will not put parsed JAR context "+ parsingContext +" in cache !");
            return false;
        }
        if (artifact.isOptional() && !externalDependency) {
            projectParsingContext.addAllPackageImports(parsingContext.getLocalPackages(), true);
        }
        parsingContext.postProcess();
        if (!parsingContext.isInCache()) {
            parsingContextCache.put(artifact, parsingContext);
        }
        return true;
    }

    protected boolean isExcludedFromScan(Artifact artifact) {
        String id = StringUtils.substringBeforeLast(artifact.toString(), ":");
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

    private int scanJar(File jarFile, boolean externalDependency, String packageDirectory, String version, boolean optional, ParsingContext parsingContext, String logPrefix) throws IOException {
        int scanned = 0;
        JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile));
        try {
            JarEntry jarEntry = null;
            getLog().debug(logPrefix + "Processing JAR file " + jarFile + "...");
            if (processJarManifest(jarFile, parsingContext, jarInputStream)) {
                getLog().debug(logPrefix + "Used OSGi bundle manifest information, but scanning for additional resources (taglibs, CNDs, etc)... ");
            }
            scanned = processJarInputStream(jarFile.getPath(), externalDependency, packageDirectory, version, optional, parsingContext, logPrefix, scanned, jarInputStream);
        } finally {
            jarInputStream.close();
        }

        if (parsingContext.getBundleClassPath().size() > 0) {
            getLog().debug(logPrefix + "Processing embedded dependencies...");
            JarFile jar = new JarFile(jarFile);
            for (String embeddedJar : parsingContext.getBundleClassPath()) {
                if (".".equals(embeddedJar)) {
                    continue;
                }
                JarEntry jarEntry = jar.getJarEntry(embeddedJar);
                if (jarEntry != null) {
                    getLog().debug(logPrefix + "Processing embedded JAR..." + jarEntry);
                    InputStream jarEntryInputStream = jar.getInputStream(jarEntry);
                    ByteArrayOutputStream entryOutputStream = new ByteArrayOutputStream();
                    IOUtils.copy(jarEntryInputStream, entryOutputStream);
                    JarInputStream entryJarInputStream = new JarInputStream(new ByteArrayInputStream(entryOutputStream.toByteArray()));
                    processJarInputStream(jarFile.getPath() + "!" + jarEntry, externalDependency, packageDirectory, version, optional, parsingContext, logPrefix, scanned, entryJarInputStream);
                    IOUtils.closeQuietly(jarEntryInputStream);
                    IOUtils.closeQuietly(entryJarInputStream);
                } else {
                    getLog().warn(logPrefix + "Couldn't find embedded JAR to parse " + embeddedJar + " in JAR " + jarFile);
                }
            }
        }

        if (parsingContext.getAdditionalFilesToParse().size() > 0) {
            getLog().debug(logPrefix + "Processing additional files to parse...");
            JarFile jar = new JarFile(jarFile);
            for (String fileToParse : parsingContext.getAdditionalFilesToParse()) {
                JarEntry jarEntry = jar.getJarEntry(fileToParse);
                if (jarEntry != null) {
                    InputStream jarEntryInputStream = jar.getInputStream(jarEntry);
                    ByteArrayOutputStream entryOutputStream = new ByteArrayOutputStream();
                    IOUtils.copy(jarEntryInputStream, entryOutputStream);
                    if (processNonTldFile(jarEntry.getName(), new ByteArrayInputStream(entryOutputStream.toByteArray()), jarFile.getPath(), optional, version, parsingContext)) {
                        scanned++;
                    }
                    IOUtils.closeQuietly(jarEntryInputStream);
                } else {
                    getLog().warn(logPrefix + "Couldn't find additional file to parse " + fileToParse + " in JAR " + jarFile);
                }
            }
            parsingContext.clearAdditionalFilesToParse();
        }
        
        return scanned;
    }

    protected int processJarInputStream(String jarFilePath, boolean externalDependency, String packageDirectory, String version, boolean optional, ParsingContext parsingContext, String logPrefix, int scanned, JarInputStream jarInputStream) throws IOException {
        JarEntry jarEntry;
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            if (jarEntry.isDirectory()) {
                continue;
            }
            String entryName = jarEntry.getName();

            if (entryName.startsWith(packageDirectory)) {
                String packageName = entryName.substring(packageDirectory.length());
                if (!packageName.contains("/.")) {
                    processLocalPackageEntry(packageName, "/", jarFilePath, version, optional, parsingContext);
                }
            }

            if (excludeJarEntry(entryName)) {
                continue;
            }
            ByteArrayOutputStream entryOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(jarInputStream, entryOutputStream);
            if (Parsers.getInstance().canParseForPhase(0, jarEntry.getName())) {
                getLog().debug(logPrefix + "  scanning JAR entry: " + jarEntry.getName());
                Parsers.getInstance().parse(0, jarEntry.getName(), new ByteArrayInputStream(entryOutputStream.toByteArray()), jarFilePath,
                        externalDependency, optional, version, getLogger(), parsingContext);
                scanned++;
            }
            if (Parsers.getInstance().canParseForPhase(1, jarEntry.getName())) {
                getLog().debug(logPrefix + "  scanning JAR entry: " + jarEntry.getName());
                if (processNonTldFile(jarEntry.getName(), new ByteArrayInputStream(entryOutputStream.toByteArray()), jarFilePath, optional, version, parsingContext)) {
                    scanned++;
                }
            }
        }
        return scanned;
    }

    private boolean processJarManifest(File jarFile, ParsingContext parsingContext, JarInputStream jarInputStream) throws IOException {
        boolean processedBundleHeadersSuccessfully = false;
        Manifest jarManifest = jarInputStream.getManifest();
        if (jarManifest != null && jarManifest.getMainAttributes() != null) {
            Attributes mainAttributes = jarManifest.getMainAttributes();
            String bundleSymbolicName = mainAttributes.getValue("Bundle-SymbolicName");
            if (bundleSymbolicName != null) {
                String bundleVersion = mainAttributes.getValue("Bundle-Version");
                String bundleClassPath = mainAttributes.getValue("Bundle-ClassPath");
                if (bundleClassPath != null && !".".equals(bundleClassPath.trim())) {
                    String[] bundleClassPathEntries = bundleClassPath.split(",");
                    for (String bundleClassPathEntry : bundleClassPathEntries) {
                        parsingContext.getBundleClassPath().add(bundleClassPathEntry.trim());
                    }
                }
                getLog().debug("OSGi bundle detected with symbolic name=" + bundleSymbolicName + " version=" + bundleVersion);
                parsingContext.setOsgiBundle(true);
                parsingContext.setVersion(bundleVersion);
                String importPackageHeaderValue = mainAttributes.getValue("Import-Package");
                String exportPackageHeaderValue = mainAttributes.getValue("Export-Package");
                String ignorePackageHeaderValue = mainAttributes.getValue("Ignore-Package");
                if (importPackageHeaderValue != null) {
                    List<ManifestValueClause> importPackageClauses = BundleUtils.getHeaderClauses("Import-Package", importPackageHeaderValue);
                    for (ManifestValueClause importPackageClause : importPackageClauses) {
                        for (String importPackagePath : importPackageClause.getPaths()) {
                            String clauseVersion = importPackageClause.getAttributes().get("version");
                            String clauseResolution = importPackageClause.getDirectives().get("resolution");
                            boolean optionalClause = false;
                            if ("optional".equals(clauseResolution)) {
                                optionalClause = true;
                            }
                            PackageInfo importPackageInfo = new PackageInfo(importPackagePath, clauseVersion, optionalClause, jarFile.getPath(), parsingContext);
                            parsingContext.addPackageImport(importPackageInfo);
                        }
                    }
                }
                if (exportPackageHeaderValue != null) {
                    List<ManifestValueClause> exportPackageClauses = BundleUtils.getHeaderClauses("Export-Package", exportPackageHeaderValue);
                    for (ManifestValueClause exportPackageClause : exportPackageClauses) {
                        for (String importPackagePath : exportPackageClause.getPaths()) {
                            String clauseVersion = exportPackageClause.getAttributes().get("version");
                            PackageInfo exportPackageInfo = new PackageInfo(importPackagePath, clauseVersion, false, jarFile.getPath(), parsingContext);
                            parsingContext.addPackageExport(exportPackageInfo);
                        }
                    }
                }
                if (ignorePackageHeaderValue != null) {
                    List<ManifestValueClause> ignorePackageClauses = BundleUtils.getHeaderClauses("Ignore-Package", ignorePackageHeaderValue);
                    for (ManifestValueClause ignorePackageClause : ignorePackageClauses) {
                        for (String importPackagePath : ignorePackageClause.getPaths()) {
                            String clauseVersion = ignorePackageClause.getAttributes().get("version");
                            boolean optionalClause = true;
                            PackageInfo ignorePackageInfo = new PackageInfo(importPackagePath, clauseVersion, optionalClause, jarFile.getPath(), parsingContext);
                            parsingContext.addPackageImport(ignorePackageInfo);
                        }
                    }
                }
                processedBundleHeadersSuccessfully = true;
            }
        }
        return processedBundleHeadersSuccessfully;
    }

    private void processLocalPackageEntry(String entryName, String fileSeparator, String entryParent, String version, boolean optional, ParsingContext parsingContext) {
        int lastSlash = entryName.lastIndexOf(fileSeparator);
        if (lastSlash == -1) {
            return;
        }
        String entryPackage = StringUtils.replace(entryName.substring(0, lastSlash), fileSeparator, ".");
        if (StringUtils.isNotEmpty(entryPackage) && !parsingContext.getLocalPackages().contains(new PackageInfo(entryPackage))
                && !entryPackage.startsWith("META-INF") && !entryPackage.startsWith("OSGI-INF")
                && !entryPackage.startsWith("OSGI-OPT") && !entryPackage.startsWith("WEB-INF")) {
            PackageInfo packageInfo = new PackageInfo(entryPackage, version, optional, entryParent + "/" + entryName, parsingContext);
            parsingContext.addLocalPackage(packageInfo);
            if (!parsingContext.isOsgiBundle()) {
                parsingContext.addPackageExport(packageInfo);
            }
        }
    }

    private boolean excludeJarEntry(String entryName) {
        if (excludedJarEntryPatterns != null) {
            for (Pattern p : excludedJarEntryPatterns) {
                if (p.matcher(entryName).matches()) {
                    getLog().debug("Matched JAR entry exclusion pattern for entry " + entryName);
                    return true;
                }
            }
        }
        return false;
    }

    private void processDirectoryTlds(File directoryFile, String version, ParsingContext parsingContext) throws IOException {
        DirectoryScanner ds = new DirectoryScanner();
        String[] excludes = excludeFromDirectoryScan.toArray(new String[excludeFromDirectoryScan.size()]);
        ds.setExcludes(excludes);
        ds.setIncludes(new String[]{"**/*.tld"});
        ds.setBasedir(directoryFile);
        ds.setCaseSensitive(true);
        ds.scan();
        String[] includedFiles = ds.getIncludedFiles();
        for (String includedFile : includedFiles) {
            Parsers.getInstance().parse(0, includedFile, new BufferedInputStream(new FileInputStream(new File(directoryFile, includedFile))), directoryFile.getPath(), false, false, version, getLogger(), parsingContext);
        }
    }

    private void processDirectory(File directoryFile, boolean optional, String version, ParsingContext parsingContext) throws IOException {
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
                processNonTldFile(includedFile, fileInputStream, directoryFile.getPath(), optional, version, parsingContext);
            } finally {
                IOUtils.closeQuietly(fileInputStream);
            }
        }
    }

    private boolean processNonTldFile(String fileName, InputStream inputStream, String fileParent, boolean optional, String version, ParsingContext parsingContext) throws IOException {
        String ext = FileUtils.getExtension(fileName).toLowerCase();

        if (!SUPPORTED_FILE_EXTENSIONS_TO_SCAN.contains(ext)) {
            return false;
        }

        return Parsers.getInstance().parse(1, fileName, inputStream, fileParent, false, optional, version, getLogger(), parsingContext);

    }

    private Logger getLogger() {
        return logger;
    }

}
