package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

/**
 * U1 – Mnożnik ceny dla kategorii
 * Warmup simulation: 10 requests
 */
public class U1UpdateCategoryPricesSimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String csvFile = "data/u1_update_category_prices.csv";

    // CSV format: categoryId,multiplier
    private static final FeederBuilder<String> feeder = csv(csvFile).random();

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("U1 Update Category Prices")
            .feed(feeder)
            .exec(
                    http("PATCH /api/shop/categories/{categoryId}/prices")
                            .patch("/api/shop/categories/#{categoryId}/prices")
                            .body(StringBody("{\"multiplier\":#{multiplier}}"))
                            .check(status().is(200)));

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol);
    }
}

