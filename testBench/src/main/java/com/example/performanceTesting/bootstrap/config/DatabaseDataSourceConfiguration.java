package com.example.performanceTesting.bootstrap.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@ConditionalOnExpression("'${app.database.type}'.equalsIgnoreCase('postgres') || '${app.database.type}'.equalsIgnoreCase('mysql')")
public class DatabaseDataSourceConfiguration {

    @Bean
    @Primary
    public DataSource dataSource(DatabaseBootstrapProperties properties) {
        DatabaseBootstrapProperties.Jdbc config = switch (properties.getDatabase().getType()) {
            case MYSQL -> properties.getDatabase().getMysql();
            case POSTGRES -> properties.getDatabase().getPostgres();
            default -> throw new IllegalStateException(
                    "Backend " + properties.getDatabase().getType() + " nie wspiera JDBC DataSource");
        };

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(config.getUrl());
        dataSource.setUsername(config.getUsername());
        dataSource.setPassword(config.getPassword());
        dataSource.setDriverClassName(config.getDriverClassName());

        if (properties.getDatabase().getType() == DatabaseType.POSTGRES) {
            dataSource.setConnectionInitSql("SET search_path TO shop,public");
        }

        return dataSource;
    }
}

