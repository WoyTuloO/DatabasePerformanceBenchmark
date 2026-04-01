package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * R4 – Dostępność produktu (Total Stock)
 * Warmup simulation: 10 requests
 * Generuje losowe product_id z zakresu 1-500000
 */
public class R4ProductAvailabilitySimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("R4 Product Availability")
            .exec(session -> session.set("productId", ThreadLocalRandom.current().nextLong(1, 500001)))
            .exec(
                    http("GET /api/shop/products/{productId}/availability")
                            .get("/api/shop/products/#{productId}/availability")
                            .check(status().in(200, 404))); // 404 OK if product doesn't exist

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol)
                .assertions(
                        global().responseTime().mean().lt(1000));
    }
}

