package com.example.performanceTesting.domain.model.shop;

import java.time.OffsetDateTime;
import java.util.List;

public final class ShopDomainModels {

    private ShopDomainModels() {
    }

    public record OperationResult(boolean success, String message) {
    }

    public record CreateOrderResult(Long orderId, OffsetDateTime createdAt) {
    }

    public record OrderItemKey(Long orderId, Integer lineNo) {
    }

    public record IdResult(Long id) {
    }

    public record InventoryResult(Long warehouseId, Long productId, Integer quantity) {
    }

    public record OrderSummary(Long orderId, String status, Integer totalCents, String currency, OffsetDateTime createdAt) {
    }

    public record CartInfo(Long orderId, OffsetDateTime createdAt) {
    }

    public record CartItem(
            Integer lineNo,
            Long productId,
            String stockKeepingUnit,
            String productName,
            Integer quantity,
            Integer unitPriceCents,
            Integer lineTotalCents
    ) {
    }

    public record ProductAvailability(Long productId, String stockKeepingUnit, String name, Boolean active, Long totalStock) {
    }

    public record ProductListItem(
            Long productId,
            String stockKeepingUnit,
            String name,
            Integer basePriceCents,
            String currency,
            Boolean active
    ) {
    }

    public record CustomerOrderDetail(
            Long orderId,
            OffsetDateTime createdAt,
            String status,
            Integer totalCents,
            String currency,
            String paymentMethod,
            String paymentStatus,
            OffsetDateTime paidAt,
            Integer lineNo,
            Integer quantity,
            Integer unitPriceCents,
            String stockKeepingUnit,
            String productName,
            String brandName,
            String categoryName
    ) {
    }

    public record BulkPriceUpdateResult(List<ProductPriceItem> updatedProducts) {
    }

    public record ProductPriceItem(Long productId, Integer basePriceCents) {
    }
}

