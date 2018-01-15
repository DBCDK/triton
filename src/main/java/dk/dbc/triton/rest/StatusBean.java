/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.rest;

import dk.dbc.serviceutils.ServiceStatus;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.Path;

@Stateless
@LocalBean
@Path("")
public class StatusBean implements ServiceStatus {}
