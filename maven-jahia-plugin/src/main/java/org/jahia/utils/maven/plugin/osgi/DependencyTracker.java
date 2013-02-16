/***
 * ASM examples: examples showing how ASM can be used
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jahia.utils.maven.plugin.osgi;

import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * DependencyTracker
 *
 * @author Eugene Kuleshov
 * @see "http://www.onjava.com/pub/a/onjava/2005/08/17/asm3.html"
 */
public class DependencyTracker {

    public static List<String> findDependencyInJar(final File jarFile, final String packageToFind) throws IOException {
        DependencyVisitor v = new DependencyVisitor();
        List<String> classesThatHaveDependency = new ArrayList<String>();
        final String dirToFind = packageToFind.replaceAll("\\.", "/");

        ZipFile f = new ZipFile(jarFile);
        Enumeration<? extends ZipEntry> en = f.entries();
        while (en.hasMoreElements()) {
            ZipEntry e = en.nextElement();
            String name = e.getName();
            if (name.endsWith(".class")) {
                try {
                    new ClassReader(f.getInputStream(e)).accept(v, 0);
                    if (v.getPackages().contains(dirToFind)) {
                        classesThatHaveDependency.add(e.getName());
                        System.out.println("Found dependency "+ packageToFind + " in class " + name + " of JAR " + jarFile);
                        // dumpVisitorResults(v);
                    }
                    v.reset();
                } catch (Throwable t) {
                    System.err.println("Error parsing class " + name + ":");
                    t.printStackTrace();
                }
            }
        }
        return classesThatHaveDependency;
    }

    private static void dumpVisitorResults(DependencyVisitor v) {
        Map<String, Map<String, Integer>> globals = v.getGlobals();
        Set<String> jarPackages = globals.keySet();
        Set<String> classPackages = v.getPackages();
        String[] jarNames = jarPackages.toArray(new String[jarPackages.size()]);
        String[] classNames = classPackages.toArray(new String[classPackages
                .size()]);
        Arrays.sort(jarNames);
        Arrays.sort(classNames);

        System.out.println("Jar packages and their dependencies: ");
        for (String jarName : jarNames) {
            System.out.println("- " + jarName);
            for (Map.Entry<String, Integer> groupEntry : globals.get(jarName).entrySet()) {
                System.out.println("  +- " + groupEntry.getKey() + " = " + groupEntry.getValue());
            }
        }

        System.out.println("Encountered package names: ");
        for (String className : classNames) {
            System.out.println("= " + className);
        }
    }

}
