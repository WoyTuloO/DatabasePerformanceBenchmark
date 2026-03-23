package com.example.performanceTesting.shop.api;

import com.example.performanceTesting.shop.application.ShopCsvService;
import com.example.performanceTesting.shop.csv.CsvSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/postgres/shop")
public class ShopCrudController {

    private final ShopCsvService service;

    public ShopCrudController(ShopCsvService service) {
        this.service = service;
    }

    @PostMapping("/orders")
    public List<Map<String, String>> createOrder(@RequestBody CreateOrderRequest request) {
        String csv = row(
            request.customerId(), request.shippingCountry(), request.shippingCity(), request.shippingPostalCode(),
            request.shippingStreet(), request.shippingBuildingNo(), request.shippingApartmentNo(), "", "", request.currency()
        );
        return toJson(service.c1CreateOrder(csv));
    }

    @PostMapping("/order-items")
    public List<Map<String, String>> createOrderItem(@RequestBody CreateOrderItemRequest request) {
        return toJson(service.c2CreateOrderItem(row(
            request.orderId(), request.productId(), request.quantity(), request.unitPriceCents()
        )));
    }

    @PostMapping("/customers")
    public List<Map<String, String>> createCustomer(@RequestBody CreateCustomerRequest request) {
        return toJson(service.c3CreateCustomer(row(
            request.email(), request.passwordHash(), request.firstName(), request.lastName(), request.phone()
        )));
    }

    @PostMapping("/products")
    public List<Map<String, String>> createProduct(@RequestBody CreateProductRequest request) {
        return toJson(service.c4CreateProduct(row(
            request.stockKeepingUnit(), request.name(), request.description(), request.brandId(),
            request.categoryId(), request.basePriceCents(), request.currency(), request.active()
        )));
    }

    @PatchMapping("/inventory")
    public List<Map<String, String>> upsertInventory(@RequestBody UpsertInventoryRequest request) {
        return toJson(service.c5UpsertInventory(row(request.warehouseId(), request.productId(), request.quantity())));
    }

    @PostMapping("/order-payments")
    public List<Map<String, String>> createOrderPayment(@RequestBody CreateOrderPaymentRequest request) {
        return toJson(service.c6CreateOrderPayment(row(
            request.orderId(), request.paymentMethodId(), request.provider(), request.amountCents(),
            request.currency(), request.status(), request.paidAt()
        )));
    }

    @GetMapping("/orders")
    public List<Map<String, String>> getOrdersByCustomer(
        @RequestParam long customerId,
        @RequestParam(defaultValue = "50") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        return toJson(service.r1GetOrdersByCustomer(row(customerId, limit, offset)));
    }

    @GetMapping("/customers/{customerId}/carts/latest-new")
    public List<Map<String, String>> getLatestNewCart(@PathVariable long customerId) {
        return toJson(service.r2GetLatestNewCart(row(customerId)));
    }

    @GetMapping("/orders/{orderId}/items")
    public List<Map<String, String>> getCartItems(@PathVariable long orderId) {
        return toJson(service.r3GetCartItems(row(orderId)));
    }

    @GetMapping("/products/{productId}/stock")
    public List<Map<String, String>> getProductStock(@PathVariable long productId) {
        return toJson(service.r4GetProductStock(row(productId)));
    }

    @GetMapping("/products")
    public List<Map<String, String>> searchProducts(
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "50") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        return toJson(service.r5SearchProducts(row(active, q, limit, offset)));
    }

    @GetMapping("/customers/{customerId}/order-details")
    public List<Map<String, String>> getOrderDetails(@PathVariable long customerId) {
        return toJson(service.r6GetOrderDetailsByCustomer(row(customerId)));
    }

    @PatchMapping("/order-items/{orderId}/{lineNo}/quantity")
    public List<Map<String, String>> updateOrderItemQuantity(
        @PathVariable long orderId,
        @PathVariable int lineNo,
        @RequestParam int quantity
    ) {
        return toJson(service.u1UpdateOrderItemQuantity(row(orderId, lineNo, quantity)));
    }

    @PatchMapping("/orders/{orderId}/status")
    public List<Map<String, String>> updateOrderStatus(@PathVariable long orderId, @RequestParam String status) {
        return toJson(service.u2UpdateOrderStatus(row(orderId, status)));
    }

    @PatchMapping("/products/{productId}/active")
    public List<Map<String, String>> updateProductActive(@PathVariable long productId, @RequestParam boolean active) {
        return toJson(service.u3UpdateProductActive(row(productId, active)));
    }

    @PatchMapping("/orders/{orderId}/recalculate-total")
    public List<Map<String, String>> recalculateOrderTotal(@PathVariable long orderId) {
        return toJson(service.u4RecalculateOrderTotal(row(orderId)));
    }

    @PatchMapping("/inventory/{warehouseId}/{productId}/quantity")
    public List<Map<String, String>> updateInventoryQuantity(
        @PathVariable long warehouseId,
        @PathVariable long productId,
        @RequestParam int quantity
    ) {
        return toJson(service.u5UpdateInventoryQuantity(row(warehouseId, productId, quantity)));
    }

    @PatchMapping("/order-payments/{orderPaymentId}/status")
    public List<Map<String, String>> updateOrderPaymentStatus(
        @PathVariable long orderPaymentId,
        @RequestParam String status,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String paidAt
    ) {
        return toJson(service.u6UpdateOrderPaymentStatus(row(orderPaymentId, status, paidAt)));
    }

    @DeleteMapping("/order-items/{orderId}/{lineNo}")
    public List<Map<String, String>> deleteOrderItem(@PathVariable long orderId, @PathVariable int lineNo) {
        return toJson(service.d1DeleteOrderItem(row(orderId, lineNo)));
    }

    @DeleteMapping("/orders/{orderId}/new")
    public List<Map<String, String>> deleteNewOrder(@PathVariable long orderId) {
        return toJson(service.d2DeleteNewOrder(row(orderId)));
    }

    @DeleteMapping("/order-payments/{orderPaymentId}")
    public List<Map<String, String>> deleteOrderPayment(@PathVariable long orderPaymentId) {
        return toJson(service.d3DeleteOrderPayment(row(orderPaymentId)));
    }

    @DeleteMapping("/customers/{customerId}")
    public List<Map<String, String>> deleteCustomer(@PathVariable long customerId) {
        return toJson(service.d4DeleteCustomer(row(customerId)));
    }

    @DeleteMapping("/warehouses/{warehouseId}")
    public List<Map<String, String>> deleteWarehouse(@PathVariable long warehouseId) {
        return toJson(service.d5DeleteWarehouse(row(warehouseId)));
    }

    @DeleteMapping("/products/{productId}/hard")
    public List<Map<String, String>> deleteProductHard(@PathVariable long productId) {
        return toJson(service.d6DeleteProductHard(row(productId)));
    }

    private static String row(Object... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            String v = values[i] == null ? "" : String.valueOf(values[i]);
            boolean quote = v.contains(",") || v.contains("\"") || v.contains("\n");
            if (quote) {
                sb.append('"').append(v.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(v);
            }
        }
        return sb.toString();
    }

    private static List<Map<String, String>> toJson(String csv) {
        List<String[]> rows = CsvSupport.parseCsvRows(csv);
        if (rows.isEmpty()) {
            return List.of();
        }
        String[] header = rows.get(0);
        List<Map<String, String>> out = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            Map<String, String> item = new LinkedHashMap<>();
            for (int c = 0; c < header.length; c++) {
                item.put(header[c], c < row.length ? row[c] : "");
            }
            out.add(item);
        }
        return out;
    }

    public record CreateOrderRequest(
        long customerId,
        String shippingCountry,
        String shippingCity,
        String shippingPostalCode,
        String shippingStreet,
        String shippingBuildingNo,
        String shippingApartmentNo,
        String currency
    ) {}

    public record CreateOrderItemRequest(long orderId, long productId, int quantity, int unitPriceCents) {}

    public record CreateCustomerRequest(String email, String passwordHash, String firstName, String lastName, String phone) {}

    public record CreateProductRequest(
        String stockKeepingUnit,
        String name,
        String description,
        long brandId,
        long categoryId,
        int basePriceCents,
        String currency,
        boolean active
    ) {}

    public record UpsertInventoryRequest(long warehouseId, long productId, int quantity) {}

    public record CreateOrderPaymentRequest(
        long orderId,
        long paymentMethodId,
        String provider,
        int amountCents,
        String currency,
        String status,
        String paidAt
    ) {}
}


