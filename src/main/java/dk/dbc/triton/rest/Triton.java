/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.rest;

import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
@ApplicationPath("/")
public class Triton extends Application {
    static final Set<Class<?>> classes = new HashSet<>();
    static {
        classes.add(JacksonFeature.class);
        classes.add(JacksonXMLProvider.class);
        classes.add(ScanBean.class);
        classes.add(StatusBean.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        final Set<Object> singletons = new HashSet<>();
        singletons.add(new JsonMapperProvider());
        singletons.add(new XmlMapperProvider());
        return singletons;
    }
}
