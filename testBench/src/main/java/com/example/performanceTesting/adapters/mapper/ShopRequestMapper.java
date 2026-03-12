package com.example.performanceTesting.adapters.mapper;

import com.example.performanceTesting.adapters.dto.request.shop.ShopCsvRequests;
import com.example.performanceTesting.application.command.shop.ShopCommands;
import com.example.performanceTesting.application.query.shop.ShopQueries;
import org.springframework.stereotype.Component;

@Component
public class ShopRequestMapper {

    public ShopCommands.CreateOrderCommand toCreateOrderCommand(ShopCsvRequests.CreateOrderCsvRequest request) {
        String[] args = splitCsv(request.csv(), 3);
        return new ShopCommands.CreateOrderCommand(parseLong(args[0]), parseLong(args[1]), args[2]);
    }

    public ShopCommands.AddOrderItemCommand toAddOrderItemCommand(ShopCsvRequests.AddOrderItemCsvRequest request) {
        String[] args = splitCsv(request.csv(), 4);
        return new ShopCommands.AddOrderItemCommand(parseLong(args[0]), parseLong(args[1]), parseInt(args[2]), parseInt(args[3]));
    }

    public ShopCommands.CreateCustomerCommand toCreateCustomerCommand(ShopCsvRequests.CreateCustomerCsvRequest request) {
        String[] args = splitCsv(request.csv(), 5);
        return new ShopCommands.CreateCustomerCommand(args[0], args[1], args[2], args[3], emptyToNull(args[4]));
    }

    public ShopCommands.CreateAddressCommand toCreateAddressCommand(ShopCsvRequests.CreateAddressCsvRequest request) {
        String[] args = splitCsv(request.csv(), 8);
        return new ShopCommands.CreateAddressCommand(parseLong(args[0]), args[1], args[2], args[3], args[4], args[5], args[6], emptyToNull(args[7]));
    }

    public ShopCommands.CreateProductCommand toCreateProductCommand(ShopCsvRequests.CreateProductCsvRequest request) {
        String[] args = splitCsv(request.csv(), 8);
        return new ShopCommands.CreateProductCommand(args[0], args[1], emptyToNull(args[2]), parseNullableLong(args[3]), parseNullableLong(args[4]), parseInt(args[5]), args[6], parseBoolean(args[7]));
    }

    public ShopCommands.UpsertInventoryCommand toUpsertInventoryCommand(ShopCsvRequests.UpsertInventoryCsvRequest request) {
        String[] args = splitCsv(request.csv(), 3);
        return new ShopCommands.UpsertInventoryCommand(parseLong(args[0]), parseLong(args[1]), parseInt(args[2]));
    }

    public ShopCommands.UpdateOrderItemQuantityCommand toUpdateOrderItemQuantityCommand(ShopCsvRequests.UpdateOrderItemQuantityCsvRequest request) {
        String[] args = splitCsv(request.csv(), 3);
        return new ShopCommands.UpdateOrderItemQuantityCommand(parseLong(args[0]), parseInt(args[1]), parseInt(args[2]));
    }

    public ShopCommands.UpdateOrderStatusCommand toUpdateOrderStatusCommand(ShopCsvRequests.UpdateOrderStatusCsvRequest request) {
        String[] args = splitCsv(request.csv(), 2);
        return new ShopCommands.UpdateOrderStatusCommand(parseLong(args[0]), args[1]);
    }

    public ShopCommands.UpdateProductActiveCommand toUpdateProductActiveCommand(ShopCsvRequests.UpdateProductActiveCsvRequest request) {
        String[] args = splitCsv(request.csv(), 2);
        return new ShopCommands.UpdateProductActiveCommand(parseLong(args[0]), parseBoolean(args[1]));
    }

    public ShopCommands.UpdateInventoryQuantityCommand toUpdateInventoryQuantityCommand(ShopCsvRequests.UpdateInventoryQuantityCsvRequest request) {
        String[] args = splitCsv(request.csv(), 3);
        return new ShopCommands.UpdateInventoryQuantityCommand(parseLong(args[0]), parseLong(args[1]), parseInt(args[2]));
    }

    public ShopCommands.BulkUpdateCategoryPricesCommand toBulkUpdateCategoryPricesCommand(ShopCsvRequests.BulkUpdateCategoryPricesCsvRequest request) {
        String[] args = splitCsv(request.csv(), 2);
        return new ShopCommands.BulkUpdateCategoryPricesCommand(parseLong(args[0]), Double.parseDouble(args[1]));
    }

    public ShopQueries.GetCustomerOrdersQuery toGetCustomerOrdersQuery(Long customerId, Integer limit, Integer offset) {
        return new ShopQueries.GetCustomerOrdersQuery(customerId, defaultIfNull(limit, 50), defaultIfNull(offset, 0));
    }

    public ShopQueries.GetLatestNewCartQuery toGetLatestNewCartQuery(Long customerId) {
        return new ShopQueries.GetLatestNewCartQuery(customerId);
    }

    public ShopQueries.GetCartItemsQuery toGetCartItemsQuery(Long orderId) {
        return new ShopQueries.GetCartItemsQuery(orderId);
    }

    public ShopQueries.GetProductAvailabilityQuery toGetProductAvailabilityQuery(Long productId) {
        return new ShopQueries.GetProductAvailabilityQuery(productId);
    }

    public ShopQueries.GetProductsQuery toGetProductsQuery(Boolean active, String q, Integer limit, Integer offset) {
        return new ShopQueries.GetProductsQuery(active, emptyToNull(q), defaultIfNull(limit, 50), defaultIfNull(offset, 0));
    }

    public ShopQueries.GetCustomerOrderDetailsQuery toGetCustomerOrderDetailsQuery(Long customerId) {
        return new ShopQueries.GetCustomerOrderDetailsQuery(customerId);
    }

    private String[] splitCsv(String csv, int expectedSize) {
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException("CSV payload is empty");
        }
        String[] values = csv.split("\\s*,\\s*", -1);
        if (values.length != expectedSize) {
            throw new IllegalArgumentException("Expected " + expectedSize + " CSV values, got " + values.length);
        }
        return values;
    }

    private Long parseLong(String value) {
        return Long.parseLong(value);
    }

    private Long parseNullableLong(String value) {
        return value == null || value.isBlank() ? null : Long.parseLong(value);
    }

    private Integer parseInt(String value) {
        return Integer.parseInt(value);
    }

    private Boolean parseBoolean(String value) {
        return Boolean.parseBoolean(value);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private int defaultIfNull(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }
}

