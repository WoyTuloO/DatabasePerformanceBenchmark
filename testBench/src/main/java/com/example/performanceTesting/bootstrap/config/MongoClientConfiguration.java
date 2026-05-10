package com.example.performanceTesting.bootstrap.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@ConditionalOnProperty(prefix = "app.database", name = "type", havingValue = "mongo")
public class MongoClientConfiguration {

    @Bean(destroyMethod = "close")
    @Primary
    public MongoClient mongoClient(DatabaseBootstrapProperties properties) {
        return MongoClients.create(properties.getDatabase().getMongo().getUri());
    }

    @Bean
    @Primary
    public MongoTemplate mongoTemplate(DatabaseBootstrapProperties properties, MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, properties.getDatabase().getMongo().getDatabase());
    }
}

