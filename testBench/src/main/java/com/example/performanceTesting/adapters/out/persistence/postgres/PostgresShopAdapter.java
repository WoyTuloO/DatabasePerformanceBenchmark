package com.example.performanceTesting.adapters.out.persistence.postgres;

import com.example.performanceTesting.adapters.out.persistence.postgres.entity.*;
import com.example.performanceTesting.adapters.out.persistence.postgres.repository.CustomerAddressRepository;
import com.example.performanceTesting.adapters.out.persistence.postgres.repository.CustomerRepository;
import com.example.performanceTesting.adapters.out.persistence.postgres.repository.InventoryRepository;
import com.example.performanceTesting.adapters.out.persistence.postgres.repository.OrderItemRepository;
import com.example.performanceTesting.adapters.out.persistence.postgres.repository.OrderRepository;
import com.example.performanceTesting.adapters.out.persistence.postgres.repository.ProductRepository;
import com.example.performanceTesting.adapters.out.persistence.postgres.repository.WarehouseRepository;
import com.example.performanceTesting.application.command.shop.ShopCommands;
import com.example.performanceTesting.application.query.shop.ShopQueries;
import com.example.performanceTesting.domain.model.shop.ShopDomainModels;
import com.example.performanceTesting.domain.port.shop.PostgresShopProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional
public class PostgresShopAdapter implements PostgresShopProvider {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final WarehouseRepository warehouseRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ShopDomainModels.CreateOrderResult createOrder(ShopCommands.CreateOrderCommand command) {
        OrderEntity order = new OrderEntity();
        order.setCustomerId(command.customerId());
        order.setShippingAddressId(command.shippingAddressId());
        order.setStatus("NEW");
        order.setTotalCents(0);
        order.setCurrency(command.currency());
        order.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        OrderEntity saved = orderRepository.save(order);
        return new ShopDomainModels.CreateOrderResult(saved.getOrderId(), saved.getCreatedAt());
    }

    @Override
    public ShopDomainModels.OrderItemKey addOrderItem(ShopCommands.AddOrderItemCommand command) {
        Integer maxLine = ((Number) entityManager.createNativeQuery("""
                SELECT COALESCE(MAX(oi.line_no), 0)
                FROM shop.order_items oi
                WHERE oi.order_id = :orderId
                """)
                .setParameter("orderId", command.orderId())
                .getSingleResult()).intValue();

        OrderItemEntity item = new OrderItemEntity();
        item.setId(new OrderItemEntity.OrderItemId(command.orderId(), maxLine + 1));
        item.setProductId(command.productId());
        item.setQuantity(command.quantity());
        item.setUnitPriceCents(command.unitPriceCents());
        OrderItemEntity saved = orderItemRepository.save(item);
        return new ShopDomainModels.OrderItemKey(saved.getId().getOrderId(), saved.getId().getLineNo());
    }

    @Override
    public ShopDomainModels.IdResult createCustomer(ShopCommands.CreateCustomerCommand command) {
        CustomerEntity customer = new CustomerEntity();
        customer.setEmail(command.email());
        customer.setPasswordHash(command.passwordHash());
        customer.setFirstName(command.firstName());
        customer.setLastName(command.lastName());
        customer.setPhone(command.phone());
        customer.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        CustomerEntity saved = customerRepository.save(customer);
        return new ShopDomainModels.IdResult(saved.getCustomerId());
    }

    @Override
    public ShopDomainModels.IdResult createAddress(ShopCommands.CreateAddressCommand command) {
        CustomerAddressEntity address = new CustomerAddressEntity();
        address.setCustomerId(command.customerId());
        address.setAddressLabel(command.addressLabel());
        address.setCountry(command.country());
        address.setCity(command.city());
        address.setPostalCode(command.postalCode());
        address.setStreet(command.street());
        address.setBuildingNo(command.buildingNo());
        address.setApartmentNo(command.apartmentNo());
        address.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        CustomerAddressEntity saved = customerAddressRepository.save(address);
        return new ShopDomainModels.IdResult(saved.getAddressId());
    }

    @Override
    public ShopDomainModels.IdResult createProduct(ShopCommands.CreateProductCommand command) {
        ProductEntity product = new ProductEntity();
        product.setStockKeepingUnit(command.stockKeepingUnit());
        product.setName(command.name());
        product.setDescription(command.description());
        product.setBrandId(command.brandId());
        product.setCategoryId(command.categoryId());
        product.setBasePriceCents(command.basePriceCents());
        product.setCurrency(command.currency());
        product.setActive(command.active() == null ? Boolean.TRUE : command.active());
        product.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        ProductEntity saved = productRepository.save(product);
        return new ShopDomainModels.IdResult(saved.getProductId());
    }

    @Override
    public ShopDomainModels.InventoryResult upsertInventory(ShopCommands.UpsertInventoryCommand command) {
        InventoryId id = new InventoryId(command.warehouseId(), command.productId());
        InventoryEntity inventory = inventoryRepository.findById(id).orElseGet(InventoryEntity::new);
        inventory.setId(id);
        inventory.setQuantity(command.quantity());
        inventory.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        InventoryEntity saved = inventoryRepository.save(inventory);
        return new ShopDomainModels.InventoryResult(saved.getId().getWarehouseId(), saved.getId().getProductId(), saved.getQuantity());
    }

    @Override
    public List<ShopDomainModels.OrderSummary> getCustomerOrders(ShopQueries.GetCustomerOrdersQuery query) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT o.order_id, o.status, o.total_cents, o.currency, o.created_at
                FROM shop.orders o
                WHERE o.customer_id = :customerId
                ORDER BY o.created_at DESC
                LIMIT :limit OFFSET :offset
                """)
                .setParameter("customerId", query.customerId())
                .setParameter("limit", query.limit())
                .setParameter("offset", query.offset())
                .getResultList();

        List<ShopDomainModels.OrderSummary> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new ShopDomainModels.OrderSummary(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    ((Number) row[2]).intValue(),
                    (String) row[3],
                    ((java.sql.Timestamp) row[4]).toInstant().atOffset(ZoneOffset.UTC)
            ));
        }
        return result;
    }

    @Override
    public ShopDomainModels.CartInfo getLatestNewCart(ShopQueries.GetLatestNewCartQuery query) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT o.order_id, o.created_at
                FROM shop.orders o
                WHERE o.customer_id = :customerId
                  AND o.status = 'NEW'
                ORDER BY o.created_at DESC
                LIMIT 1
                """)
                .setParameter("customerId", query.customerId())
                .getResultList();

        if (rows.isEmpty()) {
            return null;
        }

        Object[] row = rows.getFirst();
        return new ShopDomainModels.CartInfo(
                ((Number) row[0]).longValue(),
                ((java.sql.Timestamp) row[1]).toInstant().atOffset(ZoneOffset.UTC)
        );
    }

    @Override
    public List<ShopDomainModels.CartItem> getCartItems(ShopQueries.GetCartItemsQuery query) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT oi.line_no,
                       oi.product_id,
                       p.stock_keeping_unit,
                       p.name,
                       oi.quantity,
                       oi.unit_price_cents,
                       (oi.quantity * oi.unit_price_cents) AS line_total_cents
                FROM shop.order_items oi
                JOIN shop.products p ON p.product_id = oi.product_id
                WHERE oi.order_id = :orderId
                ORDER BY oi.line_no
                """)
                .setParameter("orderId", query.orderId())
                .getResultList();

        List<ShopDomainModels.CartItem> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new ShopDomainModels.CartItem(
                    ((Number) row[0]).intValue(),
                    ((Number) row[1]).longValue(),
                    (String) row[2],
                    (String) row[3],
                    ((Number) row[4]).intValue(),
                    ((Number) row[5]).intValue(),
                    ((Number) row[6]).intValue()
            ));
        }
        return result;
    }

    @Override
    public ShopDomainModels.ProductAvailability getProductAvailability(ShopQueries.GetProductAvailabilityQuery query) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT p.product_id,
                       p.stock_keeping_unit,
                       p.name,
                       p.active,
                       COALESCE(SUM(i.quantity), 0) AS total_stock
                FROM shop.products p
                LEFT JOIN shop.inventory i ON i.product_id = p.product_id
                WHERE p.product_id = :productId
                GROUP BY p.product_id
                """)
                .setParameter("productId", query.productId())
                .getResultList();

        if (rows.isEmpty()) {
            return null;
        }

        Object[] row = rows.getFirst();
        return new ShopDomainModels.ProductAvailability(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (Boolean) row[3],
                ((Number) row[4]).longValue()
        );
    }

    @Override
    public List<ShopDomainModels.ProductListItem> getProducts(ShopQueries.GetProductsQuery query) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT p.product_id, p.stock_keeping_unit, p.name, p.base_price_cents, p.currency, p.active
                FROM shop.products p
                WHERE (:active IS NULL OR p.active = :active)
                  AND (:q IS NULL OR p.name ILIKE '%' || :q || '%' OR p.stock_keeping_unit ILIKE '%' || :q || '%')
                ORDER BY p.created_at DESC
                LIMIT :limit OFFSET :offset
                """)
                .setParameter("active", query.active())
                .setParameter("q", query.q())
                .setParameter("limit", query.limit())
                .setParameter("offset", query.offset())
                .getResultList();

        List<ShopDomainModels.ProductListItem> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new ShopDomainModels.ProductListItem(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    ((Number) row[3]).intValue(),
                    (String) row[4],
                    (Boolean) row[5]
            ));
        }
        return result;
    }

    @Override
    public List<ShopDomainModels.CustomerOrderDetail> getCustomerOrderDetails(ShopQueries.GetCustomerOrderDetailsQuery query) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT o.order_id, o.created_at, o.status, o.total_cents, o.currency,
                       pmt.method, pmt.status AS payment_status, pmt.paid_at,
                       oi.line_no, oi.quantity, oi.unit_price_cents,
                       pr.stock_keeping_unit, pr.name AS product_name,
                       b.name AS brand_name, c.name AS category_name
                FROM shop.orders o
                JOIN shop.order_items oi    ON oi.order_id = o.order_id
                JOIN shop.products pr       ON pr.product_id = oi.product_id
                LEFT JOIN shop.brands b     ON b.brand_id = pr.brand_id
                LEFT JOIN shop.categories c ON c.category_id = pr.category_id
                LEFT JOIN shop.payments pmt ON pmt.order_id = o.order_id
                WHERE o.customer_id = :customerId
                ORDER BY o.created_at DESC, oi.line_no
                """)
                .setParameter("customerId", query.customerId())
                .getResultList();

        List<ShopDomainModels.CustomerOrderDetail> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new ShopDomainModels.CustomerOrderDetail(
                    ((Number) row[0]).longValue(),
                    ((java.sql.Timestamp) row[1]).toInstant().atOffset(ZoneOffset.UTC),
                    (String) row[2],
                    ((Number) row[3]).intValue(),
                    (String) row[4],
                    (String) row[5],
                    (String) row[6],
                    row[7] == null ? null : ((java.sql.Timestamp) row[7]).toInstant().atOffset(ZoneOffset.UTC),
                    ((Number) row[8]).intValue(),
                    ((Number) row[9]).intValue(),
                    ((Number) row[10]).intValue(),
                    (String) row[11],
                    (String) row[12],
                    (String) row[13],
                    (String) row[14]
            ));
        }
        return result;
    }

    @Override
    public ShopDomainModels.OrderItemKey updateOrderItemQuantity(ShopCommands.UpdateOrderItemQuantityCommand command) {
        Optional<OrderItemEntity> item = orderItemRepository.findById(new OrderItemEntity.OrderItemId(command.orderId(), command.lineNo()));
        if (item.isEmpty()) {
            return null;
        }
        OrderItemEntity entity = item.get();
        entity.setQuantity(command.quantity());
        orderItemRepository.save(entity);
        return new ShopDomainModels.OrderItemKey(entity.getId().getOrderId(), entity.getId().getLineNo());
    }

    @Override
    public ShopDomainModels.OperationResult updateOrderStatus(ShopCommands.UpdateOrderStatusCommand command) {
        Optional<OrderEntity> order = orderRepository.findById(command.orderId());
        if (order.isEmpty()) {
            return new ShopDomainModels.OperationResult(false, "Order not found");
        }
        OrderEntity entity = order.get();
        entity.setStatus(command.status());
        orderRepository.save(entity);
        return new ShopDomainModels.OperationResult(true, "Order status updated");
    }

    @Override
    public ShopDomainModels.OperationResult updateProductActive(ShopCommands.UpdateProductActiveCommand command) {
        Optional<ProductEntity> product = productRepository.findById(command.productId());
        if (product.isEmpty()) {
            return new ShopDomainModels.OperationResult(false, "Product not found");
        }
        ProductEntity entity = product.get();
        entity.setActive(command.active());
        productRepository.save(entity);
        return new ShopDomainModels.OperationResult(true, "Product active flag updated");
    }

    @Override
    public ShopDomainModels.OperationResult recalculateOrderTotal(ShopCommands.RecalculateOrderTotalCommand command) {
        Number total = (Number) entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(quantity * unit_price_cents), 0)
                FROM shop.order_items
                WHERE order_id = :orderId
                """)
                .setParameter("orderId", command.orderId())
                .getSingleResult();

        Optional<OrderEntity> order = orderRepository.findById(command.orderId());
        if (order.isEmpty()) {
            return new ShopDomainModels.OperationResult(false, "Order not found");
        }
        OrderEntity entity = order.get();
        entity.setTotalCents(total.intValue());
        orderRepository.save(entity);
        return new ShopDomainModels.OperationResult(true, "Order total recalculated");
    }

    @Override
    public ShopDomainModels.InventoryResult updateInventoryQuantity(ShopCommands.UpdateInventoryQuantityCommand command) {
        InventoryId id = new InventoryId(command.warehouseId(), command.productId());
        Optional<InventoryEntity> inventory = inventoryRepository.findById(id);
        if (inventory.isEmpty()) {
            return null;
        }
        InventoryEntity entity = inventory.get();
        entity.setQuantity(command.quantity());
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        inventoryRepository.save(entity);
        return new ShopDomainModels.InventoryResult(entity.getId().getWarehouseId(), entity.getId().getProductId(), entity.getQuantity());
    }

    @Override
    public ShopDomainModels.BulkPriceUpdateResult bulkUpdateCategoryPrices(ShopCommands.BulkUpdateCategoryPricesCommand command) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                UPDATE shop.products
                SET base_price_cents = GREATEST(0, (base_price_cents * :multiplier)::int)
                WHERE category_id = :categoryId
                RETURNING product_id, base_price_cents
                """)
                .setParameter("categoryId", command.categoryId())
                .setParameter("multiplier", command.multiplier())
                .getResultList();

        List<ShopDomainModels.ProductPriceItem> updated = new ArrayList<>();
        for (Object[] row : rows) {
            updated.add(new ShopDomainModels.ProductPriceItem(((Number) row[0]).longValue(), ((Number) row[1]).intValue()));
        }
        return new ShopDomainModels.BulkPriceUpdateResult(updated);
    }

    @Override
    public ShopDomainModels.OrderItemKey deleteOrderItem(ShopCommands.DeleteOrderItemCommand command) {
        OrderItemEntity.OrderItemId id = new OrderItemEntity.OrderItemId(command.orderId(), command.lineNo());
        if (!orderItemRepository.existsById(id)) {
            return null;
        }
        orderItemRepository.deleteById(id);
        return new ShopDomainModels.OrderItemKey(command.orderId(), command.lineNo());
    }

    @Override
    public ShopDomainModels.IdResult deleteOrder(ShopCommands.DeleteOrderCommand command) {
        Optional<OrderEntity> order = orderRepository.findById(command.orderId());
        if (order.isEmpty() || !"NEW".equals(order.get().getStatus())) {
            return null;
        }
        orderRepository.deleteById(command.orderId());
        return new ShopDomainModels.IdResult(command.orderId());
    }

    @Override
    public ShopDomainModels.IdResult deleteAddress(ShopCommands.DeleteAddressCommand command) {
        if (!customerAddressRepository.existsById(command.addressId())) {
            return null;
        }
        customerAddressRepository.deleteById(command.addressId());
        return new ShopDomainModels.IdResult(command.addressId());
    }

    @Override
    public ShopDomainModels.IdResult deleteCustomer(ShopCommands.DeleteCustomerCommand command) {
        if (!customerRepository.existsById(command.customerId())) {
            return null;
        }
        customerRepository.deleteById(command.customerId());
        return new ShopDomainModels.IdResult(command.customerId());
    }

    @Override
    public ShopDomainModels.IdResult deleteWarehouse(ShopCommands.DeleteWarehouseCommand command) {
        if (!warehouseRepository.existsById(command.warehouseId())) {
            return null;
        }
        warehouseRepository.deleteById(command.warehouseId());
        return new ShopDomainModels.IdResult(command.warehouseId());
    }

    @Override
    public ShopDomainModels.IdResult deleteProduct(ShopCommands.DeleteProductCommand command) {
        if (!productRepository.existsById(command.productId())) {
            return null;
        }
        productRepository.deleteById(command.productId());
        return new ShopDomainModels.IdResult(command.productId());
    }
}

