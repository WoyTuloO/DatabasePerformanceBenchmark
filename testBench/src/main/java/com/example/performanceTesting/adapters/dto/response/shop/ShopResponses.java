package com.example.performanceTesting.adapters.dto.response.shop;

import java.time.OffsetDateTime;
import java.util.List;

public final class ShopResponses {

    private ShopResponses() {
    }

    public record OperationResponse(boolean success, String message) {
    }

    public record CreateOrderResponse(Long orderId, OffsetDateTime createdAt) {
    }

    public record OrderItemKeyResponse(Long orderId, Integer lineNo) {
    }

    public record IdResponse(Long id) {
    }

    public record InventoryResponse(Long warehouseId, Long productId, Integer quantity) {
    }

    public record OrderSummaryResponse(Long orderId, String status, Integer totalCents, String currency, OffsetDateTime createdAt) {
    }

    public record CartInfoResponse(Long orderId, OffsetDateTime createdAt) {
    }

    public record CartItemResponse(Integer lineNo, Long productId, String stockKeepingUnit, String productName, Integer quantity, Integer unitPriceCents, Integer lineTotalCents) {
    }

    public record ProductAvailabilityResponse(Long productId, String stockKeepingUnit, String name, Boolean active, Long totalStock) {
    }

    public record ProductListItemResponse(Long productId, String stockKeepingUnit, String name, Integer basePriceCents, String currency, Boolean active) {
    }

    public record CustomerOrderDetailResponse(
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

    public record BulkPriceUpdateResponse(List<ProductPriceItemResponse> updatedProducts) {
    }

    public record ProductPriceItemResponse(Long productId, Integer basePriceCents) {
    }
}

