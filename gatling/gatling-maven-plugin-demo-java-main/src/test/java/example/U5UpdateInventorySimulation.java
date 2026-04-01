package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.concurrent.ThreadLocalRandom;

/**
 * U5 – Nadpisz stan magazynowy
 * Warmup simulation: 10 requests
 */
public class U5UpdateInventorySimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("U5 Update Inventory")
            .exec(session -> {
                long warehouseId = ThreadLocalRandom.current().nextLong(1, 4);
                long productId = ThreadLocalRandom.current().nextLong(1, 500001);
                int quantity = ThreadLocalRandom.current().nextInt(1, 100);
                return session.set("warehouseId", warehouseId)
                        .set("productId", productId)
                        .set("quantity", quantity);
            })
            .exec(
                    http("PATCH /api/shop/inventory/{warehouseId}/{productId}")
                            .patch("/api/shop/inventory/#{warehouseId}/#{productId}")
                            .body(StringBody("{\"quantity\":#{quantity}}"))
                            .check(status().is(200)));

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol);
    }
}

