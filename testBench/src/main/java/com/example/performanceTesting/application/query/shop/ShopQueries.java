package com.example.performanceTesting.application.query.shop;

public final class ShopQueries {

    private ShopQueries() {
    }

    public record GetCustomerOrdersQuery(Long customerId, Integer limit, Integer offset) {
    }

    public record GetLatestNewCartQuery(Long customerId) {
    }

    public record GetCartItemsQuery(Long orderId) {
    }

    public record GetProductAvailabilityQuery(Long productId) {
    }

    public record GetProductsQuery(Boolean active, String q, Integer limit, Integer offset) {
    }

    public record GetCustomerOrderDetailsQuery(Long customerId) {
    }
}

