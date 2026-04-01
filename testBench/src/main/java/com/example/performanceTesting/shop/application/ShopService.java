package com.example.performanceTesting.shop.application;

import com.example.performanceTesting.shop.model.ShopRequests;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Interfejs definiujący 24 operacje CRUD sklepu.
 * <p>
 * Aby podmienić bazę danych, wystarczy dostarczyć nową implementację
 * tego interfejsu (np. MongoShopService, RedisShopService)
 * i oznaczyć ją jako {@code @Primary} lub użyć {@code @Profile}.
 */
public interface ShopService {

    // ═══════════════════════════ CREATE (C1–C6) ═══════════════════════════

    /** C1 – Dodanie zamówienia z adresem dostawy */
    int[] createOrdersBatch(List<ShopRequests.CreateOrder> reqs);

    /** C2 – Dodanie itemu do koszyka (auto line_no) */
    Map<String, Object> addOrderItem(ShopRequests.AddOrderItem req);

    /** C3 – Dodanie klienta */
    Map<String, Object> createCustomer(ShopRequests.CreateCustomer req);

    /** C4 – Dodanie produktu */
    Map<String, Object> createProduct(ShopRequests.CreateProduct req);

    /** C5 – Upsert stanu magazynowego */
    Map<String, Object> upsertInventory(ShopRequests.UpsertInventory req);

    /** C6 – Dodanie płatności do zamówienia */
    Map<String, Object> createOrderPayment(ShopRequests.CreateOrderPayment req);

    // ═══════════════════════════ READ (R1–R6) ═══════════════════════════

    /** R1 – Brakujące produkty dla danego zamówienia (stock < ordered qty) */
    List<Map<String, Object>> getMissingProducts(long orderId);

    /** R2 – Dostępne produkty marki X w mieście Y (active, quantity > 0) */
    List<Map<String, Object>> getAvailableProductsByBrandAndCity(String brandName, String city);

    /** R3 – Zawartość koszyka (pozycje + produkty) */
    List<Map<String, Object>> getCartItems(long orderId);

    /** R4 – Sprawdzenie dostępności produktu (suma stanów w magazynach) */
    Map<String, Object> getProductAvailability(long productId);

    /** R5 – Maile klientów płacących konkretną metodą (np. BLIK) */
    List<Map<String, Object>> getCustomerEmailsByPaymentMethod(String paymentMethodCode);

    /** R6 – Szczegóły zamówień klienta z produktami, marką, kategorią i płatnością */
    List<Map<String, Object>> getCustomerOrderDetails(long customerId);

    // ═══════════════════════════ UPDATE (U1–U6) ═══════════════════════════

    /** U1 – Mnożnik ceny (obniżka/podwyżka) dla kategorii */
    int updateCategoryPrices(long categoryId, double multiplier);

    /** U2 – Zmiana statusu zamówienia na podstawie payment_id */
    int updateOrderStatusByPayment(long orderPaymentId, String status);

    /** U3 – Włączenie/wyłączenie produktu */
    int updateProductActive(long productId, boolean active);

    /** U4 – Mnożnik ceny dla danej marki */
    int updateBrandPrices(long brandId, double multiplier);

    /** U5 – Bezpośrednia aktualizacja stanu magazynowego (nadpisanie) */
    int updateInventory(long warehouseId, long productId, int quantity);

    /** U6 – Ustawienie statusu CANCELLED dla zamówień opłaconych daną metodą */
    int cancelOrdersByPaymentMethod(String code);

    // ═══════════════════════════ DELETE (D1–D6) ═══════════════════════════

    /** D1 – Czyszczenie starych zamówień klienta (przed cutoff_date) */
    int deleteOldCustomerOrders(long customerId, OffsetDateTime cutoffDate);

    /** D2 – Usunięcie koszyka (zamówienie ze statusem NEW) */
    int deleteCart(long orderId);

    /** D3 – Usunięcie z zamówień (order_items) produktów danej marki */
    int deleteOrderItemsByBrand(long brandId);

    /** D4 – Usunięcie klienta */
    int deleteCustomer(long customerId);

    /** D5 – Usunięcie magazynu (kaskadowo inventory) */
    int deleteWarehouse(long warehouseId);

    /** D6 – Usunięcie z zamówień (order_items) produktów z danej kategorii */
    int deleteOrderItemsByCategory(long categoryId);
}
