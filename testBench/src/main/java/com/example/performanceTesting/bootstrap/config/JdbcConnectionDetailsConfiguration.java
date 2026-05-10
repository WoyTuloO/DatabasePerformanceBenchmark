package com.example.performanceTesting.bootstrap.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration that provides conditional JdbcConnectionDetails beans
 * based on the selected database type. This prevents Spring from creating
 * multiple conflicting JDBC connection beans.
 */
@Configuration
public class JdbcConnectionDetailsConfiguration {

    /**
     * PostgreSQL JdbcConnectionDetails - only created when type is postgres
     */
    @Bean
    @ConditionalOnProperty(
            name = "app.database.type",
            havingValue = "postgres",
            matchIfMissing = false
    )
    public JdbcConnectionDetails postgresJdbcConnectionDetails(DatabaseBootstrapProperties properties) {
        DatabaseBootstrapProperties.Jdbc config = properties.getDatabase().getPostgres();
        return new JdbcConnectionDetails() {
            @Override
            public String getUsername() {
                return config.getUsername();
            }

            @Override
            public String getPassword() {
                return config.getPassword();
            }

            @Override
            public String getJdbcUrl() {
                return config.getUrl();
            }

            @Override
            public String getDriverClassName() {
                return config.getDriverClassName();
            }
        };
    }

    /**
     * MySQL JdbcConnectionDetails - only created when type is mysql
     */
    @Bean
    @ConditionalOnProperty(
            name = "app.database.type",
            havingValue = "mysql",
            matchIfMissing = false
    )
    public JdbcConnectionDetails mysqlJdbcConnectionDetails(DatabaseBootstrapProperties properties) {
        DatabaseBootstrapProperties.Jdbc config = properties.getDatabase().getMysql();
        return new JdbcConnectionDetails() {
            @Override
            public String getUsername() {
                return config.getUsername();
            }

            @Override
            public String getPassword() {
                return config.getPassword();
            }

            @Override
            public String getJdbcUrl() {
                return config.getUrl();
            }

            @Override
            public String getDriverClassName() {
                return config.getDriverClassName();
            }
        };
    }
}




