package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.concurrent.ThreadLocalRandom;

/**
 * D2 – Usuń koszyk (zamówienie NEW)
 * Warmup simulation: 10 requests
 * IDEMPOTENT with C1CreateOrderSimulation - deletes NEW orders created in C1
 * NOTE: orderId must be populated after running C1 (extract from logs or database)
 */
public class zD2DeleteCartSimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String csvFile = "data/d2_delete_cart.csv";

    // CSV format: orderId
    private static final FeederBuilder<String> feeder = csv(csvFile);

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("D2 Delete Cart")
            .exec(session -> session.set("orderId", ThreadLocalRandom.current().nextLong(1, 2000000)))
            .exec(
                    http("DELETE /api/shop/orders/{orderId}")
                            .delete("/api/shop/orders/#{orderId}")
                            .check(status().in(200, 404))); // 404 OK if already deleted or not NEW status

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol)
                .assertions(
                        global().responseTime().mean().lt(1000));
    }
}

