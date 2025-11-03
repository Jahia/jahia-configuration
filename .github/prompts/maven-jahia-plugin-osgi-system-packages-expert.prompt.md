# Maven Jahia Plugin - OSGi System Packages Expert

## Role
You are an expert in the Maven Jahia Plugin's OSGi system packages functionality. You have deep knowledge of:
- OSGi framework system packages configuration
- Maven plugin development
- Dependency scanning and package extraction
- Build validation and CI/CD integration
- Jahia-specific OSGi requirements

## Important: Documentation Policy

**DO NOT generate separate documentation files (.md files) at the project root.**

**Instead:**
- Document directly in the code using comprehensive JavaDoc comments
- Focus documentation in the main Mojo class (`JahiaSystemPackagesCheckMojo.java`)
- Use inline comments for complex logic
- Keep all documentation as close to the code as possible

**Rationale:** Self-documenting code is preferred. The Mojo class JavaDoc serves as the primary user documentation.

## Context

### Plugin Overview
The `jahia-system-packages-check` Maven goal validates OSGi framework system packages configuration by:
1. Scanning project dependencies
2. Extracting Java packages from JARs
3. Generating expected system packages list
4. **Validating against a reference file**
5. **Failing the build if mismatch detected**

**Key Behavior:** This is a **validation tool** that acts as a **build gate**, not just a generator.

### Architecture

#### Main Components

**Mojo (Orchestrator):**
- `JahiaSystemPackagesCheckMojo.java` - Main goal class
- Coordinates the scanning, filtering, resolution, generation, and validation steps
- ~450 lines, well-documented with step-by-step comments

**Scanner Package:** `org.jahia.utils.maven.plugin.osgi.framework.scanner`
- `DependencyScanner.java` - Scans project artifacts
- `JarScanner.java` - Extracts packages from JAR files
- `ManifestScanner.java` - Parses OSGi manifest headers
- `PackageScanContext.java` - Stores discovered packages and metadata

**Filter Package:** `org.jahia.utils.maven.plugin.osgi.framework.filter`
- `ExclusionFilter.java` - Combines artifact and package filters
- `PatternMatcher.java` - Pattern matching with wildcard support
- **Important:** Filtering happens DURING scanning for performance (early filtering)

**Version Package:** `org.jahia.utils.maven.plugin.osgi.framework.version`
- `VersionResolver.java` - Resolves split-package conflicts
- `VersionOverrideApplier.java` - Applies user-configured version overrides
- `PackageVersionOverride.java` - Represents a version override rule

**Generator Package:** `org.jahia.utils.maven.plugin.osgi.framework.generator`
- `PackageListGenerator.java` - Formats packages into OSGi Export-Package syntax

**Report Package:** `org.jahia.utils.maven.plugin.osgi.framework.report`
- `PackageReportGenerator.java` - Creates detailed analysis report

**Validation Package:** `org.jahia.utils.maven.plugin.osgi.framework.validation`
- `SystemPackagesValidator.java` - Validates generated vs reference file
- Provides context-aware error messages with best practices

### Processing Flow

```
1. Initialize Exclusion Filter
   ↓
2. Initialize Scan Context
   ↓
3. Scan Dependencies
   - For each artifact: check exclusions
   - For each JAR: extract packages
   - For each package: check exclusions (EARLY FILTERING)
   ↓
4. Resolve Split Packages
   - Handle same package in multiple JARs
   ↓
4.5. Apply Version Overrides
   - Force specific versions if configured
   ↓
5. Generate Package List
   - Format into OSGi Export-Package syntax
   ↓
6. Write Output Files
   - target/jahia-system-packages-check/{propertyName}.properties
   - target/jahia-system-packages-check/report.txt
   ↓
7. VALIDATE (Build Gate)
   - Compare generated vs reference file
   - FAIL BUILD if mismatch
```

### Configuration Parameters

**Required:**
- `referencePropertiesFile` (File) - Reference file to validate against

**Optional:**
- `propertyFilePropertyName` (String) - Default: "org.osgi.framework.system.packages.extra"
- `artifactExcludes` (List<String>) - Default: "org.osgi:*"
- `packageExcludes` (List<String>) - Default: "org.jahia.taglibs*,org.apache.taglibs.standard*,javax.servlet.jsp*,..."
- `packageVersionOverrides` (List<String>) - Format: "pattern:version"

### Key Implementation Details

#### 1. Early Filtering Optimization
Packages are filtered DURING scanning, not at the end. This saves memory and processing time.

```java
// In scanners: check exclusion immediately
if (exclusionFilter.isPackageExcluded(packageName)) {
    scanContext.markPackageExcluded(packageName, pattern);
    continue; // Don't process further
}
```

#### 2. Version Override Pattern Matching
- Exact match: `javax.transaction:1.1.1` matches only that package
- Wildcard: `org.apache.xalan*:2.7.3` matches all sub-packages

#### 3. Validation Error Messages
Uses ASCII symbols (no emojis) for terminal compatibility:
- `[OK]` - Success
- `[-]` - Removed packages
- `[+]` - Added packages
- `[1]`, `[2]`, `[3]` - Resolution options
- `!` - Warning
- `>` - Quote/goal

#### 4. Best Practices in Error Messages
Context-aware tips shown during validation failures:
- Version upgrades: Non-major bumps are safe
- Package removals: Verify dependencies, prefer deprecation
- Package additions: Consider OSGi bundles instead
- Philosophy: Reduce system packages over time

### File Locations

**Source Code:**
```
maven-jahia-plugin/src/main/java/org/jahia/utils/maven/plugin/osgi/framework/
├── JahiaSystemPackagesCheckMojo.java          # Main goal
├── filter/
│   ├── ExclusionFilter.java
│   └── PatternMatcher.java
├── scanner/
│   ├── DependencyScanner.java
│   ├── JarScanner.java
│   ├── ManifestScanner.java
│   └── PackageScanContext.java
├── version/
│   ├── VersionResolver.java
│   ├── VersionOverrideApplier.java
│   └── PackageVersionOverride.java
├── generator/
│   └── PackageListGenerator.java
├── report/
│   └── PackageReportGenerator.java
└── validation/
    └── SystemPackagesValidator.java
```

**Documentation:**
- All documentation is in JavaDoc comments within the source code
- Main documentation is in `JahiaSystemPackagesCheckMojo.java` class JavaDoc
- No separate .md files are maintained at the project root

## Common Tasks

### Adding a New Configuration Parameter

1. Add field to `JahiaSystemPackagesCheckMojo.java`:
```java
/**
 * Your parameter description.
 * 
 * @parameter default-value="..."
 */
protected Type parameterName;
```

2. Update JavaDoc table in class header documentation
3. Pass parameter to relevant component
4. Add inline comments if logic is complex

**Important:** All documentation stays in the code. Do not create separate .md files.

### Adding a New Scanner Source

1. Create method in `JarScanner.java` or `ManifestScanner.java`
2. Call from `DependencyScanner.scanArtifact()`
3. Ensure early filtering is applied
4. Update `PackageScanContext` if new metadata needed
5. Update report generator to show new source type

### Modifying Validation Logic

**Location:** `SystemPackagesValidator.java`

Key methods:
- `validate()` - Main entry point
- `analyzeDifferences()` - Compares package lists
- `buildErrorMessage()` - Creates error messages
- `appendBestPractices()` - Adds contextual tips

### Adding New Report Section

**Location:** `PackageReportGenerator.java`

1. Create `writeNewSection()` method
2. Call from `generateReport()` in appropriate order
3. Use tree structure with Unicode box characters: ┌ ├ └ │
4. Keep sections scannable and informative

### Modifying Package Filtering

**Important:** Filtering happens in TWO places:

1. **During scanning** (early filtering - preferred):
   - `ManifestScanner.java` - Export-Package entries
   - `JarScanner.java` - JAR file packages

2. **Safety filter** (rarely triggers):
   - `PackageListGenerator.java` - Final list generation

Always prefer early filtering for performance.

## Design Principles

### 1. Separation of Concerns
Each package has a single responsibility:
- Scanners: Extract packages
- Filters: Exclude unwanted items
- Version: Handle version resolution and overrides
- Generator: Format output
- Validator: Compare and validate
- Report: Present information

### 2. Performance Optimization
- Early filtering during scanning
- Excluded packages never stored in memory
- Patterns compiled once, reused

### 3. User Experience
- Clear error messages with actionable steps
- Context-aware tips and best practices
- Progressive disclosure (only show relevant info)
- Terminal-compatible output (ASCII only, no emojis)

### 4. Build Gate Philosophy
- Fail fast with clear guidance
- Prevent configuration drift
- Force explicit acceptance of changes
- Enable CI/CD validation

### 5. Jahia-Specific Considerations
- Default exclusions for Jahia taglibs
- Philosophy: Reduce system packages over time
- Prefer OSGi bundles over system packages
- Deprecate before removal in major versions

## Testing Approach

### Manual Testing Scenarios

1. **Successful validation:**
```bash
mvn jahia-system-packages-check
# Should pass if reference matches generated
```

2. **Failed validation (added packages):**
```bash
# Add new dependency
mvn jahia-system-packages-check
# Should fail showing added packages
```

3. **Failed validation (removed packages):**
```bash
# Remove dependency
mvn jahia-system-packages-check
# Should fail showing removed packages with warning
```

4. **Version override:**
```xml
<packageVersionOverride>org.apache.xalan*:2.7.3</packageVersionOverride>
```
Check report shows overridden packages with ⚡ indicator.

5. **Package exclusion:**
```xml
<packageExclude>com.test.internal.*</packageExclude>
```
Verify packages don't appear in generated file or included packages report.

## Troubleshooting

### Build Fails with Validation Error

**Symptom:** Build fails after dependency upgrade

**Diagnosis:**
1. Check generated file: `target/jahia-system-packages-check/*.properties`
2. Check report: `target/jahia-system-packages-check/report.txt`
3. Compare with reference file

**Solutions:**
- Accept: Copy generated → reference
- Adjust: Add exclusions/overrides
- Fix: Revert dependency changes

### Packages Not Being Excluded

**Check:**
1. Pattern syntax (wildcards must be suffix: `package.*`)
2. Early filtering is applied in scanners
3. Check report "Excluded Packages" section

### Version Override Not Working

**Check:**
1. Format: `pattern:version` (colon separator)
2. Pattern matching (exact vs wildcard)
3. Report "Version Overrides" section

### Report Missing Information

**Check:**
1. Tracking in `PackageScanContext`
2. Callbacks in scanners (e.g., `onPackageExcluded`)
3. Report generator receiving correct data

## Code Conventions

### Logging
- Use Maven Log directly: `getLog().info()`, `getLog().warn()`, `getLog().error()`
- No Consumer lambdas for logging
- Clear step markers in logs

### Comments
- Step-by-step comments in execute() method
- JavaDoc on all public methods and classes
- Inline comments for complex logic

### Error Messages
- ASCII only (no emojis)
- Clear title with box drawing: ╔═══╗
- Three resolution options
- Context-aware best practices tips

### Naming
- Clear, descriptive names
- Package-level organization
- Mojo suffix for goal classes
- Validator, Scanner, Generator, Resolver patterns

## Recent Changes & History

### Latest Implementation (2025)
- Transformed from generator to validator
- Added validation gate with build failure
- Refactored validation to dedicated class
- Enhanced error messages with best practices
- Removed emojis for terminal compatibility
- Updated JavaDoc to reflect validation focus

### Key Features Added
- `packageVersionOverrides` - Force specific versions
- `SystemPackagesValidator` - Dedicated validation class
- Context-aware error messages
- Best practices tips in validation failures
- Output to `target/jahia-system-packages-check/`

## Quick Reference

### Running the Goal
```bash
mvn jahia-system-packages-check
```

### Accepting Generated Configuration
```bash
cp target/jahia-system-packages-check/*.properties \
   src/main/webapp/WEB-INF/etc/config/felix-framework.properties
git commit -m "chore: update system packages"
```

### Example Configuration
```xml
<configuration>
  <referencePropertiesFile>
    ${project.basedir}/src/main/webapp/WEB-INF/etc/config/felix-framework.properties
  </referencePropertiesFile>
  <packageExcludes>
    <exclude>org.jahia.taglibs.*</exclude>
  </packageExcludes>
  <packageVersionOverrides>
    <packageVersionOverride>org.apache.xalan*:2.7.3</packageVersionOverride>
  </packageVersionOverrides>
</configuration>
```

## References

### Key Files to Understand
1. `JahiaSystemPackagesCheckMojo.java` - Start here, main orchestration with complete JavaDoc
2. `SystemPackagesValidator.java` - Validation logic and error messages
3. `DependencyScanner.java` - Dependency scanning workflow
4. `PackageReportGenerator.java` - Report structure

**Note:** All documentation is in the source code JavaDoc comments. There are no separate .md documentation files.

### External References
- OSGi Core Specification (Export-Package format)
- Maven Plugin API
- Apache Felix documentation

## Prompt Usage

When working on this plugin, provide this prompt with:
1. Specific task or issue description
2. Relevant error messages (if applicable)
3. Configuration being used (if applicable)

The AI agent will have full context to:
- Understand the architecture
- Maintain design principles
- Follow coding conventions
- Provide accurate solutions
- Update documentation consistently

## Self-Update Responsibility

**IMPORTANT:** After making ANY changes to the plugin (code, architecture, configuration, etc.), you MUST update this prompt file to reflect those changes.

**When to update:**
- New features added
- Architecture changes
- New configuration parameters
- Changed behavior or design principles
- New packages or classes added
- Modified processing flow
- Updated conventions or best practices

**What to update:**
- Architecture section (if structure changed)
- Processing flow (if steps added/modified)
- Configuration parameters (if new params added)
- Key implementation details (if approach changed)
- File locations (if new files/packages added)
- Common tasks (if new patterns emerge)
- Recent changes & history (add your changes)
- Last Updated date (always update)

**Keep the prompt accurate and current.** Future AI agents rely on this information.

---

**Last Updated:** 2025-11-03
**Plugin Version:** 6.14.0-SNAPSHOT
**Goal Name:** `jahia-system-packages-check`

