import java.io.*
import java.nio.file.*

// Get the base directory of the test project
def basedir = new File(basedir as String)
def workingDirectory = new File(basedir, "frontend") // custom working directory
def buildLog = new File(basedir, "build.log")

println "Validating integration test results..."
println "Path: ${basedir}"
println "Path: ${buildLog.getAbsolutePath()}"
// Check if build.log exists
assert buildLog.exists() : "build.log does not exist"

def logContent = buildLog.text

// validate the build was successful
assert logContent.contains("BUILD SUCCESS") : "The build was not successful"

// Verify that all mojos were executed
def mojoNames = [
    "install-node-and-corepack",
    "yarn-initialize",
    "sync-version",
    "yarn-package",
    "attach-artifact",
    "yarn-verify"
]

mojoNames.each { mojoName ->
    def message = ":${mojoName} (default-${mojoName})"
    assert logContent.contains(message) : "Build log does not contain expected execution of one mojo: ${message}"
    println "✓ Found execution of: ${message}"
}

// Verify that Node.js, Corepack and Yarn were installed
assert logContent.contains("Installing node version v20.10.0") : "Missing installation message of Node.js"
assert logContent.contains("Installing corepack version 0.32.0") : "Missing installation message of Corepack"
def nodeDir = new File(workingDirectory, "node")
assert nodeDir.exists() && nodeDir.isDirectory() : "Node.js was not installed (node/ directory missing)"
println "✓ Node.js installed successfully"
def yarnDir = new File(workingDirectory, ".yarn")
assert (yarnDir.exists() && yarnDir.isDirectory()) : "Yarn was not installed (.yarn/ directory missing)"
println "✓ Yarn installed successfully"

// Verify that 'yarn install' was executed
def nodeModulesDir = new File(workingDirectory, "node_modules")
assert nodeModulesDir.exists() : "Dependencies were not installed (node_modules directory missing)"
println "✓ Dependencies installed successfully"

// Verify that package.json version was synced
def packageJson = new File(workingDirectory, "package.json")
assert packageJson.exists() : "package.json does not exist"
def packageJsonContent = new groovy.json.JsonSlurper().parse(packageJson)
assert packageJsonContent.version == "1.0.0-SNAPSHOT" : "package.json version was not synced correctly. Expected: 1.0.0-SNAPSHOT, Got: ${packageJsonContent.version}"
println "✓ package.json version synced successfully: ${packageJsonContent.version}"

// Verify that 'myClean' was executed
assert logContent.contains("Custom clean command executed!") : "Yarn clean command was not executed or failed"
println "✓ Clean executed successfully"

// Verify that 'yarn package' was executed
def packageTgz = new File(workingDirectory, "custom/path/to/out.tgz")
assert packageTgz.exists() : "Package archive (custom/path/to/out.tgz) was not created"
println "✓ Package archive created"

// Verify that the package.tgz was attached
assert logContent.contains('full-customization/frontend/custom/path/to/out.tgz attached successfully') : "package.tgz did not get attached to the Maven project"
println "✓ Package archive attached"


// Verify that 'myVerify' was executed
assert logContent.contains("Custom verify command executed!") : "Yarn verify command was not executed or failed"
println "✓ Verification executed successfully"

println ""
println "============================================"
println "✓ All integration test validations passed! ✓"
println "============================================"
println ""

return true
