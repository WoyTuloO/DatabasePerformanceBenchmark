package com.example.performanceTesting.bootstrap.mysql;

import com.example.performanceTesting.bootstrap.DatabaseSeeder;
import com.example.performanceTesting.bootstrap.config.DatabaseBootstrapProperties;
import com.example.performanceTesting.bootstrap.config.DatabaseType;
import org.apache.commons.csv.CSVFormat;
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
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "app.database", name = "type", havingValue = "mysql")
public class MySqlDatabaseSeeder implements DatabaseSeeder {

    private static final Logger log = LoggerFactory.getLogger(MySqlDatabaseSeeder.class);
    private static final List<DateTimeFormatter> OFFSET_DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX"),      // 2024-02-02 10:00:00+00
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX"),    // 2024-02-02 10:00:00+00:00
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ"),      // 2024-02-02 10:00:00+0000
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[Z]")     // fallback
    );
    private static final List<String> TABLES_TO_TRUNCATE = List.of(
            "order_items",
            "order_payments",
            "inventory",
            "orders",
            "products",
            "payment_methods",
            "customers",
            "warehouses",
            "categories",
            "brands"
    );
    private static final Map<String, SeedTarget> SEED_TARGETS = Map.of(
            "brands", new SeedTarget("brands", List.of("brand_id", "name"), List.of(), List.of(),
                    "INSERT INTO brands (brand_id, name) VALUES (?, ?)",
                    (ps, row) -> {
                        setLong(ps, 1, row, 0);
                        setString(ps, 2, row, 1);
                    }),
            "categories", new SeedTarget("categories", List.of("category_id", "parent_category_id", "name"), List.of(), List.of(),
                    "INSERT INTO categories (category_id, parent_category_id, name) VALUES (?, ?, ?)",
                    (ps, row) -> {
                        setLong(ps, 1, row, 0);
                        setNullableLong(ps, 2, row, 1);
                        setString(ps, 3, row, 2);
                    }),
            "payment_methods", new SeedTarget("payment_methods", List.of("payment_method_id", "code", "name", "active"), List.of(), List.of("active"),
                    "INSERT INTO payment_methods (payment_method_id, code, name, active) VALUES (?, ?, ?, ?)",
                    (ps, row) -> {
                        setLong(ps, 1, row, 0);
                        setString(ps, 2, row, 1);
                        setString(ps, 3, row, 2);
                        setBoolean(ps, 4, row, 3);
                    }),
            "warehouses", new SeedTarget("warehouses", List.of("warehouse_id", "name", "city"), List.of(), List.of(),
                    "INSERT INTO warehouses (warehouse_id, name, city) VALUES (?, ?, ?)",
                    (ps, row) -> {
                        setLong(ps, 1, row, 0);
                        setString(ps, 2, row, 1);
                        setString(ps, 3, row, 2);
                    }),
            "customers", new SeedTarget("customers", List.of("customer_id", "email", "password_hash", "first_name", "last_name", "phone", "created_at"), List.of("created_at"), List.of(),
                    "INSERT INTO customers (customer_id, email, password_hash, first_name, last_name, phone, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    (ps, row) -> {
                        setLong(ps, 1, row, 0);
                        setString(ps, 2, row, 1);
                        setString(ps, 3, row, 2);
                        setString(ps, 4, row, 3);
                        setString(ps, 5, row, 4);
                        setNullableString(ps, 6, row, 5);
                        setOffsetDateTime(ps, 7, row, 6);
                    }),
            "products", new SeedTarget("products", List.of("product_id", "stock_keeping_unit", "name", "description", "brand_id", "category_id", "base_price_cents", "currency", "active", "created_at"), List.of("created_at"), List.of("active"),
                    "INSERT INTO products (product_id, stock_keeping_unit, name, description, brand_id, category_id, base_price_cents, currency, active, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
            "inventory", new SeedTarget("inventory", List.of("warehouse_id", "product_id", "quantity", "updated_at"), List.of("updated_at"), List.of(),
                    "INSERT INTO inventory (warehouse_id, product_id, quantity, updated_at) VALUES (?, ?, ?, ?)",
                    (ps, row) -> {
                        setLong(ps, 1, row, 0);
                        setLong(ps, 2, row, 1);
                        setInt(ps, 3, row, 2);
                        setOffsetDateTime(ps, 4, row, 3);
                    }),
            "orders", new SeedTarget("orders", List.of("order_id", "customer_id", "shipping_country", "shipping_city", "shipping_postal_code", "shipping_street", "shipping_building_no", "shipping_apartment_no", "status", "total_cents", "currency", "created_at"), List.of("created_at"), List.of(),
                    "INSERT INTO orders (order_id, customer_id, shipping_country, shipping_city, shipping_postal_code, shipping_street, shipping_building_no, shipping_apartment_no, status, total_cents, currency, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
            "order_payments", new SeedTarget("order_payments", List.of("order_payment_id", "order_id", "payment_method_id", "provider", "amount_cents", "currency", "status", "paid_at", "created_at"), List.of("paid_at", "created_at"), List.of(),
                    "INSERT INTO order_payments (order_payment_id, order_id, payment_method_id, provider, amount_cents, currency, status, paid_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
            "order_items", new SeedTarget("order_items", List.of("order_id", "line_no", "product_id", "quantity", "unit_price_cents"), List.of(), List.of(),
                    "INSERT INTO order_items (order_id, line_no, product_id, quantity, unit_price_cents) VALUES (?, ?, ?, ?, ?)",
                    (ps, row) -> {
                        setLong(ps, 1, row, 0);
                        setInt(ps, 2, row, 1);
                        setLong(ps, 3, row, 2);
                        setInt(ps, 4, row, 3);
                        setInt(ps, 5, row, 4);
                    })
    );
    private static final List<IndexDefinition> INDEX_DEFINITIONS = List.of(
            new IndexDefinition("products", "idx_products_category", "CREATE INDEX idx_products_category ON products (category_id)"),
            new IndexDefinition("products", "idx_products_brand_active_name", "CREATE INDEX idx_products_brand_active_name ON products (brand_id, active, name)"),
            new IndexDefinition("orders", "idx_orders_customer_created_order", "CREATE INDEX idx_orders_customer_created_order ON orders (customer_id, created_at DESC, order_id)"),
            new IndexDefinition("order_items", "idx_order_items_product", "CREATE INDEX idx_order_items_product ON order_items (product_id)"),
            new IndexDefinition("inventory", "idx_inventory_product_quantity", "CREATE INDEX idx_inventory_product_quantity ON inventory (product_id, quantity)"),
            new IndexDefinition("inventory", "idx_inventory_warehouse_quantity_product", "CREATE INDEX idx_inventory_warehouse_quantity_product ON inventory (warehouse_id, quantity, product_id)"),
            new IndexDefinition("order_payments", "idx_order_payments_order_created", "CREATE INDEX idx_order_payments_order_created ON order_payments (order_id, created_at)"),
            new IndexDefinition("order_payments", "idx_order_payments_method_order", "CREATE INDEX idx_order_payments_method_order ON order_payments (payment_method_id, order_id)"),
            new IndexDefinition("warehouses", "idx_warehouses_city_id", "CREATE INDEX idx_warehouses_city_id ON warehouses (city, warehouse_id)")
    );

    private final DatabaseBootstrapProperties properties;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final Resource schemaResource;
    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    public MySqlDatabaseSeeder(
            DatabaseBootstrapProperties properties,
            DataSource dataSource,
            JdbcTemplate jdbcTemplate,
            @Value("classpath:schema-mysql.sql") Resource schemaResource) {
        this.properties = properties;
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.schemaResource = schemaResource;
    }

    @Override
    public DatabaseType type() {
        return DatabaseType.MYSQL;
    }

    @Override
    public void bootstrap() {
        ensureSchemaExists();

        if (properties.getSeed().isResetBeforeLoad()) {
            reset();
        }

        seed(properties.getSeed().getDataset());
        applyIndexPolicy();
    }

    private void applyIndexPolicy() {
        if (properties.getDatabase().getMysql().isIndexes()) {
            createIndexes();
            return;
        }
        dropIndexes();
    }

    private void createIndexes() {
        log.info("Włączone indeksy MySQL - tworzę/utrzymuję indeksy pod operacje READ");
        for (IndexDefinition index : INDEX_DEFINITIONS) {
            if (!indexExists(index.tableName(), index.indexName())) {
                jdbcTemplate.execute(index.createSql());
            }
            log.info("Dodano indeks");

        }
    }

    private void dropIndexes() {
        log.info("Indeksy MySQL wyłączone - usuwam indeksy pod operacje READ");
        for (IndexDefinition index : INDEX_DEFINITIONS) {
            if (indexExists(index.tableName(), index.indexName())) {
                try {
                    jdbcTemplate.execute("DROP INDEX " + index.indexName() + " ON " + index.tableName());
                } catch (Exception e) {
                    if (isForeignKeyRequiredIndexError(e)) {
                        log.warn("Pomijam usunięcie indeksu {}.{} - wymagany przez constraint FK", index.tableName(), index.indexName());
                        continue;
                    }
                    throw e;
                }
            }
        }
    }

    private boolean isForeignKeyRequiredIndexError(Exception e) {
        String message = e.getMessage();
        return message != null
                && message.contains("needed in a foreign key constraint");
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """,
                Integer.class,
                tableName,
                indexName);
        return count != null && count > 0;
    }

    private void ensureSchemaExists() {
        Integer tables = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = 'customers'
                """,
                Integer.class);

        if (tables != null && tables > 0) {
            return;
        }

        log.info("Tabele MySQL nie istnieją - wykonuję inicjalizację z schema-mysql.sql");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(schemaResource);
        populator.execute(dataSource);
    }

    private void reset() {
        log.info("Resetuję stan bazy MySQL przez TRUNCATE");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : TABLES_TO_TRUNCATE) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table);
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    private void seed(String dataset) {
        List<Resource> resources = resolveSeedResources(dataset);
        log.info("Znaleziono {} plików CSV do załadowania dla datasetu={}", resources.size(), dataset);

        // Wyłączamy foreign key checks raz dla całego procesu seedowania
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("SET unique_checks = 0");
        jdbcTemplate.execute("SET autocommit = 0");
        try {
            jdbcTemplate.execute("SET sql_log_bin = 0"); // Wyłącz binary logging (jeśli mamy uprawnienia)
        } catch (Exception e) {
            log.debug("Nie udało się wyłączyć sql_log_bin (brak uprawnień SUPER), kontynuuję bez tej optymalizacji");
        }
        
        try {
            for (Resource resource : resources) {
                SeedTarget target = resolveTarget(resource);
                importResource(resource, target);
            }
        } finally {
            // Włączamy z powrotem po zakończeniu
            try {
                jdbcTemplate.execute("SET sql_log_bin = 1");
            } catch (Exception ignored) {
            }
            jdbcTemplate.execute("SET autocommit = 1");
            jdbcTemplate.execute("SET unique_checks = 1");
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        }

    }



    private List<Resource> resolveSeedResources(String dataset) {
        String baseLocation = properties.getSeed().getLocation().replaceAll("/+$", "");
        String pattern = "%s/%s/*.csv".formatted(baseLocation, dataset);

        try {
            Resource[] resources = resourcePatternResolver.getResources(pattern);
            List<Resource> ordered = Arrays.stream(resources)
                    .filter(Resource::exists)
                    .sorted(Comparator.comparingInt(this::extractOrder).thenComparing(this::resourceName))
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
        log.info("Importuję plik {} do tabeli {} (LOAD DATA LOCAL INFILE)", fileName, target.tableName());

        long startTime = System.currentTimeMillis();
        
        try {
            // Liczba rekordów przed importem
            Long countBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + target.tableName(), 
                Long.class
            );
            
            // Przygotowanie pliku do importu - skopiowanie do temp file jeśli jest w classpath
            java.io.File tempFile = prepareFileForImport(resource);
            
            // Sprawdzenie czy plik ma nagłówek
            boolean hasHeader = checkIfHasHeader(tempFile, target.headers());
            
            // Konstrukcja LOAD DATA LOCAL INFILE
            String loadDataSql = buildLoadDataSql(tempFile.getAbsolutePath(), target.tableName(), target.headers(), target.dateColumns(), target.booleanColumns(), hasHeader);
            
            log.info("Wykonuję SQL: {}", loadDataSql);
            
            // Wykonanie LOAD DATA LOCAL INFILE
            jdbcTemplate.execute(loadDataSql);
            
            // Pobranie liczby wczytanych rekordów
            Long countAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + target.tableName(), 
                Long.class
            );
            
            long importedRows = (countAfter != null ? countAfter : 0) - (countBefore != null ? countBefore : 0);
            long duration = System.currentTimeMillis() - startTime;
            long rps = importedRows > 0 && duration > 0 
                ? (importedRows * 1000 / duration) : 0;
            
            log.info("✓ Zaimportowano {} rekordów z pliku {} w {}ms ({} rec/s)", 
                importedRows, fileName, duration, rps);
            
            // Usunięcie pliku tymczasowego
            if (tempFile.getAbsolutePath().contains("temp-mysql-import")) {
                if (!tempFile.delete()) {
                    log.warn("Nie udało się usunąć pliku tymczasowego: {}", tempFile.getAbsolutePath());
                }
            }
            
        } catch (Exception e) {
            throw new IllegalStateException("Błąd importu pliku " + fileName + " do tabeli " + target.tableName(), e);
        }
    }
    
    private java.io.File prepareFileForImport(Resource resource) throws IOException {
        // Jeśli zasób jest plikiem systemowym, użyj go bezpośrednio
        try {
            java.io.File file = resource.getFile();
            if (file.exists() && file.canRead()) {
                return file;
            }
        } catch (IOException ignored) {
            // Zasób nie jest zwykłym plikiem, trzeba skopiować do temp
        }
        
        // Skopiuj do pliku tymczasowego
        java.io.File tempFile = java.io.File.createTempFile("temp-mysql-import-", ".csv");
        tempFile.deleteOnExit();
        
        try (var in = resource.getInputStream();
             var out = new java.io.FileOutputStream(tempFile)) {
            in.transferTo(out);
        }
        
        return tempFile;
    }
    
    private boolean checkIfHasHeader(java.io.File file, List<String> expectedHeaders) throws IOException {
        try (var reader = new BufferedReader(new java.io.FileReader(file, StandardCharsets.UTF_8));
             var parser = CSVFormat.DEFAULT.parse(reader)) {
            
            var iterator = parser.iterator();
            if (!iterator.hasNext()) {
                return false;
            }
            
            CSVRecord firstRecord = iterator.next();
            return isHeader(firstRecord, expectedHeaders);
        }
    }
    
    private String buildLoadDataSql(String filePath, String tableName, List<String> columns, List<String> dateColumns, List<String> booleanColumns, boolean hasHeader) {
        // Escape path dla Windows
        String escapedPath = filePath.replace("\\", "\\\\");
        
        StringBuilder sql = new StringBuilder("LOAD DATA LOCAL INFILE '");
        sql.append(escapedPath);
        sql.append("' INTO TABLE ").append(tableName);
        sql.append(" CHARACTER SET utf8mb4");
        sql.append(" FIELDS TERMINATED BY ','");
        sql.append(" OPTIONALLY ENCLOSED BY '\"'");
        sql.append(" LINES TERMINATED BY '\\n'");
        
        if (hasHeader) {
            sql.append(" IGNORE 1 LINES");
        }
        
        // Jeśli są kolumny do przekonwertowania (daty lub boolean), użyj zmiennych tymczasowych
        boolean needsConversion = !dateColumns.isEmpty() || !booleanColumns.isEmpty();
        
        if (needsConversion) {
            sql.append(" (");
            List<String> columnSpecs = new ArrayList<>();
            for (String column : columns) {
                if (dateColumns.contains(column) || booleanColumns.contains(column)) {
                    // Kolumny do konwersji wczytaj do zmiennej tymczasowej
                    columnSpecs.add("@" + column + "_raw");
                } else {
                    // Normalne kolumny
                    columnSpecs.add(column);
                }
            }
            sql.append(String.join(", ", columnSpecs));
            sql.append(")");
            
            // Dodaj SET clause do konwersji
            sql.append(" SET ");
            List<String> setStatements = new ArrayList<>();
            
            // Konwersja dat
            for (String dateColumn : dateColumns) {
                // Usuń timezone z formatu YYYY-MM-DD HH:MM:SS+00 -> YYYY-MM-DD HH:MM:SS
                // MySQL rozumie format bez timezone
                // Obsługa NULL dla pustych wartości
                setStatements.add(dateColumn + " = IF(@" + dateColumn + "_raw = '', NULL, REPLACE(@" + dateColumn + "_raw, '+00', ''))");
            }
            
            // Konwersja boolean
            for (String booleanColumn : booleanColumns) {
                // Konwersja true/t/1 -> 1, false/f/0/inne -> 0
                setStatements.add(booleanColumn + " = IF(LOWER(@" + booleanColumn + "_raw) IN ('true', 't', '1'), 1, 0)");
            }
            
            sql.append(String.join(", ", setStatements));
        } else {
            // Brak kolumn do konwersji - po prostu wczytaj wszystkie kolumny
            sql.append(" (");
            sql.append(String.join(", ", columns));
            sql.append(")");
        }
        
        return sql.toString();
    }

    private SeedTarget resolveTarget(Resource resource) {
        String normalized = resourceName(resource).toLowerCase(Locale.ROOT);

        if (normalized.contains("order_payments")) return SEED_TARGETS.get("order_payments");
        if (normalized.contains("order_items")) return SEED_TARGETS.get("order_items");
        if (normalized.contains("payment_methods")) return SEED_TARGETS.get("payment_methods");
        if (normalized.contains("warehouses")) return SEED_TARGETS.get("warehouses");
        if (normalized.contains("categories")) return SEED_TARGETS.get("categories");
        if (normalized.contains("customers")) return SEED_TARGETS.get("customers");
        if (normalized.contains("products")) return SEED_TARGETS.get("products");
        if (normalized.contains("inventory")) return SEED_TARGETS.get("inventory");
        if (normalized.contains("_orders") || normalized.startsWith("orders")) return SEED_TARGETS.get("orders");
        if (normalized.contains("brands")) return SEED_TARGETS.get("brands");

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
        ps.setTimestamp(parameterIndex, Timestamp.from(parseOffsetDateTime(row.get(rowIndex)).toInstant()));
    }

    private static void setNullableOffsetDateTime(PreparedStatement ps, int parameterIndex, CSVRecord row, int rowIndex) throws SQLException {
        String value = clean(row.get(rowIndex));
        if (value == null) {
            ps.setNull(parameterIndex, Types.TIMESTAMP);
            return;
        }
        ps.setTimestamp(parameterIndex, Timestamp.from(parseOffsetDateTime(value).toInstant()));
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
        throw new IllegalArgumentException("Nieobsługiwany format daty/czasu z offsetem: '" + value + "' (znormalizowana: '" + normalized + "')");
    }

    private static String clean(String value) {
        String trimmed = normalize(value).trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\uFEFF", "");
    }

    private record SeedTarget(String tableName, List<String> headers, List<String> dateColumns, List<String> booleanColumns, String insertSql, RowBinder binder) {
    }

    @FunctionalInterface
    private interface RowBinder {
        void bind(PreparedStatement preparedStatement, CSVRecord row) throws SQLException;
    }

    private record IndexDefinition(String tableName, String indexName, String createSql) {
    }
}

