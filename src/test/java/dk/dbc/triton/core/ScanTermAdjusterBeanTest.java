/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.core;

import dk.dbc.solr.SolrSearch;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScanTermAdjusterBeanTest {
    private static final String INDEX = "index";
    private static final String COLLECTION = "collection";

    private SolrClientFactoryBean solrClientFactoryBean = mock(SolrClientFactoryBean.class);
    private CloudSolrClient cloudSolrClient = mock(CloudSolrClient.class);
    private SolrSearch solrSearch = mock(SolrSearch.class);

    private final QueryResponse queryResponse = createQueryResponse();

    @BeforeEach
    void setupExpectations() {
        when(solrClientFactoryBean.getCloudSolrClient()).thenReturn(cloudSolrClient);
        when(solrSearch.withQuery(anyString())).thenReturn(solrSearch);
        try {
            when(solrSearch.execute()).thenReturn(queryResponse);
        } catch (IOException | SolrServerException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void updatesTerm() throws IOException, SolrServerException, ExecutionException, InterruptedException {
        final ScanTermAdjusterBean scanTermAdjusterBean = spy(createScanTermAdjusterBean());
        doReturn(solrSearch).when(scanTermAdjusterBean).createSolrSearch(cloudSolrClient, COLLECTION);

        final ScanResult.Term term = new ScanResult.Term("value", 1000);
        final Future<ScanResult.Term> termFuture =
                scanTermAdjusterBean.adjustTermFrequency(COLLECTION, INDEX, term);
        assertThat("future", termFuture.get(), is(term));
        assertThat("frequency", term.getFrequency(), is(queryResponse.getResults().getNumFound()));
    }

    @Test
    void exactMatchSearch() throws IOException, SolrServerException {
        final ScanTermAdjusterBean scanTermAdjusterBean = spy(createScanTermAdjusterBean());
        doReturn(solrSearch).when(scanTermAdjusterBean).createSolrSearch(cloudSolrClient, COLLECTION);

        final ScanResult.Term term = new ScanResult.Term("{value}", 1000);
        scanTermAdjusterBean.adjustTermFrequency(COLLECTION, INDEX, term);

        verify(solrSearch).withQuery("index:\"\\{value\\}\"");
    }

    private ScanTermAdjusterBean createScanTermAdjusterBean() {
        final ScanTermAdjusterBean scanTermAdjusterBean = new ScanTermAdjusterBean();
        scanTermAdjusterBean.solrClientFactoryBean = solrClientFactoryBean;
        return scanTermAdjusterBean;
    }

    private QueryResponse createQueryResponse() {
        final SolrDocumentList searchResult = new SolrDocumentList();
        searchResult.setNumFound(42);
        final NamedList<Object> response = new NamedList<>();
        response.add("response", searchResult);
        final QueryResponse queryResponse = new QueryResponse();
        queryResponse.setResponse(response);
        return queryResponse;
    }
}