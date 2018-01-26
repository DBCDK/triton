/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.rest;

import dk.dbc.solr.SolrScan;
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

    @EJB SolrClientFactoryBean solrClientFactoryBean;
    @EJB ScanTermAdjusterBean scanTermAdjusterBean;

    /**
     * Scans database index for a term or a phrase
     * @param term index term
     * @param index index field
     * @param collection solr collection
     * @param pos preferred term position {first|last}, defaults to first
     * @param size maximum number of entries to be return, defaults to 20
     * @param include restricts to terms matching the regular expression
     * @param withExactFrequency perform exact match search for each scan
     *                           term to adjust term frequencies,
     *                           defaults to true
     * @return 200 Ok response containing serialized {@link ScanResult}.
     *         400 Bad Request on null or empty term, index or collection param.
     *         400 Bad Request on non-existing collection.
     * @throws TritonException on internal error
     * @throws WebApplicationException on bad request
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Timed
    public Response scan(
            @QueryParam("term") String term,
            @QueryParam("index") String index,
            @QueryParam("collection") String collection,
            @QueryParam("pos") @DefaultValue("first") ScanPos pos,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("include") @DefaultValue("") String include,
            @QueryParam("withExactFrequency") @DefaultValue("true") boolean withExactFrequency)
            throws TritonException, WebApplicationException {
        verifyStringParam("term", term);
        verifyStringParam("index", index);
        verifyStringParam("collection", collection);
        ScanResult scanResult = null;
        try {
            final CloudSolrClient cloudSolrClient = solrClientFactoryBean.getCloudSolrClient();
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

    private void convertSolrExceptionAndThrow(SolrException e)
            throws TritonException, WebApplicationException {
        if (!e.getMessage().isEmpty()
                && e.getMessage().toLowerCase().startsWith("collection not found")) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(e.getMessage())
                            .build()
            );
        }
        throw new TritonException(e);
    }
}
