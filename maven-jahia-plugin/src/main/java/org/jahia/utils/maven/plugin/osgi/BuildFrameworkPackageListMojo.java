package org.jahia.utils.maven.plugin.osgi;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.eclipse.osgi.util.ManifestElement;
import org.jahia.utils.maven.plugin.SLF4JLoggerToMojoLogBridge;
import org.jahia.utils.osgi.PropertyFileUtils;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This maven goal will build the list of system packages that is exposed by the OSGi framework by default.
 * In order to this it can use as input:
 * - An existing property file that already includes a package list in OSGi format.
 * - A previously generated MANIFEST.MF by the Maven Bundle Plugin
 * - The contents of WEB-INF/classes
 * - The contents of WEB-INF/lib
 * - Dependencies of the project marked with "provided" scope.
 *
 * @goal build-framework-package-list
 * @requiresDependencyResolution test
 */
public class BuildFrameworkPackageListMojo extends AbstractMojo {

    public static final String VERSION_NUMBER_PATTERN_STRING = "([\\d\\.]*\\d)(.*)";
    private static final Pattern VERSION_NUMBER_PATTERN = Pattern.compile(VERSION_NUMBER_PATTERN_STRING);
    /**
     * Clean up version parameters. Other builders use more fuzzy definitions of
     * the version syntax. This method cleans up such a version to match an OSGi
     * version.
     *
     * @param VERSION_STRING
     * @return
     */
    static final Pattern FUZZY_VERSION = Pattern.compile("(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9](.*))?",
            Pattern.DOTALL);

    /**
     * @parameter default-value="${project.build.directory}/classes/META-INF/MANIFEST.MF"
     */
    protected File inputManifestFile;

    /**
     * @parameter default-value="${project.build.directory}/${project.build.finalName}/WEB-INF/lib"
     */
    protected List<String> jarDirectories;

    /**
     * @parameter default-value="${project.basedir}/src/main/webapp/WEB-INF/etc/config/felix-framework.properties"
     */
    protected File propertiesInputFile;

    /**
     * @parameter default-value="${project.build.directory}/generated-resources/felix-framework.properties"
     */
    protected File propertiesOutputFile;

    /**
     * @parameter
     */
    protected List<String> manualPackageList;

    /**
     * @parameter default-value="true"
     */
    protected boolean scanDependencies = true;

    /**
     * @parameter default-value="false"
     */
    protected boolean exportEachPackageOnce = false;

    /**
     * Because of bug http://jira.codehaus.org/browse/MNG-5440 we cannot use a default-value otherwise the values from
     * the POM will not be used, so we use a constant (below) and initialization code to set it if the project has
     * not set it.
     * @parameter
     */
    protected List<String> artifactExcludes;

    /**
     * Because of bug http://jira.codehaus.org/browse/MNG-5440 we set the default value at runtime using the following
     * constant
     */
    private static final String ARTIFACT_EXCLUDE_DEFAULT_VALUE = "org.jahia.modules:*,org.jahia.templates:*,org.jahia.test:*,*.jahia.modules";

    /**
     * Because of bug http://jira.codehaus.org/browse/MNG-5440 we cannot use a default-value otherwise the values from
     * the POM will not be used, so we use a constant (below) and initialization code to set it if the project has
     * not set it.
     * @parameter
     */
    protected List<String> packageExcludes;

    /**
     * Because of bug http://jira.codehaus.org/browse/MNG-5440 we set the default value at runtime using the following
     * constant. Note that if any of these packages are exported by default by Felix already, they won't be excluded since
     * we only deal with "extra" packages, not the "base" exported packages.
     */
    private static final String PACKAGE_EXCLUDE_DEFAULT_VALUE = "org.jahia.taglibs*,org.apache.taglibs.standard*,javax.servlet.jsp*,org.codehaus.groovy.ast*,javax.el*,de.odysseus.el*";

    /**
     * @parameter default-value="true"
     */
    protected boolean outputPackagesWithNoVersions = true;

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of plugins and their dependencies.
     *
     * @parameter default-value="${project.remotePluginRepositories}"
     * @readonly
     */
    private List<RemoteRepository> remoteRepos;

    /**
     * @parameter default-value="org.osgi.framework.system.packages.extra"
     */
    protected String propertyFilePropertyName = "org.osgi.framework.system.packages.extra";

    private Map<String, DependencyNode> resolvedDependencyNodes = new HashMap<String, DependencyNode>();
    private List<Pattern> artifactExclusionPatterns = new ArrayList<Pattern>();
    private List<Pattern> packageExclusionPatterns = new ArrayList<Pattern>();

    private class VersionLocation {
        private String location;
        private String version;
        private String specificationVersion;
        private long counter = 0;

        public VersionLocation(String location, String version, String specificationVersion) {
            this.location = location;
            this.version = version;
            this.specificationVersion = specificationVersion;
        }

        public String getLocation() {
            return location;
        }

        public String getVersion() {
            return version;
        }

        public String getSpecificationVersion() {
            return specificationVersion;
        }

        public void incrementCounter() {
            counter++;
        }

        public long getCounter() {
            return counter;
        }
    }

    public void setPackageExcludes(List<String> packageExcludes) {
        this.packageExcludes = packageExcludes;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        buildExclusionPatterns();

        buildPackageExcludes();

        Map<String, Map<String, VersionLocation>> packageVersionCounts = new TreeMap<String, Map<String, VersionLocation>>();
        Map<String, Set<String>> packageVersions = new TreeMap<String, Set<String>>();
        String generatedPackageList = null;

        try {
            scanExistingExports(packageVersionCounts);

            if (project != null) {


                if (scanDependencies) {
                    scanDependencies(packageVersionCounts);
                }

                // now let's scan the current project. Assuming it's been built already.

                scanClassesBuildDirectory(packageVersionCounts);

                scanJarDirectories(packageVersionCounts);

            }

            scanExistingManifest(packageVersionCounts);

            resolveSplitPackages(packageVersionCounts, packageVersions);

            if (propertiesOutputFile != null && !propertiesOutputFile.exists()) {
                propertiesOutputFile.getParentFile().mkdirs();
                propertiesOutputFile.createNewFile();
            }

            List<String> packageList = new ArrayList<String>();

            StringBuilder generatedPackageBuffer = new StringBuilder();
            for (Map.Entry<String, Set<String>> packageVersion : packageVersions.entrySet()) {
                if (packageVersion.getValue() != null) {
                    // @todo we should perform parent lookup here and re-use version if activated.
                    boolean allVersionsAreNull = true;
                    for (String versionString : packageVersion.getValue()) {
                        if (versionString != null) {
                            allVersionsAreNull = false;
                            StringBuilder packageExport = new StringBuilder();
                            packageExport.append(packageVersion.getKey());
                            versionString = cleanupVersion(versionString);
                            packageExport.append(";version\\=\"");
                            packageExport.append(versionString);
                            packageExport.append("\"");
                            if (!isPackageExcluded(packageExport.toString()) &&
                                    !isPackageExcluded(packageVersion.getKey())) {
                                packageExport.append(",");
                                if (packageList.contains(packageExport.toString())) {
                                    getLog().warn("Package export " + packageExport.toString() + " already present in list, will not add again!");
                                } else {
                                    packageList.add(packageExport.toString());
                                }
                                generatedPackageBuffer.append(packageExport);
                            } else {
                                getLog().info("Package " + packageExport.toString() + " matched exclusion list, will not be included !" );
                            }
                        }
                    }
                    if (allVersionsAreNull && outputPackagesWithNoVersions) {
                        StringBuilder packageExport = new StringBuilder();
                        packageExport.append(packageVersion.getKey());
                        if (!isPackageExcluded(packageExport.toString())) {
                            packageExport.append(",");
                            if (packageList.contains(packageExport.toString())) {
                                getLog().warn("Package export " + packageExport.toString() + " already present in list, will not add again!");
                            } else {
                                getLog().info("Adding package " + packageExport.toString() + " with no version");
                                packageList.add(packageExport.toString());
                            }
                            generatedPackageBuffer.append(packageExport);
                        } else {
                            getLog().info("Package " + packageExport.toString() + " matched exclusion list, will not be included !" );
                        }
                    }
                }
            }
            if (manualPackageList != null) {
                for (String manualPackage : manualPackageList) {
                    if (!packageList.contains(manualPackage+",") && !isPackageExcluded(manualPackage)) {
                        if (manualPackage.contains("=")) {
                            manualPackage = manualPackage.replaceAll("=", "\\=");
                        }
                        packageList.add(manualPackage +",");
                        generatedPackageBuffer.append(manualPackage);
                        generatedPackageBuffer.append(",");
                    } else if (isPackageExcluded(manualPackage)) {
                        getLog().info("Package " + manualPackage + " matched exclusion list, will not be included !" );
                    }
                }
            }
            generatedPackageList = generatedPackageBuffer.toString();
            generatedPackageList = generatedPackageList.substring(0, generatedPackageList.length() - 1); // remove the last comma
            String lastPackage = packageList.remove(packageList.size()-1);
            packageList.add(lastPackage.substring(0,lastPackage.length()-1)); // remove the last comma
            getLog().info("Found " + packageVersions.size() + " packages in dependencies.");
            // getLog().debug("org.osgi.framework.system.packages.extra="+ generatedPackageList);
            if (generatedPackageList != null && project != null) {
                project.getProperties().put("jahiaGeneratedFrameworkPackageList", generatedPackageList);
            }
            PropertyFileUtils.updatePropertyFile(
                    propertiesInputFile,
                    propertiesOutputFile,
                    propertyFilePropertyName,
                    packageList.toArray(new String[packageList.size()]),
                    new SLF4JLoggerToMojoLogBridge(getLog()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    private void buildExclusionPatterns() {
        if (artifactExcludes == null) {
            // We put the default value here because of bug http://jira.codehaus.org/browse/MNG-5440
            String[] artifactExcludesArray = ARTIFACT_EXCLUDE_DEFAULT_VALUE.split(",");
            artifactExcludes = new ArrayList(Arrays.asList(artifactExcludesArray));
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

    private void buildPackageExcludes() {
        if (packageExcludes == null) {
            // We put the default value here because of bug http://jira.codehaus.org/browse/MNG-5440
            String[] packageExcludesArray = PACKAGE_EXCLUDE_DEFAULT_VALUE.split(",");
            packageExcludes = new ArrayList(Arrays.asList(packageExcludesArray));
        }
        for (String packageExclude : packageExcludes) {
            String packageExcludePattern = packageExclude;
            packageExcludePattern = packageExcludePattern.replaceAll("\\.", "\\\\.");
            packageExcludePattern = packageExcludePattern.replaceAll("\\*", ".*");
            packageExclusionPatterns.add(Pattern.compile(packageExcludePattern));
        }
    }

    private boolean isPackageExcluded(String packageExport) {
        for (Pattern packageExclusionPattern : packageExclusionPatterns) {
            Matcher packageExclusionMatcher = packageExclusionPattern.matcher(packageExport);
            if (packageExclusionMatcher.matches()) {
                return true;
            }
        }
        return false;
    }

    private void scanExistingExports(Map<String, Map<String, VersionLocation>> packageVersionCounts) {
        if (!propertiesInputFile.exists()) {
            return;
        }
        FileInputStream fileInputStream = null;
        try {
            Properties properties = new Properties();
            fileInputStream = new FileInputStream(propertiesInputFile);
            properties.load(fileInputStream);
            String exportPropertyValue = (String) properties.get(propertyFilePropertyName);
            if (exportPropertyValue == null) {
                return;
            }
            getLog().info("Processing existing property " + propertyFilePropertyName + " from file " + propertiesInputFile + "...");
            ManifestElement[] manifestElements = ManifestElement.parseHeader("Export-Package", exportPropertyValue);
            for (ManifestElement manifestElement : manifestElements) {
                String[] packageNames = manifestElement.getValueComponents();
                String version = manifestElement.getAttribute("version");
                for (String packageName : packageNames) {
                    updateVersionLocationCounts(packageVersionCounts, propertiesInputFile.toString(), version, null, packageName);
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            IOUtils.closeQuietly(fileInputStream);
        }
    }

    private void scanExistingManifest(Map<String, Map<String, VersionLocation>> packageVersionCounts) throws IOException, Exception {
        FileInputStream in = null;
        try {
            if (inputManifestFile.exists()) {
                in = new FileInputStream(inputManifestFile);
                Manifest mf = new Manifest(in);
                String exportPackageStr = mf.getMainAttributes().getValue("Export-Package");
                String bundleVersion = mf.getMainAttributes().getValue("Bundle-Version");
                ManifestElement[] manifestElements = ManifestElement.parseHeader("Export-Package", exportPackageStr);
                for (ManifestElement manifestElement : manifestElements) {
                    String[] packageNames = manifestElement.getValueComponents();
                    String version = manifestElement.getAttribute("version");
                    if (version != null) {
                        for (String packageName : packageNames) {
                            if (version.equals(bundleVersion)) {
                                if (packageName.startsWith("org.jahia")) {
                                    updateVersionLocationCounts(packageVersionCounts, inputManifestFile.toString(), version, bundleVersion, packageName);
                                } else {
                                    updateVersionLocationCounts(packageVersionCounts, inputManifestFile.toString(), null, bundleVersion, packageName);
                                }
                            } else {
                                updateVersionLocationCounts(packageVersionCounts, inputManifestFile.toString(), version, bundleVersion, packageName);
                            }
                        }
                    } else {
                        for (String packageName : packageNames) {
                            updateVersionLocationCounts(packageVersionCounts, inputManifestFile.toString(), null, bundleVersion, packageName);
                        }
                    }
                }
                getLog().info("Found " + manifestElements.length + " package exports.");
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private void resolveSplitPackages(Map<String, Map<String, VersionLocation>> packageVersionCounts, Map<String, Set<String>> packageVersions) {
        for (Map.Entry<String, Map<String, VersionLocation>> resolvedPackageVersion : packageVersionCounts.entrySet()) {
            VersionLocation highestVersionLocation = null;
            boolean allVersionsEqual = true;
            String previousVersion = null;
            for (Map.Entry<String, VersionLocation> versionLocationEntry : resolvedPackageVersion.getValue().entrySet()) {
                if (previousVersion != null && !previousVersion.equals(versionLocationEntry.getValue().getVersion())) {
                    allVersionsEqual = false;
                    break;
                }
                previousVersion = versionLocationEntry.getValue().getVersion();
            }
            if (resolvedPackageVersion.getValue().size() > 1 && !allVersionsEqual) {
                getLog().warn("Split-package with different versions detected for package " + resolvedPackageVersion.getKey() + ":");
            }
            Set<String> versions = new HashSet<String>();
            for (Map.Entry<String, VersionLocation> versionLocationEntry : resolvedPackageVersion.getValue().entrySet()) {
                if (resolvedPackageVersion.getValue().size() > 1 && !allVersionsEqual) {
                    getLog().warn("  - " + versionLocationEntry.getKey() + " v" + versionLocationEntry.getValue().getVersion() + " count=" + versionLocationEntry.getValue().getCounter() + " Specification-Version=" + versionLocationEntry.getValue().getSpecificationVersion());
                }
                if (versionLocationEntry.getValue() == null) {
                    continue;
                }
                if (highestVersionLocation == null) {
                    highestVersionLocation = versionLocationEntry.getValue();
                } else {
                    if (highestVersionLocation.getCounter() < versionLocationEntry.getValue().getCounter()) {
                        highestVersionLocation = versionLocationEntry.getValue();
                    }
                }
                if (!versions.contains(versionLocationEntry.getValue().getVersion())) {
                    versions.add(versionLocationEntry.getValue().getVersion());
                }
            }
            if (exportEachPackageOnce) {
                versions.clear();
                versions.add(highestVersionLocation.getVersion());
                packageVersions.put(resolvedPackageVersion.getKey(), versions);
                if (resolvedPackageVersion.getValue().size() > 1 && !allVersionsEqual) {
                    getLog().warn("--> " + resolvedPackageVersion.getKey() + " v" + highestVersionLocation.getVersion());
                }
            } else {
                packageVersions.put(resolvedPackageVersion.getKey(), versions);
            }
        }
    }

    private void scanClassesBuildDirectory(Map<String, Map<String, VersionLocation>> packageVersionCounts) throws IOException {
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
                    updateVersionLocationCounts(packageVersionCounts, project.getBuild().getFinalName(), project.getVersion(), null, entryPackage);
                }
            }
        }
    }

    private void scanDependencies(Map<String, Map<String, VersionLocation>> packageVersionCounts) throws IOException {
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
                getLog().debug("Scanning dependency " + artifact.getFile());
                scanJar(packageVersionCounts, artifact.getFile(), artifact.getBaseVersion());
            }
        }
    }

    private void scanJar(Map<String, Map<String, VersionLocation>> packageVersionCounts, File jarFile, String defaultVersion) throws IOException {
        JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile));
        Manifest jarManifest = jarInputStream.getManifest();
        // Map<String, String> manifestVersions = new HashMap<String,String>();
        String specificationVersion = null;
        if (jarManifest == null) {
            getLog().warn("No MANIFEST.MF file found for dependency " + jarFile);
        } else {
            if (jarManifest.getMainAttributes() == null) {
                getLog().warn("No main attributes found in MANIFEST.MF file found for dependency " + jarFile);
            } else {
                specificationVersion = jarManifest.getMainAttributes().getValue("Specification-Version");
                if (defaultVersion == null) {
                    if (jarManifest.getMainAttributes().getValue("Bundle-Version") != null) {
                    } else if (specificationVersion != null) {
                        defaultVersion = specificationVersion;
                    } else {
                        defaultVersion = jarManifest.getMainAttributes().getValue("Implementation-Version");
                    }
                }
                for (Map.Entry<String, Attributes> manifestEntries : jarManifest.getEntries().entrySet()) {
                    String packageName = manifestEntries.getKey().replaceAll("/", ".");
                    if (packageName.endsWith(".class")) {
                        continue;
                    }
                    if (packageName.endsWith(".")) {
                        packageName = packageName.substring(0, packageName.length() - 1);
                    }
                    if (packageName.endsWith(".*")) {
                        packageName = packageName.substring(0, packageName.length() - 1);
                    }
                    int lastDotPos = packageName.lastIndexOf(".");
                    String lastPackage = packageName;
                    if (lastDotPos > -1) {
                        lastPackage = packageName.substring(lastDotPos + 1);
                    }
                    if (lastPackage.length() > 0 && Character.isUpperCase(lastPackage.charAt(0))) {
                        // ignore non package version
                        continue;
                    }
                    if (StringUtils.isEmpty(packageName) ||
                            packageName.startsWith("META-INF") ||
                            packageName.startsWith("OSGI-INF") ||
                            packageName.startsWith("OSGI-OPT") ||
                            packageName.startsWith("WEB-INF") ||
                            packageName.startsWith("org.osgi")) {
                        // ignore private package names
                        continue;
                    }
                    String packageVersion = null;
                    if (manifestEntries.getValue().getValue("Specification-Version") != null) {
                        packageVersion = manifestEntries.getValue().getValue("Specification-Version");
                    } else {
                        packageVersion = manifestEntries.getValue().getValue("Implementation-Version");
                    }
                    if (packageVersion != null) {
                        getLog().info("Found package version in " + jarFile.getName() + " MANIFEST : " + packageName + " v" + packageVersion);
                        updateVersionLocationCounts(packageVersionCounts, jarFile.getCanonicalPath(), packageVersion, specificationVersion, packageName);
                        // manifestVersions.put(packageName, packageVersion);
                    }
                }
            }
        }
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
                    if (StringUtils.isNotEmpty(entryPackage) &&
                            !entryPackage.startsWith("META-INF") &&
                            !entryPackage.startsWith("OSGI-INF") &&
                            !entryPackage.startsWith("OSGI-OPT") &&
                            !entryPackage.startsWith("WEB-INF") &&
                            !entryPackage.startsWith("org.osgi")) {
                        updateVersionLocationCounts(packageVersionCounts, jarFile.getCanonicalPath(), defaultVersion, specificationVersion, entryPackage);
                    }
                }
            }
        }
        jarInputStream.close();
    }

    private void updateVersionLocationCounts(Map<String, Map<String, VersionLocation>> packageVersionCounts,
                                             String originLocation,
                                             String newVersion,
                                             String specificationVersion,
                                             String packageName) throws IOException {
        // first check if we've already processed this package
        Map<String, VersionLocation> versionLocations = packageVersionCounts.get(packageName);
        if (versionLocations == null) {
            versionLocations = new HashMap<String, VersionLocation>();
        }

        VersionLocation existingVersionLocation = versionLocations.get(originLocation);
        if (existingVersionLocation != null) {
            existingVersionLocation.incrementCounter();
        } else {
            existingVersionLocation = new VersionLocation(originLocation, cleanupVersion(newVersion), specificationVersion);
            existingVersionLocation.incrementCounter();
        }
        versionLocations.put(originLocation, existingVersionLocation);

        packageVersionCounts.put(packageName, versionLocations);
    }

    private void scanJarDirectories(Map<String, Map<String, VersionLocation>> packageVersionCounts) throws IOException {
        if (jarDirectories == null || jarDirectories.size() == 0) {
            return;
        }
        for (String jarDirectory : jarDirectories) {
            File jarDirectoryFile = new File(jarDirectory);
            if (!jarDirectoryFile.exists() || !jarDirectoryFile.isDirectory()) {
                getLog().warn("Ignoring invalid directory " + jarDirectory + ".");
                continue;
            }
            getLog().info("Scanning JARs in directory " + jarDirectory + "...");
            DirectoryScanner ds = new DirectoryScanner();
            String[] includes = {"*.jar"};
            ds.setIncludes(includes);
            ds.setBasedir(jarDirectory);
            ds.setCaseSensitive(true);
            ds.scan();
            String[] includedFiles = ds.getIncludedFiles();
            for (String includeFile : includedFiles) {
                String version = null;
                File includedFileFile = new File(jarDirectoryFile, includeFile);
                String artifactFileName = includedFileFile.getName();
                Set<Artifact> relatedArtifacts = findArtifactsByArtifactId(artifactFileName);
                if (relatedArtifacts.size() > 1) {
                    getLog().warn("multiple matching dependencies found for artifactId " + artifactFileName);
                } else if (relatedArtifacts.size() == 1) {
                    version = relatedArtifacts.iterator().next().getBaseVersion();
                } else {
                    getLog().warn("Couldn't find dependency for artifactId " + artifactFileName);
                    // @todo let's try to extract the version from the file name.
                }

                scanJar(packageVersionCounts, includedFileFile, version);
            }
        }
    }

    private Set<Artifact> findArtifactsByArtifactId(String artifactId) {
        Set<Artifact> resultArtifacts = new HashSet<Artifact>();
        if (project == null) {
            return resultArtifacts;
        }
        if (StringUtils.isEmpty(artifactId)) {
            return resultArtifacts;
        }
        Set<Artifact> artifacts = project.getArtifacts();
        for (Artifact artifact : artifacts) {
            if (artifact.getType().equals("war")) {
                // we have a WAR dependency, we will look in that project dependencies seperately since it is not
                // directly transitive.
                Set<Artifact> warArtifacts = findInWarDependencies(artifact, artifactId);
                if (warArtifacts.size() > 0) {
                    resultArtifacts.addAll(warArtifacts);
                }
            } else if (artifact.getFile().getName().equals(artifactId)) {
                resultArtifacts.add(artifact);
            }
        }
        /*
        if (resultArtifacts.size() == 0) {
            getLog().warn("Couldn't find " + artifactId + ". Searched in: ");
            for (Artifact artifact : artifacts) {
                getLog().warn("- " + artifact.getGroupId() + ":" + artifact.getArtifactId() + " at " + artifact.getFile());
            }
        }
        */
        return resultArtifacts;
    }

    private Set<Artifact> findInWarDependencies(Artifact warArtifact, final String artifactId) {
        final Set<Artifact> matchingArtifacts = new HashSet<Artifact>();
        String artifactCoords = warArtifact.getGroupId() + ":" + warArtifact.getArtifactId() + ":" + warArtifact.getType() + ":" + warArtifact.getBaseVersion();
        DependencyNode node = null;
        if (resolvedDependencyNodes.containsKey(artifactCoords)) {
            node = resolvedDependencyNodes.get(artifactCoords);
        }

        if (node == null) {
            try {

                getLog().info("Resolving artifact " + artifactCoords + "...");
                ArtifactRequest request = new ArtifactRequest();
                request.setArtifact(
                        new DefaultArtifact(artifactCoords));
                request.setRepositories(remoteRepos);

                Dependency dependency =
                        new Dependency(new DefaultArtifact(artifactCoords), "compile");

                CollectRequest collectRequest = new CollectRequest();
                collectRequest.setRoot(dependency);
                collectRequest.setRepositories(remoteRepos);

                node = repoSystem.collectDependencies(repoSession, collectRequest).getRoot();

                DependencyRequest dependencyRequest = new DependencyRequest(node, null);

                repoSystem.resolveDependencies(repoSession, dependencyRequest);

                resolvedDependencyNodes.put(artifactCoords, node);

            } catch (DependencyCollectionException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (DependencyResolutionException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        if (node != null) {
            node.accept(new DependencyVisitor() {
                @Override
                public boolean visitEnter(DependencyNode node) {
                    if (node.getDependency().getArtifact().getFile().getName().equals(artifactId)) {
                        matchingArtifacts.add(RepositoryUtils.toArtifact(node.getDependency().getArtifact()));
                    }
                    return true;
                }

                @Override
                public boolean visitLeave(DependencyNode node) {
                    return true;
                }
            });

        }
        return matchingArtifacts;

    }

    // The following code was copied from the Maven Bundle Plugin code.
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

    static public String cleanupVersion(String version) {
        if (version != null) {
            StringBuffer result = new StringBuffer();
            Matcher m = FUZZY_VERSION.matcher(version);
            if (m.matches()) {
                String major = m.group(1);
                String minor = m.group(3);
                String micro = m.group(5);
                String qualifier = m.group(7);

                if (major != null) {
                    result.append(major);
                    if (minor != null) {
                        result.append(".");
                        result.append(minor);
                        if (micro != null) {
                            result.append(".");
                            result.append(micro);
                            if (qualifier != null) {
                                result.append(".");
                                cleanupModifier(result, qualifier);
                            }
                        } else if (qualifier != null) {
                            result.append(".0.");
                            cleanupModifier(result, qualifier);
                        } else {
                            result.append(".0");
                        }
                    } else if (qualifier != null) {
                        result.append(".0.0.");
                        cleanupModifier(result, qualifier);
                    } else {
                        result.append(".0.0");
                    }
                }
            } else {
                result.append("0.0.0.");
                cleanupModifier(result, version);
            }
            return result.toString();
        }

        return null;
    }

    static void cleanupModifier(StringBuffer result, String modifier) {
        for (int i = 0; i < modifier.length(); i++) {
            char c = modifier.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'
                    || c == '-')
                result.append(c);
            else
                result.append('_');
        }
    }

    // end of copied code.

}
