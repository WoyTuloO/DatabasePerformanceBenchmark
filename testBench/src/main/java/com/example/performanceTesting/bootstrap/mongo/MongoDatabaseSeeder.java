package com.example.performanceTesting.bootstrap.mongo;

import com.example.performanceTesting.bootstrap.DatabaseSeeder;
import com.example.performanceTesting.bootstrap.config.DatabaseBootstrapProperties;
import com.example.performanceTesting.bootstrap.config.DatabaseType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "app.database", name = "type", havingValue = "mongo")
public class MongoDatabaseSeeder implements DatabaseSeeder {

    private static final Logger log = LoggerFactory.getLogger(MongoDatabaseSeeder.class);
    private static final List<DateTimeFormatter> OFFSET_DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX")
    );
    private static final List<MongoIndexDefinition> INDEX_DEFINITIONS = List.of(
            new MongoIndexDefinition("customers", "idx_customers_customer_id", new Index().on("customer_id", Sort.Direction.ASC).unique().named("idx_customers_customer_id")),
            new MongoIndexDefinition("customers", "idx_customers_email", new Index().on("email", Sort.Direction.ASC).unique().named("idx_customers_email")),

            new MongoIndexDefinition("brands", "idx_brands_brand_id", new Index().on("brand_id", Sort.Direction.ASC).unique().named("idx_brands_brand_id")),
            new MongoIndexDefinition("brands", "idx_brands_name", new Index().on("name", Sort.Direction.ASC).named("idx_brands_name")),

            new MongoIndexDefinition("categories", "idx_categories_category_id", new Index().on("category_id", Sort.Direction.ASC).unique().named("idx_categories_category_id")),

            new MongoIndexDefinition("warehouses", "idx_warehouses_warehouse_id", new Index().on("warehouse_id", Sort.Direction.ASC).unique().named("idx_warehouses_warehouse_id")),
            new MongoIndexDefinition("warehouses", "idx_warehouses_city", new Index().on("city", Sort.Direction.ASC).named("idx_warehouses_city")),

            new MongoIndexDefinition("products", "idx_products_product_id", new Index().on("product_id", Sort.Direction.ASC).unique().named("idx_products_product_id")),
            new MongoIndexDefinition("products", "idx_products_brand_active", new Index().on("brand_id", Sort.Direction.ASC).on("active", Sort.Direction.ASC).named("idx_products_brand_active")),
            new MongoIndexDefinition("products", "idx_products_category", new Index().on("category_id", Sort.Direction.ASC).named("idx_products_category")),

            new MongoIndexDefinition("inventory", "idx_inventory_warehouse_product", new Index().on("warehouse_id", Sort.Direction.ASC).on("product_id", Sort.Direction.ASC).unique().named("idx_inventory_warehouse_product")),
            new MongoIndexDefinition("inventory", "idx_inventory_product_quantity", new Index().on("product_id", Sort.Direction.ASC).on("quantity", Sort.Direction.ASC).named("idx_inventory_product_quantity")),

            new MongoIndexDefinition("orders", "idx_orders_order_id", new Index().on("order_id", Sort.Direction.ASC).unique().named("idx_orders_order_id")),
            new MongoIndexDefinition("orders", "idx_orders_customer_created", new Index().on("customer_id", Sort.Direction.ASC).on("created_at", Sort.Direction.DESC).named("idx_orders_customer_created")),

            new MongoIndexDefinition("order_items", "idx_order_items_order_line", new Index().on("order_id", Sort.Direction.ASC).on("line_no", Sort.Direction.ASC).named("idx_order_items_order_line")),
            new MongoIndexDefinition("order_items", "idx_order_items_product", new Index().on("product_id", Sort.Direction.ASC).named("idx_order_items_product")),

            new MongoIndexDefinition("order_payments", "idx_order_payments_order_payment_id", new Index().on("order_payment_id", Sort.Direction.ASC).unique().named("idx_order_payments_order_payment_id")),
            new MongoIndexDefinition("order_payments", "idx_order_payments_order_created", new Index().on("order_id", Sort.Direction.ASC).on("created_at", Sort.Direction.ASC).named("idx_order_payments_order_created")),
            new MongoIndexDefinition("order_payments", "idx_order_payments_method_order", new Index().on("payment_method_id", Sort.Direction.ASC).on("order_id", Sort.Direction.ASC).named("idx_order_payments_method_order")),

            new MongoIndexDefinition("payment_methods", "idx_payment_methods_payment_method_id", new Index().on("payment_method_id", Sort.Direction.ASC).unique().named("idx_payment_methods_payment_method_id")),
            new MongoIndexDefinition("payment_methods", "idx_payment_methods_code", new Index().on("code", Sort.Direction.ASC).unique().named("idx_payment_methods_code"))
    );

    private final DatabaseBootstrapProperties properties;
    private final MongoTemplate mongoTemplate;
    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public MongoDatabaseSeeder(DatabaseBootstrapProperties properties, MongoTemplate mongoTemplate) {
        this.properties = properties;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public DatabaseType type() {
        return DatabaseType.MONGO;
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
        if (properties.getDatabase().getMongo().isIndexes()) {
            createIndexes();
            return;
        }
        dropIndexes();
    }

    private void createIndexes() {
        log.info("Włączone indeksy MongoDB - tworzę/utrzymuję indeksy pod operacje READ");
        for (MongoIndexDefinition definition : INDEX_DEFINITIONS) {
            mongoTemplate.indexOps(definition.collection()).ensureIndex(definition.index());
        }
    }

    private void dropIndexes() {
        log.info("Indeksy MongoDB wyłączone - usuwam indeksy pod operacje READ");
        for (MongoIndexDefinition definition : INDEX_DEFINITIONS) {
            try {
                mongoTemplate.indexOps(definition.collection()).dropIndex(definition.indexName());
            } catch (RuntimeException ignored) {
                // indeks mógł nie istnieć
            }
        }
    }

    private void reset() {
        log.info("Resetuję MongoDB przez dropDatabase");
        mongoTemplate.getDb().drop();
    }

    private void seed(String dataset) {
        List<Resource> resources = resolveSeedResources(dataset);
        log.info("Znaleziono {} plików CSV do załadowania dla datasetu={}", resources.size(), dataset);

        for (Resource resource : resources) {
            String fileName = resourceName(resource);
            String collection = resolveCollectionName(fileName);
            importResource(resource, fileName, collection);
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

    private void importResource(Resource resource, String fileName, String collectionName) {
        log.info("Importuję plik {} do kolekcji {}", fileName, collectionName);

        int batchSize = properties.getSeed().getBatchSize();
        long imported = 0;
        List<Document> batch = new ArrayList<>(Math.max(100, batchSize));

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

                batch.add(toDocument(collectionName, row));
                imported++;

                if (batch.size() >= batchSize) {
                    insertBatch(collectionName, batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                insertBatch(collectionName, batch);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Błąd importu pliku " + fileName + " do kolekcji " + collectionName, e);
        }

        log.info("Zaimportowano {} rekordów z pliku {}", imported, fileName);
    }

    private void insertBatch(String collectionName, List<Document> documents) {
        BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, collectionName);
        bulk.insert(documents);
        bulk.execute();
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

    private Document toDocument(String collection, CSVRecord row) {
        Document doc = switch (collection) {
            case "brands" -> new Document("brand_id", toLong(row, 0))
                    .append("name", normalize(row.get(1)));
            case "categories" -> new Document("category_id", toLong(row, 0))
                    .append("parent_category_id", toNullableLong(row, 1))
                    .append("name", normalize(row.get(2)));
            case "payment_methods" -> new Document("payment_method_id", toLong(row, 0))
                    .append("code", normalize(row.get(1)))
                    .append("name", normalize(row.get(2)))
                    .append("active", toBoolean(row, 3));
            case "warehouses" -> new Document("warehouse_id", toLong(row, 0))
                    .append("name", normalize(row.get(1)))
                    .append("city", normalize(row.get(2)));
            case "customers" -> new Document("customer_id", toLong(row, 0))
                    .append("email", normalize(row.get(1)))
                    .append("password_hash", normalize(row.get(2)))
                    .append("first_name", normalize(row.get(3)))
                    .append("last_name", normalize(row.get(4)))
                    .append("phone", toNullableString(row, 5))
                    .append("created_at", toDate(row, 6));
            case "products" -> new Document("product_id", toLong(row, 0))
                    .append("stock_keeping_unit", normalize(row.get(1)))
                    .append("name", normalize(row.get(2)))
                    .append("description", toNullableString(row, 3))
                    .append("brand_id", toNullableLong(row, 4))
                    .append("category_id", toNullableLong(row, 5))
                    .append("base_price_cents", toInt(row, 6))
                    .append("currency", normalize(row.get(7)))
                    .append("active", toBoolean(row, 8))
                    .append("created_at", toDate(row, 9));
            case "inventory" -> new Document("warehouse_id", toLong(row, 0))
                    .append("product_id", toLong(row, 1))
                    .append("quantity", toInt(row, 2))
                    .append("updated_at", toDate(row, 3));
            case "orders" -> new Document("order_id", toLong(row, 0))
                    .append("customer_id", toLong(row, 1))
                    .append("shipping_country", normalize(row.get(2)))
                    .append("shipping_city", normalize(row.get(3)))
                    .append("shipping_postal_code", normalize(row.get(4)))
                    .append("shipping_street", normalize(row.get(5)))
                    .append("shipping_building_no", normalize(row.get(6)))
                    .append("shipping_apartment_no", toNullableString(row, 7))
                    .append("status", normalize(row.get(8)))
                    .append("total_cents", toInt(row, 9))
                    .append("currency", normalize(row.get(10)))
                    .append("created_at", toDate(row, 11));
            case "order_payments" -> new Document("order_payment_id", toLong(row, 0))
                    .append("order_id", toLong(row, 1))
                    .append("payment_method_id", toLong(row, 2))
                    .append("provider", toNullableString(row, 3))
                    .append("amount_cents", toInt(row, 4))
                    .append("currency", normalize(row.get(5)))
                    .append("status", normalize(row.get(6)))
                    .append("paid_at", toNullableDate(row, 7))
                    .append("created_at", toDate(row, 8));
            case "order_items" -> new Document("order_id", toLong(row, 0))
                    .append("line_no", toInt(row, 1))
                    .append("product_id", toLong(row, 2))
                    .append("quantity", toInt(row, 3))
                    .append("unit_price_cents", toInt(row, 4));
            default -> throw new IllegalStateException("Nieobsługiwana kolekcja: " + collection);
        };
        return doc.append("_id", toDocumentId(collection, row));
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

    private static long toLong(CSVRecord row, int index) {
        return Long.parseLong(normalize(row.get(index)).trim());
    }

    private static Long toNullableLong(CSVRecord row, int index) {
        String value = clean(row.get(index));
        return value == null ? null : Long.parseLong(value);
    }

    private static int toInt(CSVRecord row, int index) {
        return Integer.parseInt(normalize(row.get(index)).trim());
    }

    private static boolean toBoolean(CSVRecord row, int index) {
        String value = clean(row.get(index));
        return "t".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private static String toNullableString(CSVRecord row, int index) {
        return clean(row.get(index));
    }

    private static Date toDate(CSVRecord row, int index) {
        return Date.from(parseOffsetDateTime(row.get(index)).toInstant());
    }

    private static Date toNullableDate(CSVRecord row, int index) {
        String value = clean(row.get(index));
        return value == null ? null : Date.from(parseOffsetDateTime(value).toInstant());
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
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\uFEFF", "");
    }

    private record MongoIndexDefinition(String collection, String indexName, Index index) {
    }
}