/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.utils.migration;

import org.apache.commons.io.IOUtils;
import org.jahia.commons.Version;
import org.jahia.utils.migration.model.Migration;
import org.jahia.utils.migration.model.MigrationOperation;
import org.jahia.utils.migration.model.MigrationResource;
import org.jahia.utils.migration.model.Migrations;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Main singleton for migrators access
 */
public class Migrators {

    private static final Migrators instance = new Migrators();
    private Migrations migrationsConfig = null;

    public Migrators() {
        InputStream migrationConfigInputStream = this.getClass().getClassLoader().getResourceAsStream("org/jahia/utils/migration/migrations.xml");
        try {
            migrationsConfig = unmarshal(Migrations.class, migrationConfigInputStream);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public static Migrators getInstance() {
        return instance;
    }

    private <T> T unmarshal(Class<T> docClass, InputStream inputStream) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(docClass);
        Unmarshaller u = jc.createUnmarshaller();
        T doc = (T) u.unmarshal(inputStream);
        return doc;
    }

    private void marshal(Class docClass, Object jaxbElement, OutputStream outputStream) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(docClass);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.marshal(jaxbElement, outputStream);
    }

    public boolean willMigrate(InputStream inputStream, String filePath, Version fromVersion, Version toVersion) {
        for (Migration migration : migrationsConfig.getMigrations()) {
            if (migration.getFromVersion().equals(fromVersion) &&
                    migration.getToVersion().equals(toVersion)) {
                for (MigrationResource migrationResource : migration.getMigrationResources()) {
                    Matcher resourcePatternMatcher = migrationResource.getCompiledPattern().matcher(filePath);
                    if (resourcePatternMatcher.matches()) {
                        for (MigrationOperation migrationOperation : migrationResource.getOperations()) {
                            if (migrationOperation.willMigrate(inputStream, filePath)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public List<String> migrate(InputStream inputStream, OutputStream outputStream, String filePath, Version fromVersion, Version toVersion, boolean performModification) {
        List<String> messages = new ArrayList<String>();
        for (Migration migration : migrationsConfig.getMigrations()) {
            if (migration.getFromVersion().equals(fromVersion) &&
                    migration.getToVersion().equals(toVersion)) {
                for (MigrationResource migrationResource : migration.getMigrationResources()) {
                    Matcher resourcePatternMatcher = migrationResource.getCompiledPattern().matcher(filePath);
                    if (resourcePatternMatcher.matches()) {
                        // we use byte array input and output stream to be able to iterate over the contents multiple times, feeding in the result of the last iteration into the next.
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        try {
                            IOUtils.copyLarge(inputStream, byteArrayOutputStream);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        byte[] inputByteArray = byteArrayOutputStream.toByteArray();
                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(inputByteArray);
                        byteArrayOutputStream.reset();
                        for (MigrationOperation migrationOperation : migrationResource.getOperations()) {
                            messages.addAll(migrationOperation.execute(byteArrayInputStream, byteArrayOutputStream, filePath, performModification));
                            byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                            byteArrayOutputStream.reset();
                        }
                        try {
                            IOUtils.copyLarge(byteArrayInputStream, outputStream);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return messages;
    }

}
