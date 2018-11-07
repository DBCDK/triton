/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.core;

import dk.dbc.solr.SolrFieldAnalysis;
import dk.dbc.solr.SolrSearch;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;
import java.util.concurrent.Future;

@Stateless
public class ScanTermAdjusterBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanTermAdjusterBean.class);

    @EJB SolrClientFactoryBean solrClientFactoryBean;

    /**
     * Updates frequency of the given {@link ScanResult.Term} by doing
     * an exact match search on the term value
     * @param collection solr collection
     * @param index index field
     * @param term scan term
     * @return {@link Future} containing the updated term to signal
     * completion of asynchronous operation
     * @throws TritonException on internal error
     */
    @Asynchronous
    public Future<ScanResult.Term> adjustTermFrequency(String collection, String index, ScanResult.Term term)
            throws TritonException {
        try {
            final String query = String.format("%s:\"%s\"", index, ClientUtils.escapeQueryChars(term.getValue()));
            final QueryResponse response = createSolrSearch(solrClientFactoryBean.getCloudSolrClient(), collection)
                    .withQuery(query)
                    .execute();
            // Updating the term in-place is slightly dangerous since
            // ScanResult.Term is not thread-safe. A more functional
            // approach would be to return a copy of the original term,
            // but this would force the caller to re-sort the terms,
            // which for performance reasons is not desirable,
            term.setFrequency(response.getResults().getNumFound());
            return new AsyncResult<>(term);
        } catch (IOException | SolrServerException e) {
            throw new TritonException(e);
        }
    }

    /**
     * Normalizes given term by applying analysis defined for given field type
     * @param collection solr collection
     * @param fieldType field type on which analysis is performed
     * @param term scan term
     * @return normalized term
     * @throws TritonException on failure to normalize
     */
    public String normalizeByFieldType(String collection, String fieldType, String term)
            throws TritonException {
        try {
            final String normalizedTerm = createSolrFieldAnalysis(
                    solrClientFactoryBean.getCloudSolrClient(), collection)
                    .byFieldType(fieldType, term);
            LOGGER.info("normalized term <{}> into <{}>", term, normalizedTerm);
            return normalizedTerm;
        } catch (SolrServerException e) {
            throw new TritonException(e);
        }
    }

    // These methods exist for easy partial mocking of solr
    // functionality during testing

    SolrSearch createSolrSearch(CloudSolrClient cloudSolrClient, String collection) {
        return new SolrSearch(cloudSolrClient, collection)
                .withRows(0);
    }

    SolrFieldAnalysis createSolrFieldAnalysis(CloudSolrClient cloudSolrClient, String collection) {
        return new SolrFieldAnalysis(cloudSolrClient, collection);
    }
}
