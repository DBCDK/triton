/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.core;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.Singleton;

import static javax.ejb.LockType.READ;

@Singleton
public class SolrClientFactoryBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(SolrClientFactoryBean.class);

    @Resource(lookup = "java:app/env/url/zookeeper")
    private String zookeeper;

    private CloudSolrClient cloudSolrClient;

    @PostConstruct
    public void initialize() {
        LOGGER.info("Zookeeper quorum: {}", zookeeper);
        cloudSolrClient = new CloudSolrClient.Builder()
                .withHttpClient(createHttpClient())
                .withZkHost(zookeeper)
                .build();
        cloudSolrClient.connect();
    }

    @Lock(READ)
    public CloudSolrClient getCloudSolrClient() {
        return cloudSolrClient;
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
}
