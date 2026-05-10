package com.example.performanceTesting.bootstrap.postgres;

import com.example.performanceTesting.bootstrap.DatabaseSeeder;
import com.example.performanceTesting.bootstrap.config.DatabaseBootstrapProperties;
import com.example.performanceTesting.bootstrap.config.DatabaseType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "app.database", name = "type", havingValue = "postgres")
public class PostgresDatabaseSeeder implements DatabaseSeeder {

    private static final Logger log = LoggerFactory.getLogger(PostgresDatabaseSeeder.class);
    private static final List<DateTimeFormatter> OFFSET_DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX")
    );
    private static final String TRUNCATE_SQL = """
            TRUNCATE TABLE
                shop.order_items,
                shop.order_payments,
                shop.inventory,
                shop.orders,
                shop.products,
                shop.payment_methods,
                shop.customers,
                shop.warehouses,
                shop.categories,
                shop.brands
            RESTART IDENTITY CASCADE
            """;
    private static final Map<String, SeedTarget> SEED_TARGETS = Map.of(
            "brands", new SeedTarget(
                    "brands",
                    List.of("brand_id", "name"),
                    "INSERT INTO shop.brands (brand_id, name) VALUES (?, ?)",
                    (ps, row) -> {
                        setLong(ps, 1, row, 0);
                        setString(ps, 2, row, 1);
                    }),
            "categories", new SeedTarget(
                    "categories",
                    List.of("category_id", "parent_category_id", "name"),
                    "INSERT INTO shop.categories (category_id, parent_category_id, name) VALUES (?, ?, ?)",
                    (ps, row) -> {
                        setLong(ps, 1, row, 0);
                        setNullableLong(ps, 2, row, 1);
                        setString(ps, 3, row, 2);
                    }),
            "payment_methods", new SeedTarget(
                    "payment_methods",
                    List.of("payment_method_id", "code", "name", "active"),
                    "INSERT INTO shop.payment_methods (payment_method_id, code, name, active) VALUES (?, ?, ?, ?)",
                    (ps, row) -> {
                        setLong(ps, 1, row, 0);
                        setString(ps, 2, row, 1);
                        setString(ps, 3, row, 2);
                        setBoolean(ps, 4, row, 3);
                    }),
            "warehouses", new SeedTarget(
                    "warehouses",
                    List.of("warehouse_id", "name", "city"),
                    "INSERT INTO shop.warehouses (warehouse_id, name, city) VALUES (?, ?, ?)",
                    (ps, row) -> {
                        setLong(ps, 1, row, 0);
                        setString(ps, 2, row, 1);
                        setString(ps, 3, row, 2);
                    }),
            "customers", new SeedTarget(
                    "customers",
                    List.of("customer_id", "email", "password_hash", "first_name", "last_name", "phone", "created_at"),
                    "INSERT INTO shop.customers (customer_id, email, password_hash, first_name, last_name, phone, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    (ps, row) -> {
                        setLong(ps, 1, row, 0);
                        setString(ps, 2, row, 1);
                        setString(ps, 3, row, 2);
                        setString(ps, 4, row, 3);
                        setString(ps, 5, row, 4);
                        setNullableString(ps, 6, row, 5);
                        setOffsetDateTime(ps, 7, row, 6);
                    }),
            "products", new SeedTarget(
                    "products",
                    List.of("product_id", "stock_keeping_unit", "name", "description", "brand_id", "category_id", "base_price_cents", "currency", "active", "created_at"),
                    "INSERT INTO shop.products (product_id, stock_keeping_unit, name, description, brand_id, category_id, base_price_cents, currency, active, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    (ps, row) -> {
                        setLong(ps, 1, row, 0);
                        setString(ps, 2, row, 1);
                        setString(ps, 3, row, 2);
                        setNullableString(ps, 4, row, 3);
                        setNullableLong(ps, 5, row, 4);
                        setNullableLong(ps, 6, row, 5);
                        setInt(ps, 7, row, 6);
                        setString(ps, 8, row, 7);
                        setBoolean(ps, 9, row, 8);
                        setOffsetDateTime(ps, 10, row, 9);
                    }),
            "inventory", new SeedTarget(
                    "inventory",
                    List.of("warehouse_id", "product_id", "quantity", "updated_at"),
                    "INSERT INTO shop.inventory (warehouse_id, product_id, quantity, updated_at) VALUES (?, ?, ?, ?)",
                    (ps, row) -> {
                        setLong(ps, 1, row, 0);
                        setLong(ps, 2, row, 1);
                        setInt(ps, 3, row, 2);
                        setOffsetDateTime(ps, 4, row, 3);
                    }),
            "orders", new SeedTarget(
                    "orders",
                    List.of("order_id", "customer_id", "shipping_country", "shipping_city", "shipping_postal_code", "shipping_street", "shipping_building_no", "shipping_apartment_no", "status", "total_cents", "currency", "created_at"),
                    "INSERT INTO shop.orders (order_id, customer_id, shipping_country, shipping_city, shipping_postal_code, shipping_street, shipping_building_no, shipping_apartment_no, status, total_cents, currency, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    (ps, row) -> {
                        setLong(ps, 1, row, 0);
                        setLong(ps, 2, row, 1);
                        setString(ps, 3, row, 2);
                        setString(ps, 4, row, 3);
                        setString(ps, 5, row, 4);
                        setString(ps, 6, row, 5);
                        setString(ps, 7, row, 6);
                        setNullableString(ps, 8, row, 7);
                        setString(ps, 9, row, 8);
                        setInt(ps, 10, row, 9);
                        setString(ps, 11, row, 10);
                        setOffsetDateTime(ps, 12, row, 11);
                    }),
            "order_payments", new SeedTarget(
                    "order_payments",
                    List.of("order_payment_id", "order_id", "payment_method_id", "provider", "amount_cents", "currency", "status", "paid_at", "created_at"),
                    "INSERT INTO shop.order_payments (order_payment_id, order_id, payment_method_id, provider, amount_cents, currency, status, paid_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    (ps, row) -> {
                        setLong(ps, 1, row, 0);
                        setLong(ps, 2, row, 1);
                        setLong(ps, 3, row, 2);
                        setNullableString(ps, 4, row, 3);
                        setInt(ps, 5, row, 4);
                        setString(ps, 6, row, 5);
                        setString(ps, 7, row, 6);
                        setNullableOffsetDateTime(ps, 8, row, 7);
                        setOffsetDateTime(ps, 9, row, 8);
                    }),
            "order_items", new SeedTarget(
                    "order_items",
                    List.of("order_id", "line_no", "product_id", "quantity", "unit_price_cents"),
                    "INSERT INTO shop.order_items (order_id, line_no, product_id, quantity, unit_price_cents) VALUES (?, ?, ?, ?, ?)",
                    (ps, row) -> {
                        setLong(ps, 1, row, 0);
                        setInt(ps, 2, row, 1);
                        setLong(ps, 3, row, 2);
                        setInt(ps, 4, row, 3);
                        setInt(ps, 5, row, 4);
                    })
    );
    private static final List<SequenceTarget> SEQUENCE_TARGETS = List.of(
            new SequenceTarget("brands", "brand_id"),
            new SequenceTarget("categories", "category_id"),
            new SequenceTarget("customers", "customer_id"),
            new SequenceTarget("products", "product_id"),
            new SequenceTarget("warehouses", "warehouse_id"),
            new SequenceTarget("orders", "order_id"),
            new SequenceTarget("payment_methods", "payment_method_id"),
            new SequenceTarget("order_payments", "order_payment_id")
    );
    private static final List<IndexDefinition> INDEX_DEFINITIONS = List.of(
            new IndexDefinition("idx_products_category",
                    "CREATE INDEX IF NOT EXISTS idx_products_category ON shop.products(category_id)"),
            new IndexDefinition("idx_products_brand",
                    "CREATE INDEX IF NOT EXISTS idx_products_brand ON shop.products(brand_id)"),
            new IndexDefinition("idx_products_active_brand",
                    "CREATE INDEX IF NOT EXISTS idx_products_active_brand ON shop.products(active, brand_id)"),
            new IndexDefinition("idx_orders_customer_created",
                    "CREATE INDEX IF NOT EXISTS idx_orders_customer_created ON shop.orders(customer_id, created_at DESC)"),
            new IndexDefinition("idx_order_items_product",
                    "CREATE INDEX IF NOT EXISTS idx_order_items_product ON shop.order_items(product_id)"),
            new IndexDefinition("idx_inventory_product",
                    "CREATE INDEX IF NOT EXISTS idx_inventory_product ON shop.inventory(product_id)"),
            new IndexDefinition("idx_inventory_product_quantity",
                    "CREATE INDEX IF NOT EXISTS idx_inventory_product_quantity ON shop.inventory(product_id, quantity)"),
            new IndexDefinition("idx_order_payments_order",
                    "CREATE INDEX IF NOT EXISTS idx_order_payments_order ON shop.order_payments(order_id)"),
            new IndexDefinition("idx_order_payments_order_created",
                    "CREATE INDEX IF NOT EXISTS idx_order_payments_order_created ON shop.order_payments(order_id, created_at)"),
            new IndexDefinition("idx_order_payments_method",
                    "CREATE INDEX IF NOT EXISTS idx_order_payments_method ON shop.order_payments(payment_method_id)"),
            new IndexDefinition("idx_warehouses_city",
                    "CREATE INDEX IF NOT EXISTS idx_warehouses_city ON shop.warehouses(city)")
    );

    private final DatabaseBootstrapProperties properties;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final Resource schemaResource;
    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    public PostgresDatabaseSeeder(
            DatabaseBootstrapProperties properties,
            DataSource dataSource,
            JdbcTemplate jdbcTemplate,
            @Value("classpath:schema.txt") Resource schemaResource) {
        this.properties = properties;
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.schemaResource = schemaResource;
    }

    @Override
    public DatabaseType type() {
        return DatabaseType.POSTGRES;
    }

    @Override
    public void bootstrap() {
        ensureSchemaExists();

        if (properties.getSeed().isResetBeforeLoad()) {
            reset();
        }

        seed(properties.getSeed().getDataset());
        syncSequences();
        applyIndexPolicy();
    }

    private void applyIndexPolicy() {
        if (properties.getDatabase().getPostgres().isIndexes()) {
            createIndexes();
            return;
        }
        dropIndexes();
    }

    private void createIndexes() {
        log.info("Włączone indeksy PostgreSQL - tworzę/utrzymuję indeksy pod operacje READ");
        for (IndexDefinition index : INDEX_DEFINITIONS) {
            jdbcTemplate.execute(index.createSql());
        }
    }

    private void dropIndexes() {
        log.info("Indeksy PostgreSQL wyłączone - usuwam indeksy pod operacje READ");
        for (IndexDefinition index : INDEX_DEFINITIONS) {
            jdbcTemplate.execute("DROP INDEX IF EXISTS shop." + index.name());
        }
    }

    private void ensureSchemaExists() {
        Integer tables = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'shop' AND table_name = 'customers'
                """,
                Integer.class);

        if (tables != null && tables > 0) {
            return;
        }

        log.info("Schemat shop nie istnieje - wykonuję inicjalizację z schema.txt");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(schemaResource);
        populator.execute(dataSource);
    }

    private void reset() {
        log.info("Resetuję stan bazy PostgreSQL przez TRUNCATE ... RESTART IDENTITY CASCADE");
        jdbcTemplate.execute(TRUNCATE_SQL);
    }

    private void seed(String dataset) {
        List<Resource> resources = resolveSeedResources(dataset);
        log.info("Znaleziono {} plików CSV do załadowania dla datasetu={}", resources.size(), dataset);

        for (Resource resource : resources) {
            SeedTarget target = resolveTarget(resource);
            importResource(resource, target);
        }
    }

    private List<Resource> resolveSeedResources(String dataset) {
        String baseLocation = properties.getSeed().getLocation().replaceAll("/+$", "");
        String pattern = "%s/%s/*.csv".formatted(baseLocation, dataset);

        try {
            Resource[] resources = resourcePatternResolver.getResources(pattern);
            List<Resource> ordered = Arrays.stream(resources)
                    .filter(Resource::exists)
                    .sorted(Comparator
                            .comparingInt(this::extractOrder)
                            .thenComparing(this::resourceName))
                    .collect(Collectors.toList());

            if (ordered.isEmpty()) {
                throw new IllegalStateException("Nie znaleziono plików CSV dla patternu: " + pattern);
            }

            return ordered;
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się odczytać zasobów datasetu: " + dataset, e);
        }
    }

    private void importResource(Resource resource, SeedTarget target) {
        String fileName = resourceName(resource);
        log.info("Importuję plik {} do tabeli shop.{}", fileName, target.tableName());

        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            long importedRows = 0;
            int batchedRows = 0;

            try (Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
                 CSVParser parser = CSVFormat.DEFAULT.builder()
                         .setIgnoreEmptyLines(true)
                         .build()
                         .parse(reader);
                 PreparedStatement preparedStatement = connection.prepareStatement(target.insertSql())) {

                boolean firstNonEmptyRecord = true;
                for (CSVRecord record : parser) {
                    if (isBlank(record)) {
                        continue;
                    }

                    if (firstNonEmptyRecord) {
                        firstNonEmptyRecord = false;
                        if (isHeader(record, target.headers())) {
                            continue;
                        }
                    }

                    target.binder().bind(preparedStatement, record);
                    preparedStatement.addBatch();
                    batchedRows++;
                    importedRows++;

                    if (batchedRows >= properties.getSeed().getBatchSize()) {
                        preparedStatement.executeBatch();
                        connection.commit();
                        preparedStatement.clearBatch();
                        batchedRows = 0;
                    }
                }

                if (batchedRows > 0) {
                    preparedStatement.executeBatch();
                    connection.commit();
                }
            } catch (Exception e) {
                connection.rollback();
                throw new IllegalStateException("Błąd importu pliku " + fileName + " do tabeli shop." + target.tableName(), e);
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }

            log.info("Zaimportowano {} rekordów z pliku {}", importedRows, fileName);
        } catch (SQLException e) {
            throw new IllegalStateException("Nie udało się zaimportować pliku " + fileName, e);
        }
    }

    private void syncSequences() {
        log.info("Synchronizuję sekwencje PostgreSQL po seedingu");
        for (SequenceTarget target : SEQUENCE_TARGETS) {
            String sql = """
                    SELECT setval(
                        pg_get_serial_sequence('shop.%s', '%s'),
                        COALESCE((SELECT MAX(%s) FROM shop.%s), 1),
                        (SELECT MAX(%s) IS NOT NULL FROM shop.%s)
                    )
                    """.formatted(
                    target.tableName(), target.idColumn(),
                    target.idColumn(), target.tableName(),
                    target.idColumn(), target.tableName());
            jdbcTemplate.execute(sql);
        }
    }

    private SeedTarget resolveTarget(Resource resource) {
        String normalized = resourceName(resource).toLowerCase(Locale.ROOT);

        if (normalized.contains("order_payments")) {
            return SEED_TARGETS.get("order_payments");
        }
        if (normalized.contains("order_items")) {
            return SEED_TARGETS.get("order_items");
        }
        if (normalized.contains("payment_methods")) {
            return SEED_TARGETS.get("payment_methods");
        }
        if (normalized.contains("warehouses")) {
            return SEED_TARGETS.get("warehouses");
        }
        if (normalized.contains("categories")) {
            return SEED_TARGETS.get("categories");
        }
        if (normalized.contains("customers")) {
            return SEED_TARGETS.get("customers");
        }
        if (normalized.contains("products")) {
            return SEED_TARGETS.get("products");
        }
        if (normalized.contains("inventory")) {
            return SEED_TARGETS.get("inventory");
        }
        if (normalized.contains("_orders") || normalized.startsWith("orders")) {
            return SEED_TARGETS.get("orders");
        }
        if (normalized.contains("brands")) {
            return SEED_TARGETS.get("brands");
        }

        throw new IllegalStateException("Nieznany plik seedujący: " + resourceName(resource));
    }

    private boolean isHeader(CSVRecord record, List<String> headers) {
        if (record.size() != headers.size()) {
            return false;
        }

        for (int i = 0; i < headers.size(); i++) {
            if (!headers.get(i).equalsIgnoreCase(normalize(record.get(i)).trim())) {
                return false;
            }
        }
        return true;
    }

    private boolean isBlank(CSVRecord record) {
        for (String value : record) {
            if (!value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private int extractOrder(Resource resource) {
        String fileName = resourceName(resource);
        int underscoreIndex = fileName.indexOf('_');
        if (underscoreIndex <= 0) {
            throw new IllegalStateException("Plik seedujący nie ma prefiksu liczbowego: " + fileName);
        }
        return Integer.parseInt(fileName.substring(0, underscoreIndex));
    }

    private String resourceName(Resource resource) {
        String filename = resource.getFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalStateException("Zasób nie ma nazwy pliku: " + resource);
        }
        return filename;
    }

    private static void setLong(PreparedStatement ps, int parameterIndex, CSVRecord row, int rowIndex) throws SQLException {
        ps.setLong(parameterIndex, Long.parseLong(normalize(row.get(rowIndex)).trim()));
    }

    private static void setNullableLong(PreparedStatement ps, int parameterIndex, CSVRecord row, int rowIndex) throws SQLException {
        String value = clean(row.get(rowIndex));
        if (value == null) {
            ps.setNull(parameterIndex, Types.BIGINT);
            return;
        }
        ps.setLong(parameterIndex, Long.parseLong(value));
    }

    private static void setInt(PreparedStatement ps, int parameterIndex, CSVRecord row, int rowIndex) throws SQLException {
        ps.setInt(parameterIndex, Integer.parseInt(normalize(row.get(rowIndex)).trim()));
    }

    private static void setString(PreparedStatement ps, int parameterIndex, CSVRecord row, int rowIndex) throws SQLException {
        ps.setString(parameterIndex, normalize(row.get(rowIndex)));
    }

    private static void setNullableString(PreparedStatement ps, int parameterIndex, CSVRecord row, int rowIndex) throws SQLException {
        String value = clean(row.get(rowIndex));
        if (value == null) {
            ps.setNull(parameterIndex, Types.VARCHAR);
            return;
        }
        ps.setString(parameterIndex, value);
    }

    private static void setBoolean(PreparedStatement ps, int parameterIndex, CSVRecord row, int rowIndex) throws SQLException {
        String value = clean(row.get(rowIndex));
        boolean parsed = "t".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "1".equals(value);
        ps.setBoolean(parameterIndex, parsed);
    }

    private static void setOffsetDateTime(PreparedStatement ps, int parameterIndex, CSVRecord row, int rowIndex) throws SQLException {
        ps.setObject(parameterIndex, parseOffsetDateTime(row.get(rowIndex)), Types.TIMESTAMP_WITH_TIMEZONE);
    }

    private static void setNullableOffsetDateTime(PreparedStatement ps, int parameterIndex, CSVRecord row, int rowIndex) throws SQLException {
        String value = clean(row.get(rowIndex));
        if (value == null) {
            ps.setNull(parameterIndex, Types.TIMESTAMP_WITH_TIMEZONE);
            return;
        }
        ps.setObject(parameterIndex, parseOffsetDateTime(value), Types.TIMESTAMP_WITH_TIMEZONE);
    }

    private static OffsetDateTime parseOffsetDateTime(String value) {
        String normalized = value.trim();
        for (DateTimeFormatter formatter : OFFSET_DATE_TIME_FORMATTERS) {
            try {
                return OffsetDateTime.parse(normalized, formatter);
            } catch (Exception ignored) {
                // próbujemy kolejny format
            }
        }
        throw new IllegalArgumentException("Nieobsługiwany format daty/czasu z offsetem: " + value);
    }

    private static String clean(String value) {
        String trimmed = normalize(value).trim();
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\uFEFF", "");
    }

    private record SeedTarget(String tableName, List<String> headers, String insertSql, RowBinder binder) {
    }

    private record SequenceTarget(String tableName, String idColumn) {
    }

    private record IndexDefinition(String name, String createSql) {
    }

    @FunctionalInterface
    private interface RowBinder {
        void bind(PreparedStatement preparedStatement, CSVRecord row) throws SQLException;
    }
}




