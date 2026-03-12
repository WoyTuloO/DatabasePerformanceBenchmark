package com.example.performanceTesting.application.service;

import com.example.performanceTesting.application.command.shop.ShopCommands;
import com.example.performanceTesting.application.query.shop.ShopQueries;
import com.example.performanceTesting.domain.model.shop.ShopDomainModels;
import com.example.performanceTesting.domain.port.shop.PostgresShopProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostgresShopService {

    private final PostgresShopProvider provider;

    public ShopDomainModels.CreateOrderResult createOrder(ShopCommands.CreateOrderCommand command) {
        return provider.createOrder(command);
    }

    public ShopDomainModels.OrderItemKey addOrderItem(ShopCommands.AddOrderItemCommand command) {
        return provider.addOrderItem(command);
    }

    public ShopDomainModels.IdResult createCustomer(ShopCommands.CreateCustomerCommand command) {
        return provider.createCustomer(command);
    }

    public ShopDomainModels.IdResult createAddress(ShopCommands.CreateAddressCommand command) {
        return provider.createAddress(command);
    }

    public ShopDomainModels.IdResult createProduct(ShopCommands.CreateProductCommand command) {
        return provider.createProduct(command);
    }

    public ShopDomainModels.InventoryResult upsertInventory(ShopCommands.UpsertInventoryCommand command) {
        return provider.upsertInventory(command);
    }

    public List<ShopDomainModels.OrderSummary> getCustomerOrders(ShopQueries.GetCustomerOrdersQuery query) {
        return provider.getCustomerOrders(query);
    }

    public ShopDomainModels.CartInfo getLatestNewCart(ShopQueries.GetLatestNewCartQuery query) {
        return provider.getLatestNewCart(query);
    }

    public List<ShopDomainModels.CartItem> getCartItems(ShopQueries.GetCartItemsQuery query) {
        return provider.getCartItems(query);
    }

    public ShopDomainModels.ProductAvailability getProductAvailability(ShopQueries.GetProductAvailabilityQuery query) {
        return provider.getProductAvailability(query);
    }

    public List<ShopDomainModels.ProductListItem> getProducts(ShopQueries.GetProductsQuery query) {
        return provider.getProducts(query);
    }

    public List<ShopDomainModels.CustomerOrderDetail> getCustomerOrderDetails(ShopQueries.GetCustomerOrderDetailsQuery query) {
        return provider.getCustomerOrderDetails(query);
    }

    public ShopDomainModels.OrderItemKey updateOrderItemQuantity(ShopCommands.UpdateOrderItemQuantityCommand command) {
        return provider.updateOrderItemQuantity(command);
    }

    public ShopDomainModels.OperationResult updateOrderStatus(ShopCommands.UpdateOrderStatusCommand command) {
        return provider.updateOrderStatus(command);
    }

    public ShopDomainModels.OperationResult updateProductActive(ShopCommands.UpdateProductActiveCommand command) {
        return provider.updateProductActive(command);
    }

    public ShopDomainModels.OperationResult recalculateOrderTotal(ShopCommands.RecalculateOrderTotalCommand command) {
        return provider.recalculateOrderTotal(command);
    }

    public ShopDomainModels.InventoryResult updateInventoryQuantity(ShopCommands.UpdateInventoryQuantityCommand command) {
        return provider.updateInventoryQuantity(command);
    }

    public ShopDomainModels.BulkPriceUpdateResult bulkUpdateCategoryPrices(ShopCommands.BulkUpdateCategoryPricesCommand command) {
        return provider.bulkUpdateCategoryPrices(command);
    }

    public ShopDomainModels.OrderItemKey deleteOrderItem(ShopCommands.DeleteOrderItemCommand command) {
        return provider.deleteOrderItem(command);
    }

    public ShopDomainModels.IdResult deleteOrder(ShopCommands.DeleteOrderCommand command) {
        return provider.deleteOrder(command);
    }

    public ShopDomainModels.IdResult deleteAddress(ShopCommands.DeleteAddressCommand command) {
        return provider.deleteAddress(command);
    }

    public ShopDomainModels.IdResult deleteCustomer(ShopCommands.DeleteCustomerCommand command) {
        return provider.deleteCustomer(command);
    }

    public ShopDomainModels.IdResult deleteWarehouse(ShopCommands.DeleteWarehouseCommand command) {
        return provider.deleteWarehouse(command);
    }

    public ShopDomainModels.IdResult deleteProduct(ShopCommands.DeleteProductCommand command) {
        return provider.deleteProduct(command);
    }
}

