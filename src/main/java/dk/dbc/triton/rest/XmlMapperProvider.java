/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
@Produces({ MediaType.APPLICATION_XML })
public class XmlMapperProvider implements ContextResolver<XmlMapper> {
    private final XmlMapper xmlMapper;

    public XmlMapperProvider() {
        final JacksonXmlModule module = new JacksonXmlModule();
        module.setDefaultUseWrapper(true);
        xmlMapper = new XmlMapper(module);
        xmlMapper.configure(SerializationFeature.INDENT_OUTPUT, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    @Override
    public XmlMapper getContext(Class<?> aClass) {
        return xmlMapper;
    }
}
