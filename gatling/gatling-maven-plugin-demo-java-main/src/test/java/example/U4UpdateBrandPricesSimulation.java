package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.concurrent.ThreadLocalRandom;

/**
 * U4 – Mnożnik ceny dla marki
 * Warmup simulation: 10 requests
 */
public class U4UpdateBrandPricesSimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("U4 Update Brand Prices")
            .exec(session -> {
                long brandId = ThreadLocalRandom.current().nextLong(1, 30);
                double multiplier = ThreadLocalRandom.current().nextDouble(0.5, 2);
                return session.set("brandId", brandId).set("multiplier", multiplier);
            })
            .exec(
                    http("PATCH /api/shop/brands/{brandId}/prices")
                            .patch("/api/shop/brands/#{brandId}/prices")
                            .body(StringBody("{\"multiplier\":#{multiplier}}"))
                            .check(status().is(200)));

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol);
    }
}

