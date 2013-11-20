package org.jahia.utils.osgi;

import java.util.HashSet;
import java.util.Set;

/**
 * Library of utilities to handle package names
 */
public class PackageUtils {

    public static Set<String> getPackagesFromClass(final String fqnClassName) {
        Set<String> packages = new HashSet<String>();
        // Split for generics
        String[] classNames = fqnClassName.split("<|>|,");
        for(String className : classNames){
            // remove all blank space in class name
            className = className.replaceAll("\\s+","");
            String[] classNameParts = className.split("\\.");
            if (classNameParts.length > 1 &&
                    classNameParts[classNameParts.length-2].length() > 0 &&
                    Character.isUpperCase(classNameParts[classNameParts.length-2].charAt(0))) {
                // we found a static import, we will return all parts except the last two.
                StringBuilder packageNameBuilder = new StringBuilder();
                for (int i=0; i < classNameParts.length - 2; i++) {
                    packageNameBuilder.append(classNameParts[i]);
                    if (i < classNameParts.length - 3) {
                        packageNameBuilder.append(".");
                    }
                }
                if (packageNameBuilder.length() > 0) {
                    packages.add(packageNameBuilder.toString());
                    continue;
                } else {
                    continue;
                }
            }
            int lastDot = className.lastIndexOf(".");
            if (lastDot < 0) {
                continue;
            }
            packages.add(className.substring(0, lastDot));
        }
        return packages;
    }
}
