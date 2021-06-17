/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.rest;

import dk.dbc.solr.SolrScan;
import dk.dbc.solr.SolrSearch;
import dk.dbc.triton.core.ScanMapBean;
import dk.dbc.triton.core.ScanPos;
import dk.dbc.triton.core.ScanResult;
import dk.dbc.triton.core.ScanTermAdjusterBean;
import dk.dbc.triton.core.SolrClientFactoryBean;
import dk.dbc.triton.core.TritonException;
import dk.dbc.util.Stopwatch;
import dk.dbc.util.Timed;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.SolrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Stateless
@Path("scan")
public class ScanBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanBean.class);

    @Inject SolrClientFactoryBean solrClientFactoryBean;
    @EJB ScanTermAdjusterBean scanTermAdjusterBean;
    @EJB ScanMapBean scanMapBean;

    /**
     * Scans database index for a term or a phrase
     * @param term index term
     * @param indexParam index field
     * @param collectionParam solr collection, defaults to value of
     *                        environment variable DEFAULT_COLLECTION
     * @param pos preferred term position {first|last}, defaults to first
     * @param size maximum number of entries to be return, defaults to 20
     * @param include restricts to terms matching the regular expression
     * @param withExactFrequency perform exact match search for each scan
     *                           term to adjust term frequencies,
     *                           defaults to true
     * @param fieldType normalize input term before scan using analysis
     *                  phases defined by this field type
     * @return 200 Ok response containing serialized {@link ScanResult}.
     *         400 Bad Request on null or empty term or index param.
     *         400 Bad Request on non-existing collection.
     * @throws TritonException on internal error
     * @throws WebApplicationException on bad request
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Timed
    @AccessLogged
    public Response scan(
            @QueryParam("term") String term,
            @QueryParam("index") String indexParam,
            @QueryParam("collection") String collectionParam,
            @QueryParam("pos") @DefaultValue("first") ScanPos pos,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("include") @DefaultValue("") String include,
            @QueryParam("withExactFrequency") @DefaultValue("true") boolean withExactFrequency,
            @QueryParam("fieldType") @DefaultValue("dbc-scan") String fieldType)
            throws TritonException, WebApplicationException {
        verifyStringParam("term", term);
        verifyStringParam("index", indexParam);
        final String collection = collectionParam != null && !collectionParam.trim().isEmpty() ?
                collectionParam : solrClientFactoryBean.getDefaultCollection();
        ScanResult scanResult = null;
        try {
            final CloudSolrClient cloudSolrClient = solrClientFactoryBean.getCloudSolrClient();
            if (LOGGER.isDebugEnabled()) {
                solrClientFactoryBean.logLiveReplicas(collection);
            }
            final String index = scanMapBean.resolve(collection, indexParam);
            LOGGER.info("Index parameter {} resolved to {}", indexParam, index);
            term = normalizeTermByFieldType(collection, fieldType, term);
            final SolrScan solrScan = createSolrScan(cloudSolrClient, collection)
                    .withField(index)
                    .withLimit(size);
            if (pos == ScanPos.FIRST) {
                solrScan.withLower(term).withLowerInclusive(true);
            } else {
                solrScan.withUpper(term).withUpperInclusive(true);
            }
            if (!include.isEmpty()) {
                solrScan.withRegex(include);
            }
            scanResult = ScanResult.of(solrScan.execute());
            if (scanResult.getTerms().isEmpty()) {
                verifyIndex(cloudSolrClient, collection, scanResult.getIndex());
            }

            if (withExactFrequency) {
                adjustTermFrequencies(collection, index, scanResult);
            }
        } catch (SolrException e) {
            convertSolrExceptionAndThrow(e);
        } catch (IOException | SolrServerException e) {
            throw new TritonException(e);
        }
        return Response.ok(scanResult).build();
    }

    // This method exists for easy partial mocking of solr
    // functionality during testing
    SolrScan createSolrScan(CloudSolrClient cloudSolrClient, String collection) {
        return new SolrScan(cloudSolrClient, collection)
                .withSort(SolrScan.SortType.INDEX);
    }

    // This method exists for easy partial mocking of solr
    // functionality during testing
    SolrSearch createSolrSearch(CloudSolrClient cloudSolrClient, String collection) {
        return new SolrSearch(cloudSolrClient, collection);
    }

    private String normalizeTermByFieldType(String collection, String fieldType, String term) {
        final Stopwatch stopwatch = new Stopwatch();
        try {
            return scanTermAdjusterBean.normalizeByFieldType(collection, fieldType, term);
        } finally {
            LOGGER.info("normalizeTermByFieldType took {} {}",
                    stopwatch.getElapsedTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
        }
    }

    private void adjustTermFrequencies(String collection, String index, ScanResult scanResult)
            throws TritonException {
        final Stopwatch stopwatch = new Stopwatch();
        try {
            final List<ScanResult.Term> terms = scanResult.getTerms();
            final List<Future<ScanResult.Term>> futures = new ArrayList<>(terms.size());
            for (ScanResult.Term term : terms) {
                futures.add(scanTermAdjusterBean.adjustTermFrequency(collection, index, term));
            }
            for (Future<ScanResult.Term> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new TritonException(e);
        } finally {
            LOGGER.info("adjustTermFrequencies took {} {}",
                    stopwatch.getElapsedTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
        }
    }

    private void verifyStringParam(String name, String value)
            throws WebApplicationException {
        if (value == null || value.trim().isEmpty()) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(name + " parameter is mandatory")
                            .build()
            );
        }
    }

    private void verifyIndex(CloudSolrClient cloudSolrClient, String collection, String index)
            throws IOException, SolrServerException {
        /* Since a solr terms request does not report an error
           in case of an unknown index, we do a simple search
           instead. */
        createSolrSearch(cloudSolrClient, collection)
                .withQuery(index + ":test")
                .withRows(0)
                .execute();
    }

    private void convertSolrExceptionAndThrow(SolrException e)
            throws TritonException, WebApplicationException {
        if (e.code() == 400) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(e.getMessage())
                            .build());
        }
        throw new TritonException(e);
    }
}
