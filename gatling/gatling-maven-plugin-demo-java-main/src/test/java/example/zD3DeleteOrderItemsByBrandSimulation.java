package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.concurrent.ThreadLocalRandom;

/**
 * D3 – Usuń pozycje zamówień produktów danej marki
 * Warmup simulation: 10 requests
 */
public class zD3DeleteOrderItemsByBrandSimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("D3 Delete Order Items By Brand")
            .exec(session -> session.set("brandId", ThreadLocalRandom.current().nextLong(1, 30)))
            .exec(
                    http("DELETE /api/shop/brands/{brandId}/order-items")
                            .delete("/api/shop/brands/#{brandId}/order-items")
                            .check(status().is(200)));

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol);
    }
}

