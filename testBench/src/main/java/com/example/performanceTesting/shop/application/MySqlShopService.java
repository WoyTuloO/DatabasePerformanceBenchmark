package com.example.performanceTesting.shop.application;

import com.example.performanceTesting.bootstrap.config.DatabaseType;
import com.example.performanceTesting.shop.model.ShopRequests;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@ConditionalOnProperty(prefix = "app.database", name = "type", havingValue = "mysql")
public class MySqlShopService implements DatabaseAwareShopService {

    private final NamedParameterJdbcTemplate jdbc;

    public MySqlShopService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public DatabaseType type() {
        return DatabaseType.MYSQL;
    }

    private Map<String, Object> queryOneOrEmpty(String sql, MapSqlParameterSource params) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);
        return rows.isEmpty() ? Collections.emptyMap() : rows.getFirst();
    }

    private long lastInsertId() {
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", new MapSqlParameterSource(), Long.class);
        if (id == null) {
            throw new IllegalStateException("LAST_INSERT_ID() zwrócił null");
        }
        return id;
    }

    private Timestamp toTimestamp(OffsetDateTime dateTime) {
        return dateTime == null ? null : Timestamp.from(dateTime.toInstant());
    }

    @Override
    public int[] createOrdersBatch(List<ShopRequests.CreateOrder> reqs) {
        try {
            var sql = """
                INSERT INTO orders (
                    customer_id, shipping_country, shipping_city, shipping_postal_code,
                    shipping_street, shipping_building_no, shipping_apartment_no,
                    status, total_cents, currency, created_at
                ) VALUES (
                    :customer_id, :shipping_country, :shipping_city, :shipping_postal_code,
                    :shipping_street, :shipping_building_no, :shipping_apartment_no,
                    'NEW', 0, :currency, CURRENT_TIMESTAMP
                )
                """;

            MapSqlParameterSource[] batchParams = reqs.stream()
                    .map(req -> new MapSqlParameterSource()
                            .addValue("customer_id", req.customerId())
                            .addValue("shipping_country", req.shippingCountry())
                            .addValue("shipping_city", req.shippingCity())
                            .addValue("shipping_postal_code", req.shippingPostalCode())
                            .addValue("shipping_street", req.shippingStreet())
                            .addValue("shipping_building_no", req.shippingBuildingNo())
                            .addValue("shipping_apartment_no", req.shippingApartmentNo(), Types.VARCHAR)
                            .addValue("currency", req.currency()))
                    .toArray(MapSqlParameterSource[]::new);

            return jdbc.batchUpdate(sql, batchParams);
        } catch (Exception e) {
            System.err.println("ERROR in createOrdersBatch: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public Map<String, Object> addOrderItem(ShopRequests.AddOrderItem req) {
        var params = new MapSqlParameterSource("order_id", req.orderId());
        Integer nextLineNo = jdbc.queryForObject(
                "SELECT COALESCE(MAX(line_no), 0) + 1 FROM order_items WHERE order_id = :order_id",
                params,
                Integer.class);

        if (nextLineNo == null) {
            throw new IllegalStateException("Nie udało się wyznaczyć line_no");
        }

        var insertSql = """
                INSERT INTO order_items (order_id, line_no, product_id, quantity, unit_price_cents)
                VALUES (:order_id, :line_no, :product_id, :quantity, :unit_price_cents)
                """;

        jdbc.update(insertSql, new MapSqlParameterSource()
                .addValue("order_id", req.orderId())
                .addValue("line_no", nextLineNo)
                .addValue("product_id", req.productId())
                .addValue("quantity", req.quantity())
                .addValue("unit_price_cents", req.unitPriceCents()));

        return Map.of("order_id", req.orderId(), "line_no", nextLineNo);
    }

    @Override
    public Map<String, Object> createCustomer(ShopRequests.CreateCustomer req) {
        var sql = """
                INSERT INTO customers (email, password_hash, first_name, last_name, phone, created_at)
                VALUES (:email, :password_hash, :first_name, :last_name, :phone, CURRENT_TIMESTAMP)
                """;

        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("email", req.email())
                .addValue("password_hash", req.passwordHash())
                .addValue("first_name", req.firstName())
                .addValue("last_name", req.lastName())
                .addValue("phone", req.phone(), Types.VARCHAR));

        return Map.of("customer_id", lastInsertId());
    }

    @Override
    public Map<String, Object> createProduct(ShopRequests.CreateProduct req) {
        var sql = """
                INSERT INTO products (
                    stock_keeping_unit, name, description, brand_id, category_id,
                    base_price_cents, currency, active, created_at
                ) VALUES (
                    :stock_keeping_unit, :name, :description, :brand_id, :category_id,
                    :base_price_cents, :currency, TRUE, CURRENT_TIMESTAMP
                )
                """;

        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("stock_keeping_unit", req.stockKeepingUnit())
                .addValue("name", req.name())
                .addValue("description", req.description(), Types.VARCHAR)
                .addValue("brand_id", req.brandId(), Types.BIGINT)
                .addValue("category_id", req.categoryId(), Types.BIGINT)
                .addValue("base_price_cents", req.basePriceCents())
                .addValue("currency", req.currency()));

        return Map.of("product_id", lastInsertId());
    }

    @Override
    public Map<String, Object> upsertInventory(ShopRequests.UpsertInventory req) {
        var sql = """
                INSERT INTO inventory (warehouse_id, product_id, quantity, updated_at)
                VALUES (:warehouse_id, :product_id, :quantity, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                    quantity = VALUES(quantity),
                    updated_at = CURRENT_TIMESTAMP
                """;

        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("warehouse_id", req.warehouseId())
                .addValue("product_id", req.productId())
                .addValue("quantity", req.quantity()));

        return Map.of(
                "warehouse_id", req.warehouseId(),
                "product_id", req.productId(),
                "quantity", req.quantity());
    }

    @Override
    public Map<String, Object> createOrderPayment(ShopRequests.CreateOrderPayment req) {
        var sql = """
                INSERT INTO order_payments (
                    order_id, payment_method_id, provider, amount_cents,
                    currency, status, paid_at, created_at
                ) VALUES (
                    :order_id, :payment_method_id, :provider, :amount_cents,
                    :currency, :status, :paid_at, CURRENT_TIMESTAMP
                )
                """;

        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("order_id", req.orderId())
                .addValue("payment_method_id", req.paymentMethodId())
                .addValue("provider", req.provider(), Types.VARCHAR)
                .addValue("amount_cents", req.amountCents())
                .addValue("currency", req.currency())
                .addValue("status", req.status())
                .addValue("paid_at", toTimestamp(req.paidAt()), Types.TIMESTAMP));

        return Map.of("order_payment_id", lastInsertId());
    }

    @Override
    public List<Map<String, Object>> getMissingProducts(long orderId) {
        var sql = """
                SELECT
                    oi.line_no, p.product_id, p.stock_keeping_unit, p.name,
                    oi.quantity AS ordered_quantity, COALESCE(SUM(i.quantity), 0) AS total_stock
                FROM order_items oi
                JOIN products p ON p.product_id = oi.product_id
                LEFT JOIN inventory i ON i.product_id = p.product_id
                WHERE oi.order_id = :order_id
                GROUP BY oi.line_no, p.product_id, p.stock_keeping_unit, p.name, oi.quantity
                HAVING COALESCE(SUM(i.quantity), 0) < oi.quantity
                ORDER BY oi.line_no
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource("order_id", orderId));
    }

    @Override
    public List<Map<String, Object>> getAvailableProductsByBrandAndCity(String brandName, String city) {
        var sql = """
                SELECT DISTINCT
                    p.product_id, p.stock_keeping_unit, p.name, p.base_price_cents, p.currency, p.active
                FROM products p
                JOIN brands b ON b.brand_id = p.brand_id
                JOIN inventory i ON i.product_id = p.product_id
                JOIN warehouses w ON w.warehouse_id = i.warehouse_id
                WHERE b.name = :brand_name AND w.city = :city AND i.quantity > 0 AND p.active = TRUE
                ORDER BY p.name
                """;
        var params = new MapSqlParameterSource().addValue("brand_name", brandName).addValue("city", city);
        return jdbc.queryForList(sql, params);
    }

    @Override
    public List<Map<String, Object>> getCartItems(long orderId) {
        var sql = """
                SELECT
                    oi.line_no, p.product_id, p.stock_keeping_unit, p.name,
                    oi.quantity, oi.unit_price_cents, (oi.quantity * oi.unit_price_cents) AS line_total_cents
                FROM order_items oi
                JOIN products p ON p.product_id = oi.product_id
                WHERE oi.order_id = :order_id
                ORDER BY oi.line_no
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource("order_id", orderId));
    }

    @Override
    public Map<String, Object> getProductAvailability(long productId) {
        var sql = """
                SELECT
                    p.product_id, p.stock_keeping_unit, p.name, p.active,
                    COALESCE(SUM(i.quantity), 0) AS total_stock
                FROM products p
                LEFT JOIN inventory i ON i.product_id = p.product_id
                WHERE p.product_id = :product_id
                GROUP BY p.product_id, p.stock_keeping_unit, p.name, p.active
                """;
        return queryOneOrEmpty(sql, new MapSqlParameterSource("product_id", productId));
    }

    @Override
    public Integer getCustomerEmailsByPaymentMethod(String paymentMethodCode) {
        var sql = """
            SELECT COUNT(DISTINCT c.email)
            FROM order_payments op
            JOIN orders o ON o.order_id = op.order_id
            JOIN customers c ON c.customer_id = o.customer_id
            JOIN payment_methods pm ON pm.payment_method_id = op.payment_method_id
            WHERE pm.code = :payment_method
            """;

        var params = new MapSqlParameterSource("payment_method", paymentMethodCode);

        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null ? count : 0;
    }

    @Override
    public List<Map<String, Object>> getCustomerOrderDetails(long customerId) {
        var sql = """
                SELECT
                    o.order_id, o.created_at, o.status, o.total_cents, o.currency,
                    op.order_payment_id, pm.code AS payment_method_code, pm.name AS payment_method_name,
                    op.provider, op.status AS payment_status, op.amount_cents AS payment_amount, op.paid_at,
                    oi.line_no, oi.quantity, oi.unit_price_cents,
                    pr.stock_keeping_unit, pr.name AS product_name,
                    b.name AS brand_name, c.name AS category_name
                FROM orders o
                JOIN order_items oi ON oi.order_id = o.order_id
                JOIN products pr ON pr.product_id = oi.product_id
                LEFT JOIN brands b ON b.brand_id = pr.brand_id
                LEFT JOIN categories c ON c.category_id = pr.category_id
                LEFT JOIN order_payments op ON op.order_id = o.order_id
                LEFT JOIN payment_methods pm ON pm.payment_method_id = op.payment_method_id
                WHERE o.customer_id = :customer_id
                ORDER BY o.created_at DESC, oi.line_no, op.created_at
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource("customer_id", customerId));
    }

    @Override
    public int updateCategoryPrices(long categoryId, double multiplier) {
        var sql = """
                UPDATE products
                SET base_price_cents = GREATEST(0, CAST(base_price_cents * :multiplier AS SIGNED))
                WHERE category_id = :category_id
                """;
        var params = new MapSqlParameterSource().addValue("multiplier", multiplier).addValue("category_id", categoryId);
        return jdbc.update(sql, params);
    }

    @Override
    public int updateOrderStatusByPayment(long orderPaymentId, String status) {
        var sql = """
                UPDATE orders o
                JOIN order_payments op ON o.order_id = op.order_id
                SET o.status = :status
                WHERE op.order_payment_id = :order_payment_id
                """;
        var params = new MapSqlParameterSource().addValue("status", status).addValue("order_payment_id", orderPaymentId);
        return jdbc.update(sql, params);
    }

    @Override
    public int updateProductActive(long productId, boolean active) {
        var sql = """
                UPDATE products
                SET active = :active
                WHERE product_id = :product_id
                """;
        var params = new MapSqlParameterSource().addValue("active", active).addValue("product_id", productId);
        return jdbc.update(sql, params);
    }

    @Override
    public int updateBrandPrices(long brandId, double multiplier) {
        var sql = """
                UPDATE products
                SET base_price_cents = CAST(base_price_cents * :multiplier AS SIGNED)
                WHERE brand_id = :brand_id
                """;
        var params = new MapSqlParameterSource().addValue("multiplier", multiplier).addValue("brand_id", brandId);
        return jdbc.update(sql, params);
    }

    @Override
    public int updateInventory(long warehouseId, long productId, int quantity) {
        var sql = """
                UPDATE inventory
                SET quantity = :quantity, updated_at = CURRENT_TIMESTAMP
                WHERE warehouse_id = :warehouse_id AND product_id = :product_id
                """;
        var params = new MapSqlParameterSource()
                .addValue("quantity", quantity)
                .addValue("warehouse_id", warehouseId)
                .addValue("product_id", productId);
        return jdbc.update(sql, params);
    }

    @Override
    public int cancelOrdersByPaymentMethod(String code) {
        var sql = """
                UPDATE orders o
                JOIN order_payments op ON o.order_id = op.order_id
                JOIN payment_methods pm ON pm.payment_method_id = op.payment_method_id
                SET o.status = 'CANCELLED'
                WHERE pm.code = :code
                """;
        return jdbc.update(sql, new MapSqlParameterSource("code", code));
    }

    @Override
    public int deleteOldCustomerOrders(long customerId, OffsetDateTime cutoffDate) {
        var sql = """
                DELETE FROM orders
                WHERE customer_id = :customer_id AND created_at < :cutoff_date
                """;
        var params = new MapSqlParameterSource()
                .addValue("customer_id", customerId)
                .addValue("cutoff_date", toTimestamp(cutoffDate), Types.TIMESTAMP);
        return jdbc.update(sql, params);
    }

    @Override
    public int deleteCart(long orderId) {
        var sql = """
                DELETE FROM orders
                WHERE order_id = :order_id AND status = 'NEW'
                """;
        return jdbc.update(sql, new MapSqlParameterSource("order_id", orderId));
    }

    @Override
    public int deleteOrderItemsByBrand(long brandId) {
        if (brandId <= 0) {
            return 0;
        }

        var sql = """
                DELETE oi
                FROM order_items oi
                JOIN products p ON oi.product_id = p.product_id
                WHERE p.brand_id = :brand_id
                  AND oi.quantity < 2
                  AND MOD(oi.product_id, :brand_id) < 5
                """;
        return jdbc.update(sql, new MapSqlParameterSource("brand_id", brandId));
    }

    @Override
    public int deleteCustomer(long customerId) {
        return jdbc.update("DELETE FROM customers WHERE customer_id = :customer_id",
                new MapSqlParameterSource("customer_id", customerId));
    }

    @Override
    public int deleteWarehouse(long warehouseId) {
        return jdbc.update("DELETE FROM warehouses WHERE warehouse_id = :warehouse_id",
                new MapSqlParameterSource("warehouse_id", warehouseId));
    }

    @Override
    public int deleteOrderItemsByCategory(long categoryId) {
        var sql = """
                DELETE FROM order_items
                WHERE product_id IN (
                    SELECT product_id FROM products WHERE category_id = :category_id
                )
                """;
        return jdbc.update(sql, new MapSqlParameterSource("category_id", categoryId));
    }
}

