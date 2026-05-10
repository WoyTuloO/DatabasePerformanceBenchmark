package com.example.performanceTesting.shop.application;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.QueryScanConsistency;
import com.example.performanceTesting.bootstrap.config.DatabaseBootstrapProperties;
import com.example.performanceTesting.bootstrap.config.DatabaseType;
import com.example.performanceTesting.shop.model.ShopRequests;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "app.database", name = "type", havingValue = "couchbase")
public class CouchbaseShopService implements DatabaseAwareShopService {

    private final Cluster cluster;
    private final String bucket;
    private final String scope;

    public CouchbaseShopService(Cluster cluster, Bucket couchbaseBucket, DatabaseBootstrapProperties properties) {
        this.cluster = cluster;
        this.bucket = couchbaseBucket.name();
        this.scope = properties.getDatabase().getCouchbase().getScope();
    }

    @Override
    public DatabaseType type() {
        return DatabaseType.COUCHBASE;
    }

    @Override
    public int[] createOrdersBatch(List<ShopRequests.CreateOrder> reqs) {
        int[] out = new int[reqs.size()];
        for (int i = 0; i < reqs.size(); i++) {
            ShopRequests.CreateOrder req = reqs.get(i);
            long orderId = nextId("orders", "order_id");
            String key = "order::" + orderId;

            query("""
                    INSERT INTO %s (KEY, VALUE)
                    VALUES ($key, {
                        "order_id": $order_id,
                        "customer_id": $customer_id,
                        "shipping_country": $shipping_country,
                        "shipping_city": $shipping_city,
                        "shipping_postal_code": $shipping_postal_code,
                        "shipping_street": $shipping_street,
                        "shipping_building_no": $shipping_building_no,
                        "shipping_apartment_no": $shipping_apartment_no,
                        "status": "NEW",
                        "total_cents": 0,
                        "currency": $currency,
                        "created_at": NOW_STR()
                    })
                    """.formatted(c("orders")), JsonObject.create()
                    .put("key", key)
                    .put("order_id", orderId)
                    .put("customer_id", req.customerId())
                    .put("shipping_country", req.shippingCountry())
                    .put("shipping_city", req.shippingCity())
                    .put("shipping_postal_code", req.shippingPostalCode())
                    .put("shipping_street", req.shippingStreet())
                    .put("shipping_building_no", req.shippingBuildingNo())
                    .put("shipping_apartment_no", req.shippingApartmentNo())
                    .put("currency", req.currency()));
            out[i] = 1;
        }
        return out;
    }

    @Override
    public Map<String, Object> addOrderItem(ShopRequests.AddOrderItem req) {
        long lineNo = nextLineNo(req.orderId());
        String key = "order_item::" + req.orderId() + "::" + lineNo;

        query("""
                INSERT INTO %s (KEY, VALUE)
                VALUES ($key, {
                    "order_id": $order_id,
                    "line_no": $line_no,
                    "product_id": $product_id,
                    "quantity": $quantity,
                    "unit_price_cents": $unit_price_cents
                })
                """.formatted(c("order_items")), JsonObject.create()
                .put("key", key)
                .put("order_id", req.orderId())
                .put("line_no", lineNo)
                .put("product_id", req.productId())
                .put("quantity", req.quantity())
                .put("unit_price_cents", req.unitPriceCents()));

        return Map.of("order_id", req.orderId(), "line_no", lineNo);
    }

    @Override
    public Map<String, Object> createCustomer(ShopRequests.CreateCustomer req) {
        long customerId = nextId("customers", "customer_id");
        String key = "customer::" + customerId;

        query("""
                INSERT INTO %s (KEY, VALUE)
                VALUES ($key, {
                    "customer_id": $customer_id,
                    "email": $email,
                    "password_hash": $password_hash,
                    "first_name": $first_name,
                    "last_name": $last_name,
                    "phone": $phone,
                    "created_at": NOW_STR()
                })
                """.formatted(c("customers")), JsonObject.create()
                .put("key", key)
                .put("customer_id", customerId)
                .put("email", req.email())
                .put("password_hash", req.passwordHash())
                .put("first_name", req.firstName())
                .put("last_name", req.lastName())
                .put("phone", req.phone()));

        return Map.of("customer_id", customerId);
    }

    @Override
    public Map<String, Object> createProduct(ShopRequests.CreateProduct req) {
        long productId = nextId("products", "product_id");
        String key = "product::" + productId;

        query("""
                INSERT INTO %s (KEY, VALUE)
                VALUES ($key, {
                    "product_id": $product_id,
                    "stock_keeping_unit": $stock_keeping_unit,
                    "name": $name,
                    "description": $description,
                    "brand_id": $brand_id,
                    "category_id": $category_id,
                    "base_price_cents": $base_price_cents,
                    "currency": $currency,
                    "active": true,
                    "created_at": NOW_STR()
                })
                """.formatted(c("products")), JsonObject.create()
                .put("key", key)
                .put("product_id", productId)
                .put("stock_keeping_unit", req.stockKeepingUnit())
                .put("name", req.name())
                .put("description", req.description())
                .put("brand_id", req.brandId())
                .put("category_id", req.categoryId())
                .put("base_price_cents", req.basePriceCents())
                .put("currency", req.currency()));

        return Map.of("product_id", productId);
    }

    @Override
    public Map<String, Object> upsertInventory(ShopRequests.UpsertInventory req) {
        String key = "inventory::" + req.warehouseId() + "::" + req.productId();
        query("""
                UPSERT INTO %s (KEY, VALUE)
                VALUES ($key, {
                    "warehouse_id": $warehouse_id,
                    "product_id": $product_id,
                    "quantity": $quantity,
                    "updated_at": NOW_STR()
                })
                """.formatted(c("inventory")), JsonObject.create()
                .put("key", key)
                .put("warehouse_id", req.warehouseId())
                .put("product_id", req.productId())
                .put("quantity", req.quantity()));

        return Map.of("warehouse_id", req.warehouseId(), "product_id", req.productId(), "quantity", req.quantity());
    }

    @Override
    public Map<String, Object> createOrderPayment(ShopRequests.CreateOrderPayment req) {
        long orderPaymentId = nextId("order_payments", "order_payment_id");
        String key = "order_payment::" + orderPaymentId;

        query("""
                INSERT INTO %s (KEY, VALUE)
                VALUES ($key, {
                    "order_payment_id": $order_payment_id,
                    "order_id": $order_id,
                    "payment_method_id": $payment_method_id,
                    "provider": $provider,
                    "amount_cents": $amount_cents,
                    "currency": $currency,
                    "status": $status,
                    "paid_at": $paid_at,
                    "created_at": NOW_STR()
                })
                """.formatted(c("order_payments")), JsonObject.create()
                .put("key", key)
                .put("order_payment_id", orderPaymentId)
                .put("order_id", req.orderId())
                .put("payment_method_id", req.paymentMethodId())
                .put("provider", req.provider())
                .put("amount_cents", req.amountCents())
                .put("currency", req.currency())
                .put("status", req.status())
                .put("paid_at", req.paidAt() == null ? null : req.paidAt().toString()));

        return Map.of("order_payment_id", orderPaymentId);
    }

    @Override
    public List<Map<String, Object>> getMissingProducts(long orderId) {
        QueryResult result = query("""
                SELECT
                  oi.line_no,
                  p.product_id,
                  p.stock_keeping_unit,
                  p.name,
                  oi.quantity AS ordered_quantity,
                  COALESCE(SUM(i.quantity), 0) AS total_stock
                FROM %s oi
                JOIN %s p ON p.product_id = oi.product_id
                LEFT JOIN %s i ON i.product_id = p.product_id
                WHERE oi.order_id = $order_id
                GROUP BY oi.line_no, p.product_id, p.stock_keeping_unit, p.name, oi.quantity
                HAVING COALESCE(SUM(i.quantity), 0) < oi.quantity
                ORDER BY oi.line_no
                """.formatted(c("order_items"), c("products"), c("inventory")), JsonObject.create().put("order_id", orderId));
        return rows(result);
    }

    @Override
    public List<Map<String, Object>> getAvailableProductsByBrandAndCity(String brandName, String city) {
        QueryResult result = query("""
                SELECT DISTINCT
                  p.product_id,
                  p.stock_keeping_unit,
                  p.name,
                  p.base_price_cents,
                  p.currency,
                  p.active
                FROM %s p
                JOIN %s b ON b.brand_id = p.brand_id
                JOIN %s i ON i.product_id = p.product_id
                JOIN %s w ON w.warehouse_id = i.warehouse_id
                WHERE b.name = $brand_name AND w.city = $city AND i.quantity > 0 AND p.active = true
                ORDER BY p.name
                """.formatted(c("products"), c("brands"), c("inventory"), c("warehouses")), JsonObject.create()
                .put("brand_name", brandName)
                .put("city", city));
        return rows(result);
    }

    @Override
    public List<Map<String, Object>> getCartItems(long orderId) {
        QueryResult result = query("""
                SELECT
                  oi.line_no,
                  p.product_id,
                  p.stock_keeping_unit,
                  p.name,
                  oi.quantity,
                  oi.unit_price_cents,
                  (oi.quantity * oi.unit_price_cents) AS line_total_cents
                FROM %s oi
                JOIN %s p ON p.product_id = oi.product_id
                WHERE oi.order_id = $order_id
                ORDER BY oi.line_no
                """.formatted(c("order_items"), c("products")), JsonObject.create().put("order_id", orderId));
        return rows(result);
    }

    @Override
    public Map<String, Object> getProductAvailability(long productId) {
        QueryResult result = query("""
                SELECT
                  p.product_id,
                  p.stock_keeping_unit,
                  p.name,
                  p.active,
                  COALESCE(SUM(i.quantity), 0) AS total_stock
                FROM %s p
                LEFT JOIN %s i ON i.product_id = p.product_id
                WHERE p.product_id = $product_id
                GROUP BY p.product_id, p.stock_keeping_unit, p.name, p.active
                """.formatted(c("products"), c("inventory")), JsonObject.create().put("product_id", productId));
        return firstOrEmpty(result);
    }

    @Override
    public Integer getCustomerEmailsByPaymentMethod(String paymentMethodCode) {
        QueryResult result = query("""
            SELECT COUNT(DISTINCT c.email) AS total
            FROM %s op
            JOIN %s o ON o.order_id = op.order_id
            JOIN %s c ON c.customer_id = o.customer_id
            JOIN %s pm ON pm.payment_method_id = op.payment_method_id
            WHERE pm.code = $payment_method
            """.formatted(c("order_payments"), c("orders"), c("customers"), c("payment_methods")),
                JsonObject.create().put("payment_method", paymentMethodCode));

        return result.rowsAsObject().stream()
                .findFirst()
                .map(row -> row.getInt("total"))
                .orElse(0);
    }

    @Override
    public List<Map<String, Object>> getCustomerOrderDetails(long customerId) {
        QueryResult result = query("""
                SELECT
                    o.order_id,
                    o.created_at,
                    o.status,
                    o.total_cents,
                    o.currency,
                    op.order_payment_id,
                    pm.code AS payment_method_code,
                    pm.name AS payment_method_name,
                    op.provider,
                    op.status AS payment_status,
                    op.amount_cents AS payment_amount,
                    op.paid_at,
                    oi.line_no,
                    oi.quantity,
                    oi.unit_price_cents,
                    pr.stock_keeping_unit,
                    pr.name AS product_name,
                    b.name AS brand_name,
                    c.name AS category_name
                FROM %s o
                JOIN %s oi ON oi.order_id = o.order_id
                JOIN %s pr ON pr.product_id = oi.product_id
                LEFT JOIN %s b ON b.brand_id = pr.brand_id
                LEFT JOIN %s c ON c.category_id = pr.category_id
                LEFT JOIN %s op ON op.order_id = o.order_id
                LEFT JOIN %s pm ON pm.payment_method_id = op.payment_method_id
                WHERE o.customer_id = $customer_id
                ORDER BY o.created_at DESC, oi.line_no, op.created_at
                """.formatted(c("orders"), c("order_items"), c("products"), c("brands"), c("categories"), c("order_payments"), c("payment_methods")),
                JsonObject.create().put("customer_id", customerId));
        return rows(result);
    }

    @Override
    public int updateCategoryPrices(long categoryId, double multiplier) {
        return mutationCount(query("""
                UPDATE %s p
                SET p.base_price_cents = GREATEST(0, ROUND(p.base_price_cents * $multiplier))
                WHERE p.category_id = $category_id
                """.formatted(c("products")), JsonObject.create().put("multiplier", multiplier).put("category_id", categoryId)));
    }

    @Override
    public int updateOrderStatusByPayment(long orderPaymentId, String status) {
        return mutationCount(query("""
                UPDATE %s o
                SET o.status = $status
                WHERE o.order_id IN (
                    SELECT RAW op.order_id FROM %s op WHERE op.order_payment_id = $order_payment_id
                )
                """.formatted(c("orders"), c("order_payments")), JsonObject.create()
                .put("status", status)
                .put("order_payment_id", orderPaymentId)));
    }

    @Override
    public int updateProductActive(long productId, boolean active) {
        return mutationCount(query("""
                UPDATE %s p
                SET p.active = $active
                WHERE p.product_id = $product_id
                """.formatted(c("products")), JsonObject.create().put("active", active).put("product_id", productId)));
    }

    @Override
    public int updateBrandPrices(long brandId, double multiplier) {
        return mutationCount(query("""
                UPDATE %s p
                SET p.base_price_cents = ROUND(p.base_price_cents * $multiplier)
                WHERE p.brand_id = $brand_id
                """.formatted(c("products")), JsonObject.create().put("multiplier", multiplier).put("brand_id", brandId)));
    }

    @Override
    public int updateInventory(long warehouseId, long productId, int quantity) {
        return mutationCount(query("""
                UPDATE %s i
                SET i.quantity = $quantity,
                    i.updated_at = NOW_STR()
                WHERE i.warehouse_id = $warehouse_id AND i.product_id = $product_id
                """.formatted(c("inventory")), JsonObject.create()
                .put("quantity", quantity)
                .put("warehouse_id", warehouseId)
                .put("product_id", productId)));
    }

    @Override
    public int cancelOrdersByPaymentMethod(String code) {
        return mutationCount(query("""
                UPDATE %s o
                SET o.status = "CANCELLED"
                WHERE o.order_id IN (
                    SELECT RAW op.order_id
                    FROM %s op
                    JOIN %s pm ON pm.payment_method_id = op.payment_method_id
                    WHERE pm.code = $code
                )
                """.formatted(c("orders"), c("order_payments"), c("payment_methods")), JsonObject.create().put("code", code)));
    }

    @Override
    public int deleteOldCustomerOrders(long customerId, OffsetDateTime cutoffDate) {
        return mutationCount(query("""
                DELETE FROM %s o
                WHERE o.customer_id = $customer_id
                  AND o.created_at < $cutoff_date
                """.formatted(c("orders")), JsonObject.create()
                .put("customer_id", customerId)
                .put("cutoff_date", cutoffDate.toString())));
    }

    @Override
    public int deleteCart(long orderId) {
        return mutationCount(query("""
                DELETE FROM %s o
                WHERE o.order_id = $order_id AND o.status = "NEW"
                """.formatted(c("orders")), JsonObject.create().put("order_id", orderId)));
    }

    @Override
    public int deleteOrderItemsByBrand(long brandId) {
        if (brandId <= 0) {
            return 0;
        }
        return mutationCount(query("""
                DELETE FROM %s oi
                WHERE oi.product_id IN (
                    SELECT RAW p.product_id
                    FROM %s p
                    WHERE p.brand_id = $brand_id
                )
                  AND oi.quantity < 2
                  AND oi.product_id %% $brand_id < 5
                """.formatted(c("order_items"), c("products")), JsonObject.create().put("brand_id", brandId)));
    }

    @Override
    public int deleteCustomer(long customerId) {
        return mutationCount(query("""
                DELETE FROM %s c
                WHERE c.customer_id = $customer_id
                """.formatted(c("customers")), JsonObject.create().put("customer_id", customerId)));
    }

    @Override
    public int deleteWarehouse(long warehouseId) {
        return mutationCount(query("""
                DELETE FROM %s w
                WHERE w.warehouse_id = $warehouse_id
                """.formatted(c("warehouses")), JsonObject.create().put("warehouse_id", warehouseId)));
    }

    @Override
    public int deleteOrderItemsByCategory(long categoryId) {
        return mutationCount(query("""
                DELETE FROM %s oi
                WHERE oi.product_id IN (
                    SELECT RAW p.product_id FROM %s p WHERE p.category_id = $category_id
                )
                """.formatted(c("order_items"), c("products")), JsonObject.create().put("category_id", categoryId)));
    }

    private String c(String collection) {
        return "`" + bucket + "`.`" + scope + "`.`" + collection + "`";
    }

    private QueryResult query(String statement, JsonObject params) {
        return cluster.query(statement, QueryOptions.queryOptions()
                .scanConsistency(QueryScanConsistency.REQUEST_PLUS)
                .parameters(params == null ? JsonObject.create() : params));
    }

    private List<Map<String, Object>> rows(QueryResult result) {
        List<Map<String, Object>> out = new ArrayList<>();
        result.rowsAsObject().forEach(row -> out.add(row.toMap()));
        return out;
    }

    private Map<String, Object> firstOrEmpty(QueryResult result) {
        List<Map<String, Object>> rows = rows(result);
        return rows.isEmpty() ? Collections.emptyMap() : rows.getFirst();
    }

    private int mutationCount(QueryResult result) {
        return Optional.ofNullable(result.metaData().metrics())
                .flatMap(metrics -> metrics)
                .map(metrics -> (int) metrics.mutationCount())
                .orElse(0);
    }

    private long nextId(String collection, String idField) {
        QueryResult result = query("""
                SELECT COALESCE(MAX(d.%s), 0) + 1 AS next_id
                FROM %s d
                """.formatted(idField, c(collection)), JsonObject.create());
        return firstOrEmpty(result).get("next_id") == null ? 1L : ((Number) firstOrEmpty(result).get("next_id")).longValue();
    }

    private long nextLineNo(long orderId) {
        QueryResult result = query("""
                SELECT COALESCE(MAX(oi.line_no), 0) + 1 AS next_line
                FROM %s oi
                WHERE oi.order_id = $order_id
                """.formatted(c("order_items")), JsonObject.create().put("order_id", orderId));
        return firstOrEmpty(result).get("next_line") == null ? 1L : ((Number) firstOrEmpty(result).get("next_line")).longValue();
    }
}


