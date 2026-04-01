package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * C4 – Utwórz produkt
 * Warmup simulation: 10 requests
 * Generates random product data and saves created product_id to file
 */
public class C4CreateProductSimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String endpoint = "/api/shop/products";
    private static final String createdIdsFile = "target/created_product_ids.txt";
    
    private static final String[] PRODUCT_NAMES = {"Laptop", "Smartphone", "Tablet", "Monitor", "Keyboard", "Mouse", "Headphones", "Camera", "Speaker", "Smartwatch"};
    private static final String[] CURRENCIES = {"PLN", "EUR", "USD"};

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("C4 Create Product")
            .exec(session -> {
                // Generate random product data
                String sku = "WARM-SKU-" + UUID.randomUUID().toString().substring(0, 8);
                String name = "Warmup " + PRODUCT_NAMES[ThreadLocalRandom.current().nextInt(PRODUCT_NAMES.length)] + " " + ThreadLocalRandom.current().nextInt(1000, 10000);
                String description = "Test product for warmup - " + UUID.randomUUID().toString();
                long brandId = ThreadLocalRandom.current().nextLong(1, 30);
                long categoryId = ThreadLocalRandom.current().nextLong(1, 30);
                int basePriceCents = ThreadLocalRandom.current().nextInt(5000, 500000);
                String currency = CURRENCIES[ThreadLocalRandom.current().nextInt(CURRENCIES.length)];
                
                return session
                    .set("stockKeepingUnit", sku)
                    .set("name", name)
                    .set("description", description)
                    .set("brandId", brandId)
                    .set("categoryId", categoryId)
                    .set("basePriceCents", basePriceCents)
                    .set("currency", currency);
            })
            .exec(
                    http("POST /api/shop/products")
                            .post(endpoint)
                            .body(StringBody("{\"stockKeepingUnit\":\"#{stockKeepingUnit}\",\"name\":\"#{name}\",\"description\":\"#{description}\",\"brandId\":#{brandId},\"categoryId\":#{categoryId},\"basePriceCents\":#{basePriceCents},\"currency\":\"#{currency}\"}"))
                            .check(status().is(200)));

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol);
    }
}
