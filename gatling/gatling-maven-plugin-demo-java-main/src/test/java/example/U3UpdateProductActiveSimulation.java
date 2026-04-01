package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.concurrent.ThreadLocalRandom;

/**
 * U3 – Włącz/wyłącz produkt
 * Warmup simulation: 10 requests
 */
public class U3UpdateProductActiveSimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("U3 Update Product Active")
            .exec(session -> {
                long productId = ThreadLocalRandom.current().nextLong(1, 500001);
                boolean isToBeActive = ThreadLocalRandom.current().nextBoolean();
                return session.set("productId", productId).set("status", isToBeActive);
            })
            .exec(
                    http("PATCH /api/shop/products/{productId}/active")
                            .patch("/api/shop/products/#{productId}/active")
                            .body(StringBody("{\"active\":#{status}}"))
                            .check(status().is(200)));

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol);
    }
}

