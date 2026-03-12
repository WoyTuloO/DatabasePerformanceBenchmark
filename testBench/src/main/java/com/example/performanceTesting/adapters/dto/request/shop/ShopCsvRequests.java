package com.example.performanceTesting.adapters.dto.request.shop;

public final class ShopCsvRequests {

    private ShopCsvRequests() {
    }

    public record CreateOrderCsvRequest(String csv) {
    }

    public record AddOrderItemCsvRequest(String csv) {
    }

    public record CreateCustomerCsvRequest(String csv) {
    }

    public record CreateAddressCsvRequest(String csv) {
    }

    public record CreateProductCsvRequest(String csv) {
    }

    public record UpsertInventoryCsvRequest(String csv) {
    }

    public record UpdateOrderItemQuantityCsvRequest(String csv) {
    }

    public record UpdateOrderStatusCsvRequest(String csv) {
    }

    public record UpdateProductActiveCsvRequest(String csv) {
    }

    public record UpdateInventoryQuantityCsvRequest(String csv) {
    }

    public record BulkUpdateCategoryPricesCsvRequest(String csv) {
    }
}
