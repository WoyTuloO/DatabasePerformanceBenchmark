package com.example.performanceTesting.shop.application;

import com.example.performanceTesting.shop.csv.CsvSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class ShopCsvService {

    private final EntityManager entityManager;

    public ShopCsvService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public String c1CreateOrder(String csv) {
        List<String[]> rows = parseRows(csv, 10, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                INSERT INTO shop.orders (
                  customer_id, shipping_country, shipping_city, shipping_postal_code,
                  shipping_street, shipping_building_no, shipping_apartment_no,
                  status, total_cents, currency
                ) VALUES (
                  :customerId, :shippingCountry, :shippingCity, :shippingPostalCode,
                  :shippingStreet, :shippingBuildingNo, :shippingApartmentNo,
                  'NEW', 0, :currency
                )
                RETURNING order_id, created_at
                """);

            q.setParameter("customerId", CsvSupport.asLong(row[0]));
            q.setParameter("shippingCountry", row[1]);
            q.setParameter("shippingCity", row[2]);
            q.setParameter("shippingPostalCode", row[3]);
            q.setParameter("shippingStreet", row[4]);
            q.setParameter("shippingBuildingNo", row[5]);
            q.setParameter("shippingApartmentNo", CsvSupport.nullable(row[6]));
            q.setParameter("currency", row[9]);
            out.add(toRow(q.getSingleResult()));
        }

        return CsvSupport.toCsv(new String[] {"order_id", "created_at"}, out);
    }

    @Transactional
    public String c2CreateOrderItem(String csv) {
        List<String[]> rows = parseRows(csv, 4, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                WITH next_line AS (
                  SELECT COALESCE(MAX(line_no), 0) + 1 AS line_no
                  FROM shop.order_items
                  WHERE order_id = :orderId
                )
                INSERT INTO shop.order_items (order_id, line_no, product_id, quantity, unit_price_cents)
                SELECT :orderId, nl.line_no, :productId, :quantity, :unitPriceCents
                FROM next_line nl
                RETURNING order_id, line_no
                """);

            q.setParameter("orderId", CsvSupport.asLong(row[0]));
            q.setParameter("productId", CsvSupport.asLong(row[1]));
            q.setParameter("quantity", CsvSupport.asInt(row[2]));
            q.setParameter("unitPriceCents", CsvSupport.asInt(row[3]));
            out.add(toRow(q.getSingleResult()));
        }

        return CsvSupport.toCsv(new String[] {"order_id", "line_no"}, out);
    }

    @Transactional
    public String c3CreateCustomer(String csv) {
        List<String[]> rows = parseRows(csv, 5, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                INSERT INTO shop.customers (email, password_hash, first_name, last_name, phone)
                VALUES (:email, :passwordHash, :firstName, :lastName, :phone)
                RETURNING customer_id
                """);

            q.setParameter("email", row[0]);
            q.setParameter("passwordHash", row[1]);
            q.setParameter("firstName", row[2]);
            q.setParameter("lastName", row[3]);
            q.setParameter("phone", CsvSupport.nullable(row[4]));
            out.add(toRow(q.getSingleResult()));
        }

        return CsvSupport.toCsv(new String[] {"customer_id"}, out);
    }

    @Transactional
    public String c4CreateProduct(String csv) {
        List<String[]> rows = parseRows(csv, 8, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                INSERT INTO shop.products (
                  stock_keeping_unit, name, description, brand_id,
                  category_id, base_price_cents, currency, active
                ) VALUES (
                  :sku, :name, :description, :brandId,
                  :categoryId, :basePriceCents, :currency, TRUE
                )
                RETURNING product_id
                """);

            q.setParameter("sku", row[0]);
            q.setParameter("name", row[1]);
            q.setParameter("description", CsvSupport.nullable(row[2]));
            q.setParameter("brandId", CsvSupport.asLong(row[3]));
            q.setParameter("categoryId", CsvSupport.asLong(row[4]));
            q.setParameter("basePriceCents", CsvSupport.asInt(row[5]));
            q.setParameter("currency", row[6]);
            out.add(toRow(q.getSingleResult()));
        }

        return CsvSupport.toCsv(new String[] {"product_id"}, out);
    }

    @Transactional
    public String c5UpsertInventory(String csv) {
        List<String[]> rows = parseRows(csv, 3, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                INSERT INTO shop.inventory (warehouse_id, product_id, quantity)
                VALUES (:warehouseId, :productId, :quantity)
                ON CONFLICT (warehouse_id, product_id)
                DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = now()
                RETURNING warehouse_id, product_id, quantity
                """);

            q.setParameter("warehouseId", CsvSupport.asLong(row[0]));
            q.setParameter("productId", CsvSupport.asLong(row[1]));
            q.setParameter("quantity", CsvSupport.asInt(row[2]));
            out.add(toRow(q.getSingleResult()));
        }

        return CsvSupport.toCsv(new String[] {"warehouse_id", "product_id", "quantity"}, out);
    }

    @Transactional
    public String c6CreateOrderPayment(String csv) {
        List<String[]> rows = parseRows(csv, 7, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                INSERT INTO shop.order_payments (
                  order_id, payment_method_id, provider, amount_cents,
                  currency, status, paid_at
                ) VALUES (
                  :orderId, :paymentMethodId, :provider, :amountCents,
                  :currency, :status, :paidAt
                )
                RETURNING order_payment_id, order_id, status
                """);

            q.setParameter("orderId", CsvSupport.asLong(row[0]));
            q.setParameter("paymentMethodId", CsvSupport.asLong(row[1]));
            q.setParameter("provider", CsvSupport.nullable(row[2]));
            q.setParameter("amountCents", CsvSupport.asInt(row[3]));
            q.setParameter("currency", row[4]);
            q.setParameter("status", row[5]);
            q.setParameter("paidAt", CsvSupport.asOffsetDateTime(row[6]));
            out.add(toRow(q.getSingleResult()));
        }

        return CsvSupport.toCsv(new String[] {"order_payment_id", "order_id", "status"}, out);
    }

    @Transactional(readOnly = true)
    public String r1GetOrdersByCustomer(String csv) {
        List<String[]> rows = parseRows(csv, 3, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                SELECT o.order_id, o.status, o.total_cents, o.currency, o.created_at
                FROM shop.orders o
                WHERE o.customer_id = :customerId
                ORDER BY o.created_at DESC
                LIMIT :limit OFFSET :offset
                """);
            q.setParameter("customerId", CsvSupport.asLong(row[0]));
            q.setParameter("limit", CsvSupport.asInt(row[1]));
            q.setParameter("offset", CsvSupport.asInt(row[2]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {"order_id", "status", "total_cents", "currency", "created_at"}, out);
    }

    @Transactional(readOnly = true)
    public String r2GetLatestNewCart(String csv) {
        List<String[]> rows = parseRows(csv, 1, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                SELECT o.order_id, o.created_at, o.total_cents, o.currency
                FROM shop.orders o
                WHERE o.customer_id = :customerId AND o.status = 'NEW'
                ORDER BY o.created_at DESC
                LIMIT 1
                """);
            q.setParameter("customerId", CsvSupport.asLong(row[0]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {"order_id", "created_at", "total_cents", "currency"}, out);
    }

    @Transactional(readOnly = true)
    public String r3GetCartItems(String csv) {
        List<String[]> rows = parseRows(csv, 1, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                SELECT oi.line_no, oi.product_id, p.stock_keeping_unit, p.name,
                       oi.quantity, oi.unit_price_cents,
                       (oi.quantity * oi.unit_price_cents) AS line_total_cents
                FROM shop.order_items oi
                JOIN shop.products p ON p.product_id = oi.product_id
                WHERE oi.order_id = :orderId
                ORDER BY oi.line_no
                """);
            q.setParameter("orderId", CsvSupport.asLong(row[0]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {
            "line_no", "product_id", "stock_keeping_unit", "name",
            "quantity", "unit_price_cents", "line_total_cents"
        }, out);
    }

    @Transactional(readOnly = true)
    public String r4GetProductStock(String csv) {
        List<String[]> rows = parseRows(csv, 1, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                SELECT p.product_id, p.stock_keeping_unit, p.name, p.active,
                       COALESCE(SUM(i.quantity), 0) AS total_stock
                FROM shop.products p
                LEFT JOIN shop.inventory i ON i.product_id = p.product_id
                WHERE p.product_id = :productId
                GROUP BY p.product_id, p.stock_keeping_unit, p.name, p.active
                """);
            q.setParameter("productId", CsvSupport.asLong(row[0]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {
            "product_id", "stock_keeping_unit", "name", "active", "total_stock"
        }, out);
    }

    @Transactional(readOnly = true)
    public String r5SearchProducts(String csv) {
        List<String[]> rows = parseRows(csv, 4, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                SELECT p.product_id, p.stock_keeping_unit, p.name,
                       p.base_price_cents, p.currency, p.active
                FROM shop.products p
                WHERE (:active IS NULL OR p.active = :active)
                  AND (:q IS NULL OR p.name ILIKE '%' || :q || '%' OR p.stock_keeping_unit ILIKE '%' || :q || '%')
                ORDER BY p.created_at DESC
                LIMIT :limit OFFSET :offset
                """);
            q.setParameter("active", CsvSupport.asBoolean(row[0]));
            q.setParameter("q", CsvSupport.nullable(row[1]));
            q.setParameter("limit", CsvSupport.asInt(row[2]));
            q.setParameter("offset", CsvSupport.asInt(row[3]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {
            "product_id", "stock_keeping_unit", "name", "base_price_cents", "currency", "active"
        }, out);
    }

    @Transactional(readOnly = true)
    public String r6GetOrderDetailsByCustomer(String csv) {
        List<String[]> rows = parseRows(csv, 1, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                SELECT o.order_id, o.created_at, o.status, o.total_cents, o.currency,
                       op.order_payment_id, pm.code AS payment_method_code,
                       pm.name AS payment_method_name, op.provider,
                       op.status AS payment_status, op.amount_cents AS payment_amount_cents,
                       op.paid_at, oi.line_no, oi.quantity, oi.unit_price_cents,
                       pr.stock_keeping_unit, pr.name AS product_name,
                       b.name AS brand_name, c.name AS category_name
                FROM shop.orders o
                JOIN shop.order_items oi ON oi.order_id = o.order_id
                JOIN shop.products pr ON pr.product_id = oi.product_id
                LEFT JOIN shop.brands b ON b.brand_id = pr.brand_id
                LEFT JOIN shop.categories c ON c.category_id = pr.category_id
                LEFT JOIN shop.order_payments op ON op.order_id = o.order_id
                LEFT JOIN shop.payment_methods pm ON pm.payment_method_id = op.payment_method_id
                WHERE o.customer_id = :customerId
                ORDER BY o.created_at DESC, oi.line_no, op.created_at
                """);
            q.setParameter("customerId", CsvSupport.asLong(row[0]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {
            "order_id", "created_at", "status", "total_cents", "currency",
            "order_payment_id", "payment_method_code", "payment_method_name", "provider",
            "payment_status", "payment_amount_cents", "paid_at", "line_no", "quantity",
            "unit_price_cents", "stock_keeping_unit", "product_name", "brand_name", "category_name"
        }, out);
    }

    @Transactional
    public String u1UpdateOrderItemQuantity(String csv) {
        List<String[]> rows = parseRows(csv, 3, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                UPDATE shop.order_items
                SET quantity = :quantity
                WHERE order_id = :orderId AND line_no = :lineNo
                RETURNING order_id, line_no, quantity
                """);
            q.setParameter("quantity", CsvSupport.asInt(row[2]));
            q.setParameter("orderId", CsvSupport.asLong(row[0]));
            q.setParameter("lineNo", CsvSupport.asInt(row[1]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {"order_id", "line_no", "quantity"}, out);
    }

    @Transactional
    public String u2UpdateOrderStatus(String csv) {
        List<String[]> rows = parseRows(csv, 2, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                UPDATE shop.orders
                SET status = :status
                WHERE order_id = :orderId
                RETURNING order_id, status
                """);
            q.setParameter("status", row[1]);
            q.setParameter("orderId", CsvSupport.asLong(row[0]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {"order_id", "status"}, out);
    }

    @Transactional
    public String u3UpdateProductActive(String csv) {
        List<String[]> rows = parseRows(csv, 2, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                UPDATE shop.products
                SET active = :active
                WHERE product_id = :productId
                RETURNING product_id, active
                """);
            q.setParameter("active", CsvSupport.asBoolean(row[1]));
            q.setParameter("productId", CsvSupport.asLong(row[0]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {"product_id", "active"}, out);
    }

    @Transactional
    public String u4RecalculateOrderTotal(String csv) {
        List<String[]> rows = parseRows(csv, 1, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                UPDATE shop.orders o
                SET total_cents = COALESCE((
                  SELECT SUM(quantity * unit_price_cents)
                  FROM shop.order_items oi
                  WHERE oi.order_id = o.order_id
                ), 0)
                WHERE o.order_id = :orderId
                RETURNING order_id, total_cents
                """);
            q.setParameter("orderId", CsvSupport.asLong(row[0]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {"order_id", "total_cents"}, out);
    }

    @Transactional
    public String u5UpdateInventoryQuantity(String csv) {
        List<String[]> rows = parseRows(csv, 3, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                UPDATE shop.inventory
                SET quantity = :quantity, updated_at = now()
                WHERE warehouse_id = :warehouseId AND product_id = :productId
                RETURNING warehouse_id, product_id, quantity
                """);
            q.setParameter("quantity", CsvSupport.asInt(row[2]));
            q.setParameter("warehouseId", CsvSupport.asLong(row[0]));
            q.setParameter("productId", CsvSupport.asLong(row[1]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {"warehouse_id", "product_id", "quantity"}, out);
    }

    @Transactional
    public String u6UpdateOrderPaymentStatus(String csv) {
        List<String[]> rows = parseRows(csv, 3, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                UPDATE shop.order_payments
                SET status = :status, paid_at = :paidAt
                WHERE order_payment_id = :orderPaymentId
                RETURNING order_payment_id, order_id, status, paid_at
                """);
            q.setParameter("status", row[1]);
            q.setParameter("paidAt", CsvSupport.asOffsetDateTime(row[2]));
            q.setParameter("orderPaymentId", CsvSupport.asLong(row[0]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {"order_payment_id", "order_id", "status", "paid_at"}, out);
    }

    @Transactional
    public String d1DeleteOrderItem(String csv) {
        List<String[]> rows = parseRows(csv, 2, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                DELETE FROM shop.order_items
                WHERE order_id = :orderId AND line_no = :lineNo
                RETURNING order_id, line_no
                """);
            q.setParameter("orderId", CsvSupport.asLong(row[0]));
            q.setParameter("lineNo", CsvSupport.asInt(row[1]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {"order_id", "line_no"}, out);
    }

    @Transactional
    public String d2DeleteNewOrder(String csv) {
        List<String[]> rows = parseRows(csv, 1, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                DELETE FROM shop.orders
                WHERE order_id = :orderId AND status = 'NEW'
                RETURNING order_id
                """);
            q.setParameter("orderId", CsvSupport.asLong(row[0]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {"order_id"}, out);
    }

    @Transactional
    public String d3DeleteOrderPayment(String csv) {
        List<String[]> rows = parseRows(csv, 1, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                DELETE FROM shop.order_payments
                WHERE order_payment_id = :orderPaymentId
                RETURNING order_payment_id, order_id
                """);
            q.setParameter("orderPaymentId", CsvSupport.asLong(row[0]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {"order_payment_id", "order_id"}, out);
    }

    @Transactional
    public String d4DeleteCustomer(String csv) {
        List<String[]> rows = parseRows(csv, 1, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                DELETE FROM shop.customers
                WHERE customer_id = :customerId
                RETURNING customer_id
                """);
            q.setParameter("customerId", CsvSupport.asLong(row[0]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {"customer_id"}, out);
    }

    @Transactional
    public String d5DeleteWarehouse(String csv) {
        List<String[]> rows = parseRows(csv, 1, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                DELETE FROM shop.warehouses
                WHERE warehouse_id = :warehouseId
                RETURNING warehouse_id
                """);
            q.setParameter("warehouseId", CsvSupport.asLong(row[0]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {"warehouse_id"}, out);
    }

    @Transactional
    public String d6DeleteProductHard(String csv) {
        List<String[]> rows = parseRows(csv, 1, false, null);
        List<List<Object>> out = new ArrayList<>();

        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                DELETE FROM shop.products
                WHERE product_id = :productId
                RETURNING product_id
                """);
            q.setParameter("productId", CsvSupport.asLong(row[0]));
            out.addAll(toRows(q.getResultList()));
        }

        return CsvSupport.toCsv(new String[] {"product_id"}, out);
    }

    @Transactional
    public String importCustomers(String csv) {
        List<String[]> rows = parseRows(csv, 7, false, null);
        List<List<Object>> out = new ArrayList<>();
        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                INSERT INTO shop.customers (customer_id, email, password_hash, first_name, last_name, phone, created_at)
                VALUES (:customerId, :email, :passwordHash, :firstName, :lastName, :phone, :createdAt)
                ON CONFLICT (customer_id) DO UPDATE SET
                  email = EXCLUDED.email,
                  password_hash = EXCLUDED.password_hash,
                  first_name = EXCLUDED.first_name,
                  last_name = EXCLUDED.last_name,
                  phone = EXCLUDED.phone,
                  created_at = EXCLUDED.created_at
                RETURNING customer_id
                """);
            q.setParameter("customerId", CsvSupport.asLong(row[0]));
            q.setParameter("email", row[1]);
            q.setParameter("passwordHash", row[2]);
            q.setParameter("firstName", row[3]);
            q.setParameter("lastName", row[4]);
            q.setParameter("phone", CsvSupport.nullable(row[5]));
            q.setParameter("createdAt", CsvSupport.asOffsetDateTime(row[6]));
            out.add(toRow(q.getSingleResult()));
        }
        return CsvSupport.toCsv(new String[] {"customer_id"}, out);
    }

    @Transactional
    public String importProducts(String csv) {
        List<String[]> rows = parseRows(csv, 10, false, null);
        List<List<Object>> out = new ArrayList<>();
        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                INSERT INTO shop.products (
                  product_id, stock_keeping_unit, name, description,
                  brand_id, category_id, base_price_cents, currency, active, created_at
                ) VALUES (
                  :productId, :sku, :name, :description,
                  :brandId, :categoryId, :basePriceCents, :currency, :active, :createdAt
                )
                ON CONFLICT (product_id) DO UPDATE SET
                  stock_keeping_unit = EXCLUDED.stock_keeping_unit,
                  name = EXCLUDED.name,
                  description = EXCLUDED.description,
                  brand_id = EXCLUDED.brand_id,
                  category_id = EXCLUDED.category_id,
                  base_price_cents = EXCLUDED.base_price_cents,
                  currency = EXCLUDED.currency,
                  active = EXCLUDED.active,
                  created_at = EXCLUDED.created_at
                RETURNING product_id
                """);
            q.setParameter("productId", CsvSupport.asLong(row[0]));
            q.setParameter("sku", row[1]);
            q.setParameter("name", row[2]);
            q.setParameter("description", CsvSupport.nullable(row[3]));
            q.setParameter("brandId", CsvSupport.asLong(row[4]));
            q.setParameter("categoryId", CsvSupport.asLong(row[5]));
            q.setParameter("basePriceCents", CsvSupport.asInt(row[6]));
            q.setParameter("currency", row[7]);
            q.setParameter("active", CsvSupport.asBoolean(row[8]));
            q.setParameter("createdAt", CsvSupport.asOffsetDateTime(row[9]));
            out.add(toRow(q.getSingleResult()));
        }
        return CsvSupport.toCsv(new String[] {"product_id"}, out);
    }

    @Transactional
    public String importInventory(String csv) {
        List<String[]> rows = parseRows(csv, 4, false, null);
        List<List<Object>> out = new ArrayList<>();
        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                INSERT INTO shop.inventory (warehouse_id, product_id, quantity, updated_at)
                VALUES (:warehouseId, :productId, :quantity, :updatedAt)
                ON CONFLICT (warehouse_id, product_id) DO UPDATE SET
                  quantity = EXCLUDED.quantity,
                  updated_at = EXCLUDED.updated_at
                RETURNING warehouse_id, product_id, quantity
                """);
            q.setParameter("warehouseId", CsvSupport.asLong(row[0]));
            q.setParameter("productId", CsvSupport.asLong(row[1]));
            q.setParameter("quantity", CsvSupport.asInt(row[2]));
            q.setParameter("updatedAt", CsvSupport.asOffsetDateTime(row[3]));
            out.add(toRow(q.getSingleResult()));
        }
        return CsvSupport.toCsv(new String[] {"warehouse_id", "product_id", "quantity"}, out);
    }

    @Transactional
    public String importOrders(String csv) {
        String[] header = {
            "order_id", "customer_id", "shipping_country", "shipping_city", "shipping_postal_code",
            "shipping_street", "shipping_building_no", "shipping_apartment_no", "status",
            "total_cents", "currency", "created_at"
        };
        List<String[]> rows = parseRows(csv, 12, true, header);
        List<List<Object>> out = new ArrayList<>();
        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                INSERT INTO shop.orders (
                  order_id, customer_id, shipping_country, shipping_city, shipping_postal_code,
                  shipping_street, shipping_building_no, shipping_apartment_no,
                  status, total_cents, currency, created_at
                ) VALUES (
                  :orderId, :customerId, :shippingCountry, :shippingCity, :shippingPostalCode,
                  :shippingStreet, :shippingBuildingNo, :shippingApartmentNo,
                  :status, :totalCents, :currency, :createdAt
                )
                ON CONFLICT (order_id) DO UPDATE SET
                  customer_id = EXCLUDED.customer_id,
                  shipping_country = EXCLUDED.shipping_country,
                  shipping_city = EXCLUDED.shipping_city,
                  shipping_postal_code = EXCLUDED.shipping_postal_code,
                  shipping_street = EXCLUDED.shipping_street,
                  shipping_building_no = EXCLUDED.shipping_building_no,
                  shipping_apartment_no = EXCLUDED.shipping_apartment_no,
                  status = EXCLUDED.status,
                  total_cents = EXCLUDED.total_cents,
                  currency = EXCLUDED.currency,
                  created_at = EXCLUDED.created_at
                RETURNING order_id
                """);
            q.setParameter("orderId", CsvSupport.asLong(row[0]));
            q.setParameter("customerId", CsvSupport.asLong(row[1]));
            q.setParameter("shippingCountry", row[2]);
            q.setParameter("shippingCity", row[3]);
            q.setParameter("shippingPostalCode", row[4]);
            q.setParameter("shippingStreet", row[5]);
            q.setParameter("shippingBuildingNo", row[6]);
            q.setParameter("shippingApartmentNo", CsvSupport.nullable(row[7]));
            q.setParameter("status", row[8]);
            q.setParameter("totalCents", CsvSupport.asInt(row[9]));
            q.setParameter("currency", row[10]);
            q.setParameter("createdAt", CsvSupport.asOffsetDateTime(row[11]));
            out.add(toRow(q.getSingleResult()));
        }
        return CsvSupport.toCsv(new String[] {"order_id"}, out);
    }

    @Transactional
    public String importOrderPayments(String csv) {
        List<String[]> rows = parseRows(csv, 9, false, null);
        List<List<Object>> out = new ArrayList<>();
        for (String[] row : rows) {
            Query q = entityManager.createNativeQuery("""
                INSERT INTO shop.order_payments (
                  order_payment_id, order_id, payment_method_id, provider,
                  amount_cents, currency, status, paid_at, created_at
                ) VALUES (
                  :orderPaymentId, :orderId, :paymentMethodId, :provider,
                  :amountCents, :currency, :status, :paidAt, :createdAt
                )
                ON CONFLICT (order_payment_id) DO UPDATE SET
                  order_id = EXCLUDED.order_id,
                  payment_method_id = EXCLUDED.payment_method_id,
                  provider = EXCLUDED.provider,
                  amount_cents = EXCLUDED.amount_cents,
                  currency = EXCLUDED.currency,
                  status = EXCLUDED.status,
                  paid_at = EXCLUDED.paid_at,
                  created_at = EXCLUDED.created_at
                RETURNING order_payment_id
                """);
            q.setParameter("orderPaymentId", CsvSupport.asLong(row[0]));
            q.setParameter("orderId", CsvSupport.asLong(row[1]));
            q.setParameter("paymentMethodId", CsvSupport.asLong(row[2]));
            q.setParameter("provider", CsvSupport.nullable(row[3]));
            q.setParameter("amountCents", CsvSupport.asInt(row[4]));
            q.setParameter("currency", row[5]);
            q.setParameter("status", row[6]);
            q.setParameter("paidAt", CsvSupport.asOffsetDateTime(row[7]));
            q.setParameter("createdAt", CsvSupport.asOffsetDateTime(row[8]));
            out.add(toRow(q.getSingleResult()));
        }
        return CsvSupport.toCsv(new String[] {"order_payment_id"}, out);
    }

    private List<String[]> parseRows(String csv, int expectedColumns, boolean allowHeader, String[] header) {
        List<String[]> rows = CsvSupport.parseCsvRows(csv);
        if (allowHeader && header != null) {
            rows = CsvSupport.stripOptionalHeader(rows, header);
        }
        if (rows.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "CSV body is empty");
        }
        for (String[] row : rows) {
            if (row.length != expectedColumns) {
                throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Invalid CSV row length. Expected " + expectedColumns + " but got " + row.length
                );
            }
        }
        return rows;
    }

    private List<List<Object>> toRows(List<?> source) {
        List<List<Object>> rows = new ArrayList<>();
        for (Object row : source) {
            rows.add(toRow(row));
        }
        return rows;
    }

    private List<Object> toRow(Object row) {
        if (row instanceof Object[] array) {
            List<Object> out = new ArrayList<>(array.length);
            for (Object item : array) {
                out.add(item);
            }
            return out;
        }
        return List.of(row);
    }
}


