/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.rest;

import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.common.util.NamedList;

import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("scan")
public class ScanBean {
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response scan() {
        // TODO: 15-01-18 replace with actual scan
        final NamedList<Number> entries = new NamedList<>();
        entries.add("a", 1);
        entries.add("b", 2);
        entries.add("c", 3);
        final NamedList<NamedList<Number>> fieldTerms = new NamedList<>();
        fieldTerms.add("author", entries);
        final TermsResponse scanResult = new TermsResponse(fieldTerms);

        return Response.ok(scanResult).build();
    }
}
