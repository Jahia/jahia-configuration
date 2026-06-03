import java.io.*
import java.nio.file.*

// Get the base directory of the test project
def basedir = new File(basedir as String)
def buildLog = new File(basedir, "build.log")

println "Validating integration test results..."

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
    "attach-artifact"
]

mojoNames.each { mojoName ->
    def message = ":${mojoName} (default-${mojoName})"
    assert logContent.contains(message) : "Build log does not contain expected execution of one mojo: ${message}"
    println "✓ Found execution of: ${message}"
}

// Verify that Node.js, Corepack and Yarn were installed
assert logContent.contains("Installing node version v22.21.1") : "Missing installation message of Node.js"
assert logContent.contains("Installing corepack version 0.34.5") : "Missing installation message of Corepack"
def nodeDir = new File(basedir, "node")
assert nodeDir.exists() && nodeDir.isDirectory() : "Node.js was not installed (node/ directory missing)"
println "✓ Node.js installed successfully"
def yarnDir = new File(basedir, ".yarn")
assert yarnDir.exists() && yarnDir.isDirectory() : "Yarn was not installed (.yarn/ directory missing)"
println "✓ Yarn installed successfully"

// Verify that 'yarn install' was executed
def nodeModulesDir = new File(basedir, "node_modules")
assert nodeModulesDir.exists() : "Dependencies were not installed (node_modules directory missing)"
println "✓ Dependencies installed successfully"

// Verify that package.json version was synced
def packageJson = new File(basedir, "package.json")
assert packageJson.exists() : "package.json does not exist"
def packageJsonContent = new groovy.json.JsonSlurper().parse(packageJson)
assert packageJsonContent.version == "1.0.0-SNAPSHOT" : "package.json version was not synced correctly. Expected: 1.0.0-SNAPSHOT, Got: ${packageJsonContent.version}"
println "✓ package.json version synced successfully: ${packageJsonContent.version}"

// Verify that 'yarn package' was executed
def packageTgz = new File(basedir, "dist/package.tgz")
assert packageTgz.exists() && packageTgz.isFile() : "Package archive (dist/package.tgz) was not created"
println "✓ Package archive created"

// Verify that the package.tgz was attached
assert logContent.contains('minimal/dist/package.tgz attached successfully') : "package.tgz did not get attached to the Maven project"
println "✓ Package archive attached"

println ""
println "============================================"
println "✓ All integration test validations passed! ✓"
println "============================================"
println ""

return true
