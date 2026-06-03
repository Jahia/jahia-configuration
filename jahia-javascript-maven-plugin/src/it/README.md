# Integration Tests for Jahia JavaScript Maven Plugin

## Overview

This directory contains integration tests for the jahia-javascript-maven-plugin. The tests use the Maven Invoker Plugin to execute Maven builds against test projects and validate the results.

## Test Structure

Each integration test is a separate directory under `src/it/` containing:

- **pom.xml** - Maven project configuration for the test
- **package.json** - JavaScript project configuration
- **verify.groovy** - Groovy script to validate test results
- **README.md** - Documentation for the specific test
- **.gitignore** - Ignores generated files

## Available Tests

### minimal

The simplest integration test with minimal configuration. Uses only the plugin's default settings with `<extensions>true</extensions>`.

**What it tests:**
- Default plugin behavior with no custom configuration
- All mojos execute with their default parameters
- Validates the "happy path" with zero configuration

**Location:** `src/it/minimal/`

### basic-yarn-berry

Standard integration test with basic Yarn Berry configuration. Validates that all plugin mojos execute correctly in a standard build lifecycle.

**What it tests:**
- Default lifecycle execution with Yarn Berry (Yarn 4.x)
- Corepack integration with `packageManager` field in package.json
- Version synchronization between Maven and package.json
- Optional mojos (clean, verify) with custom scripts

**Location:** `src/it/basic-yarn-berry/`

### full-customization

Advanced integration test demonstrating all customization options available in the plugin.

**What it tests:**
- Custom `workingDirectory` parameter (frontend subdirectory)
- Custom Node.js and Corepack versions
- Custom command names for all mojos (myClean, myPackage, myVerify)
- Custom package file path
- All plugin parameters working together

**Location:** `src/it/full-customization/`
