/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.rest;

import dk.dbc.solr.SolrScan;
import dk.dbc.triton.core.ScanPos;
import dk.dbc.triton.core.ScanResultTest;
import dk.dbc.triton.core.SolrClientFactoryBean;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.common.SolrException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScanBeanTest {
    private static final String TERM = "term";
    private static final String INDEX = "index";
    private static final String COLLECTION = "collection";
    private static final String INCLUDE = "include";
    private static final ScanPos POS = ScanPos.FIRST;
    private static final int SIZE = 20;
    private static final boolean WITH_EXACT_FREQUENCY = false;

    private SolrClientFactoryBean solrClientFactoryBean = mock(SolrClientFactoryBean.class);
    private CloudSolrClient cloudSolrClient = mock(CloudSolrClient.class);
    private SolrScan solrScan = mock(SolrScan.class);
    private TermsResponse termsResponse = ScanResultTest.createTermsResponse(INDEX);

    private ScanBean scanBean = createScanBean();

    @BeforeEach
    void setupExpectations() {
        when(solrClientFactoryBean.getCloudSolrClient()).thenReturn(cloudSolrClient);
        when(solrScan.withField(INDEX)).thenReturn(solrScan);
        when(solrScan.withLimit(SIZE)).thenReturn(solrScan);
        when(solrScan.withLower(TERM)).thenReturn(solrScan);
        when(solrScan.withLowerInclusive(true)).thenReturn(solrScan);
        when(solrScan.withUpper(TERM)).thenReturn(solrScan);
        when(solrScan.withUpperInclusive(true)).thenReturn(solrScan);
        when(solrScan.withRegex(INCLUDE)).thenReturn(solrScan);
        try {
            when(solrScan.execute()).thenReturn(termsResponse);
        } catch (IOException | SolrServerException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void scan_termParamIsMandatory() {
        WebApplicationException e = assertThrows(WebApplicationException.class, () ->
                scanBean.scan(null, INDEX, COLLECTION, POS, SIZE, INCLUDE, WITH_EXACT_FREQUENCY),
                "term is null");
        assertThat("term is null => Bad Request",
                e.getResponse().getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        e = assertThrows(WebApplicationException.class, () ->
                scanBean.scan(" ", INDEX, COLLECTION, POS, SIZE, INCLUDE, WITH_EXACT_FREQUENCY),
                "term is empty");
        assertThat("term is empty => Bad Request",
                e.getResponse().getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    void scan_indexParamIsMandatory() {
        WebApplicationException e = assertThrows(WebApplicationException.class, () ->
                scanBean.scan(TERM, null, COLLECTION, POS, SIZE, INCLUDE, WITH_EXACT_FREQUENCY),
                "index is null");
        assertThat("index is null => Bad Request",
                e.getResponse().getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        e = assertThrows(WebApplicationException.class, () ->
                scanBean.scan(TERM, " ", COLLECTION, POS, SIZE, INCLUDE, WITH_EXACT_FREQUENCY),
                "term is empty");
        assertThat("index is empty => Bad Request",
                e.getResponse().getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    void scan_collectionParamIsMandatory() {
        WebApplicationException e = assertThrows(WebApplicationException.class, () ->
                scanBean.scan(TERM, INDEX, null, POS, SIZE, INCLUDE, WITH_EXACT_FREQUENCY),
                "collection is null");
        assertThat("collection is null => Bad Request",
                e.getResponse().getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        e = assertThrows(WebApplicationException.class, () ->
                scanBean.scan(TERM, INDEX, " ", POS, SIZE, INCLUDE, WITH_EXACT_FREQUENCY),
                "collection is empty");
        assertThat("collection is empty => Bad Request",
                e.getResponse().getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    void scan_collectionNotFound() throws IOException, SolrServerException {
        final ScanBean scanBean = spy(createScanBean());
        doReturn(solrScan).when(scanBean).createSolrScan(cloudSolrClient, COLLECTION);
        doThrow(new SolrException(SolrException.ErrorCode.NOT_FOUND, "Collection not found"))
                .when(solrScan).execute();
        final WebApplicationException e = assertThrows(WebApplicationException.class, () ->
                scanBean.scan(TERM, INDEX, COLLECTION, POS, SIZE, INCLUDE, WITH_EXACT_FREQUENCY),
                "collection not found");
        assertThat("collection not found => Bad Request",
                e.getResponse().getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    void scan() throws IOException, SolrServerException, InterruptedException, ExecutionException, TimeoutException {
        final ScanBean scanBean = spy(createScanBean());
        doReturn(solrScan).when(scanBean).createSolrScan(cloudSolrClient, COLLECTION);

        assertThat("scan",
                scanBean.scan(TERM, INDEX, COLLECTION, POS, SIZE, INCLUDE, WITH_EXACT_FREQUENCY).getStatus(),
                is(Response.Status.OK.getStatusCode()));

        verify(solrScan).withField(INDEX);
        verify(solrScan).withLimit(SIZE);
        verify(solrScan).withLower(TERM);
        verify(solrScan).withLowerInclusive(true);
        verify(solrScan).withRegex(INCLUDE);

        assertThat("scan pos=last",
                scanBean.scan(TERM, INDEX, COLLECTION, ScanPos.LAST, SIZE, INCLUDE, WITH_EXACT_FREQUENCY).getStatus(),
                is(Response.Status.OK.getStatusCode()));

        verify(solrScan).withUpper(TERM);
        verify(solrScan).withUpperInclusive(true);
    }

    private ScanBean createScanBean() {
        final ScanBean scanBean = new ScanBean();
        scanBean.solrClientFactoryBean = solrClientFactoryBean;
        return scanBean;
    }
}