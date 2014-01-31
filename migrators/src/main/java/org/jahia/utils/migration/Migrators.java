package org.jahia.utils.migration;

import org.jahia.utils.migration.model.Migrations;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.OutputStream;

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

    public <T> T unmarshal(Class<T> docClass, InputStream inputStream) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(docClass);
        Unmarshaller u = jc.createUnmarshaller();
        T doc = (T) u.unmarshal(inputStream);
        return doc;
    }

    public void marshal(Class docClass, Object jaxbElement, OutputStream outputStream) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(docClass);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.marshal(jaxbElement, outputStream);
    }

}
