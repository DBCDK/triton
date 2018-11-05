/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.UriInfo;

@AccessLogged
public class RequestLogger implements ContainerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLogger.class);

    @Override
    public void filter(ContainerRequestContext containerRequestContext) {
        final UriInfo uriInfo = containerRequestContext.getUriInfo();
        LOGGER.info("{} {}", containerRequestContext.getMethod(), uriInfo.getRequestUri());
    }
}
