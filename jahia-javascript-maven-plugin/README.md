# Jahia JavaScript Maven Plugin

A Maven plugin for building JavaScript modules with custom packaging. This plugin integrates Yarn and Node.js into the Maven build lifecycle, allowing JavaScript/TypeScript projects to be built using standard Maven commands.

## Overview

The Jahia JavaScript Maven Plugin provides mojos (Maven goals) that are mapped to the Maven default lifecycle phases. Each mojo corresponds to a specific lifecycle phase and executes the appropriate Yarn command when applicable.

## Plugin Configuration

Add the plugin to your `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jahia.configuration</groupId>
            <artifactId>jahia-javascript-maven-plugin</artifactId>
            <version>6.15.0-SNAPSHOT</version>
            <extensions>true</extensions>
        </plugin>
    </plugins>
</build>
```

## Mojos and Lifecycle Mapping

The plugin's mojos are named and mapped to Maven lifecycle phases, with corresponding default Yarn commands:

| Mojo Name | Maven Lifecycle Phase | Default Yarn Command | Description |
|-----------|----------------------|---------------------|-------------|
| `install-node-and-yarn` | `initialize` | N/A | Installs Node.js and Yarn locally |
| `yarn-initialize` | `initialize` | `yarn install` | Installs dependencies |
| `sync-version` | `process-resources` | N/A | Syncs package.json version with Maven version |
| `yarn-clean` | `clean` | `yarn clean` | Cleans build artifacts |
| `yarn-package` | `package` | `yarn build` | Builds the project |
| `attach-artifact` | `package` | N/A | Attaches the built package to Maven |
| `yarn-verify` | `verify` | `yarn verify` | Runs verification/tests |
| `yarn-deploy` | `deploy` | `npm publish` | Publishes to npm registry |

## Common Parameters

All Yarn-based mojos inherit common parameters from `AbstractYarnMojo`:

### `workingDirectory`
- **Property:** N/A
- **Default:** `${project.basedir}`
- **Description:** Working directory where node and yarn will be installed and executed.

**Example:**
```xml
<configuration>
    <workingDirectory>${project.basedir}/frontend</workingDirectory>
</configuration>
```

## Mojo-Specific Parameters

### install-node-and-yarn

Installs Node.js and Yarn locally in the project.

#### Parameters:
- **`nodeVersion`**
  - **Property:** `nodeVersion`
  - **Default:** `v22.21.1`
  - **Description:** Node.js version to install.

- **`yarnVersion`**
  - **Property:** `yarnVersion`
  - **Default:** `v1.22.22`
  - **Description:** Yarn version to install.

**Example:**
```xml
<plugin>
    <groupId>org.jahia.configuration</groupId>
    <artifactId>jahia-javascript-maven-plugin</artifactId>
    <version>6.15.0-SNAPSHOT</version>
    <configuration>
        <nodeVersion>v20.10.0</nodeVersion>
        <yarnVersion>v1.22.19</yarnVersion>
    </configuration>
</plugin>
```

**Command line:**
```bash
mvn jahia-javascript-module:install-node-and-yarn -DnodeVersion=v20.10.0
```

---

### yarn-clean

Executes a Yarn clean command.

#### Parameters:
- **`command`**
  - **Property:** `yarnClean.command`
  - **Default:** `clean`
  - **Description:** Custom Yarn command to execute instead of the default.

**Example:**
```xml
<configuration>
    <command>clean --force</command>
</configuration>
```

**Command line:**
```bash
mvn clean -DyarnClean.command="clean --force"
```

---

### yarn-initialize

Installs project dependencies using Yarn.

#### Parameters:
- **`command`**
  - **Property:** `yarnInitialize.command`
  - **Default:** `install`
  - **Description:** Custom Yarn command to execute instead of the default.

**Example:**
```xml
<configuration>
    <command>install --frozen-lockfile</command>
</configuration>
```

**Command line:**
```bash
mvn initialize -DyarnInitialize.command="install --frozen-lockfile"
```

---

### sync-version

Synchronizes the `package.json` version with the Maven project version.

#### Parameters:
- **`workingDirectory`**
  - **Property:** N/A
  - **Default:** `${project.basedir}`
  - **Description:** Directory containing the package.json file.

**Example:**
```xml
<configuration>
    <workingDirectory>${project.basedir}</workingDirectory>
</configuration>
```

**Notes:**
- This mojo automatically updates the `version` field in `package.json` to match the Maven `<version>`.
- Useful for keeping versions in sync between Maven and npm/Yarn ecosystems.

---

### yarn-package

Builds the JavaScript project (typically runs a build script).

#### Parameters:
- **`command`**
  - **Property:** `yarnPackage.command`
  - **Default:** `build`
  - **Description:** Custom Yarn command to execute instead of the default.

**Example:**
```xml
<configuration>
    <command>build:production</command>
</configuration>
```

**Command line:**
```bash
mvn package -DyarnPackage.command="build:production"
```

---

### attach-artifact

Attaches the built package (`.tgz` file) as the Maven artifact.

#### Parameters:
- **`packageFile`**
  - **Property:** `packageFile`
  - **Default:** `${project.basedir}/dist/package.tgz`
  - **Description:** Path to the package.tgz file to attach.

**Example:**
```xml
<configuration>
    <packageFile>${project.build.directory}/my-package.tgz</packageFile>
</configuration>
```

**Command line:**
```bash
mvn package -DpackageFile=./build/package.tgz
```

---

### yarn-verify

Runs verification tasks (tests, linting, etc.).

#### Parameters:
- **`command`**
  - **Property:** `yarnVerify.command`
  - **Default:** `verify`
  - **Description:** Custom Yarn command to execute instead of the default.

**Example:**
```xml
<configuration>
    <command>test --coverage</command>
</configuration>
```

**Command line:**
```bash
mvn verify -DyarnVerify.command="test --coverage"
```

---

### yarn-deploy

Publishes the package to an npm registry.

#### Parameters:
- **`access`**
  - **Property:** `yarnDeploy.access`
  - **Default:** `public`
  - **Description:** Access level for npm publish. Valid values: `public` or `private`.

- **`snapshotTag`**
  - **Property:** `yarnDeploy.snapshotTag`
  - **Default:** `alpha`
  - **Description:** Tag to use for SNAPSHOT versions (non-SNAPSHOT versions use `latest`).

**Example:**
```xml
<configuration>
    <access>private</access>
    <snapshotTag>beta</snapshotTag>
</configuration>
```

**Command line:**
```bash
mvn deploy -DyarnDeploy.access=private -DyarnDeploy.snapshotTag=beta
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
1. Clean the project (yarn clean)
2. Install Node.js and Yarn
3. Install dependencies (yarn install)
4. Sync package.json version
5. Build the project (yarn build)
6. Attach the package artifact

### Custom Build Command

```bash
mvn package -DyarnPackage.command="build:prod"
```

### Skip Tests During Build

```bash
mvn package -Dmaven.test.skip=true
```

### Deploy to Private Registry

```bash
mvn deploy -DyarnDeploy.access=private
```

### Use Different Node/Yarn Versions

```bash
mvn install -DnodeVersion=v18.19.0 -DyarnVersion=v1.22.21
```

## Integration with Maven Lifecycle

The plugin follows the standard Maven lifecycle:

```
clean -> initialize -> process-resources -> package -> verify -> deploy
   ↓          ↓              ↓                 ↓         ↓         ↓
yarn clean  yarn install  sync-version   yarn build  yarn verify  npm publish
```

## Goal Prefix

The plugin uses the goal prefix `jahia-javascript-module`. You can execute individual goals:

```bash
mvn jahia-javascript-module:install-node-and-yarn
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
