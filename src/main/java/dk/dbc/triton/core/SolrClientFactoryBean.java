/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.core;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.io.IOException;

import static javax.ejb.LockType.READ;

@Startup
@Singleton
public class SolrClientFactoryBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(SolrClientFactoryBean.class);
    private static final String ZOOKEEPER_NOT_CONFIGURED = "ZOOKEEPER property not set";
    private static final String DEFAULT_COLLECTION_NOT_CONFIGURED = "DEFAULT_COLLECTION property not set";

    @Inject
    @ConfigProperty(name = "ZOOKEEPER", defaultValue = ZOOKEEPER_NOT_CONFIGURED)
    private String zookeeper;

    @Inject
    @ConfigProperty(name = "DEFAULT_COLLECTION", defaultValue = DEFAULT_COLLECTION_NOT_CONFIGURED)
    private String defaultCollection;

    private CloudSolrClient cloudSolrClient;

    @PostConstruct
    public void initialize() {
        LOGGER.info("Zookeeper quorum: {}", zookeeper);
        cloudSolrClient = new CloudSolrClient.Builder()
                .withHttpClient(createHttpClient())
                .withZkHost(zookeeper)
                .build();
        cloudSolrClient.connect();
        pingDefaultCollection();
    }

    @Lock(READ)
    public CloudSolrClient getCloudSolrClient() {
        return cloudSolrClient;
    }

    @Lock(READ)
    public String getDefaultCollection() {
        return defaultCollection;
    }

    private HttpClient createHttpClient() {
        // At some point we may need to make these configurable
        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.set(HttpClientUtil.PROP_SO_TIMEOUT, 5000);
        params.set(HttpClientUtil.PROP_CONNECTION_TIMEOUT, 5000);
        params.set(HttpClientUtil.PROP_MAX_CONNECTIONS, 32);
        params.set(HttpClientUtil.PROP_MAX_CONNECTIONS_PER_HOST, 32);
        return HttpClientUtil.createClient(params);
    }

    private void pingDefaultCollection() {
        if (defaultCollection != null
                && !DEFAULT_COLLECTION_NOT_CONFIGURED.equals(defaultCollection)) {
            try {
                final SolrPing ping = new SolrPing();
                final SolrPingResponse pingResponse = ping.process(
                        cloudSolrClient, defaultCollection);
                if (pingResponse.getStatus() != 0) {
                    throw new TritonException(String.format(
                            "Unable to ping collection %s", defaultCollection));
                }
                LOGGER.info("Pinged solr collection {} in {} ms",
                        defaultCollection, pingResponse.getQTime());
            } catch (SolrServerException | IOException e) {
                throw new TritonException(String.format(
                        "Unable to ping collection %s", defaultCollection), e);
            }
        }
    }
}
