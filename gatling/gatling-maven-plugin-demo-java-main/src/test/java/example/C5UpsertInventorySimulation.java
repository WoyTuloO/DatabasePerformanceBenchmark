package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.concurrent.ThreadLocalRandom;

/**
 * C5 – Upsert stanu magazynowego
 * Warmup simulation: 10 requests
 * Generates random inventory data
 */
public class C5UpsertInventorySimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String endpoint = "/api/shop/inventory";

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("C5 Upsert Inventory")
            .exec(session -> {
                long warehouseId = ThreadLocalRandom.current().nextLong(2, 4);
                long productId = ThreadLocalRandom.current().nextLong(1, 500001);
                int quantity = ThreadLocalRandom.current().nextInt(10, 100);

                return session
                        .set("warehouseId", warehouseId)
                        .set("productId", productId)
                        .set("quantity", quantity);
            })
            .exec(
                    http("PUT /api/shop/inventory")
                            .put(endpoint)
                            .body(StringBody("{\"warehouseId\":#{warehouseId},\"productId\":#{productId},\"quantity\":#{quantity}}"))
                            .check(status().is(200)));

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol);
    }
}
