/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.rest;

import dk.dbc.triton.core.ScanPos;
import dk.dbc.triton.core.ScanResult;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.common.util.NamedList;

import javax.ejb.Stateless;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("scan")
public class ScanBean {
    /**
     * Scans database index for a term or a phrase
     * @param term index term
     * @param index index field
     * @param pos preferred term position {first|last}, defaults to first
     * @param size maximum number of entries to be return, defaults to 20
     * @param include restricts to terms matching the regular expression
     * @return 200 Ok response containing serialized {@link TermsResponse}
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response scan(
            @QueryParam("term") String term,
            @QueryParam("index") String index,
            @QueryParam("pos") @DefaultValue("first") ScanPos pos,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("include") @DefaultValue("") String include) {
        // TODO: 15-01-18 replace with actual scan
        final NamedList<Number> entries = new NamedList<>();
        entries.add("a", 1);
        entries.add("b", 2);
        entries.add("c", 3);
        final NamedList<NamedList<Number>> fieldTerms = new NamedList<>();
        fieldTerms.add("author", entries);
        final ScanResult scanResult = ScanResult.of(new TermsResponse(fieldTerms));

        return Response.ok(scanResult).build();
    }
}
