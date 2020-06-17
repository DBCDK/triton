/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.core;

import org.apache.solr.common.cloud.Aliases;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.ZkStateReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static javax.ejb.LockType.READ;

/**
 * Resolves scan index names from aliases
 * <p>
 * Loads map (if present) for each collection in the cluster at startup from
 * the file /configs/{configName}/scanMap.txt in the zookeeper. The
 * content of scanMap.txt must be a well-formed Java property file
 * (precise semantics described here:
 * https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html#load-java.io.Reader-)
 * </p>
 */
@Startup
@Singleton
@DependsOn("SolrClientFactoryBean")
public class ScanMapBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanMapBean.class);
    private static final String SCAN_MAP_FILE = "scanMap.txt";

    @EJB SolrClientFactoryBean solrClientFactoryBean;

    Map<String, Properties> collectionProperties = new HashMap<>();

    @PostConstruct
    public void initialize() {
        final ZkStateReader clusterStateReader = solrClientFactoryBean.getCloudSolrClient().getZkStateReader();
        final Aliases aliases = clusterStateReader.getAliases();
        final Map<String, String> collectionAliasMap = aliases.getCollectionAliasMap();
        LOGGER.info("Known aliases: {}", collectionAliasMap);
        for (String collection : clusterStateReader.getClusterState().getCollectionsMap().keySet()) {
            final Optional<String> scanMapUrl = getScanMapUrl(collection);
            if (scanMapUrl.isPresent()) {
                final Optional<Properties> scanMap = fetchScanMap(scanMapUrl.get());
                if (scanMap.isPresent()) {
                    collectionProperties.put(collection, scanMap.get());
                    LOGGER.info("Fetched {} for {}", SCAN_MAP_FILE, collection);
                    collectionAliasMap.forEach((alias, name) -> {
                        if (collection.equals(name)) {
                            collectionProperties.put(alias, scanMap.get());
                            LOGGER.info("Stored {} for alias {} as copy of {}", SCAN_MAP_FILE, alias, collection);
                        }
                    });
                }
            }
        }
    }

    /**
     * Resolves given index alias against specific collection map
     * @param collection solr collection
     * @param indexAlias index alias
     * @return mapped value or original value if no mapping exists
     */
    @Lock(READ)
    public String resolve(String collection, String indexAlias) {
        if (!collectionProperties.containsKey(collection)) {
            return indexAlias;
        }
        return collectionProperties.get(collection).getProperty(indexAlias, indexAlias);
    }

    /* Returns the scan map URL for the given collection
       or empty if no shard URL could be determined */
    private Optional<String> getScanMapUrl(String collection) {
        final ZkStateReader zkStateReader = solrClientFactoryBean.getCloudSolrClient()
                .getZkStateReader();
        final DocCollection docCollection = zkStateReader.getClusterState()
                .getCollectionOrNull(collection);
        if (docCollection != null) {
            for (Slice slice : docCollection.getSlices()) {
                for (Replica replica : slice.getReplicas()) {
                    if (replica.getState() == Replica.State.ACTIVE) {
                        return Optional.of(replica.getCoreUrl()
                                + "admin/file?wt=json&file=scanMap.txt&contentType=text%2Fplain%3Bcharset%3Dutf-8");
                    }
                }
            }
        }
        return Optional.empty();
    }

    /* Returns scan map located at given URL
       or empty if no scan map could be found */
    private Optional<Properties> fetchScanMap(String url) {
        try {
            final URLConnection urlConnection = new URL(url).openConnection();
            try (InputStream inputStream = urlConnection.getInputStream()) {
                final Properties properties = new Properties();
                properties.load(inputStream);
                return Optional.of(properties);
            }
        } catch (FileNotFoundException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new TritonException(e);
        }
    }
}
