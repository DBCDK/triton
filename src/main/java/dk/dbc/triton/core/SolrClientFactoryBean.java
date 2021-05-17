/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.core;

import dk.dbc.solr.ZkParams;
import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.ZkCoreNodeProps;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Set;

import static javax.ejb.LockType.READ;

@ApplicationScoped
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
        final ZkParams zkParams = ZkParams.create(zookeeper);
        cloudSolrClient = new CloudSolrClient.Builder(zkParams.getZkHosts(), zkParams.getZkChroot())
                .withHttpClient(createHttpClient())
                .build();
        cloudSolrClient.connect();
        pingDefaultCollection();
    }

    public CloudSolrClient getCloudSolrClient() {
        return cloudSolrClient;
    }

    public String getDefaultCollection() {
        return defaultCollection;
    }

    public void logLiveReplicas(String collection) {
        final String collectionName = resolveCollectionAlias(collection);
        final ZkStateReader zkStateReader = cloudSolrClient.getZkStateReader();
        final ClusterState clusterState = zkStateReader.getClusterState();
        final Set<String> liveNodes = clusterState.getLiveNodes();
        final DocCollection docCollection = clusterState.getCollectionOrNull(collectionName);
        if (docCollection != null) {
            for (Slice slice : docCollection.getSlices()) {
                for (Replica replica : slice.getReplicas()) {
                    if (replica.getState() == Replica.State.ACTIVE && liveNodes.contains(replica.getNodeName())) {
                        final ZkCoreNodeProps zkProps = new ZkCoreNodeProps(replica);
                        LOGGER.info("{} live replica base URL: {}", collectionName, zkProps.getBaseUrl());
                    }
                }
            }
        }
    }

    public String resolveCollectionAlias(String collection) {
        final String resolvedName = cloudSolrClient.getClusterStateProvider().resolveAlias(collection).get(0);
        if (!collection.equals(resolvedName)) {
            LOGGER.info("Collection name {} is an alias for {}", collection, resolvedName);
        }
        return resolvedName;
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

    public boolean pingDefaultCollection() {
        if (!DEFAULT_COLLECTION_NOT_CONFIGURED.equals(defaultCollection)) {
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

                return true;
            } catch (SolrServerException | IOException e) {
                throw new TritonException(String.format(
                        "Unable to ping collection %s", defaultCollection), e);
            }
        }
        return false;
    }

    @Produces
    @Liveness
    @Lock(READ)
    public HealthCheck livenessPing() {
        return () -> HealthCheckResponse.named("ping-solr").status(pingDefaultCollection()).build();
    }

    @Produces
    @Readiness
    @Lock(READ)
    public HealthCheck readinessPing() {
        return () -> HealthCheckResponse.named("ping-solr").status(pingDefaultCollection()).build();
    }
}
