package com.example.performanceTesting.application.command.shop;

public final class ShopCommands {

    private ShopCommands() {
    }

    public record CreateOrderCommand(Long customerId, Long shippingAddressId, String currency) {
    }

    public record AddOrderItemCommand(Long orderId, Long productId, Integer quantity, Integer unitPriceCents) {
    }

    public record CreateCustomerCommand(String email, String passwordHash, String firstName, String lastName, String phone) {
    }

    public record CreateAddressCommand(
            Long customerId,
            String addressLabel,
            String country,
            String city,
            String postalCode,
            String street,
            String buildingNo,
            String apartmentNo
    ) {
    }

    public record CreateProductCommand(
            String stockKeepingUnit,
            String name,
            String description,
            Long brandId,
            Long categoryId,
            Integer basePriceCents,
            String currency,
            Boolean active
    ) {
    }

    public record UpsertInventoryCommand(Long warehouseId, Long productId, Integer quantity) {
    }

    public record UpdateOrderItemQuantityCommand(Long orderId, Integer lineNo, Integer quantity) {
    }

    public record UpdateOrderStatusCommand(Long orderId, String status) {
    }

    public record UpdateProductActiveCommand(Long productId, Boolean active) {
    }

    public record RecalculateOrderTotalCommand(Long orderId) {
    }

    public record UpdateInventoryQuantityCommand(Long warehouseId, Long productId, Integer quantity) {
    }

    public record BulkUpdateCategoryPricesCommand(Long categoryId, Double multiplier) {
    }

    public record DeleteOrderItemCommand(Long orderId, Integer lineNo) {
    }

    public record DeleteOrderCommand(Long orderId) {
    }

    public record DeleteAddressCommand(Long addressId) {
    }

    public record DeleteCustomerCommand(Long customerId) {
    }

    public record DeleteWarehouseCommand(Long warehouseId) {
    }

    public record DeleteProductCommand(Long productId) {
    }
}

