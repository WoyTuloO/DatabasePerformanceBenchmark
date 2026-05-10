package com.example.performanceTesting.shop.application;
import com.example.performanceTesting.bootstrap.config.DatabaseBootstrapProperties;
import com.example.performanceTesting.bootstrap.config.DatabaseType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
@Configuration
public class ShopServiceSelectorConfiguration {

    @Bean
    @Primary
    public ShopService shopService(DatabaseBootstrapProperties properties, List<DatabaseAwareShopService> services) {
        DatabaseType targetType = properties.getDatabase().getType();
        return services.stream()
                .filter(service -> service.type() == targetType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Brak implementacji ShopService dla typu bazy: " + targetType));
    }

}