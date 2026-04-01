package com.example.performanceTesting.shop.model;

import lombok.experimental.UtilityClass;

import java.time.OffsetDateTime;

/**
 * Wszystkie DTO żądań dla operacji CRUD sklepu.
 */
@UtilityClass
public final class ShopRequests {

    // ═══════════════════════════ CREATE ═══════════════════════════

    /** C1 – Dodanie zamówienia */
    public record CreateOrder(
            long customerId,
            String shippingCountry,
            String shippingCity,
            String shippingPostalCode,
            String shippingStreet,
            String shippingBuildingNo,
            String shippingApartmentNo,
            String currency
    ) {}

    /** C2 – Dodanie itemu do koszyka */
    public record AddOrderItem(
            long orderId,
            long productId,
            int quantity,
            int unitPriceCents
    ) {}

    /** C3 – Dodanie klienta */
    public record CreateCustomer(
            String email,
            String passwordHash,
            String firstName,
            String lastName,
            String phone
    ) {}

    /** C4 – Dodanie produktu */
    public record CreateProduct(
            String stockKeepingUnit,
            String name,
            String description,
            Long brandId,
            Long categoryId,
            int basePriceCents,
            String currency
    ) {}

    /** C5 – Upsert stanu magazynowego */
    public record UpsertInventory(
            long warehouseId,
            long productId,
            int quantity
    ) {}

    /** C6 – Dodanie płatności do zamówienia */
    public record CreateOrderPayment(
            long orderId,
            long paymentMethodId,
            String provider,
            int amountCents,
            String currency,
            String status,
            OffsetDateTime paidAt
    ) {}

    // ═══════════════════════════ UPDATE (body) ═══════════════════════════

    /** U1 / U4 – Mnożnik ceny */
    public record PriceMultiplier(double multiplier) {}

    /** U2 – Zmiana statusu zamówienia */
    public record UpdateStatus(String status) {}

    /** U3 – Zmiana aktywności produktu */
    public record UpdateActive(boolean active) {}

    /** U5 – Zmiana ilości (quantity) */
    public record UpdateQuantity(int quantity) {}
}
