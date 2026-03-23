package com.example.performanceTesting.shop.api;

import com.example.performanceTesting.shop.application.ShopCsvService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/postgres/shop")
public class ShopCsvController {

    private static final String CSV = "text/csv";
    private final ShopCsvService service;

    public ShopCsvController(ShopCsvService service) {
        this.service = service;
    }

    @PostMapping(path = "/c1/orders", consumes = CSV, produces = CSV)
    public ResponseEntity<String> c1(@RequestBody String body) { return csv(service.c1CreateOrder(body)); }
    @PostMapping(path = "/c2/order-items", consumes = CSV, produces = CSV)
    public ResponseEntity<String> c2(@RequestBody String body) { return csv(service.c2CreateOrderItem(body)); }
    @PostMapping(path = "/c3/customers", consumes = CSV, produces = CSV)
    public ResponseEntity<String> c3(@RequestBody String body) { return csv(service.c3CreateCustomer(body)); }
    @PostMapping(path = "/c4/products", consumes = CSV, produces = CSV)
    public ResponseEntity<String> c4(@RequestBody String body) { return csv(service.c4CreateProduct(body)); }
    @PostMapping(path = "/c5/inventory/upsert", consumes = CSV, produces = CSV)
    public ResponseEntity<String> c5(@RequestBody String body) { return csv(service.c5UpsertInventory(body)); }
    @PostMapping(path = "/c6/order-payments", consumes = CSV, produces = CSV)
    public ResponseEntity<String> c6(@RequestBody String body) { return csv(service.c6CreateOrderPayment(body)); }

    @PostMapping(path = "/r1/orders/by-customer", consumes = CSV, produces = CSV)
    public ResponseEntity<String> r1(@RequestBody String body) { return csv(service.r1GetOrdersByCustomer(body)); }
    @PostMapping(path = "/r2/carts/last-new", consumes = CSV, produces = CSV)
    public ResponseEntity<String> r2(@RequestBody String body) { return csv(service.r2GetLatestNewCart(body)); }
    @PostMapping(path = "/r3/carts/items", consumes = CSV, produces = CSV)
    public ResponseEntity<String> r3(@RequestBody String body) { return csv(service.r3GetCartItems(body)); }
    @PostMapping(path = "/r4/products/stock", consumes = CSV, produces = CSV)
    public ResponseEntity<String> r4(@RequestBody String body) { return csv(service.r4GetProductStock(body)); }
    @PostMapping(path = "/r5/products/search", consumes = CSV, produces = CSV)
    public ResponseEntity<String> r5(@RequestBody String body) { return csv(service.r5SearchProducts(body)); }
    @PostMapping(path = "/r6/orders/details", consumes = CSV, produces = CSV)
    public ResponseEntity<String> r6(@RequestBody String body) { return csv(service.r6GetOrderDetailsByCustomer(body)); }

    @PostMapping(path = "/u1/order-items/quantity", consumes = CSV, produces = CSV)
    public ResponseEntity<String> u1(@RequestBody String body) { return csv(service.u1UpdateOrderItemQuantity(body)); }
    @PostMapping(path = "/u2/orders/status", consumes = CSV, produces = CSV)
    public ResponseEntity<String> u2(@RequestBody String body) { return csv(service.u2UpdateOrderStatus(body)); }
    @PostMapping(path = "/u3/products/active", consumes = CSV, produces = CSV)
    public ResponseEntity<String> u3(@RequestBody String body) { return csv(service.u3UpdateProductActive(body)); }
    @PostMapping(path = "/u4/orders/recalculate-total", consumes = CSV, produces = CSV)
    public ResponseEntity<String> u4(@RequestBody String body) { return csv(service.u4RecalculateOrderTotal(body)); }
    @PostMapping(path = "/u5/inventory/quantity", consumes = CSV, produces = CSV)
    public ResponseEntity<String> u5(@RequestBody String body) { return csv(service.u5UpdateInventoryQuantity(body)); }
    @PostMapping(path = "/u6/order-payments/status", consumes = CSV, produces = CSV)
    public ResponseEntity<String> u6(@RequestBody String body) { return csv(service.u6UpdateOrderPaymentStatus(body)); }

    @PostMapping(path = "/d1/order-items", consumes = CSV, produces = CSV)
    public ResponseEntity<String> d1(@RequestBody String body) { return csv(service.d1DeleteOrderItem(body)); }
    @PostMapping(path = "/d2/orders/new", consumes = CSV, produces = CSV)
    public ResponseEntity<String> d2(@RequestBody String body) { return csv(service.d2DeleteNewOrder(body)); }
    @PostMapping(path = "/d3/order-payments", consumes = CSV, produces = CSV)
    public ResponseEntity<String> d3(@RequestBody String body) { return csv(service.d3DeleteOrderPayment(body)); }
    @PostMapping(path = "/d4/customers", consumes = CSV, produces = CSV)
    public ResponseEntity<String> d4(@RequestBody String body) { return csv(service.d4DeleteCustomer(body)); }
    @PostMapping(path = "/d5/warehouses", consumes = CSV, produces = CSV)
    public ResponseEntity<String> d5(@RequestBody String body) { return csv(service.d5DeleteWarehouse(body)); }
    @PostMapping(path = "/d6/products/hard", consumes = CSV, produces = CSV)
    public ResponseEntity<String> d6(@RequestBody String body) { return csv(service.d6DeleteProductHard(body)); }

    @PostMapping(path = "/seed/customers", consumes = CSV, produces = CSV)
    public ResponseEntity<String> seedCustomers(@RequestBody String body) { return csv(service.importCustomers(body)); }
    @PostMapping(path = "/seed/products", consumes = CSV, produces = CSV)
    public ResponseEntity<String> seedProducts(@RequestBody String body) { return csv(service.importProducts(body)); }
    @PostMapping(path = "/seed/inventory", consumes = CSV, produces = CSV)
    public ResponseEntity<String> seedInventory(@RequestBody String body) { return csv(service.importInventory(body)); }
    @PostMapping(path = "/seed/orders", consumes = CSV, produces = CSV)
    public ResponseEntity<String> seedOrders(@RequestBody String body) { return csv(service.importOrders(body)); }
    @PostMapping(path = "/seed/order-payments", consumes = CSV, produces = CSV)
    public ResponseEntity<String> seedOrderPayments(@RequestBody String body) { return csv(service.importOrderPayments(body)); }

    private ResponseEntity<String> csv(String body) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(CSV)).body(body);
    }
}


