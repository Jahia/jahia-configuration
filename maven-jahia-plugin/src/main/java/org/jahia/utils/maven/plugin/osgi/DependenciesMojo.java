/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.utils.maven.plugin.osgi;

import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.bundleplugin.BundlePlugin;
import org.apache.felix.bundleplugin.DependencyEmbedder;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jahia.utils.maven.plugin.SLF4JLoggerToMojoLogBridge;
import org.jahia.utils.maven.plugin.support.AetherHelper;
import org.jahia.utils.maven.plugin.support.AetherHelperFactory;
import org.jahia.utils.maven.plugin.support.ArtifactProcessor;
import org.jahia.utils.maven.plugin.support.MavenAetherHelperUtils;
import org.jahia.utils.osgi.BundleUtils;
import org.jahia.utils.osgi.ManifestValueClause;
import org.jahia.utils.osgi.PackageUtils;
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
public class DependenciesMojo extends BundlePlugin {

    protected static final Set<String> SUPPORTED_FILE_EXTENSIONS_TO_SCAN = new HashSet<String>(Arrays.asList("jsp",
            "jspf", "tag", "tagf", "cnd", "drl", "xml", "groovy"));
    
    protected static final Set<String> DEPENDENCIES_SCAN_PACKAGING = new HashSet<String>(Arrays.asList("jar", "war"));

    protected static final Set<String> DEPENDENCIES_SCAN_SCOPES = new HashSet<String>(Arrays.asList(
            Artifact.SCOPE_PROVIDED, Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME));

    private AetherHelper aetherHelper;

    /**
     * @component
     * @required
     * @readonly
     */
    protected PlexusContainer container;

    /**
     * The current build session instance.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession mavenSession;

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
     * The directory for the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    protected String projectBuildDirectory;

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

    /**
     * @parameter default-value="true"
     */
    protected boolean jahiaDependsCapabilitiesActivated = true;

    /**
     * @parameter default-value=","
     */
    protected String jahiaDependsCapabilitiesPrefix = ",";

    protected List<Pattern> artifactExclusionPatterns = new ArrayList<Pattern>();
    protected Logger logger = new SLF4JLoggerToMojoLogBridge(getLog());
    protected ParsingContextCache parsingContextCache;
    protected Collection<String> inlinedPaths = new LinkedHashSet<String>();
    protected Collection<Artifact> embeddedArtifacts = new LinkedHashSet<Artifact>();

    protected AetherHelper getAetherHelper() throws MojoExecutionException {
        if (aetherHelper == null) {
            aetherHelper = AetherHelperFactory.create(container, project, mavenSession, getLog());
        }
        return aetherHelper;
    }

    @Override
    public void setLog(Log log) {
        super.setLog(log);
        logger = new SLF4JLoggerToMojoLogBridge(log);
    }

    @Override
    public void execute() throws MojoExecutionException {
        setBuildDirectory(projectBuildDirectory);
        setOutputDirectory(new File(projectOutputDirectory));
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

        Map<String,String> originalInstructions = new LinkedHashMap<String,String>();
        if (project.getPlugin("org.apache.felix:maven-bundle-plugin") != null) {
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
        } else {
            // we are not in a bundle project
            if (project.getPackaging().equals("war")) {
                for (Artifact artifact : project.getArtifacts()) {
                    if (!artifact.getScope().toLowerCase().equals("provided") &&
                            !artifact.getScope().toLowerCase().equals("test")) {
                        // artifact will be embedded in WAR
                        embeddedArtifacts.add(artifact);
                    }
                }
            }
        }

        List<PackageInfo> existingPackageImports = getExistingImportPackages(projectParsingContext);
        projectParsingContext.addAllPackageImports(existingPackageImports);

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
            throw new MojoExecutionException("Error while scanning dependencies", e);
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error while scanning dependencies", e);
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
                throw new MojoExecutionException("Error processing resource directory " + scanDirectoryFile, e);
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

        SortedSet<PackageInfo> childLocalPackagesToRemoveFromImport = projectParsingContext.getChildrenLocalPackagesToRemoveFromImports();for (PackageInfo childLocalPackageToRemove : childLocalPackagesToRemoveFromImport) {
                PackageUtils.removeMatchingVersions(projectParsingContext.getPackageImports(), childLocalPackageToRemove);
        }

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

        project.getProperties().put("jahia.plugin.requiredModulesCapabilities", "");
        project.getProperties().put("jahia.plugin.providedModulesCapabilities", "");


        if (jahiaDependsCapabilitiesActivated) {
            try {
                if (originalInstructions.containsKey("Jahia-Depends") && !ArrayUtils.contains(StringUtils.split(originalInstructions.get("_removeheaders"),", \n"), "Jahia-Depends")) {
                    getLog().info("Building OSGi capabilities for Jahia module dependencies...");
                    StringBuilder jahiaDependsRequireCapabilities = new StringBuilder();
                    jahiaDependsRequireCapabilities.append(jahiaDependsCapabilitiesPrefix);
                    String jahiaDependsValue = originalInstructions.get("Jahia-Depends");
                    if (StringUtils.isNotEmpty(jahiaDependsValue)) {
                        String[] jahiaDependsArray = jahiaDependsValue.split(",");
                        int counter = 0;
                        for (String jahiaDependsEntry : jahiaDependsArray) {
                            jahiaDependsRequireCapabilities.append("com.jahia.modules.dependencies; filter:=\"(moduleIdentifier=").append(jahiaDependsEntry.trim()).append(")\"");
                            if (counter < jahiaDependsArray.length - 1) {
                                jahiaDependsRequireCapabilities.append(",");
                            }
                            counter++;
                        }
                        project.getProperties().put("jahia.plugin.requiredModulesCapabilities", jahiaDependsRequireCapabilities.toString());
                    }
                }
                project.getProperties().put("jahia.plugin.providedModulesCapabilities", jahiaDependsCapabilitiesPrefix + "com.jahia.modules.dependencies; moduleIdentifier=\"" + project.getArtifactId() + "\"");
            } catch (Exception e) {
                getLog().error("Error generating capabilities from Jahia-Depends", e);
            }
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

    public List<PackageInfo> getExistingImportPackages(ParsingContext projectParsingContext) throws MojoExecutionException {
        List<PackageInfo> existingPackageImports = new ArrayList<PackageInfo>();
        if (existingImports != null) {
            try {
                List<ManifestValueClause> existingImportValueClauses = BundleUtils.getHeaderClauses("Import-Package", existingImports);
                for (ManifestValueClause existingImportValueClause : existingImportValueClauses) {
                    String clauseVersion = existingImportValueClause.getAttributes().get("version");
                    String clauseResolution = existingImportValueClause.getDirectives().get("resolution");
                    boolean optionalClause = false;
                    if ("optional".equals(clauseResolution)) {
                        optionalClause = true;
                    }
                    for (String existingImportPath : existingImportValueClause.getPaths()) {
                        existingPackageImports.add(new PackageInfo(existingImportPath, clauseVersion, optionalClause, "Maven plugin configuration", projectParsingContext));
                    }
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Error while parsing existing import clauses", e);
            }
            /*
            String[] existingImportsArray = StringUtils.split(existingImports, ", \n\r");
            for (String existingImport : existingImportsArray) {
                projectParsingContext.addPackageImport(new PackageInfo(existingImport, null, false, "Maven plugin configuration", projectParsingContext));
            }
            */
        }
        return existingPackageImports;
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

    protected void scanClassesBuildDirectory(ParsingContext parsingContext) throws IOException, DependencyResolutionRequiredException, MojoExecutionException {
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
        getLog().info("Scanning project dependencies...");
        int scanned = 0;

        for (Artifact artifact : project.getDependencyArtifacts()) {
            if (artifact.isOptional()) {
                getLog().info("Scanning optional dependency " + artifact + "...");
            }

            if (!DEPENDENCIES_SCAN_PACKAGING.contains(artifact.getType())
                    || !DEPENDENCIES_SCAN_SCOPES.contains(artifact.getScope()) || isExcludedFromScan(artifact)) {
                continue;
            }

            final int scannedCopy = scanned;
            final DependenciesMojo dependenciesMojo = this;
            getAetherHelper().processArtifactAndDependencies(artifact, artifact.isOptional(), new ArtifactProcessor() {

                @Override
                public boolean isExternal(Artifact artifact) {
                    for (String inlinedPath : inlinedPaths) {
                        if (inlinedPath.startsWith(artifact.getFile().getPath())) {
                            return false;
                        }
                    }
                    for (Artifact embeddedArtifact : embeddedArtifacts) {
                        if (artifact.equals(embeddedArtifact)) {
                            return false;
                        }
                    }
                    return true;
                }

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
        } else {
            parsingContext.setOptional(false);
        }
        if (externalDependency) {
            parsingContext.setExternal(true);
        } else {
            parsingContext.setExternal(false);
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
        /*
        if (artifact.isOptional() && !externalDependency) {
            projectParsingContext.addAllPackageImports(parsingContext.getLocalPackages(), true);
        }
        */
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

        if (jarFile.isDirectory()) {
            getLog().debug(logPrefix + "Processing dependency directory " + jarFile + "...");
            processDirectoryTlds(jarFile, version, parsingContext);
            processDirectory(jarFile, false, version, parsingContext);
            return scanned;
        }

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

    protected void resolveEmbeddedDependencies( MavenProject currentProject, Builder builder ) throws Exception
    {
        if ( currentProject.getBasedir() != null )
        {
            // update BND instructions to add included Maven resources
            includeMavenResources( currentProject, builder, getLog() );

            // calculate default export/private settings based on sources
            addLocalPackages( new File(projectOutputDirectory), builder );

            // tell BND where the current project source resides
            addMavenSourcePath( currentProject, builder, getLog() );
        }

        // update BND instructions to embed selected Maven dependencies
        Collection<Artifact> embeddableArtifacts = getEmbeddableArtifacts( currentProject, builder );
        DependencyEmbedder dependencyEmbedder = new DependencyEmbedder( getLog(), embeddableArtifacts );
        dependencyEmbedder.processHeaders( builder );
        inlinedPaths = dependencyEmbedder.getInlinedPaths();
        embeddedArtifacts = dependencyEmbedder.getEmbeddedArtifacts();
    }

    /* code copied from BundlePlugin because resources were private */

    private static final String LOCAL_PACKAGES = "{local-packages}";

    private static void addLocalPackages( File outputDirectory, Analyzer analyzer ) throws IOException
    {
        Packages packages = new Packages();

        if ( outputDirectory != null && outputDirectory.isDirectory() )
        {
            // scan classes directory for potential packages
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( outputDirectory );
            scanner.setIncludes( new String[]
                    { "**/*.class" } );

            scanner.addDefaultExcludes();
            scanner.scan();

            String[] paths = scanner.getIncludedFiles();
            for ( int i = 0; i < paths.length; i++ )
            {
                packages.put( analyzer.getPackageRef( getPackageName( paths[i] ) ) );
            }
        }

        Packages exportedPkgs = new Packages();
        Packages privatePkgs = new Packages();

        boolean noprivatePackages = "!*".equals( analyzer.getProperty( Analyzer.PRIVATE_PACKAGE ) );

        for ( Descriptors.PackageRef pkg : packages.keySet() )
        {
            // mark all source packages as private by default (can be overridden by export list)
            privatePkgs.put( pkg );

            // we can't export the default package (".") and we shouldn't export internal packages
            String fqn = pkg.getFQN();
            if ( noprivatePackages || !( ".".equals( fqn ) || fqn.contains( ".internal" ) || fqn.contains( ".impl" ) ) )
            {
                exportedPkgs.put( pkg );
            }
        }

        Properties properties = analyzer.getProperties();
        String exported = properties.getProperty( Analyzer.EXPORT_PACKAGE );
        if ( exported == null )
        {
            if ( !properties.containsKey( Analyzer.EXPORT_CONTENTS ) )
            {
                // no -exportcontents overriding the exports, so use our computed list
                for ( Attrs attrs : exportedPkgs.values() )
                {
                    attrs.put( Constants.SPLIT_PACKAGE_DIRECTIVE, "merge-first" );
                }
                properties.setProperty( Analyzer.EXPORT_PACKAGE, Processor.printClauses( exportedPkgs ) );
            }
            else
            {
                // leave Export-Package empty (but non-null) as we have -exportcontents
                properties.setProperty( Analyzer.EXPORT_PACKAGE, "" );
            }
        }
        else if ( exported.indexOf( LOCAL_PACKAGES ) >= 0 )
        {
            String newExported = org.codehaus.plexus.util.StringUtils.replace(exported, LOCAL_PACKAGES, Processor.printClauses(exportedPkgs));
            properties.setProperty( Analyzer.EXPORT_PACKAGE, newExported );
        }

        String internal = properties.getProperty( Analyzer.PRIVATE_PACKAGE );
        if ( internal == null )
        {
            if ( !privatePkgs.isEmpty() )
            {
                for ( Attrs attrs : privatePkgs.values() )
                {
                    attrs.put( Constants.SPLIT_PACKAGE_DIRECTIVE, "merge-first" );
                }
                properties.setProperty( Analyzer.PRIVATE_PACKAGE, Processor.printClauses( privatePkgs ) );
            }
            else
            {
                // if there are really no private packages then use "!*" as this will keep the Bnd Tool happy
                properties.setProperty( Analyzer.PRIVATE_PACKAGE, "!*" );
            }
        }
        else if ( internal.indexOf( LOCAL_PACKAGES ) >= 0 )
        {
            String newInternal = org.codehaus.plexus.util.StringUtils.replace(internal, LOCAL_PACKAGES, Processor.printClauses(privatePkgs));
            properties.setProperty( Analyzer.PRIVATE_PACKAGE, newInternal );
        }
    }

    private static String getPackageName( String filename )
    {
        int n = filename.lastIndexOf( File.separatorChar );
        return n < 0 ? "." : filename.substring( 0, n ).replace( File.separatorChar, '.' );
    }

}
