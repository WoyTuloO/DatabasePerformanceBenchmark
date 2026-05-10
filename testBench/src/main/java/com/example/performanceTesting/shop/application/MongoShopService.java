package com.example.performanceTesting.shop.application;

import com.example.performanceTesting.bootstrap.config.DatabaseType;
import com.example.performanceTesting.shop.model.ShopRequests;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "app.database", name = "type", havingValue = "mongo")
public class MongoShopService implements DatabaseAwareShopService {

    private final MongoTemplate mongoTemplate;

    public MongoShopService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public DatabaseType type() {
        return DatabaseType.MONGO;
    }

    @Override
    public int[] createOrdersBatch(List<ShopRequests.CreateOrder> reqs) {
        int[] out = new int[reqs.size()];
        long nextOrderId = allocateIdRange("orders", "order_id", reqs.size());

        List<Document> docs = new ArrayList<>(reqs.size());
        for (int i = 0; i < reqs.size(); i++) {
            ShopRequests.CreateOrder req = reqs.get(i);
            docs.add(new Document("_id", "order::" + nextOrderId)
                    .append("order_id", nextOrderId)
                    .append("customer_id", req.customerId())
                    .append("shipping_country", req.shippingCountry())
                    .append("shipping_city", req.shippingCity())
                    .append("shipping_postal_code", req.shippingPostalCode())
                    .append("shipping_street", req.shippingStreet())
                    .append("shipping_building_no", req.shippingBuildingNo())
                    .append("shipping_apartment_no", req.shippingApartmentNo())
                    .append("status", "NEW")
                    .append("total_cents", 0)
                    .append("currency", req.currency())
                    .append("created_at", Date.from(Instant.now())));
            out[i] = 1;
            nextOrderId++;
        }

        if (!docs.isEmpty()) {
            mongoTemplate.getCollection("orders").insertMany(docs);
        }

        return out;
    }

    @Override
    public Map<String, Object> addOrderItem(ShopRequests.AddOrderItem req) {
        int nextLineNo = nextLineNo(req.orderId());
        Document doc = new Document("_id", "order_item::" + req.orderId() + "::" + nextLineNo)
                .append("order_id", req.orderId())
                .append("line_no", nextLineNo)
                .append("product_id", req.productId())
                .append("quantity", req.quantity())
                .append("unit_price_cents", req.unitPriceCents());
        mongoTemplate.getCollection("order_items").insertOne(doc);
        return Map.of("order_id", req.orderId(), "line_no", nextLineNo);
    }

    @Override
    public Map<String, Object> createCustomer(ShopRequests.CreateCustomer req) {
        long customerId = nextId("customers", "customer_id");
        Document doc = new Document("_id", "customer::" + customerId)
                .append("customer_id", customerId)
                .append("email", req.email())
                .append("password_hash", req.passwordHash())
                .append("first_name", req.firstName())
                .append("last_name", req.lastName())
                .append("phone", req.phone())
                .append("created_at", Date.from(Instant.now()));
        mongoTemplate.getCollection("customers").insertOne(doc);
        return Map.of("customer_id", customerId);
    }

    @Override
    public Map<String, Object> createProduct(ShopRequests.CreateProduct req) {
        long productId = nextId("products", "product_id");
        Document doc = new Document("_id", "product::" + productId)
                .append("product_id", productId)
                .append("stock_keeping_unit", req.stockKeepingUnit())
                .append("name", req.name())
                .append("description", req.description())
                .append("brand_id", req.brandId())
                .append("category_id", req.categoryId())
                .append("base_price_cents", req.basePriceCents())
                .append("currency", req.currency())
                .append("active", true)
                .append("created_at", Date.from(Instant.now()));
        mongoTemplate.getCollection("products").insertOne(doc);
        return Map.of("product_id", productId);
    }

    @Override
    public Map<String, Object> upsertInventory(ShopRequests.UpsertInventory req) {
        UpdateResult result = mongoTemplate.getCollection("inventory").updateOne(
                Filters.and(
                        Filters.eq("warehouse_id", req.warehouseId()),
                        Filters.eq("product_id", req.productId())),
                new Document("$set", new Document("quantity", req.quantity())
                        .append("updated_at", Date.from(Instant.now())))
                        .append("$setOnInsert", new Document("_id", "inventory::" + req.warehouseId() + "::" + req.productId())
                                .append("warehouse_id", req.warehouseId())
                                .append("product_id", req.productId())),
                new UpdateOptions().upsert(true));

        if (result.getMatchedCount() == 0 && result.getUpsertedId() == null) {
            throw new IllegalStateException("Nie udało się wykonać upsert dla inventory");
        }

        return Map.of(
                "warehouse_id", req.warehouseId(),
                "product_id", req.productId(),
                "quantity", req.quantity());
    }

    @Override
    public Map<String, Object> createOrderPayment(ShopRequests.CreateOrderPayment req) {
        long orderPaymentId = nextId("order_payments", "order_payment_id");
        Document doc = new Document("_id", "order_payment::" + orderPaymentId)
                .append("order_payment_id", orderPaymentId)
                .append("order_id", req.orderId())
                .append("payment_method_id", req.paymentMethodId())
                .append("provider", req.provider())
                .append("amount_cents", req.amountCents())
                .append("currency", req.currency())
                .append("status", req.status())
                .append("paid_at", req.paidAt() == null ? null : Date.from(req.paidAt().toInstant()))
                .append("created_at", Date.from(Instant.now()));
        mongoTemplate.getCollection("order_payments").insertOne(doc);
        return Map.of("order_payment_id", orderPaymentId);
    }

    @Override
    public List<Map<String, Object>> getMissingProducts(long orderId) {
        List<Document> pipeline = List.of(
                new Document("$match", new Document("order_id", orderId)),
                new Document("$lookup", new Document("from", "products")
                        .append("localField", "product_id")
                        .append("foreignField", "product_id")
                        .append("as", "product")),
                new Document("$unwind", "$product"),
                new Document("$lookup", new Document("from", "inventory")
                        .append("localField", "product_id")
                        .append("foreignField", "product_id")
                        .append("as", "inv")),
                new Document("$addFields", new Document("total_stock", new Document("$sum", "$inv.quantity"))),
                new Document("$match", new Document("$expr", new Document("$lt", List.of("$total_stock", "$quantity")))),
                new Document("$project", new Document("_id", 0)
                        .append("line_no", "$line_no")
                        .append("product_id", "$product.product_id")
                        .append("stock_keeping_unit", "$product.stock_keeping_unit")
                        .append("name", "$product.name")
                        .append("ordered_quantity", "$quantity")
                        .append("total_stock", "$total_stock")),
                new Document("$sort", new Document("line_no", 1))
        );
        return aggregate("order_items", pipeline);
    }

    @Override
    public List<Map<String, Object>> getAvailableProductsByBrandAndCity(String brandName, String city) {
        List<Document> pipeline = List.of(
                new Document("$match", new Document("active", true)),
                new Document("$lookup", new Document("from", "brands")
                        .append("localField", "brand_id")
                        .append("foreignField", "brand_id")
                        .append("as", "brand")),
                new Document("$unwind", "$brand"),
                new Document("$match", new Document("brand.name", brandName)),
                new Document("$lookup", new Document("from", "inventory")
                        .append("localField", "product_id")
                        .append("foreignField", "product_id")
                        .append("as", "inv")),
                new Document("$unwind", "$inv"),
                new Document("$match", new Document("inv.quantity", new Document("$gt", 0))),
                new Document("$lookup", new Document("from", "warehouses")
                        .append("localField", "inv.warehouse_id")
                        .append("foreignField", "warehouse_id")
                        .append("as", "warehouse")),
                new Document("$unwind", "$warehouse"),
                new Document("$match", new Document("warehouse.city", city)),
                new Document("$group", new Document("_id", "$product_id")
                        .append("product_id", new Document("$first", "$product_id"))
                        .append("stock_keeping_unit", new Document("$first", "$stock_keeping_unit"))
                        .append("name", new Document("$first", "$name"))
                        .append("base_price_cents", new Document("$first", "$base_price_cents"))
                        .append("currency", new Document("$first", "$currency"))
                        .append("active", new Document("$first", "$active"))),
                new Document("$project", new Document("_id", 0)
                        .append("product_id", 1)
                        .append("stock_keeping_unit", 1)
                        .append("name", 1)
                        .append("base_price_cents", 1)
                        .append("currency", 1)
                        .append("active", 1)),
                new Document("$sort", new Document("name", 1))
        );
        return aggregate("products", pipeline);
    }

    @Override
    public List<Map<String, Object>> getCartItems(long orderId) {
        List<Document> pipeline = List.of(
                new Document("$match", new Document("order_id", orderId)),
                new Document("$lookup", new Document("from", "products")
                        .append("localField", "product_id")
                        .append("foreignField", "product_id")
                        .append("as", "product")),
                new Document("$unwind", "$product"),
                new Document("$project", new Document("_id", 0)
                        .append("line_no", 1)
                        .append("product_id", "$product.product_id")
                        .append("stock_keeping_unit", "$product.stock_keeping_unit")
                        .append("name", "$product.name")
                        .append("quantity", 1)
                        .append("unit_price_cents", 1)
                        .append("line_total_cents", new Document("$multiply", List.of("$quantity", "$unit_price_cents")))),
                new Document("$sort", new Document("line_no", 1))
        );
        return aggregate("order_items", pipeline);
    }

    @Override
    public Map<String, Object> getProductAvailability(long productId) {
        List<Document> pipeline = List.of(
                new Document("$match", new Document("product_id", productId)),
                new Document("$lookup", new Document("from", "inventory")
                        .append("localField", "product_id")
                        .append("foreignField", "product_id")
                        .append("as", "inv")),
                new Document("$addFields", new Document("total_stock", new Document("$sum", "$inv.quantity"))),
                new Document("$project", new Document("_id", 0)
                        .append("product_id", 1)
                        .append("stock_keeping_unit", 1)
                        .append("name", 1)
                        .append("active", 1)
                        .append("total_stock", 1))
        );
        List<Map<String, Object>> rows = aggregate("products", pipeline);
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    @Override
    public Integer getCustomerEmailsByPaymentMethod(String paymentMethodCode) {
        List<Document> pipeline = List.of(
                new Document("$lookup", new Document("from", "payment_methods")
                        .append("localField", "payment_method_id")
                        .append("foreignField", "payment_method_id")
                        .append("as", "pm")),
                new Document("$unwind", "$pm"),
                new Document("$match", new Document("pm.code", paymentMethodCode)),
                new Document("$lookup", new Document("from", "orders")
                        .append("localField", "order_id")
                        .append("foreignField", "order_id")
                        .append("as", "order")),
                new Document("$unwind", "$order"),
                new Document("$lookup", new Document("from", "customers")
                        .append("localField", "order.customer_id")
                        .append("foreignField", "customer_id")
                        .append("as", "customer")),
                new Document("$unwind", "$customer"),
                new Document("$group", new Document("_id", "$customer.email")),
                new Document("$count", "totalCount")
        );

        List<Map<String, Object>> result = aggregate("order_payments", pipeline);

        return result.stream()
                .findFirst()
                .map(row -> ((Number) row.get("totalCount")).intValue())
                .orElse(0);
    }

    @Override
    public List<Map<String, Object>> getCustomerOrderDetails(long customerId) {
        List<Document> pipeline = List.of(
                new Document("$match", new Document("customer_id", customerId)),
                new Document("$lookup", new Document("from", "order_items")
                        .append("localField", "order_id")
                        .append("foreignField", "order_id")
                        .append("as", "oi")),
                new Document("$unwind", "$oi"),
                new Document("$lookup", new Document("from", "products")
                        .append("localField", "oi.product_id")
                        .append("foreignField", "product_id")
                        .append("as", "pr")),
                new Document("$unwind", "$pr"),
                new Document("$lookup", new Document("from", "brands")
                        .append("localField", "pr.brand_id")
                        .append("foreignField", "brand_id")
                        .append("as", "b")),
                new Document("$unwind", new Document("path", "$b").append("preserveNullAndEmptyArrays", true)),
                new Document("$lookup", new Document("from", "categories")
                        .append("localField", "pr.category_id")
                        .append("foreignField", "category_id")
                        .append("as", "c")),
                new Document("$unwind", new Document("path", "$c").append("preserveNullAndEmptyArrays", true)),
                new Document("$lookup", new Document("from", "order_payments")
                        .append("localField", "order_id")
                        .append("foreignField", "order_id")
                        .append("as", "op")),
                new Document("$unwind", new Document("path", "$op").append("preserveNullAndEmptyArrays", true)),
                new Document("$lookup", new Document("from", "payment_methods")
                        .append("localField", "op.payment_method_id")
                        .append("foreignField", "payment_method_id")
                        .append("as", "pm")),
                new Document("$unwind", new Document("path", "$pm").append("preserveNullAndEmptyArrays", true)),
                new Document("$project", new Document("_id", 0)
                        .append("order_id", "$order_id")
                        .append("created_at", "$created_at")
                        .append("status", "$status")
                        .append("total_cents", "$total_cents")
                        .append("currency", "$currency")
                        .append("order_payment_id", "$op.order_payment_id")
                        .append("payment_method_code", "$pm.code")
                        .append("payment_method_name", "$pm.name")
                        .append("provider", "$op.provider")
                        .append("payment_status", "$op.status")
                        .append("payment_amount", "$op.amount_cents")
                        .append("paid_at", "$op.paid_at")
                        .append("line_no", "$oi.line_no")
                        .append("quantity", "$oi.quantity")
                        .append("unit_price_cents", "$oi.unit_price_cents")
                        .append("stock_keeping_unit", "$pr.stock_keeping_unit")
                        .append("product_name", "$pr.name")
                        .append("brand_name", "$b.name")
                        .append("category_name", "$c.name")),
                new Document("$sort", new Document("created_at", -1).append("line_no", 1).append("op.created_at", 1))
        );
        return aggregate("orders", pipeline);
    }

    @Override
    public int updateCategoryPrices(long categoryId, double multiplier) {
        List<Document> products = mongoTemplate.find(
                Query.query(Criteria.where("category_id").is(categoryId)),
                Document.class,
                "products");

        int updated = 0;
        MongoCollection<Document> collection = mongoTemplate.getCollection("products");
        for (Document product : products) {
            int current = ((Number) product.getOrDefault("base_price_cents", 0)).intValue();
            int next = Math.max(0, (int) (current * multiplier));
            UpdateResult result = collection.updateOne(
                    Filters.eq("product_id", ((Number) product.get("product_id")).longValue()),
                    new Document("$set", new Document("base_price_cents", next)));
            updated += (int) result.getModifiedCount();
        }
        return updated;
    }

    @Override
    public int updateOrderStatusByPayment(long orderPaymentId, String status) {
        Document payment = mongoTemplate.findOne(
                Query.query(Criteria.where("order_payment_id").is(orderPaymentId)),
                Document.class,
                "order_payments");
        if (payment == null) {
            return 0;
        }

        long orderId = ((Number) payment.get("order_id")).longValue();
        UpdateResult result = mongoTemplate.getCollection("orders").updateMany(
                Filters.eq("order_id", orderId),
                new Document("$set", new Document("status", status)));
        return (int) result.getModifiedCount();
    }

    @Override
    public int updateProductActive(long productId, boolean active) {
        UpdateResult result = mongoTemplate.getCollection("products").updateMany(
                Filters.eq("product_id", productId),
                new Document("$set", new Document("active", active)));
        return (int) result.getModifiedCount();
    }

    @Override
    public int updateBrandPrices(long brandId, double multiplier) {
        List<Document> products = mongoTemplate.find(
                Query.query(Criteria.where("brand_id").is(brandId)),
                Document.class,
                "products");

        int updated = 0;
        MongoCollection<Document> collection = mongoTemplate.getCollection("products");
        for (Document product : products) {
            int current = ((Number) product.getOrDefault("base_price_cents", 0)).intValue();
            int next = (int) (current * multiplier);
            UpdateResult result = collection.updateOne(
                    Filters.eq("product_id", ((Number) product.get("product_id")).longValue()),
                    new Document("$set", new Document("base_price_cents", next)));
            updated += (int) result.getModifiedCount();
        }
        return updated;
    }

    @Override
    public int updateInventory(long warehouseId, long productId, int quantity) {
        UpdateResult result = mongoTemplate.getCollection("inventory").updateMany(
                Filters.and(
                        Filters.eq("warehouse_id", warehouseId),
                        Filters.eq("product_id", productId)),
                new Document("$set", new Document("quantity", quantity)
                        .append("updated_at", Date.from(Instant.now()))));
        return (int) result.getModifiedCount();
    }

    @Override
    public int cancelOrdersByPaymentMethod(String code) {
        List<Document> methods = mongoTemplate.find(
                Query.query(Criteria.where("code").is(code)),
                Document.class,
                "payment_methods");
        if (methods.isEmpty()) {
            return 0;
        }

        List<Long> methodIds = methods.stream()
                .map(doc -> ((Number) doc.get("payment_method_id")).longValue())
                .toList();

        List<Document> payments = mongoTemplate.find(
                Query.query(Criteria.where("payment_method_id").in(methodIds)),
                Document.class,
                "order_payments");
        if (payments.isEmpty()) {
            return 0;
        }

        List<Long> orderIds = payments.stream()
                .map(doc -> ((Number) doc.get("order_id")).longValue())
                .distinct()
                .toList();

        UpdateResult result = mongoTemplate.getCollection("orders").updateMany(
                Filters.in("order_id", orderIds),
                new Document("$set", new Document("status", "CANCELLED")));
        return (int) result.getModifiedCount();
    }

    @Override
    public int deleteOldCustomerOrders(long customerId, OffsetDateTime cutoffDate) {
        DeleteResult result = mongoTemplate.getCollection("orders").deleteMany(
                Filters.and(
                        Filters.eq("customer_id", customerId),
                        Filters.lt("created_at", Date.from(cutoffDate.toInstant()))));
        return (int) result.getDeletedCount();
    }

    @Override
    public int deleteCart(long orderId) {
        DeleteResult result = mongoTemplate.getCollection("orders").deleteMany(
                Filters.and(
                        Filters.eq("order_id", orderId),
                        Filters.eq("status", "NEW")));
        return (int) result.getDeletedCount();
    }

    @Override
    public int deleteOrderItemsByBrand(long brandId) {
        if (brandId <= 0) {
            return 0;
        }

        List<Document> products = mongoTemplate.find(
                Query.query(Criteria.where("brand_id").is(brandId)),
                Document.class,
                "products");
        if (products.isEmpty()) {
            return 0;
        }

        List<Long> productIds = products.stream()
                .map(doc -> ((Number) doc.get("product_id")).longValue())
                .toList();

        DeleteResult result = mongoTemplate.getCollection("order_items").deleteMany(
                Filters.and(
                        Filters.in("product_id", productIds),
                        Filters.lt("quantity", 2),
                        Filters.expr(new Document("$lt", List.of(
                                new Document("$mod", List.of("$product_id", brandId)),
                                5)))));
        return (int) result.getDeletedCount();
    }

    @Override
    public int deleteCustomer(long customerId) {
        DeleteResult result = mongoTemplate.getCollection("customers").deleteMany(Filters.eq("customer_id", customerId));
        return (int) result.getDeletedCount();
    }

    @Override
    public int deleteWarehouse(long warehouseId) {
        DeleteResult result = mongoTemplate.getCollection("warehouses").deleteMany(Filters.eq("_id", "warehouse::" + warehouseId));
        return (int) result.getDeletedCount();
    }

    @Override
    public int deleteOrderItemsByCategory(long categoryId) {
        List<Document> products = mongoTemplate.find(
                Query.query(Criteria.where("category_id").is(categoryId)),
                Document.class,
                "products");
        if (products.isEmpty()) {
            return 0;
        }

        List<Long> productIds = products.stream()
                .map(doc -> ((Number) doc.get("product_id")).longValue())
                .toList();

        DeleteResult result = mongoTemplate.getCollection("order_items").deleteMany(Filters.in("product_id", productIds));
        return (int) result.getDeletedCount();
    }

    private long nextId(String collection, String idField) {
        return allocateIdRange(collection, idField, 1);
    }

    private long allocateIdRange(String collection, String idField, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count musi być > 0");
        }

        long initialSeq = readCurrentMaxId(collection, idField);
        String counterId = collection + "::" + idField;

        // Krok 1: inicjalizacja dokumentu licznika (tylko jeśli nie istnieje).
        // Nie łączymy setOnInsert(seq) i inc(seq) w jednym update, bo Mongo zwraca code 40.
        Query ensureQuery = Query.query(Criteria.where("_id").is(counterId));
        Update ensureUpdate = new Update().setOnInsert("seq", initialSeq);
        mongoTemplate.findAndModify(
                ensureQuery,
                ensureUpdate,
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                Document.class,
                "counters");

        // Krok 2: atomowa alokacja zakresu przez inkrementację seq.
        Query query = Query.query(Criteria.where("_id").is(counterId));
        Update update = new Update().inc("seq", count);

        Document counter = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Document.class,
                "counters");

        if (counter == null || counter.get("seq") == null) {
            throw new IllegalStateException("Nie udało się zaalokować ID dla " + counterId);
        }

        long upperBound = ((Number) counter.get("seq")).longValue();
        return upperBound - count + 1;
    }

    private long readCurrentMaxId(String collection, String idField) {
        Query query = new Query()
                .with(Sort.by(Sort.Direction.DESC, idField))
                .limit(1);
        query.fields().include(idField);

        Document doc = mongoTemplate.findOne(query, Document.class, collection);
        if (doc == null || doc.get(idField) == null) {
            return 1L;
        }
        return ((Number) doc.get(idField)).longValue();
    }

    private int nextLineNo(long orderId) {
        Query query = Query.query(Criteria.where("order_id").is(orderId))
                .with(Sort.by(Sort.Direction.DESC, "line_no"))
                .limit(1);
        query.fields().include("line_no");

        Document doc = mongoTemplate.findOne(query, Document.class, "order_items");
        if (doc == null || doc.get("line_no") == null) {
            return 1;
        }
        return ((Number) doc.get("line_no")).intValue() + 1;
    }

    private List<Map<String, Object>> aggregate(String collectionName, List<Document> pipeline) {
        List<Document> rows = mongoTemplate.getCollection(collectionName)
                .aggregate(pipeline)
                .into(new ArrayList<>());

        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (Document row : rows) {
            out.add(toMap(row));
        }
        return out;
    }

    private Map<String, Object> toMap(Document row) {
        Map<String, Object> map = new LinkedHashMap<>(row);
        map.remove("_id");
        return map;
    }
}

