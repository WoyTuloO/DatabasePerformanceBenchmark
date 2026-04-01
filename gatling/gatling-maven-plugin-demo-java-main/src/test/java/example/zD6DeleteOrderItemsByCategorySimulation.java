package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

/**
 * D6 – Usuń pozycje zamówień produktów danej kategorii
 * Warmup simulation: 10 requests
 */
public class zD6DeleteOrderItemsByCategorySimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String csvFile = "data/d6_delete_order_items_by_category.csv";

    // CSV format: categoryId
    private static final FeederBuilder<String> feeder = csv(csvFile);

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("D6 Delete Order Items By Category")
            .feed(feeder)
            .exec(
                    http("DELETE /api/shop/categories/{categoryId}/order-items")
                            .delete("/api/shop/categories/#{categoryId}/order-items")
                            .check(status().is(200)));
    {
        setUp(scenario.injectOpen(rampUsers(10).during(3)))
                .protocols(httpProtocol);
    }
}

