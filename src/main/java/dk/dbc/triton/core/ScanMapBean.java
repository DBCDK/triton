/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.core;

import org.apache.solr.client.solrj.impl.ZkClientClusterStateProvider;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
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

    HashMap<String, Properties> collectionProperties = new HashMap<>();

    @PostConstruct
    public void initialize() {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("triton");

            final ZkStateReader clusterStateReader = solrClientFactoryBean.getCloudSolrClient().getZkStateReader();
            for (String collection : clusterStateReader.getClusterState().getCollectionsMap().keySet()) {
                collectionProperties.put(collection, fetchScanMap(collection, tempDir));
            }
        } catch (IOException e) {
            throw new TritonException(e);
        } finally {
            deleteDirectory(tempDir);
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

    private void deleteDirectory(Path dir) {
        if (dir != null) {
            try {
                Files.walk(dir)
                        .sorted(Comparator.reverseOrder()) // ensures containing dir after files
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                LOGGER.error("Error deleting dir {}", dir.toAbsolutePath());
            }
        }
    }

    private Properties fetchScanMap(String collection, Path tempDir) {
        final Properties properties = new Properties();
        try {
            final ZkStateReader clusterStateReader = solrClientFactoryBean.getCloudSolrClient().getZkStateReader();
            final String configName = clusterStateReader.readConfigName(collection);

            final ZkClientClusterStateProvider clusterStateProvider = (ZkClientClusterStateProvider)
                    solrClientFactoryBean.getCloudSolrClient().getClusterStateProvider();
            final Path localDir = tempDir.resolve(collection);
            clusterStateProvider.downloadConfig(configName, localDir);

            final Path scanMapFile = localDir.resolve(SCAN_MAP_FILE);
            if (Files.exists(scanMapFile)) {
                try (InputStream inputStream = Files.newInputStream(scanMapFile)) {
                    properties.load(inputStream);
                }
            }
        } catch (SolrException | IOException e) {
            if (e.getCause() == null
                    || !(e.getCause() instanceof KeeperException.NoNodeException)) {
                throw new TritonException(e);
            }
            LOGGER.info("No config found for collection {}", collection);
        }
        return properties;
    }
}
