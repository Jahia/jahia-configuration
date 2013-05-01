package org.jahia.utils.osgi;

/**
 * Library of utilities to handle package names
 */
public class PackageUtils {

    public static String getPackageFromClass(final String fqnClassName) {
        String[] classNameParts = fqnClassName.split("\\.");
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
                return packageNameBuilder.toString();
            } else {
                return null;
            }
        }
        int lastDot = fqnClassName.lastIndexOf(".");
        if (lastDot < 0) {
            return null;
        }
        return fqnClassName.substring(0, lastDot);
    }

}
