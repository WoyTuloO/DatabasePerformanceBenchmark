package com.example.performanceTesting.domain.port.shop;

import com.example.performanceTesting.application.command.shop.ShopCommands;
import com.example.performanceTesting.application.query.shop.ShopQueries;
import com.example.performanceTesting.domain.model.shop.ShopDomainModels;

import java.util.List;

public interface PostgresShopProvider {

    ShopDomainModels.CreateOrderResult createOrder(ShopCommands.CreateOrderCommand command);

    ShopDomainModels.OrderItemKey addOrderItem(ShopCommands.AddOrderItemCommand command);

    ShopDomainModels.IdResult createCustomer(ShopCommands.CreateCustomerCommand command);

    ShopDomainModels.IdResult createAddress(ShopCommands.CreateAddressCommand command);

    ShopDomainModels.IdResult createProduct(ShopCommands.CreateProductCommand command);

    ShopDomainModels.InventoryResult upsertInventory(ShopCommands.UpsertInventoryCommand command);

    List<ShopDomainModels.OrderSummary> getCustomerOrders(ShopQueries.GetCustomerOrdersQuery query);

    ShopDomainModels.CartInfo getLatestNewCart(ShopQueries.GetLatestNewCartQuery query);

    List<ShopDomainModels.CartItem> getCartItems(ShopQueries.GetCartItemsQuery query);

    ShopDomainModels.ProductAvailability getProductAvailability(ShopQueries.GetProductAvailabilityQuery query);

    List<ShopDomainModels.ProductListItem> getProducts(ShopQueries.GetProductsQuery query);

    List<ShopDomainModels.CustomerOrderDetail> getCustomerOrderDetails(ShopQueries.GetCustomerOrderDetailsQuery query);

    ShopDomainModels.OrderItemKey updateOrderItemQuantity(ShopCommands.UpdateOrderItemQuantityCommand command);

    ShopDomainModels.OperationResult updateOrderStatus(ShopCommands.UpdateOrderStatusCommand command);

    ShopDomainModels.OperationResult updateProductActive(ShopCommands.UpdateProductActiveCommand command);

    ShopDomainModels.OperationResult recalculateOrderTotal(ShopCommands.RecalculateOrderTotalCommand command);

    ShopDomainModels.InventoryResult updateInventoryQuantity(ShopCommands.UpdateInventoryQuantityCommand command);

    ShopDomainModels.BulkPriceUpdateResult bulkUpdateCategoryPrices(ShopCommands.BulkUpdateCategoryPricesCommand command);

    ShopDomainModels.OrderItemKey deleteOrderItem(ShopCommands.DeleteOrderItemCommand command);

    ShopDomainModels.IdResult deleteOrder(ShopCommands.DeleteOrderCommand command);

    ShopDomainModels.IdResult deleteAddress(ShopCommands.DeleteAddressCommand command);

    ShopDomainModels.IdResult deleteCustomer(ShopCommands.DeleteCustomerCommand command);

    ShopDomainModels.IdResult deleteWarehouse(ShopCommands.DeleteWarehouseCommand command);

    ShopDomainModels.IdResult deleteProduct(ShopCommands.DeleteProductCommand command);
}

