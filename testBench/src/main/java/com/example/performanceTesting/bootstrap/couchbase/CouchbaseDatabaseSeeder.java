package com.example.performanceTesting.bootstrap.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.example.performanceTesting.bootstrap.DatabaseSeeder;
import com.example.performanceTesting.bootstrap.config.DatabaseBootstrapProperties;
import com.example.performanceTesting.bootstrap.config.DatabaseType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "app.database", name = "type", havingValue = "couchbase")
public class CouchbaseDatabaseSeeder implements DatabaseSeeder {

    private static final Logger log = LoggerFactory.getLogger(CouchbaseDatabaseSeeder.class);
    private static final List<DateTimeFormatter> OFFSET_DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX")
    );

    private final DatabaseBootstrapProperties properties;
    private final Cluster cluster;
    private final Bucket bucket;
    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public CouchbaseDatabaseSeeder(DatabaseBootstrapProperties properties, Cluster cluster, Bucket bucket) {
        this.properties = properties;
        this.cluster = cluster;
        this.bucket = bucket;
    }

    @Override
    public DatabaseType type() {
        return DatabaseType.COUCHBASE;
    }

    @Override
    public void bootstrap() {
        if (properties.getSeed().isResetBeforeLoad()) {
            reset();
        }
        seed(properties.getSeed().getDataset());
        applyIndexPolicy();
    }

    private void applyIndexPolicy() {
        if (properties.getDatabase().getCouchbase().isIndexes()) {
            createIndexes();
            return;
        }
        dropIndexes();
    }

    private void createIndexes() {
        log.info("Włączone indeksy Couchbase - tworzę/utrzymuję indeksy pod operacje READ");

        List<String> statements = List.of(
                "CREATE INDEX IF NOT EXISTS idx_brands_name ON %s(name)".formatted(c("brands")),
                "CREATE INDEX IF NOT EXISTS idx_warehouses_city ON %s(city)".formatted(c("warehouses")),
                "CREATE INDEX IF NOT EXISTS idx_products_brand_active ON %s(brand_id, active)".formatted(c("products")),
                "CREATE INDEX IF NOT EXISTS idx_products_category ON %s(category_id)".formatted(c("products")),
                "CREATE INDEX IF NOT EXISTS idx_products_active ON %s(active)".formatted(c("products")),
                "CREATE INDEX IF NOT EXISTS idx_inventory_product ON %s(product_id)".formatted(c("inventory")),
                "CREATE INDEX IF NOT EXISTS idx_inventory_product_quantity ON %s(product_id, quantity)".formatted(c("inventory")),
                "CREATE INDEX IF NOT EXISTS idx_orders_customer_created ON %s(customer_id, created_at DESC)".formatted(c("orders")),
                "CREATE INDEX IF NOT EXISTS idx_order_items_order_line ON %s(order_id, line_no)".formatted(c("order_items")),
                "CREATE INDEX IF NOT EXISTS idx_order_items_product ON %s(product_id)".formatted(c("order_items")),
                "CREATE INDEX IF NOT EXISTS idx_order_payments_order ON %s(order_id)".formatted(c("order_payments")),
                "CREATE INDEX IF NOT EXISTS idx_order_payments_order_created ON %s(order_id, created_at)".formatted(c("order_payments")),
                "CREATE INDEX IF NOT EXISTS idx_order_payments_method ON %s(payment_method_id)".formatted(c("order_payments")),
                "CREATE INDEX IF NOT EXISTS idx_payment_methods_code ON %s(code)".formatted(c("payment_methods")),
                "CREATE INDEX IF NOT EXISTS idx_customers_email ON %s(email)".formatted(c("customers"))
        );

        for (String statement : statements) {
            cluster.query(statement);
        }
    }

    private void dropIndexes() {
        log.info("Indeksy Couchbase wyłączone - usuwam indeksy pod operacje READ");
        String bucketName = properties.getDatabase().getCouchbase().getBucket();
        String scopeName = properties.getDatabase().getCouchbase().getScope();

        List<String> indexNames = List.of(
                "idx_brands_name",
                "idx_warehouses_city",
                "idx_products_brand_active",
                "idx_products_category",
                "idx_products_active",
                "idx_inventory_product",
                "idx_inventory_product_quantity",
                "idx_orders_customer_created",
                "idx_order_items_order_line",
                "idx_order_items_product",
                "idx_order_payments_order",
                "idx_order_payments_order_created",
                "idx_order_payments_method",
                "idx_payment_methods_code",
                "idx_customers_email"
        );

        for (String indexName : indexNames) {
            cluster.query("DROP INDEX IF EXISTS `%s`.`%s`.`%s`".formatted(bucketName, scopeName, indexName));
        }
    }

    private void reset() {
        String bucketName = properties.getDatabase().getCouchbase().getBucket();
        log.info("Resetuję Couchbase przez flush bucketa {}", bucketName);
        cluster.buckets().flushBucket(bucketName);
    }

    private void seed(String dataset) {
        List<Resource> resources = resolveSeedResources(dataset);
        log.info("Znaleziono {} plików CSV do załadowania dla datasetu={}", resources.size(), dataset);

        for (Resource resource : resources) {
            String fileName = resourceName(resource);
            String collectionName = resolveCollectionName(fileName);
            Collection collection = bucket.scope(properties.getDatabase().getCouchbase().getScope()).collection(collectionName);
            importResource(resource, collectionName, collection);
        }
    }

    private List<Resource> resolveSeedResources(String dataset) {
        String baseLocation = properties.getSeed().getLocation().replaceAll("/+$", "");
        String pattern = "%s/%s/*.csv".formatted(baseLocation, dataset);

        try {
            Resource[] resources = resolver.getResources(pattern);
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

    private void importResource(Resource resource, String collectionName, Collection collection) {
        String fileName = resourceName(resource);
        log.info("Importuję plik {} do kolekcji {}", fileName, collectionName);

        long imported = 0;
        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(true).build().parse(reader)) {

            boolean headerSkipped = false;
            for (CSVRecord row : parser) {
                if (isBlank(row)) {
                    continue;
                }
                if (!headerSkipped && looksLikeHeader(row)) {
                    headerSkipped = true;
                    continue;
                }
                headerSkipped = true;

                JsonObject doc = toDocument(collectionName, row);
                String id = toDocumentId(collectionName, row);
                collection.upsert(id, doc);
                imported++;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Błąd importu pliku " + fileName + " do kolekcji " + collectionName, e);
        }

        log.info("Zaimportowano {} rekordów z pliku {}", imported, fileName);
    }

    private String resolveCollectionName(String fileName) {
        String normalized = fileName.toLowerCase(Locale.ROOT);
        if (normalized.contains("order_payments")) return "order_payments";
        if (normalized.contains("order_items")) return "order_items";
        if (normalized.contains("payment_methods")) return "payment_methods";
        if (normalized.contains("warehouses")) return "warehouses";
        if (normalized.contains("categories")) return "categories";
        if (normalized.contains("customers")) return "customers";
        if (normalized.contains("products")) return "products";
        if (normalized.contains("inventory")) return "inventory";
        if (normalized.contains("_orders") || normalized.startsWith("orders")) return "orders";
        if (normalized.contains("brands")) return "brands";
        throw new IllegalStateException("Nieznany plik seedujący: " + fileName);
    }

    private JsonObject toDocument(String collection, CSVRecord row) {
        return switch (collection) {
            case "brands" -> JsonObject.create().put("brand_id", toLong(row, 0)).put("name", normalize(row.get(1)));
            case "categories" -> JsonObject.create()
                    .put("category_id", toLong(row, 0))
                    .put("parent_category_id", toNullableLong(row, 1))
                    .put("name", normalize(row.get(2)));
            case "payment_methods" -> JsonObject.create()
                    .put("payment_method_id", toLong(row, 0))
                    .put("code", normalize(row.get(1)))
                    .put("name", normalize(row.get(2)))
                    .put("active", toBoolean(row, 3));
            case "warehouses" -> JsonObject.create()
                    .put("warehouse_id", toLong(row, 0))
                    .put("name", normalize(row.get(1)))
                    .put("city", normalize(row.get(2)));
            case "customers" -> JsonObject.create()
                    .put("customer_id", toLong(row, 0))
                    .put("email", normalize(row.get(1)))
                    .put("password_hash", normalize(row.get(2)))
                    .put("first_name", normalize(row.get(3)))
                    .put("last_name", normalize(row.get(4)))
                    .put("phone", toNullableString(row, 5))
                    .put("created_at", toIsoString(row, 6));
            case "products" -> JsonObject.create()
                    .put("product_id", toLong(row, 0))
                    .put("stock_keeping_unit", normalize(row.get(1)))
                    .put("name", normalize(row.get(2)))
                    .put("description", toNullableString(row, 3))
                    .put("brand_id", toNullableLong(row, 4))
                    .put("category_id", toNullableLong(row, 5))
                    .put("base_price_cents", toInt(row, 6))
                    .put("currency", normalize(row.get(7)))
                    .put("active", toBoolean(row, 8))
                    .put("created_at", toIsoString(row, 9));
            case "inventory" -> JsonObject.create()
                    .put("warehouse_id", toLong(row, 0))
                    .put("product_id", toLong(row, 1))
                    .put("quantity", toInt(row, 2))
                    .put("updated_at", toIsoString(row, 3));
            case "orders" -> JsonObject.create()
                    .put("order_id", toLong(row, 0))
                    .put("customer_id", toLong(row, 1))
                    .put("shipping_country", normalize(row.get(2)))
                    .put("shipping_city", normalize(row.get(3)))
                    .put("shipping_postal_code", normalize(row.get(4)))
                    .put("shipping_street", normalize(row.get(5)))
                    .put("shipping_building_no", normalize(row.get(6)))
                    .put("shipping_apartment_no", toNullableString(row, 7))
                    .put("status", normalize(row.get(8)))
                    .put("total_cents", toInt(row, 9))
                    .put("currency", normalize(row.get(10)))
                    .put("created_at", toIsoString(row, 11));
            case "order_payments" -> JsonObject.create()
                    .put("order_payment_id", toLong(row, 0))
                    .put("order_id", toLong(row, 1))
                    .put("payment_method_id", toLong(row, 2))
                    .put("provider", toNullableString(row, 3))
                    .put("amount_cents", toInt(row, 4))
                    .put("currency", normalize(row.get(5)))
                    .put("status", normalize(row.get(6)))
                    .put("paid_at", toNullableIsoString(row, 7))
                    .put("created_at", toIsoString(row, 8));
            case "order_items" -> JsonObject.create()
                    .put("order_id", toLong(row, 0))
                    .put("line_no", toInt(row, 1))
                    .put("product_id", toLong(row, 2))
                    .put("quantity", toInt(row, 3))
                    .put("unit_price_cents", toInt(row, 4));
            default -> throw new IllegalStateException("Nieobsługiwana kolekcja: " + collection);
        };
    }

    private String toDocumentId(String collection, CSVRecord row) {
        return switch (collection) {
            case "brands" -> "brand::" + toLong(row, 0);
            case "categories" -> "category::" + toLong(row, 0);
            case "payment_methods" -> "payment_method::" + toLong(row, 0);
            case "warehouses" -> "warehouse::" + toLong(row, 0);
            case "customers" -> "customer::" + toLong(row, 0);
            case "products" -> "product::" + toLong(row, 0);
            case "inventory" -> "inventory::" + toLong(row, 0) + "::" + toLong(row, 1);
            case "orders" -> "order::" + toLong(row, 0);
            case "order_payments" -> "order_payment::" + toLong(row, 0);
            case "order_items" -> "order_item::" + toLong(row, 0) + "::" + toInt(row, 1);
            default -> throw new IllegalStateException("Nieobsługiwana kolekcja: " + collection);
        };
    }

    private boolean looksLikeHeader(CSVRecord row) {
        return row.stream().allMatch(value -> normalize(value).matches("[a-zA-Z_]+"));
    }

    private boolean isBlank(CSVRecord row) {
        return row.stream().allMatch(String::isBlank);
    }

    private int extractOrder(Resource resource) {
        String name = resourceName(resource);
        int idx = name.indexOf('_');
        if (idx <= 0) {
            throw new IllegalStateException("Plik seedujący nie ma prefiksu liczbowego: " + name);
        }
        return Integer.parseInt(name.substring(0, idx));
    }

    private String resourceName(Resource resource) {
        String name = resource.getFilename();
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Zasób nie ma nazwy pliku: " + resource);
        }
        return name;
    }

    private String c(String collection) {
        String bucketName = properties.getDatabase().getCouchbase().getBucket();
        String scopeName = properties.getDatabase().getCouchbase().getScope();
        return "`" + bucketName + "`.`" + scopeName + "`.`" + collection + "`";
    }

    private static long toLong(CSVRecord row, int index) {
        return Long.parseLong(normalize(row.get(index)).trim());
    }

    private static int toInt(CSVRecord row, int index) {
        return Integer.parseInt(normalize(row.get(index)).trim());
    }

    private static Long toNullableLong(CSVRecord row, int index) {
        String value = normalize(row.get(index)).trim();
        return value.isEmpty() ? null : Long.parseLong(value);
    }

    private static boolean toBoolean(CSVRecord row, int index) {
        String value = normalize(row.get(index)).trim();
        return "t".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private static String toNullableString(CSVRecord row, int index) {
        String value = normalize(row.get(index)).trim();
        return value.isEmpty() ? null : value;
    }

    private static String toIsoString(CSVRecord row, int index) {
        return parseOffsetDateTime(normalize(row.get(index))).toString();
    }

    private static String toNullableIsoString(CSVRecord row, int index) {
        String value = normalize(row.get(index)).trim();
        if (value.isEmpty()) {
            return null;
        }
        return parseOffsetDateTime(value).toString();
    }

    private static OffsetDateTime parseOffsetDateTime(String value) {
        for (DateTimeFormatter formatter : OFFSET_DATE_TIME_FORMATTERS) {
            try {
                return OffsetDateTime.parse(value.trim(), formatter);
            } catch (Exception ignored) {
                // próbujemy kolejny format
            }
        }
        throw new IllegalArgumentException("Nieobsługiwany format daty/czasu z offsetem: " + value);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\uFEFF", "");
    }
}