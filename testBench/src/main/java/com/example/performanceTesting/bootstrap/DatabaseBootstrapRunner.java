package com.example.performanceTesting.bootstrap;

import com.example.performanceTesting.bootstrap.config.DatabaseBootstrapProperties;
import com.example.performanceTesting.bootstrap.config.DatabaseType;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(0)
public class DatabaseBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBootstrapRunner.class);

    private final DatabaseBootstrapProperties properties;
    private final List<DatabaseSeeder> databaseSeeders;

    public DatabaseBootstrapRunner(DatabaseBootstrapProperties properties, List<DatabaseSeeder> databaseSeeders) {
        this.properties = properties;
        this.databaseSeeders = databaseSeeders;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (!properties.getSeed().isEnabled()) {
            log.info("Auto-seeding bazy jest wyłączony (app.seed.enabled=false).");
            return;
        }

        DatabaseType targetType = properties.getDatabase().getType();
        DatabaseSeeder seeder = databaseSeeders.stream()
                .filter(candidate -> candidate.type() == targetType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Brak implementacji DatabaseSeeder dla typu bazy: " + targetType));

        log.info("Uruchamiam bootstrap bazy dla typu={} i datasetu={}", targetType, properties.getSeed().getDataset());
        seeder.bootstrap();
    }
}


