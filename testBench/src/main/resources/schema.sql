CREATE SCHEMA IF NOT EXISTS shop;

CREATE TABLE IF NOT EXISTS shop.customers (
    customer_id BIGSERIAL PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    phone TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS shop.customer_addresses (
    address_id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES shop.customers(customer_id) ON DELETE CASCADE,
    address_label TEXT NOT NULL DEFAULT 'main',
    country TEXT NOT NULL DEFAULT 'PL',
    city TEXT NOT NULL,
    postal_code TEXT NOT NULL,
    street TEXT NOT NULL,
    building_no TEXT NOT NULL,
    apartment_no TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS shop.brands (
    brand_id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS shop.categories (
    category_id BIGSERIAL PRIMARY KEY,
    parent_category_id BIGINT REFERENCES shop.categories(category_id) ON DELETE SET NULL,
    name TEXT NOT NULL,
    UNIQUE(parent_category_id, name)
);

CREATE TABLE IF NOT EXISTS shop.products (
    product_id BIGSERIAL PRIMARY KEY,
    stock_keeping_unit TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    description TEXT,
    brand_id BIGINT REFERENCES shop.brands(brand_id) ON DELETE SET NULL,
    category_id BIGINT REFERENCES shop.categories(category_id) ON DELETE SET NULL,
    base_price_cents INT NOT NULL CHECK (base_price_cents >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'PLN',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS shop.warehouses (
    warehouse_id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    city TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS shop.inventory (
    warehouse_id BIGINT NOT NULL REFERENCES shop.warehouses(warehouse_id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES shop.products(product_id) ON DELETE CASCADE,
    quantity INT NOT NULL CHECK (quantity >= 0),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (warehouse_id, product_id)
);

CREATE TABLE IF NOT EXISTS shop.orders (
    order_id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES shop.customers(customer_id),
    shipping_address_id BIGINT REFERENCES shop.customer_addresses(address_id),
    status TEXT NOT NULL CHECK (status IN ('NEW','PAID','SHIPPED','CANCELLED','RETURNED')),
    total_cents INT NOT NULL CHECK (total_cents >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'PLN',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS shop.order_items (
    order_id BIGINT NOT NULL REFERENCES shop.orders(order_id) ON DELETE CASCADE,
    line_no INT NOT NULL,
    product_id BIGINT NOT NULL REFERENCES shop.products(product_id),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price_cents INT NOT NULL CHECK (unit_price_cents >= 0),
    PRIMARY KEY (order_id, line_no)
);

CREATE TABLE IF NOT EXISTS shop.payments (
    payment_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE REFERENCES shop.orders(order_id) ON DELETE CASCADE,
    method TEXT NOT NULL CHECK (method IN ('CARD','BLIK','TRANSFER','COD')),
    provider TEXT,
    amount_cents INT NOT NULL CHECK (amount_cents >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'PLN',
    status TEXT NOT NULL CHECK (status IN ('PENDING','AUTHORIZED','CAPTURED','FAILED','REFUNDED')),
    paid_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_products_category ON shop.products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_brand ON shop.products(brand_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer_created ON shop.orders(customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_items_product ON shop.order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_product ON shop.inventory(product_id);

