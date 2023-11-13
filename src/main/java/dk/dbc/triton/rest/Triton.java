/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.rest;

import com.fasterxml.jackson.jakarta.rs.xml.JacksonXMLProvider;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.util.Set;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
@ApplicationPath("/")
public class Triton extends Application {
    static final Set<Class<?>> CLASSES = Set.of(ScanBean.class, RequestLogger.class, JacksonFeature.class, JacksonXMLProvider.class);

    @Override
    public Set<Class<?>> getClasses() {
        return CLASSES;
    }

    @Override
    public Set<Object> getSingletons() {
        return Set.of(new JsonMapperProvider(), new XmlMapperProvider());
    }
}
