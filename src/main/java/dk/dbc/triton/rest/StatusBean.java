/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.rest;

import dk.dbc.serviceutils.ServiceStatus;
import dk.dbc.triton.core.SolrClientFactoryBean;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Stateless
@LocalBean
@Path("")
public class StatusBean implements ServiceStatus {
    @EJB SolrClientFactoryBean solrClientFactoryBean;

    @Override
    public Response getStatus() {
        solrClientFactoryBean.pingDefaultCollection();
        return Response.ok().entity(OK_ENTITY).build();
    }
}
