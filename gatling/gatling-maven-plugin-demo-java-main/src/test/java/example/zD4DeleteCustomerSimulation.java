package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.concurrent.ThreadLocalRandom;

/**
 * D4 – Usuń klienta
 * Warmup simulation: 10 requests
 * IDEMPOTENT with C3CreateCustomerSimulation - deletes customers created in C3
 * NOTE: Run extract-customer-ids-from-db.ps1 after C3 to populate d4_delete_customer.csv
 */
public class zD4DeleteCustomerSimulation extends Simulation {

    private static final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");

    private static final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .shareConnections();

    private static final ScenarioBuilder scenario = scenario("D4 Delete Customer")
            .exec(session -> session.set("customerId", ThreadLocalRandom.current().nextLong(1, 1000000)))
            .exec(
                    http("DELETE /api/shop/customers/{customerId}")
                            .delete("/api/shop/customers/#{customerId}")
                            .check(status().in(200, 404))); // 404 OK if already deleted

    {
        setUp(scenario.injectOpen(rampUsers(10).during(10)))
                .protocols(httpProtocol)
                .assertions(
                        global().responseTime().mean().lt(1000));
    }
}

