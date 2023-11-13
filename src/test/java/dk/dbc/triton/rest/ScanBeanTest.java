/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.rest;

import dk.dbc.solr.SolrScan;
import dk.dbc.solr.SolrSearch;
import dk.dbc.triton.core.ScanMapBeanTest;
import dk.dbc.triton.core.ScanPos;
import dk.dbc.triton.core.ScanResult;
import dk.dbc.triton.core.ScanResultTest;
import dk.dbc.triton.core.ScanTermAdjusterBean;
import dk.dbc.triton.core.SolrClientFactoryBean;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.common.SolrException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScanBeanTest {
    private static final String TERM = "TeRm";
    private static final String TERM_NORMALIZED = "term";
    private static final String INDEX = "scan.mti";
    private static final String COLLECTION = "collection";
    private static final String INCLUDE = "include";
    private static final ScanPos POS = ScanPos.FIRST;
    private static final int SIZE = 20;
    private static final boolean WITHOUT_EXACT_FREQUENCY = false;
    private static final String FIELD_TYPE = "dbc-phrase";

    private SolrClientFactoryBean solrClientFactoryBean = mock(SolrClientFactoryBean.class);
    private CloudSolrClient cloudSolrClient = mock(CloudSolrClient.class);
    private SolrScan solrScan = mock(SolrScan.class);
    private TermsResponse termsResponse = ScanResultTest.createTermsResponse(INDEX);
    private ScanTermAdjusterBean scanTermAdjusterBean = mock(ScanTermAdjusterBean.class);
    private Future<ScanResult.Term> future = mock(Future.class);

    private ScanBean scanBean = createScanBean();

    @BeforeEach
    void setupExpectations() {
        try {
            when(solrClientFactoryBean.getCloudSolrClient()).thenReturn(cloudSolrClient);
            when(scanTermAdjusterBean.adjustTermFrequency(eq(COLLECTION), eq(INDEX), any(ScanResult.Term.class)))
                    .thenReturn(null);
            when(scanTermAdjusterBean.normalizeByFieldType(COLLECTION, FIELD_TYPE, TERM))
                    .thenReturn(TERM_NORMALIZED);
            when(solrScan.withField(INDEX)).thenReturn(solrScan);
            when(solrScan.withLimit(SIZE)).thenReturn(solrScan);
            when(solrScan.withLower(TERM_NORMALIZED)).thenReturn(solrScan);
            when(solrScan.withLowerInclusive(true)).thenReturn(solrScan);
            when(solrScan.withUpper(TERM_NORMALIZED)).thenReturn(solrScan);
            when(solrScan.withUpperInclusive(true)).thenReturn(solrScan);
            when(solrScan.withRegex(INCLUDE)).thenReturn(solrScan);
            when(solrScan.execute()).thenReturn(termsResponse);
        } catch (IOException | SolrServerException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void scan_termParamIsMandatory() {
        WebApplicationException e = assertThrows(WebApplicationException.class, () ->
                scanBean.scan(null, INDEX, COLLECTION, POS, SIZE, INCLUDE,
                        WITHOUT_EXACT_FREQUENCY, FIELD_TYPE),
                "term is null");
        assertThat("term is null => Bad Request",
                e.getResponse().getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        e = assertThrows(WebApplicationException.class, () ->
                scanBean.scan(" ", INDEX, COLLECTION, POS, SIZE, INCLUDE,
                        WITHOUT_EXACT_FREQUENCY, FIELD_TYPE),
                "term is empty");
        assertThat("term is empty => Bad Request",
                e.getResponse().getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    void scan_indexParamIsMandatory() {
        WebApplicationException e = assertThrows(WebApplicationException.class, () ->
                scanBean.scan(TERM, null, COLLECTION, POS, SIZE, INCLUDE,
                        WITHOUT_EXACT_FREQUENCY, FIELD_TYPE),
                "index is null");
        assertThat("index is null => Bad Request",
                e.getResponse().getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        e = assertThrows(WebApplicationException.class, () ->
                scanBean.scan(TERM, " ", COLLECTION, POS, SIZE, INCLUDE,
                        WITHOUT_EXACT_FREQUENCY, FIELD_TYPE),
                "term is empty");
        assertThat("index is empty => Bad Request",
                e.getResponse().getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    void scan_collectionNotFound() throws IOException, SolrServerException {
        final ScanBean scanBean = spy(createScanBean());
        doReturn(solrScan).when(scanBean).createSolrScan(cloudSolrClient, COLLECTION);
        doThrow(new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Collection not found"))
                .when(solrScan).execute();
        final WebApplicationException e = assertThrows(WebApplicationException.class, () ->
                scanBean.scan(TERM, INDEX, COLLECTION, POS, SIZE, INCLUDE,
                        WITHOUT_EXACT_FREQUENCY, FIELD_TYPE),
                "collection not found");
        assertThat("collection not found => Bad Request",
                e.getResponse().getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    void scan_indexNotFound() throws IOException, SolrServerException {
        final ScanBean scanBean = spy(createScanBean());
        doReturn(solrScan).when(scanBean).createSolrScan(cloudSolrClient, COLLECTION);
        final TermsResponse termsResponse = ScanResultTest.createTermsResponse(INDEX);
        termsResponse.getTermMap().clear();
        when(solrScan.execute()).thenReturn(termsResponse);

        final SolrSearch solrSearch = mock(SolrSearch.class);
        doReturn(solrSearch).when(scanBean).createSolrSearch(cloudSolrClient, COLLECTION);
        when(solrSearch.withQuery(anyString())).thenReturn(solrSearch);
        when(solrSearch.withRows(0)).thenReturn(solrSearch);
        doThrow(new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Index not found"))
                .when(solrSearch).execute();

        final WebApplicationException e = assertThrows(WebApplicationException.class, () ->
                scanBean.scan(TERM, INDEX, COLLECTION, POS, SIZE, INCLUDE,
                        WITHOUT_EXACT_FREQUENCY, FIELD_TYPE),
                "Index not found");
        assertThat("Index not found => Bad Request",
                e.getResponse().getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    void scan() {
        final ScanBean scanBean = spy(createScanBean());
        doReturn(solrScan).when(scanBean).createSolrScan(cloudSolrClient, COLLECTION);

        assertThat("scan",
                scanBean.scan(TERM, INDEX, COLLECTION, POS, SIZE, INCLUDE,
                        WITHOUT_EXACT_FREQUENCY, FIELD_TYPE).getStatus(),
                is(Response.Status.OK.getStatusCode()));

        verify(solrScan).withField(INDEX);
        verify(solrScan).withLimit(SIZE);
        verify(solrScan).withLower(TERM_NORMALIZED);
        verify(solrScan).withLowerInclusive(true);
        verify(solrScan).withRegex(INCLUDE);

        assertThat("scan pos=last",
                scanBean.scan(TERM, INDEX, COLLECTION, ScanPos.LAST, SIZE, INCLUDE,
                        WITHOUT_EXACT_FREQUENCY, FIELD_TYPE).getStatus(),
                is(Response.Status.OK.getStatusCode()));

        verify(solrScan).withUpper(TERM_NORMALIZED);
        verify(solrScan).withUpperInclusive(true);
    }

    @Test
    void scan_withIndexAlias() {
        final ScanBean scanBean = spy(createScanBean());
        doReturn(solrScan).when(scanBean).createSolrScan(cloudSolrClient, COLLECTION);

        assertThat("scan",
                scanBean.scan(TERM, "mti", COLLECTION, POS, SIZE, INCLUDE,
                        WITHOUT_EXACT_FREQUENCY, FIELD_TYPE).getStatus(),
                is(Response.Status.OK.getStatusCode()));

        verify(solrScan).withField(INDEX);
    }

    @Test
    void scan_withExactFrequency() {
        when(scanTermAdjusterBean.adjustTermFrequency(eq(COLLECTION), eq(INDEX), any(ScanResult.Term.class)))
                .thenReturn(future);
        final ScanBean scanBean = spy(createScanBean());
        doReturn(solrScan).when(scanBean).createSolrScan(cloudSolrClient, COLLECTION);

        assertThat("scan",
                scanBean.scan(TERM, INDEX, COLLECTION, POS, SIZE, INCLUDE,
                        true, FIELD_TYPE).getStatus(),
                is(Response.Status.OK.getStatusCode()));

        verify(scanTermAdjusterBean).adjustTermFrequency(COLLECTION, INDEX,
                new ScanResult.Term("a", 1));
        verify(scanTermAdjusterBean).adjustTermFrequency(COLLECTION, INDEX,
                new ScanResult.Term("b", 2));
        verify(scanTermAdjusterBean).adjustTermFrequency(COLLECTION, INDEX,
                new ScanResult.Term("c", 3));
    }

    @Test
    void scan_normalizeTermByFieldType() {
        final ScanBean scanBean = spy(createScanBean());
        doReturn(solrScan).when(scanBean).createSolrScan(cloudSolrClient, COLLECTION);

        scanBean.scan(TERM, INDEX, COLLECTION, POS, SIZE, INCLUDE, WITHOUT_EXACT_FREQUENCY, FIELD_TYPE);

        verify(scanTermAdjusterBean).normalizeByFieldType(COLLECTION, FIELD_TYPE, TERM);
    }

    private ScanBean createScanBean() {
        final ScanBean scanBean = new ScanBean();
        scanBean.solrClientFactoryBean = solrClientFactoryBean;
        scanBean.scanTermAdjusterBean = scanTermAdjusterBean;
        scanBean.scanMapBean = ScanMapBeanTest.newScanMapBean();
        return scanBean;
    }
}
