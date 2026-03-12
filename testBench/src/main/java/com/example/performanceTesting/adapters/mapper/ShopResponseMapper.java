package com.example.performanceTesting.adapters.mapper;

import com.example.performanceTesting.adapters.dto.response.shop.ShopResponses;
import com.example.performanceTesting.domain.model.shop.ShopDomainModels;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShopResponseMapper {

    public ShopResponses.CreateOrderResponse toResponse(ShopDomainModels.CreateOrderResult result) {
        return new ShopResponses.CreateOrderResponse(result.orderId(), result.createdAt());
    }

    public ShopResponses.OrderItemKeyResponse toResponse(ShopDomainModels.OrderItemKey key) {
        return new ShopResponses.OrderItemKeyResponse(key.orderId(), key.lineNo());
    }

    public ShopResponses.IdResponse toResponse(ShopDomainModels.IdResult idResult) {
        return new ShopResponses.IdResponse(idResult.id());
    }

    public ShopResponses.InventoryResponse toResponse(ShopDomainModels.InventoryResult result) {
        return new ShopResponses.InventoryResponse(result.warehouseId(), result.productId(), result.quantity());
    }

    public ShopResponses.OperationResponse toResponse(ShopDomainModels.OperationResult result) {
        return new ShopResponses.OperationResponse(result.success(), result.message());
    }

    public List<ShopResponses.OrderSummaryResponse> toOrderSummaries(List<ShopDomainModels.OrderSummary> rows) {
        return rows.stream().map(row -> new ShopResponses.OrderSummaryResponse(row.orderId(), row.status(), row.totalCents(), row.currency(), row.createdAt())).toList();
    }

    public ShopResponses.CartInfoResponse toResponse(ShopDomainModels.CartInfo cartInfo) {
        return new ShopResponses.CartInfoResponse(cartInfo.orderId(), cartInfo.createdAt());
    }

    public List<ShopResponses.CartItemResponse> toCartItems(List<ShopDomainModels.CartItem> rows) {
        return rows.stream().map(row -> new ShopResponses.CartItemResponse(row.lineNo(), row.productId(), row.stockKeepingUnit(), row.productName(), row.quantity(), row.unitPriceCents(), row.lineTotalCents())).toList();
    }

    public ShopResponses.ProductAvailabilityResponse toResponse(ShopDomainModels.ProductAvailability row) {
        return new ShopResponses.ProductAvailabilityResponse(row.productId(), row.stockKeepingUnit(), row.name(), row.active(), row.totalStock());
    }

    public List<ShopResponses.ProductListItemResponse> toProducts(List<ShopDomainModels.ProductListItem> rows) {
        return rows.stream().map(row -> new ShopResponses.ProductListItemResponse(row.productId(), row.stockKeepingUnit(), row.name(), row.basePriceCents(), row.currency(), row.active())).toList();
    }

    public List<ShopResponses.CustomerOrderDetailResponse> toOrderDetails(List<ShopDomainModels.CustomerOrderDetail> rows) {
        return rows.stream().map(row -> new ShopResponses.CustomerOrderDetailResponse(
                row.orderId(),
                row.createdAt(),
                row.status(),
                row.totalCents(),
                row.currency(),
                row.paymentMethod(),
                row.paymentStatus(),
                row.paidAt(),
                row.lineNo(),
                row.quantity(),
                row.unitPriceCents(),
                row.stockKeepingUnit(),
                row.productName(),
                row.brandName(),
                row.categoryName()
        )).toList();
    }

    public ShopResponses.BulkPriceUpdateResponse toResponse(ShopDomainModels.BulkPriceUpdateResult result) {
        return new ShopResponses.BulkPriceUpdateResponse(result.updatedProducts().stream()
                .map(x -> new ShopResponses.ProductPriceItemResponse(x.productId(), x.basePriceCents()))
                .toList());
    }
}

