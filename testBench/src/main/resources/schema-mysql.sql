CREATE TABLE IF NOT EXISTS customers (
    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    first_name VARCHAR(120) NOT NULL,
    last_name VARCHAR(120) NOT NULL,
    phone VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS brands (
    brand_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS categories (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_category_id BIGINT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_category_id)
        REFERENCES categories (category_id) ON DELETE SET NULL,
    UNIQUE KEY uq_categories_parent_name (parent_category_id, name)
);

CREATE TABLE IF NOT EXISTS products (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_keeping_unit VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    brand_id BIGINT NULL,
    category_id BIGINT NULL,
    base_price_cents INT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'PLN',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id)
        REFERENCES brands (brand_id) ON DELETE SET NULL,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id)
        REFERENCES categories (category_id) ON DELETE SET NULL,
    CONSTRAINT chk_products_base_price CHECK (base_price_cents >= 0)
);

CREATE TABLE IF NOT EXISTS warehouses (
    warehouse_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    city VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS inventory (
    warehouse_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (warehouse_id, product_id),
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (warehouse_id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE,
    CONSTRAINT chk_inventory_quantity CHECK (quantity >= 0)
);

CREATE TABLE IF NOT EXISTS orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    shipping_country VARCHAR(50) NOT NULL DEFAULT 'PL',
    shipping_city VARCHAR(255) NOT NULL,
    shipping_postal_code VARCHAR(32) NOT NULL,
    shipping_street VARCHAR(255) NOT NULL,
    shipping_building_no VARCHAR(32) NOT NULL,
    shipping_apartment_no VARCHAR(32),
    status VARCHAR(20) NOT NULL,
    total_cents INT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'PLN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id) ON DELETE CASCADE,
    CONSTRAINT chk_orders_total_cents CHECK (total_cents >= 0)
);

CREATE TABLE IF NOT EXISTS order_items (
    order_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price_cents INT NOT NULL,
    PRIMARY KEY (order_id, line_no),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)
        REFERENCES products (product_id),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_price CHECK (unit_price_cents >= 0)
);

CREATE TABLE IF NOT EXISTS payment_methods (
    payment_method_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS order_payments (
    order_payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    payment_method_id BIGINT NOT NULL,
    provider VARCHAR(255),
    amount_cents INT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'PLN',
    status VARCHAR(32) NOT NULL,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_payments_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id) ON DELETE CASCADE,
    CONSTRAINT fk_order_payments_method FOREIGN KEY (payment_method_id)
        REFERENCES payment_methods (payment_method_id),
    CONSTRAINT chk_order_payments_amount CHECK (amount_cents >= 0)
);

CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_brand_active_name ON products (brand_id, active, name);
CREATE INDEX idx_orders_customer_created_order ON orders (customer_id, created_at DESC, order_id);
CREATE INDEX idx_order_items_product ON order_items (product_id);
CREATE INDEX idx_inventory_product_quantity ON inventory (product_id, quantity);
CREATE INDEX idx_inventory_warehouse_quantity_product ON inventory (warehouse_id, quantity, product_id);
CREATE INDEX idx_order_payments_order_created ON order_payments (order_id, created_at);
CREATE INDEX idx_order_payments_method_order ON order_payments (payment_method_id, order_id);
CREATE INDEX idx_warehouses_city_id ON warehouses (city, warehouse_id);

