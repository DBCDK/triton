/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dk.dbc.triton.rest.JsonMapperProvider;
import dk.dbc.triton.rest.XmlMapperProvider;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.common.util.NamedList;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ScanResultTest {
    public static TermsResponse createTermsResponse(String... indexes) {
        final NamedList<Number> indexTerms = new NamedList<>();
        indexTerms.add("a", 1);
        indexTerms.add("b", 2);
        indexTerms.add("c", 3);
        final NamedList<NamedList<Number>> list = new NamedList<>();
        for (String index : indexes) {
            list.add(index, indexTerms);
        }
        return new TermsResponse(list);
    }

    @Test
    void throwsOnNullTermsResponse() {
        assertThrows(NullPointerException.class, () -> ScanResult.of(null));
    }

    @Test
    void throwsOnMultipleIndexesInTermResponse() {
        final TermsResponse termsResponse = createTermsResponse("author", "title");
        assertThrows(IllegalArgumentException.class, () -> ScanResult.of(termsResponse));
    }

    @Test
    void emptyTermsResponse() {
        assertThat(ScanResult.of(new TermsResponse(new NamedList<>())), is(ScanResult.EMPTY));
    }

    @Test
    void xmlOutput() throws IOException {
        final XmlMapperProvider xmlMapperProvider = new XmlMapperProvider();
        final XmlMapper xmlMapper = xmlMapperProvider.getContext(ScanResultTest.class);
        final StringWriter stringWriter = new StringWriter();
        final TermsResponse termsResponse = createTermsResponse("author");
        xmlMapper.writeValue(stringWriter, ScanResult.of(termsResponse));

        final String expected = new String(
                readResource("src/test/resources/scanresult.xml"),
                StandardCharsets.UTF_8);
        assertThat(stringWriter.toString(), is(expected));
    }

    @Test
    void jsonOutput() throws IOException {
        final JsonMapperProvider jsonMapperProvider = new JsonMapperProvider();
        final ObjectMapper objectMapper = jsonMapperProvider.getContext(ScanResultTest.class);
        final StringWriter stringWriter = new StringWriter();
        final TermsResponse termsResponse = createTermsResponse("author");
        objectMapper.writeValue(stringWriter, ScanResult.of(termsResponse));

        final String expected = new String(
                readResource("src/test/resources/scanresult.json"),
                StandardCharsets.UTF_8);
        assertThat(stringWriter.toString(), is(expected));
    }

    private byte[] readResource(String resource) {
        try {
            return Files.readAllBytes(Paths.get(resource));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}