package org.jahia.utils.maven.plugin.osgi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jahia.utils.maven.plugin.AetherAwareMojo;

/**
 * A little utility goal to locate a package inside the project's dependencies, including optional or provided ones.
 *
 * @goal find-packages
 * @requiresDependencyResolution test
 */

public class FindPackagesMojo extends AetherAwareMojo {

    /**
     * @parameter default-value="${packageNames}"
     */
    protected List<String> packageNames = new ArrayList<String>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (packageNames == null || packageNames.size() == 0) {
            getLog().warn("No package names specified, will abort now !");
            return;
        }
        getLog().info("Scanning project dependencies...");
        final Map<String, List<String>> foundPackages = getAetherHelper().findPackages(project, packageNames);
        getLog().info("=================================================================================");
        getLog().info("SEARCH RESULTS");
        getLog().info("---------------------------------------------------------------------------------");
        for (String packageName : packageNames) {
            if (!foundPackages.containsKey(packageName)) {
                getLog().warn("Couldn't find " + packageName + " anywhere!");
            } else {
                getLog().info(
                        "Found package " + packageName + " in "
                                + StringUtils.join(foundPackages.get(packageName), " -> "));
            }
        }
    }

}
