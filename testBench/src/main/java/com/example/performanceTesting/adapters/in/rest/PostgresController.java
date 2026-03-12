package com.example.performanceTesting.adapters.in.rest;

import com.example.performanceTesting.adapters.dto.request.shop.ShopCsvRequests;
import com.example.performanceTesting.adapters.mapper.ShopRequestMapper;
import com.example.performanceTesting.adapters.mapper.ShopResponseMapper;
import com.example.performanceTesting.application.command.shop.ShopCommands;
import com.example.performanceTesting.application.query.shop.ShopQueries;
import com.example.performanceTesting.application.service.PostgresShopService;
import com.example.performanceTesting.domain.model.shop.ShopDomainModels;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/postgres/shop")
public class PostgresController {

    private final PostgresShopService service;
    private final ShopRequestMapper requestMapper;
    private final ShopResponseMapper responseMapper;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/create/order")
    public Object createOrder(@RequestBody ShopCsvRequests.CreateOrderCsvRequest request) {
        return responseMapper.toResponse(service.createOrder(requestMapper.toCreateOrderCommand(request)));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/create/order-item")
    public Object addOrderItem(@RequestBody ShopCsvRequests.AddOrderItemCsvRequest request) {
        return responseMapper.toResponse(service.addOrderItem(requestMapper.toAddOrderItemCommand(request)));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/create/customer")
    public Object createCustomer(@RequestBody ShopCsvRequests.CreateCustomerCsvRequest request) {
        return responseMapper.toResponse(service.createCustomer(requestMapper.toCreateCustomerCommand(request)));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/create/address")
    public Object createAddress(@RequestBody ShopCsvRequests.CreateAddressCsvRequest request) {
        return responseMapper.toResponse(service.createAddress(requestMapper.toCreateAddressCommand(request)));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/create/product")
    public Object createProduct(@RequestBody ShopCsvRequests.CreateProductCsvRequest request) {
        return responseMapper.toResponse(service.createProduct(requestMapper.toCreateProductCommand(request)));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/create/inventory")
    public Object upsertInventory(@RequestBody ShopCsvRequests.UpsertInventoryCsvRequest request) {
        return responseMapper.toResponse(service.upsertInventory(requestMapper.toUpsertInventoryCommand(request)));
    }

    @GetMapping("/read/customer/{customerId}/orders")
    public Object getCustomerOrders(@PathVariable Long customerId,
                                    @RequestParam(required = false) Integer limit,
                                    @RequestParam(required = false) Integer offset) {
        ShopQueries.GetCustomerOrdersQuery query = requestMapper.toGetCustomerOrdersQuery(customerId, limit, offset);
        return responseMapper.toOrderSummaries(service.getCustomerOrders(query));
    }

    @GetMapping("/read/customer/{customerId}/cart")
    public ResponseEntity<?> getLatestCart(@PathVariable Long customerId) {
        ShopDomainModels.CartInfo result = service.getLatestNewCart(requestMapper.toGetLatestNewCartQuery(customerId));
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(responseMapper.toResponse(result));
    }

    @GetMapping("/read/order/{orderId}/items")
    public Object getCartItems(@PathVariable Long orderId) {
        return responseMapper.toCartItems(service.getCartItems(requestMapper.toGetCartItemsQuery(orderId)));
    }

    @GetMapping("/read/product/{productId}/availability")
    public ResponseEntity<?> getProductAvailability(@PathVariable Long productId) {
        ShopDomainModels.ProductAvailability result = service.getProductAvailability(requestMapper.toGetProductAvailabilityQuery(productId));
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(responseMapper.toResponse(result));
    }

    @GetMapping("/read/products")
    public Object getProducts(@RequestParam(required = false) Boolean active,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) Integer limit,
                              @RequestParam(required = false) Integer offset) {
        return responseMapper.toProducts(service.getProducts(requestMapper.toGetProductsQuery(active, q, limit, offset)));
    }

    @GetMapping("/read/customer/{customerId}/order-details")
    public Object getCustomerOrderDetails(@PathVariable Long customerId) {
        return responseMapper.toOrderDetails(service.getCustomerOrderDetails(requestMapper.toGetCustomerOrderDetailsQuery(customerId)));
    }

    @PutMapping("/update/order-item-quantity")
    public ResponseEntity<?> updateOrderItemQuantity(@RequestBody ShopCsvRequests.UpdateOrderItemQuantityCsvRequest request) {
        ShopDomainModels.OrderItemKey result = service.updateOrderItemQuantity(requestMapper.toUpdateOrderItemQuantityCommand(request));
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(responseMapper.toResponse(result));
    }

    @PutMapping("/update/order-status")
    public ResponseEntity<?> updateOrderStatus(@RequestBody ShopCsvRequests.UpdateOrderStatusCsvRequest request) {
        ShopDomainModels.OperationResult result = service.updateOrderStatus(requestMapper.toUpdateOrderStatusCommand(request));
        if (!result.success()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(responseMapper.toResponse(result));
    }

    @PutMapping("/update/product-active")
    public ResponseEntity<?> updateProductActive(@RequestBody ShopCsvRequests.UpdateProductActiveCsvRequest request) {
        ShopDomainModels.OperationResult result = service.updateProductActive(requestMapper.toUpdateProductActiveCommand(request));
        if (!result.success()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(responseMapper.toResponse(result));
    }

    @PutMapping("/update/order/{orderId}/recalculate-total")
    public ResponseEntity<?> recalculateOrderTotal(@PathVariable Long orderId) {
        ShopCommands.RecalculateOrderTotalCommand command = new ShopCommands.RecalculateOrderTotalCommand(orderId);
        ShopDomainModels.OperationResult result = service.recalculateOrderTotal(command);
        if (!result.success()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(responseMapper.toResponse(result));
    }

    @PutMapping("/update/inventory-quantity")
    public ResponseEntity<?> updateInventoryQuantity(@RequestBody ShopCsvRequests.UpdateInventoryQuantityCsvRequest request) {
        ShopDomainModels.InventoryResult result = service.updateInventoryQuantity(requestMapper.toUpdateInventoryQuantityCommand(request));
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(responseMapper.toResponse(result));
    }

    @PutMapping("/update/category-prices")
    public Object bulkUpdateCategoryPrices(@RequestBody ShopCsvRequests.BulkUpdateCategoryPricesCsvRequest request) {
        return responseMapper.toResponse(service.bulkUpdateCategoryPrices(requestMapper.toBulkUpdateCategoryPricesCommand(request)));
    }

    @DeleteMapping("/delete/order/{orderId}/item/{lineNo}")
    public ResponseEntity<?> deleteOrderItem(@PathVariable Long orderId, @PathVariable Integer lineNo) {
        ShopDomainModels.OrderItemKey result = service.deleteOrderItem(new ShopCommands.DeleteOrderItemCommand(orderId, lineNo));
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(responseMapper.toResponse(result));
    }

    @DeleteMapping("/delete/order/{orderId}")
    public ResponseEntity<?> deleteOrder(@PathVariable Long orderId) {
        ShopDomainModels.IdResult result = service.deleteOrder(new ShopCommands.DeleteOrderCommand(orderId));
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(responseMapper.toResponse(result));
    }

    @DeleteMapping("/delete/address/{addressId}")
    public ResponseEntity<?> deleteAddress(@PathVariable Long addressId) {
        ShopDomainModels.IdResult result = service.deleteAddress(new ShopCommands.DeleteAddressCommand(addressId));
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(responseMapper.toResponse(result));
    }

    @DeleteMapping("/delete/customer/{customerId}")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long customerId) {
        ShopDomainModels.IdResult result = service.deleteCustomer(new ShopCommands.DeleteCustomerCommand(customerId));
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(responseMapper.toResponse(result));
    }

    @DeleteMapping("/delete/warehouse/{warehouseId}")
    public ResponseEntity<?> deleteWarehouse(@PathVariable Long warehouseId) {
        ShopDomainModels.IdResult result = service.deleteWarehouse(new ShopCommands.DeleteWarehouseCommand(warehouseId));
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(responseMapper.toResponse(result));
    }

    @DeleteMapping("/delete/product/{productId}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long productId) {
        ShopDomainModels.IdResult result = service.deleteProduct(new ShopCommands.DeleteProductCommand(productId));
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(responseMapper.toResponse(result));
    }
}
