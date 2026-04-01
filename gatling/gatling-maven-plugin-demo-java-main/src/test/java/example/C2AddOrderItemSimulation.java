package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.concurrent.ThreadLocalRandom;

/**
 * C2 – Dodaj pozycję do koszyka
 * Warmup simulation: 10 requests
 * Generates random order item data
 */
public class C2AddOrderItemSimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String endpoint = "/api/shop/order-items";

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("C2 Add Order Item")
            .exec(session -> {
                // Generate random order item data
                long orderId = ThreadLocalRandom.current().nextLong(1, 2000000);
                long productId = ThreadLocalRandom.current().nextLong(1, 100001);
                int quantity = ThreadLocalRandom.current().nextInt(1, 11);
                int unitPriceCents = ThreadLocalRandom.current().nextInt(1000, 100000);

                return session
                        .set("orderId", orderId)
                        .set("productId", productId)
                        .set("quantity", quantity)
                        .set("unitPriceCents", unitPriceCents);
            })
            .exec(
                    http("POST /api/shop/order-items")
                            .post(endpoint)
                            .body(StringBody("{\"orderId\":#{orderId},\"productId\":#{productId},\"quantity\":#{quantity},\"unitPriceCents\":#{unitPriceCents}}"))
                            .check(status().is(200)));

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol);
    }
}
