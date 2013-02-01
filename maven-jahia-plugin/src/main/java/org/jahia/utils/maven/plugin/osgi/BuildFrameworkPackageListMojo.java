package org.jahia.utils.maven.plugin.osgi;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.tika.io.IOUtils;
import org.codehaus.plexus.util.DirectoryScanner;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.*;
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
 * This maven goal will use a MANIFEST.MF file generated by the Maven Bundle plugin to build the list of system
 * packages that is exposed by the OSGi framework by default.
 *
 * @goal buildFrameworkPackageList
 * @requiresDependencyResolution runtime
 */
public class BuildFrameworkPackageListMojo extends AbstractMojo {

    public static final String VERSION_NUMBER_PATTERN_STRING = "([\\d\\.]*\\d)(.*)";
    private static final Pattern VERSION_NUMBER_PATTERN = Pattern.compile(VERSION_NUMBER_PATTERN_STRING);

    /**
     * @parameter default-value="${project.build.directory}/classes/META-INF/MANIFEST.MF"
     */
    protected File inputManifestFile;

    /**
     * @parameter default-value="${project.build.directory}"
     */
    protected File output;

    /**
     * @parameter default-value="${project.build.directory}/${project.build.finalName}/WEB-INF/lib"
     */
    protected List<String> jarDirectories;

    /**
     * @parameter default-value="false"
     */
    protected boolean scanDependencies = false;

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

    private Map<String, DependencyNode> resolvedDependencyNodes = new HashMap<String, DependencyNode>();

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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        FileInputStream in = null;
        Map<String, Map<String, VersionLocation>> packageVersionCounts = new TreeMap<String, Map<String, VersionLocation>>();
        Map<String, String> packageVersions = new TreeMap<String, String>();
        String generatedPackageList = null;
        try {
            if (project != null) {
                // first let's scan the dependencies

                if (scanDependencies) {
                    scanDependencies(packageVersionCounts);
                }

                // now let's scan the current project. Assuming it's been built already.

                scanClassesBuildDirectory(packageVersionCounts);

                scanJarDirectories(packageVersionCounts);

                scanJarDirectories(packageVersionCounts);

                resolveSplitPackages(packageVersionCounts, packageVersions);
            }

            if (inputManifestFile.exists()) {
                in = new FileInputStream(inputManifestFile);
                Manifest mf = new Manifest(in);
                String exportPackageStr = mf.getMainAttributes().getValue("Export-Package");
                String bundleVersion = mf.getMainAttributes().getValue("Bundle-Version");
                ManifestElement[] manifestElements = ManifestElement.parseHeader("Export-Package", exportPackageStr);
                for (ManifestElement manifestElement : manifestElements) {
                    String value = manifestElement.getValue();
                    String version = manifestElement.getAttribute("version");
                    if (version != null) {
                        if (version.equals(bundleVersion)) {
                            if (value.startsWith("org.jahia")) {
                                packageVersions.put(value, version);
                            } else {
                                if (!packageVersions.containsKey(value)) {
                                    packageVersions.put(value, null);
                                }
                            }
                        } else {
                            packageVersions.put(value, version);
                        }
                    } else {
                        if (!packageVersions.containsKey(value)) {
                            packageVersions.put(value, null);
                        }
                    }
                }
                System.out.println("Found " + manifestElements.length + " package exports.");
            }
            StringBuffer generatedPackageBuffer = new StringBuffer();
            for (Map.Entry<String, String> packageVersion : packageVersions.entrySet()) {
                StringBuffer packageExport = new StringBuffer();
                packageExport.append(packageVersion.getKey());
                if (packageVersion.getValue() != null) {
                    // @todo we should perform parent lookup here and re-use version if activated.
                    packageExport.append(";version=\"");
                    String versionString = packageVersion.getValue();
                    Matcher versionMatcher = VERSION_NUMBER_PATTERN.matcher(versionString);
                    if (versionMatcher.matches()) {
                        versionString = versionMatcher.group(1);
                    }
                    packageExport.append(versionString);
                    packageExport.append("\"");
                }
                packageExport.append(",");
                generatedPackageBuffer.append(packageExport);
                System.out.println("    " + packageExport + "\\");
            }
            generatedPackageList = generatedPackageBuffer.toString();
            generatedPackageList = generatedPackageList.substring(generatedPackageList.length() - 1); // remove the last comma
            System.out.println("Found " + packageVersions.size() + " packages in dependencies.");
            if (generatedPackageList != null && project != null) {
                project.getProperties().put("jahiaGeneratedFrameworkPackageList", generatedPackageList);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (BundleException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private void resolveSplitPackages(Map<String, Map<String, VersionLocation>> packageVersionCounts, Map<String, String> packageVersions) {
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
                System.out.println("Split-package with different versions detected for package " + resolvedPackageVersion.getKey() + ":");
            }
            for (Map.Entry<String, VersionLocation> versionLocationEntry : resolvedPackageVersion.getValue().entrySet()) {
                if (resolvedPackageVersion.getValue().size() > 1 && !allVersionsEqual) {
                    System.out.println("  - " + versionLocationEntry.getKey() + " v" + versionLocationEntry.getValue().getVersion() + " count=" + versionLocationEntry.getValue().getCounter() + " Specification-Version=" + versionLocationEntry.getValue().getSpecificationVersion());
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
            }
            packageVersions.put(resolvedPackageVersion.getKey(), highestVersionLocation.getVersion());
            if (resolvedPackageVersion.getValue().size() > 1 && !allVersionsEqual) {
                System.out.println("--> " + resolvedPackageVersion.getKey() + " v" + highestVersionLocation.getVersion());
            }
        }
    }

    private void scanClassesBuildDirectory(Map<String, Map<String, VersionLocation>> packageVersionCounts) throws IOException {
        File outputDirectoryFile = new File(project.getBuild().getOutputDirectory());
        System.out.println("Scanning project build directory " + outputDirectoryFile.getCanonicalPath());
        DirectoryScanner ds = new DirectoryScanner();
        String[] excludes = {"META-INF/**", "OSGI-INF/**", "OSGI-OPT/**", "WEB-INF/**"};
        ds.setExcludes(excludes);
        ds.setBasedir(outputDirectoryFile);
        ds.setCaseSensitive(true);
        ds.scan();
        String[] includedFiles = ds.getIncludedFiles();
        for (String includedFile : includedFiles) {
            // System.out.println("Processing file " + includedFile + "...");
            String entryPackage = "";
            int lastSlash = includedFile.lastIndexOf("/");
            if (lastSlash > -1) {
                entryPackage = includedFile.substring(0, lastSlash);
                entryPackage = entryPackage.replaceAll("/", ".");
                if (StringUtils.isNotEmpty(entryPackage) &&
                        !entryPackage.startsWith("META-INF") &&
                        !entryPackage.startsWith("OSGI-INF") &&
                        !entryPackage.startsWith("OSGI-OPT") &&
                        !entryPackage.startsWith("WEB-INF")) {
                    updateVersionLocationCounts(packageVersionCounts, project.getBuild().getFinalName(), project.getVersion(), null, entryPackage);
                }
            }
        }
    }

    private void scanDependencies(Map<String, Map<String, VersionLocation>> packageVersionCounts) throws IOException {
        System.out.println("Scanning project dependencies...");
        for (Artifact artifact : project.getArtifacts()) {
            if (!artifact.getType().equals("jar")) {
                System.out.println("Ignoring artifact " + artifact.getFile() + " since it is of type " + artifact.getType());
                continue;
            }
            scanJar(packageVersionCounts, artifact.getFile(), artifact.getBaseVersion());
        }
    }

    private void scanJar(Map<String, Map<String, VersionLocation>> packageVersionCounts, File jarFile, String defaultVersion) throws IOException {
        JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile));
        Manifest jarManifest = jarInputStream.getManifest();
        // Map<String, String> manifestVersions = new HashMap<String,String>();
        String specificationVersion = null;
        if (jarManifest == null) {
            System.out.println("Warning: no MANIFEST.MF file found for dependency " + jarFile);
        } else {
            if (jarManifest.getMainAttributes() == null) {
                System.out.println("Warning: no main attributes found in MANIFEST.MF file found for dependency " + jarFile);
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
                            packageName.startsWith("WEB-INF")) {
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
                        System.out.println("Found package version in "+jarFile.getName()+" MANIFEST : " + packageName + " v" + packageVersion);
                        updateVersionLocationCounts(packageVersionCounts, jarFile.getCanonicalPath(), packageVersion, specificationVersion, packageName);
                        // manifestVersions.put(packageName, packageVersion);
                    }
                }
            }
        }
        JarEntry jarEntry = null;
        // System.out.println("Processing file " + artifact.getFile() + "...");
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
                            !entryPackage.startsWith("WEB-INF")) {
                        updateVersionLocationCounts(packageVersionCounts, jarFile.getCanonicalPath(), defaultVersion, specificationVersion, entryPackage);
                    }
                }
            }
        }
        jarInputStream.close();
    }

    private void updateVersionLocationCounts(Map<String, Map<String, VersionLocation>> packageVersionCounts,
                                             String location,
                                             String defaultVersion,
                                             String specificationVersion,
                                             String entryPackage) throws IOException {
        Map<String, VersionLocation> versionLocations = null;
        if (packageVersionCounts.containsKey(entryPackage)) {
            versionLocations = packageVersionCounts.get(entryPackage);
        } else {
            versionLocations = new HashMap<String, VersionLocation>();
        }
        VersionLocation existingVersionLocation = versionLocations.get(location);
        if (existingVersionLocation != null) {
            existingVersionLocation.incrementCounter();
        } else {
            existingVersionLocation = new VersionLocation(location, defaultVersion, specificationVersion);
            existingVersionLocation.incrementCounter();
        }
        versionLocations.put(location, existingVersionLocation);

        packageVersionCounts.put(entryPackage, versionLocations);
    }

    private void scanJarDirectories(Map<String, Map<String, VersionLocation>> packageVersionCounts) throws IOException {
        if (jarDirectories == null || jarDirectories.size() == 0) {
            return;
        }
        for (String jarDirectory : jarDirectories) {
            File jarDirectoryFile = new File(jarDirectory);
            if (!jarDirectoryFile.exists() || !jarDirectoryFile.isDirectory()) {
                System.out.println("Ignoring invalid directory " + jarDirectory + ".");
                continue;
            }
            System.out.println("Scanning JARs in directory " + jarDirectory + "...");
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
                    System.out.println("Warning : multiple matching dependencies found for artifactId " + artifactFileName);
                } else if (relatedArtifacts.size() == 1) {
                    version = relatedArtifacts.iterator().next().getBaseVersion();
                } else {
                    System.out.println("Couldn't find dependency for artifactId " + artifactFileName);
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
            System.out.println("Couldn't find " + artifactId + ". Searched in: ");
            for (Artifact artifact : artifacts) {
                System.out.println("- " + artifact.getGroupId() + ":" + artifact.getArtifactId() + " at " + artifact.getFile());
            }
        }
        */
        return resultArtifacts;
    }

    private Set<Artifact> findInWarDependencies(Artifact warArtifact, final String artifactId) {
        final Set<Artifact> matchingArtifacts = new HashSet<Artifact>();
        ArtifactRequest request = new ArtifactRequest();
        String artifactCoords = warArtifact.getGroupId() + ":" + warArtifact.getArtifactId() + ":" + warArtifact.getType() + ":" + warArtifact.getBaseVersion();
        DependencyNode node = null;
        if (resolvedDependencyNodes.containsKey(artifactCoords)) {
            node = resolvedDependencyNodes.get(artifactCoords);
        }

        if (node == null) {
            try {

                getLog().info("Resolving artifact " + artifactCoords + "...");
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
}
