# Wytyczne Implementacji: ShopService (24 Metody)

Niniejszy dokument zawiera opis logiki biznesowej oraz surowe zapytania SQL wyciągnięte z `PostgresShopService`. Służy on jako wzorzec (blueprint) dla implementacji w innych silnikach bazodanowych (MySQL, MongoDB, Couchbase).

---

## 🟢 CREATE (Operacje Zapisu)

1. **`createOrdersBatch`**
    * **Opis:** Masowy zapis nowych zamówień.
    * **SQL:**
      ```sql
      INSERT INTO orders (customer_id, shipping_country, shipping_city, shipping_postal_code, shipping_street, shipping_building_no, shipping_apartment_no, status, total_cents, currency, created_at)
      VALUES (:customer_id, :shipping_country, :shipping_city, :shipping_postal_code, :shipping_street, :shipping_building_no, :shipping_apartment_no, 'NEW', 0, :currency, CURRENT_TIMESTAMP);
      ```

2. **`addOrderItem`**
    * **Opis:** Dodanie pozycji do zamówienia. Wylicza kolejny numer linii (`line_no`) dla danego zamówienia.
    * **SQL:**
      ```sql
      INSERT INTO order_items (order_id, line_no, product_id, quantity, unit_price_cents)
      VALUES (:order_id, COALESCE((SELECT MAX(line_no) FROM order_items WHERE order_id = :order_id), 0) + 1, :product_id, :quantity, :unit_price_cents)
      RETURNING order_id, line_no;
      ```

3. **`createCustomer`**
    * **Opis:** Rejestracja klienta w systemie.
    * **SQL:**
      ```sql
      INSERT INTO customers (email, password_hash, first_name, last_name, phone, created_at)
      VALUES (:email, :password_hash, :first_name, :last_name, :phone, CURRENT_TIMESTAMP)
      RETURNING customer_id;
      ```

4. **`createProduct`**
    * **Opis:** Dodanie nowego produktu do katalogu.
    * **SQL:**
      ```sql
      INSERT INTO products (stock_keeping_unit, name, description, brand_id, category_id, base_price_cents, currency, active, created_at)
      VALUES (:stock_keeping_unit, :name, :description, :brand_id, :category_id, :base_price_cents, :currency, TRUE, CURRENT_TIMESTAMP)
      RETURNING product_id;
      ```

5. **`upsertInventory`**
    * **Opis:** Aktualizacja stanu magazynowego. Jeśli rekord nie istnieje (para warehouse-product), zostaje utworzony.
    * **SQL:**
      ```sql
      INSERT INTO inventory (warehouse_id, product_id, quantity, updated_at)
      VALUES (:warehouse_id, :product_id, :quantity, CURRENT_TIMESTAMP)
      ON CONFLICT (warehouse_id, product_id) DO UPDATE
      SET quantity = EXCLUDED.quantity, updated_at = CURRENT_TIMESTAMP
      RETURNING warehouse_id, product_id, quantity;
      ```

6. **`createOrderPayment`**
    * **Opis:** Rejestracja nowej płatności do zamówienia.
    * **SQL:**
      ```sql
      INSERT INTO order_payments (order_id, payment_method_id, provider, amount_cents, currency, status, paid_at, created_at)
      VALUES (:order_id, :payment_method_id, :provider, :amount_cents, :currency, :status, :paid_at, CURRENT_TIMESTAMP)
      RETURNING order_payment_id;
      ```

---

## 🔵 READ (Odczyt i Agregacja)

7. **`getMissingProducts`**
    * **Opis:** Zwraca pozycje zamówienia, dla których suma stanów magazynowych jest mniejsza niż zamówiona ilość.
    * **SQL:**
      ```sql
      SELECT oi.line_no, p.product_id, p.stock_keeping_unit, p.name, oi.quantity AS ordered_quantity, COALESCE(SUM(i.quantity), 0) AS total_stock
      FROM order_items oi
      JOIN products p ON p.product_id = oi.product_id
      LEFT JOIN inventory i ON i.product_id = p.product_id
      WHERE oi.order_id = :order_id
      GROUP BY oi.line_no, p.product_id, p.stock_keeping_unit, p.name, oi.quantity
      HAVING COALESCE(SUM(i.quantity), 0) < oi.quantity
      ORDER BY oi.line_no;
      ```

8. **`getAvailableProductsByBrandAndCity`**
    * **Opis:** Filtrowanie aktywnych produktów po marce i dostępności w magazynach w konkretnym mieście.
    * **SQL:**
      ```sql
      SELECT DISTINCT p.product_id, p.stock_keeping_unit, p.name, p.base_price_cents, p.currency, p.active
      FROM products p
      JOIN brands b ON b.brand_id = p.brand_id
      JOIN inventory i ON i.product_id = p.product_id
      JOIN warehouses w ON w.warehouse_id = i.warehouse_id
      WHERE b.name = :brand_name AND w.city = :city AND i.quantity > 0 AND p.active = TRUE
      ORDER BY p.name;
      ```

9. **`getCartItems`**
    * **Opis:** Pobranie wszystkich pozycji danego zamówienia wraz z wyliczonym kosztem całkowitym linii.
    * **SQL:**
      ```sql
      SELECT oi.line_no, p.product_id, p.stock_keeping_unit, p.name, oi.quantity, oi.unit_price_cents, (oi.quantity * oi.unit_price_cents) AS line_total_cents
      FROM order_items oi
      JOIN products p ON p.product_id = oi.product_id
      WHERE oi.order_id = :order_id
      ORDER BY oi.line_no;
      ```

10. **`getProductAvailability`**
    * **Opis:** Sprawdzenie sumarycznego stanu magazynowego produktu (wszystkie magazyny).
    * **SQL:**
      ```sql
      SELECT p.product_id, p.stock_keeping_unit, p.name, p.active, COALESCE(SUM(i.quantity), 0) AS total_stock
      FROM products p
      LEFT JOIN inventory i ON i.product_id = p.product_id
      WHERE p.product_id = :product_id
      GROUP BY p.product_id, p.stock_keeping_unit, p.name, p.active;
      ```

11. **`getCustomerEmailsByPaymentMethod`**
    * **Opis:** Pobranie listy unikalnych e-maili klientów, którzy korzystali z danej metody płatności.
    * **SQL:**
      ```sql
      SELECT DISTINCT c.email
      FROM order_payments op
      JOIN orders o ON o.order_id = op.order_id
      JOIN customers c ON c.customer_id = o.customer_id
      JOIN payment_methods pm ON pm.payment_method_id = op.payment_method_id
      WHERE pm.code = :payment_method;
      ```

12. **`getCustomerOrderDetails`**
    * **Opis:** Agregacja wszystkich informacji o zamówieniach klienta (szczegóły zamówienia, produkty, płatności).
    * **SQL:**
      ```sql
      SELECT o.order_id, o.created_at, o.status, o.total_cents, o.currency, op.order_payment_id, pm.code AS payment_method_code, pm.name AS payment_method_name, op.provider, op.status AS payment_status, op.amount_cents AS payment_amount, op.paid_at, oi.line_no, oi.quantity, oi.unit_price_cents, pr.stock_keeping_unit, pr.name AS product_name, b.name AS brand_name, c.name AS category_name
      FROM orders o
      JOIN order_items oi ON oi.order_id = o.order_id
      JOIN products pr ON pr.product_id = oi.product_id
      LEFT JOIN brands b ON b.brand_id = pr.brand_id
      LEFT JOIN categories c ON c.category_id = pr.category_id
      LEFT JOIN order_payments op ON op.order_id = o.order_id
      LEFT JOIN payment_methods pm ON pm.payment_method_id = op.payment_method_id
      WHERE o.customer_id = :customer_id
      ORDER BY o.created_at DESC, oi.line_no, op.created_at;
      ```

---

## 🟡 UPDATE (Modyfikacje)

13. **`updateCategoryPrices`**
    * **Opis:** Procentowa zmiana cen dla wszystkich produktów w danej kategorii.
    * **SQL:**
      ```sql
      UPDATE products SET base_price_cents = GREATEST(0, CAST(base_price_cents * :multiplier AS integer)) WHERE category_id = :category_id;
      ```

14. **`updateOrderStatusByPayment`**
    * **Opis:** Zmiana statusu zamówienia powiązanego z konkretną płatnością.
    * **SQL:**
      ```sql
      UPDATE orders o SET status = :status FROM order_payments op WHERE o.order_id = op.order_id AND op.order_payment_id = :order_payment_id;
      ```

15. **`updateProductActive`**
    * **Opis:** Włączenie/wyłączenie widoczności produktu.
    * **SQL:**
      ```sql
      UPDATE products SET active = :active WHERE product_id = :product_id;
      ```

16. **`updateBrandPrices`**
    * **Opis:** Procentowa zmiana cen dla wszystkich produktów danej marki.
    * **SQL:**
      ```sql
      UPDATE products SET base_price_cents = CAST(base_price_cents * :multiplier AS integer) WHERE brand_id = :brand_id;
      ```

17. **`updateInventory`**
    * **Opis:** Ręczne ustawienie stanu magazynowego.
    * **SQL:**
      ```sql
      UPDATE inventory SET quantity = :quantity, updated_at = CURRENT_TIMESTAMP WHERE warehouse_id = :warehouse_id AND product_id = :product_id;
      ```

18. **`cancelOrdersByPaymentMethod`**
    * **Opis:** Masowe anulowanie zamówień powiązanych z określonym kodem metody płatności.
    * **SQL:**
      ```sql
      UPDATE orders o SET status = 'CANCELLED' FROM order_payments op JOIN payment_methods pm ON pm.payment_method_id = op.payment_method_id WHERE o.order_id = op.order_id AND pm.code = :code;
      ```

---

## 🔴 DELETE (Usuwanie)

19. **`deleteOldCustomerOrders`**
    * **Opis:** Usuwanie starych zamówień klienta na podstawie daty odcięcia.
    * **SQL:**
      ```sql
      DELETE FROM orders WHERE customer_id = :customer_id AND created_at < :cutoff_date;
      ```

20. **`deleteCart`**
    * **Opis:** Usuwanie zamówienia o statusie 'NEW' (traktowanego jako koszyk).
    * **SQL:**
      ```sql
      DELETE FROM orders WHERE order_id = :order_id AND status = 'NEW';
      ```

21. **`deleteOrderItemsByBrand`**
    * **Opis:** Usuwanie pozycji zamówień dla danej marki spełniających specyficzne warunki (ilość i ID).
    * **SQL:**
      ```sql
      DELETE FROM order_items oi USING products p WHERE oi.product_id = p.product_id AND p.brand_id = :brand_id AND oi.quantity < 2 AND oi.product_id % :brand_id < 5;
      ```

22. **`deleteCustomer`**
    * **Opis:** Całkowite usunięcie profilu klienta.
    * **SQL:**
      ```sql
      DELETE FROM customers WHERE customer_id = :customer_id;
      ```

23. **`deleteWarehouse`**
    * **Opis:** Usunięcie magazynu z systemu.
    * **SQL:**
      ```sql
      DELETE FROM warehouses WHERE warehouse_id = :warehouse_id;
      ```

24. **`deleteOrderItemsByCategory`**
    * **Opis:** Usuwanie pozycji zamówień powiązanych z produktami z określonej kategorii.
    * **SQL:**
      ```sql
      DELETE FROM order_items WHERE product_id IN (SELECT product_id FROM products WHERE category_id = :category_id);
      ```