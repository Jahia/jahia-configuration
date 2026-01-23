# Jahia JavaScript Maven Plugin

A Maven plugin for building JavaScript modules with custom packaging. This plugin integrates Yarn and Node.js into the Maven build
lifecycle, allowing JavaScript/TypeScript projects to be built using standard Maven commands.

## Overview

The Jahia JavaScript Maven Plugin provides mojos (Maven goals) that are mapped to the Maven default lifecycle phases. Each mojo corresponds
to a specific lifecycle phase and executes the appropriate Yarn command when applicable.

## Plugin Configuration

Add the plugin to your `pom.xml`:

```xml

<build>
    <plugins>
        <plugin>
            <groupId>org.jahia.configuration</groupId>
            <artifactId>jahia-javascript-maven-plugin</artifactId>
            <version>${jahia-javascript-maven-plugin.version}</version> <!-- Replace with the plugin version -->
            <extensions>true</extensions>
        </plugin>
    </plugins>
</build>
```

## Mojos and Lifecycle Mapping

The plugin's mojos are named and mapped to Maven lifecycle phases, with corresponding default Yarn commands:

| Mojo Name                   | Maven Lifecycle Phase | Default Yarn Command | Optional? | Description                                   |
|-----------------------------|-----------------------|----------------------|-----------|-----------------------------------------------|
| `install-node-and-corepack` | `initialize`          | N/A                  | N/A       | Installs Node.js and Corepack locally         |
| `yarn-initialize`           | `initialize`          | `yarn install`       | No        | Installs dependencies                         |
| `sync-version`              | `process-resources`   | N/A                  | N/A       | Syncs package.json version with Maven version |
| `yarn-clean`                | `clean`               | `yarn clean`         | Yes       | Cleans build artifacts                        |
| `yarn-package`              | `package`             | `yarn package`       | No        | Builds the project                            |
| `attach-artifact`           | `package`             | N/A                  | N/A       | Attaches the built package to Maven           |
| `yarn-verify`               | `verify`              | `yarn verify`        | Yes       | Runs verification/tests                       |
| `yarn-deploy`               | `deploy`              | `npm publish`        | No        | Publishes to npm registry                     |

### Optional Commands

Commands marked as **"Optional"** have special behavior:

- If you **don't specify a custom command** via the mojo's `command` parameter, the plugin will check if the default script exists in your
  `package.json`
- If the script is **not found**, the mojo execution will be **skipped gracefully** with an info message
- If the script **is found**, it will be executed normally

This allows projects to work with the plugin even if they don't define all optional scripts. For example:

- A project without a `verify` script in `package.json` will skip the `yarn-verify` mojo without failing
- A project without a `clean` script will skip the `yarn-clean` mojo without failing

**Required commands** (marked as "No") will **fail the build** if the script is not found in `package.json`.

## Common Parameters

All Mojos inherit common parameters from `AbstractYarnMojo`:

### `workingDirectory`

- **Property:** `jahia.js.workingDirectory`
- **Default:** `${project.basedir}`
- **Description:** Working directory where node and yarn will be installed and executed.

**Example:**

```xml

<configuration>
    <workingDirectory>${project.basedir}/frontend</workingDirectory>
</configuration>
```

## Mojo-Specific Parameters

### install-node-and-corepack

Installs Node.js and Corepack locally in the project.

#### Parameters:

- **`nodeVersion`**
    - **Property:** `jahia.js.nodeVersion`
    - **Default:** `v22.21.1`
    - **Description:** Node.js version to install.

- **`corepackVersion`**
    - **Property:** `jahia.js.corepackVersion`
    - **Default:** `0.34.5`
    - **Description:** Corepack version to install.

**Example:**

```xml

<plugin>
    <groupId>org.jahia.configuration</groupId>
    <artifactId>jahia-javascript-maven-plugin</artifactId>
    <version>6.15.0-SNAPSHOT</version>
    <configuration>
        <nodeVersion>v20.10.0</nodeVersion>
        <corepackVersion>0.32.0</corepackVersion>
    </configuration>
</plugin>
```

> **Note:** As Corepack is used, you **must** include a `packageManager` field in your `package.json`, e.g.,
`"packageManager": "yarn@4.9.4"`.

---

### yarn-clean

Executes a Yarn clean command.

**This is an optional command.** If you don't specify a custom command and the `clean` script is not defined in your `package.json`, this
mojo will be skipped without failing the build.

#### Parameters:

- **`yarnCleanCommand`**
    - **Property:** `jahia.js.yarnClean.command`
    - **Default:** `clean`
    - **Description:** Custom Yarn command to execute instead of the default.

**Example:**

```xml

<configuration>
    <yarnCleanCommand>myCustomClean</yarnCleanCommand>
</configuration>
```

---

### yarn-initialize

Installs project dependencies using a `yarn install` command.

#### Parameters:

None.

---

### sync-version

Synchronizes the `package.json` version with the Maven project version, which is useful in particular during the release process.

#### Parameters:

None.

---

### yarn-package

Builds the JavaScript project (typically runs a build script).

#### Parameters:

- **`yarnPackageCommand`**
    - **Property:** `jahia.js.yarnPackage.command`
    - **Default:** `package`
    - **Description:** Custom Yarn command to execute instead of the default.

**Example:**

```xml

<configuration>
    <yarnPackageCommand>build:production</yarnPackageCommand>
</configuration>
```

---

### attach-artifact

Attaches the built package (`.tgz` file) as the Maven artifact.

#### Parameters:

- **`packageFile`**
    - **Property:** `jahia.js.packageFile`
    - **Default:** `dist/package.tgz` (relative to the working directory)
    - **Description:** Path to the package.tgz file to attach, can be absolute or relative to the working directory.

**Example:**

```xml

<configuration>
    <packageFile>${project.build.directory}/my-package.tgz</packageFile>
</configuration>
```

---

### yarn-verify

Runs verification tasks (tests, linting, etc.).

**This is an optional command.** If you don't specify a custom command and the `verify` script is not defined in your `package.json`, this
mojo will be skipped without failing the build.

#### Parameters:

- **`yarnVerifyCommand`**
    - **Property:** `jahia.js.yarnVerify.command`
    - **Default:** `verify`
    - **Description:** Custom Yarn command to execute instead of the default.

**Example:**

```xml

<configuration>
    <yarnVerifyCommand>tests:unittest</yarnVerifyCommand>
</configuration>
```

---

### yarn-deploy

Publishes the package to a npm registry.

#### Parameters:

- **`yarnDeployAccess`**
    - **Property:** `jahia.js.yarnDeploy.access`
    - **Default:** `public`
    - **Description:** Access level for npm publish. Valid values: `public` or `private`.

- **`yarnDeploySnapshotTag`**
    - **Property:** `jahia.js.yarnDeploy.snapshotTag`
    - **Default:** `alpha`
    - **Description:** Tag to use for SNAPSHOT versions (non-SNAPSHOT versions use `latest`).

**Example:**

```xml

<configuration>
    <yarnDeployAccess>private</yarnDeployAccess>
    <yarnDeploySnapshotTag>beta</yarnDeploySnapshotTag>
</configuration>
```

**Notes:**

- Automatically appends `--provenance` flag for npm publish
- SNAPSHOT versions are published with the configured `snapshotTag`
- Non-SNAPSHOT versions are published with the `latest` tag

---

## Usage Examples

### Basic Build

```bash
mvn clean install
```

This will:

1. Clean the project (yarn clean - **skipped if no `clean` script in package.json**)
2. Install Node.js and Corepack
3. Install dependencies (yarn install)
4. Sync package.json version
5. Build the project (yarn package)
6. Attach the package artifact
7. Run verification (yarn verify - **skipped if no `verify` script in package.json**)

### Custom Build Command

```bash
mvn package -Djahia.js.yarnPackage.command="build:prod"
```

### Use Different Node/Yarn Versions

```bash
mvn install -Djahia.js.nodeVersion=v20.10.0 -Djahia.js.corepackVersion=0.32.0
```

## Goal Prefix

The plugin uses the goal prefix `jahia-javascript-module`. You can execute individual goals:

```bash
mvn jahia-javascript-module:install-node-and-corepack
mvn jahia-javascript-module:yarn-initialize
mvn jahia-javascript-module:sync-version
mvn jahia-javascript-module:yarn-clean
mvn jahia-javascript-module:yarn-package
mvn jahia-javascript-module:attach-artifact
mvn jahia-javascript-module:yarn-verify
mvn jahia-javascript-module:yarn-deploy
```

## Requirements

- Maven 3.6 or higher
- Java 11 or higher

## Dependencies

The plugin internally uses:

- `frontend-maven-plugin` (1.15.4) - For Node.js and Yarn installation and execution
- `jackson-databind` (3.0.3) - For JSON parsing and manipulation
- `mojo-executor` (2.4.0) - For executing Maven plugin goals programmatically

## Notes

- Node.js and Yarn are installed locally in the project directory, not globally
- The plugin expects a `package.json` file in the working directory
- All Yarn commands can be customized via their respective `command` parameters
- The plugin follows Maven conventions for artifact management and deployment

## Development

### Building the Plugin

To build the plugin from source:

```bash
mvn clean install
```

### Running Integration Tests

The plugin includes integration tests that validate all mojos work correctly. Integration tests are automatically executed during the build.

To run only the integration tests:

```bash
mvn invoker:run
```

To skip integration tests during build:

```bash
mvn clean install -Dinvoker.skip=true
```

### Integration Tests

See [Integration Tests](./src/it/README.md) for more details.
