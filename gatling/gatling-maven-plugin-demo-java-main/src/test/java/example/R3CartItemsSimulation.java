package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * R3 – Zawartość koszyka
 * Warmup simulation: 10 requests
 * Generuje losowe order_id z zakresu 1-500000
 */
public class R3CartItemsSimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("R3 Cart Items")
            .exec(session -> session.set("orderId", ThreadLocalRandom.current().nextLong(1, 500001)))
            .exec(
                    http("GET /api/shop/orders/{orderId}/items")
                            .get("/api/shop/orders/#{orderId}/items")
                            .check(status().in(200, 404))); // 404 OK if order doesn't exist

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol)
                .assertions(
                        global().responseTime().mean().lt(1000));
    }
}

