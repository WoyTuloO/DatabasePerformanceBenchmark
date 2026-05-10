package com.example.performanceTesting.bootstrap.config;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.diagnostics.WaitUntilReadyOptions;
import com.couchbase.client.java.query.QueryScanConsistency;
import com.couchbase.client.core.service.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(prefix = "app.database", name = "type", havingValue = "couchbase")
public class CouchbaseClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CouchbaseClientConfiguration.class);

    @Bean(destroyMethod = "shutdown")
    public ClusterEnvironment couchbaseEnvironment() {
        return ClusterEnvironment.builder()
                .timeoutConfig(timeout -> timeout.queryTimeout(Duration.ofSeconds(30)))
                .build();
    }

    @Bean(destroyMethod = "disconnect")
    public Cluster couchbaseCluster(DatabaseBootstrapProperties properties, ClusterEnvironment environment) {
        DatabaseBootstrapProperties.Couchbase config = properties.getDatabase().getCouchbase();
        log.info("Łączę z Couchbase: connectionString={}, bucket={}, scope={}",
                config.getConnectionString(), config.getBucket(), config.getScope());

        Cluster cluster = Cluster.connect(
                config.getConnectionString(),
                com.couchbase.client.java.ClusterOptions.clusterOptions(config.getUsername(), config.getPassword())
                        .environment(environment));

        cluster.waitUntilReady(Duration.ofSeconds(45),
                WaitUntilReadyOptions.waitUntilReadyOptions()
                        .serviceTypes(ServiceType.KV, ServiceType.QUERY));

        cluster.query("SELECT 1", com.couchbase.client.java.query.QueryOptions.queryOptions()
                .scanConsistency(QueryScanConsistency.REQUEST_PLUS)
                .timeout(Duration.ofSeconds(20)));

        log.info("Połączenie z Couchbase gotowe (KV/QUERY).");
        return cluster;
    }

    @Bean
    public Bucket couchbaseBucket(DatabaseBootstrapProperties properties, Cluster cluster) {
        DatabaseBootstrapProperties.Couchbase config = properties.getDatabase().getCouchbase();
        Bucket bucket = cluster.bucket(config.getBucket());
        bucket.waitUntilReady(Duration.ofSeconds(45));
        log.info("Bucket Couchbase gotowy: {}", config.getBucket());
        return bucket;
    }
}

